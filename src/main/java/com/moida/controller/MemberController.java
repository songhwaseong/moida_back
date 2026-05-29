package com.moida.controller;

import com.moida.common.exception.BusinessException;
import com.moida.common.exception.ErrorCode;
import com.moida.common.request.LoginRequest;
import com.moida.common.request.SignupRequest;
import com.moida.common.response.ApiResponse;
import com.moida.common.response.LoginResponse;
import com.moida.domain.member.Member;
import com.moida.domain.member.MemberService;
import com.moida.security.JwtTokenProvider;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import com.moida.common.response.*;                 // 소셜 로그인 response
import com.moida.domain.member.SocialLoginService;  // 소셜 로그인 서비스
import java.util.Map;                               // 소셜 로그인 요청 파라미터 받을 때
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.security.core.Authentication;
import com.moida.common.request.CompleteSocialProfileRequest;

import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
public class MemberController {
    private final MemberService memberService;
    private final AuthenticationManager authenticationManager;
    private final JwtTokenProvider jwtTokenProvider;
    private final SocialLoginService socialLoginService; // 소셜 로그인 처리 서비스
    //

    @PostMapping("/login")
    public ResponseEntity<ApiResponse<LoginResponse>> login(@RequestBody LoginRequest request) {

        Authentication authentication = authenticationManager.authenticate(
                new UsernamePasswordAuthenticationToken(request.getEmail(), request.getPassword())
        );

        Member member = memberService.findByEmail(request.getEmail())
                .orElseThrow(() -> new BusinessException(ErrorCode.MEMBER_NOT_FOUND));
        if (!member.isActive()) {
            throw new BusinessException(ErrorCode.MEMBER_ACCOUNT_INACTIVE);
        }

        String token = jwtTokenProvider.createAccessToken(
                member.getId(), member.getEmail(), member.getRole()
        );

        return ResponseEntity.ok(ApiResponse.success(
                new LoginResponse(token, member, false)
        ));

    }
    @PostMapping("/signup")
    public ResponseEntity<ApiResponse<String>> signup(
            @Valid
            @RequestBody SignupRequest request,                // ← Member → SignupRequest
            BindingResult bindingResult) {

        if(bindingResult.hasErrors()){
            String errorMessage = bindingResult.getFieldErrors().stream()
                    .map(e -> e.getField() + ": " + e.getDefaultMessage())
                    .collect(Collectors.joining(", "));
            throw new BusinessException(ErrorCode.INVALID_INPUT, errorMessage);
        }

        if(memberService.existsByEmail(request.getEmail())){
            throw new BusinessException(ErrorCode.DUPLICATE_EMAIL);
        }

        memberService.signup(request);

        return ResponseEntity.ok(ApiResponse.success("회원 가입 성공"));
    }
    @GetMapping("/check-nickname")
    public ResponseEntity<ApiResponse<Long>> checkNickname(@RequestParam String value) {
        long count = memberService.countByNickname(value);
        return ResponseEntity.ok(ApiResponse.success(count));
    }
    //코드 → 토큰 → 사용자 정보 → 회원 조회/가입 → JWT 발급
    @PostMapping("/kakaoLogin")
    public ResponseEntity<ApiResponse<LoginResponse>> kakaoLogin(@RequestBody Map<String, String> params) {
        // 프론트에서 받은 인가 코드로 카카오 액세스 토큰 교환
        String accessToken = socialLoginService.getKkoAccessToken(params.get("code"));
        // 액세스 토큰으로 카카오 사용자 정보 조회
        KakaoUserResponse userInfo = socialLoginService.getKakaoUserInfo(accessToken);
        String email = userInfo.kakaoAccount().email();
        boolean isNewUser = !memberService.existsByEmail(email);
        // 이메일로 기존 회원 조회, 없으면 자동 가입
        // userInfo.kakaoAccount().email()로 접근
        // userInfo.kakaoAccount().profile().nickname()로 접근
        Member member = socialLoginService.findOrRegisterSocialMember(
                userInfo.kakaoAccount().email(),
                userInfo.kakaoAccount().profile().nickname(),
                "KAKAO"
        );
        // 기존 로그인과 동일하게 JWT 토큰 발급
        String token = jwtTokenProvider.createAccessToken(member.getId(), member.getEmail(), member.getRole());
        return ResponseEntity.ok(ApiResponse.success(new LoginResponse(token, member, isNewUser)));
    }

    @PostMapping("/naverLogin")
    public ResponseEntity<ApiResponse<LoginResponse>> naverLogin(@RequestBody Map<String, String> params) {
        // 프론트에서 받은 인가 코드 + state로 네이버 액세스 토큰 교환
        String accessToken = socialLoginService.getNavAccessToken(params.get("code"), params.get("state"));
        // 액세스 토큰으로 네이버 사용자 정보 조회
        NaverUserResponse userInfo = socialLoginService.getNaverUserInfo(accessToken);
        String email = userInfo.response().email();
        boolean isNewUser = !memberService.existsByEmail(email);
        // 이메일로 기존 회원 조회, 없으면 자동 가입
        // userInfo.response()로 접근
        Member member = socialLoginService.findOrRegisterSocialMember(
                userInfo.response().email(),
                userInfo.response().name(),
                "NAVER"
        );
        String token = jwtTokenProvider.createAccessToken(member.getId(), member.getEmail(), member.getRole());
        return ResponseEntity.ok(ApiResponse.success(new LoginResponse(token, member, isNewUser)));
    }

    @PostMapping("/googleLogin")
    public ResponseEntity<ApiResponse<LoginResponse>> googleLogin(@RequestBody Map<String, String> params) {
        // 프론트에서 받은 인가 코드로 구글 액세스 토큰 교환
        String accessToken = socialLoginService.getGoogleAccessToken(params.get("code"));
        // 액세스 토큰으로 구글 사용자 정보 조회
        GoogleUserResponse userInfo = socialLoginService.getGoogleUserInfo(accessToken);
        String email = userInfo.email();
        boolean isNewUser = !memberService.existsByEmail(email);
        // 이메일로 기존 회원 조회, 없으면 자동 가입
        //userInfo로 접근
        Member member = socialLoginService.findOrRegisterSocialMember(
                userInfo.email(),
                userInfo.name(),
                "GOOGLE"
        );
        String token = jwtTokenProvider.createAccessToken(member.getId(), member.getEmail(), member.getRole());
        return ResponseEntity.ok(ApiResponse.success(new LoginResponse(token, member, isNewUser)));
    }
    @PutMapping("/complete-social-profile")
    public ResponseEntity<ApiResponse<String>> completeSocialProfile(
            @RequestBody CompleteSocialProfileRequest request,
            Authentication authentication) {
        memberService.completeSocialProfile(authentication.getName(), request.getNickname(), request.getPhone());
        return ResponseEntity.ok(ApiResponse.success("프로필 등록 완료"));
    }

}
