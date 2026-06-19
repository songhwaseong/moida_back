package com.moida.controller;

import com.moida.common.exception.BusinessException;
import com.moida.common.exception.ErrorCode;
import com.moida.common.request.CompleteSocialProfileRequest;
import com.moida.common.request.LoginRequest;
import com.moida.common.request.SignupRequest;
import com.moida.common.response.ApiResponse;
import com.moida.common.response.GoogleUserResponse;
import com.moida.common.response.KakaoUserResponse;
import com.moida.common.response.LoginResponse;
import com.moida.common.response.NaverUserResponse;
import com.moida.common.response.RefreshTokenResponse;
import com.moida.common.util.ClientIpResolver;
import com.moida.domain.audit.AdminLoginLogService;
import com.moida.domain.auth.PhoneVerificationService;
import com.moida.domain.auth.AuthCookieService;
import com.moida.domain.auth.OAuthFlowService;
import com.moida.domain.auth.RefreshTokenService;
import com.moida.domain.auth.VerificationPurpose;
import com.moida.domain.member.Member;
import com.moida.domain.member.MemberService;
import com.moida.domain.member.SocialLoginService;
import com.moida.security.CustomUserDetails;
import com.moida.security.JwtTokenProvider;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.AuthenticationException;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.transaction.annotation.Transactional;

import java.util.Map;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
public class MemberController {
    private final MemberService memberService;
    private final AuthenticationManager authenticationManager;
    private final JwtTokenProvider jwtTokenProvider;
    private final SocialLoginService socialLoginService;
    private final PhoneVerificationService phoneVerificationService;
    private final AdminLoginLogService adminLoginLogService;
    private final RefreshTokenService refreshTokenService;
    private final AuthCookieService authCookieService;
    private final OAuthFlowService oauthFlowService;

    @PostMapping("/login")
    public ResponseEntity<ApiResponse<LoginResponse>> login(
            @RequestBody LoginRequest request,
            HttpServletRequest httpRequest,
            HttpServletResponse httpResponse) {

        String ip = ClientIpResolver.resolve(httpRequest);
        String userAgent = httpRequest.getHeader("User-Agent");

        // brute-force 방어: 반복 실패로 일시 잠긴 계정은 비밀번호 검증조차 시도하지 않는다.
        memberService.assertLoginNotLocked(request.getEmail());

        try {
            authenticationManager.authenticate(
                    new UsernamePasswordAuthenticationToken(request.getEmail(), request.getPassword())
            );
        } catch (AuthenticationException e) {
            memberService.recordLoginFailure(request.getEmail());
            adminLoginLogService.recordFailure(request.getEmail(), ip, userAgent);
            throw e;
        }

        Member member = memberService.findByEmail(request.getEmail())
                .orElseThrow(() -> new BusinessException(ErrorCode.MEMBER_NOT_FOUND));
        if (!member.isActive()) {
            throw new BusinessException(ErrorCode.MEMBER_ACCOUNT_INACTIVE);
        }

        // 비밀번호 검증 성공 → 누적 실패 카운트와 잠금 상태를 초기화한다.
        memberService.recordLoginSuccess(member.getEmail());

        // Passwordless 차단 검사를 성공 로그 기록보다 먼저 수행한다.
        LoginResponse loginResponse = createLoginResponse(member, false);
        authCookieService.issue(httpResponse, loginResponse.getRefreshToken());
        adminLoginLogService.recordSuccess(member, ip, userAgent);

        return ResponseEntity.ok(ApiResponse.success(loginResponse));
    }

    @PostMapping("/refresh")
    public ResponseEntity<ApiResponse<RefreshTokenResponse>> refresh(
            HttpServletRequest request,
            HttpServletResponse response
    ) {
        String refreshToken = authCookieService.requireRefreshToken(request);
        // 서명/만료 + "refresh 타입"인지 검증 (access 토큰을 refresh 로 오용하는 것을 차단)
        if (!jwtTokenProvider.validateToken(refreshToken)
                || !jwtTokenProvider.isTokenType(refreshToken, JwtTokenProvider.TYPE_REFRESH)) {
            throw new BusinessException(ErrorCode.INVALID_TOKEN, "유효하지 않은 refresh 토큰입니다.");
        }

        Authentication authentication = jwtTokenProvider.getAuthentication(refreshToken);
        CustomUserDetails principal = (CustomUserDetails) authentication.getPrincipal();

        // 서버에 저장된 토큰과 일치하는지 확인 (로그아웃됐거나 이미 회전된 토큰은 거부)
        refreshTokenService.validate(principal.getMemberId(), refreshToken);

        Member member = memberService.findByEmail(principal.getEmail())
                .orElseThrow(() -> new BusinessException(ErrorCode.MEMBER_NOT_FOUND));

        if (!member.isActive()) {
            throw new BusinessException(ErrorCode.MEMBER_ACCOUNT_INACTIVE);
        }
        if (member.currentTokenVersion() != jwtTokenProvider.getTokenVersion(refreshToken)) {
            throw new BusinessException(ErrorCode.INVALID_TOKEN);
        }

        String newAccessToken = jwtTokenProvider.createAccessToken(
                member.getId(), member.getEmail(), member.getRole(), member.currentTokenVersion());
        String newRefreshToken = jwtTokenProvider.createRefreshToken(
                member.getId(), member.getEmail(), member.getRole(), member.currentTokenVersion());
        // 회전: 새 refresh 토큰을 저장해 방금 사용한 토큰을 무효화한다.
        refreshTokenService.store(member.getId(), newRefreshToken, jwtTokenProvider.getRefreshTokenValidity());
        authCookieService.issue(response, newRefreshToken);

        return ResponseEntity.ok(ApiResponse.success(
                new RefreshTokenResponse(newAccessToken)
        ));
    }

    @PostMapping("/logout")
    public ResponseEntity<ApiResponse<String>> logout(
            Authentication authentication,
            HttpServletRequest request,
            HttpServletResponse response) {
        // access 토큰이 유효하면 필터가 인증을 세팅한다. 해당 회원의 저장된 refresh 토큰을 폐기한다.
        if (authentication != null && authentication.getPrincipal() instanceof CustomUserDetails principal) {
            refreshTokenService.revoke(principal.getMemberId());
        } else {
            String refreshToken = authCookieService.getRefreshToken(request);
            if (refreshToken != null && !refreshToken.isBlank()) {
                authCookieService.validateCsrf(request);
                if (jwtTokenProvider.validateToken(refreshToken)
                        && jwtTokenProvider.isTokenType(refreshToken, JwtTokenProvider.TYPE_REFRESH)) {
                    CustomUserDetails principal = (CustomUserDetails) jwtTokenProvider
                            .getAuthentication(refreshToken).getPrincipal();
                    refreshTokenService.revoke(principal.getMemberId());
                }
            }
        }
        authCookieService.clear(response);
        return ResponseEntity.ok(ApiResponse.success("로그아웃 되었습니다."));
    }

    @PostMapping("/signup")
    @Transactional
    public ResponseEntity<ApiResponse<String>> signup(
            @Valid @RequestBody SignupRequest request,
            BindingResult bindingResult) {

        if (bindingResult.hasErrors()) {
            String errorMessage = bindingResult.getFieldErrors().stream()
                    .map(e -> e.getField() + ": " + e.getDefaultMessage())
                    .collect(Collectors.joining(", "));
            throw new BusinessException(ErrorCode.INVALID_INPUT, errorMessage);
        }

        if (!phoneVerificationService.consumeVerified(request.getPhone(), VerificationPurpose.SIGNUP)) {
            throw new BusinessException(ErrorCode.PHONE_NOT_VERIFIED);
        }

        memberService.signup(request);

        return ResponseEntity.ok(ApiResponse.success("회원 가입 성공"));
    }

    @GetMapping("/check-nickname")
    public ResponseEntity<ApiResponse<Long>> checkNickname(@RequestParam String value) {
        long count = memberService.countByNickname(value);
        return ResponseEntity.ok(ApiResponse.success(count));
    }

    @GetMapping("/check-email")
    public ResponseEntity<ApiResponse<Boolean>> checkEmail(@RequestParam String value) {
        boolean available = !memberService.existsByEmail(value);
        return ResponseEntity.ok(ApiResponse.success(available));
    }

    @PostMapping("/kakaoLogin")
    public ResponseEntity<ApiResponse<LoginResponse>> kakaoLogin(
            @RequestBody Map<String, String> params,
            HttpServletRequest request,
            HttpServletResponse response) {
        oauthFlowService.consume("KAKAO", params.get("state"), request, response);
        String accessToken = socialLoginService.getKkoAccessToken(params.get("code"));
        KakaoUserResponse userInfo = socialLoginService.getKakaoUserInfo(accessToken);

        SocialLoginService.SocialLoginResult result = socialLoginService.findOrRegisterSocialMember(
                userInfo.kakaoAccount().email(),
                userInfo.kakaoAccount().profile().nickname(),
                "KAKAO",
                String.valueOf(userInfo.id()),
                Boolean.TRUE.equals(userInfo.kakaoAccount().emailValid())
                        && Boolean.TRUE.equals(userInfo.kakaoAccount().emailVerified())
        );

        LoginResponse login = createLoginResponse(result.member(), result.newUser());
        authCookieService.issue(response, login.getRefreshToken());
        return ResponseEntity.ok(ApiResponse.success(login));
    }

    @PostMapping("/naverLogin")
    public ResponseEntity<ApiResponse<LoginResponse>> naverLogin(
            @RequestBody Map<String, String> params,
            HttpServletRequest request,
            HttpServletResponse response) {
        oauthFlowService.consume("NAVER", params.get("state"), request, response);
        String accessToken = socialLoginService.getNavAccessToken(params.get("code"), params.get("state"));
        NaverUserResponse userInfo = socialLoginService.getNaverUserInfo(accessToken);

        SocialLoginService.SocialLoginResult result = socialLoginService.findOrRegisterSocialMember(
                userInfo.response().email(),
                userInfo.response().name(),
                "NAVER",
                userInfo.response().id(),
                "00".equals(userInfo.resultcode()) && userInfo.response().email() != null
        );

        LoginResponse login = createLoginResponse(result.member(), result.newUser());
        authCookieService.issue(response, login.getRefreshToken());
        return ResponseEntity.ok(ApiResponse.success(login));
    }

    @PostMapping("/googleLogin")
    public ResponseEntity<ApiResponse<LoginResponse>> googleLogin(
            @RequestBody Map<String, String> params,
            HttpServletRequest request,
            HttpServletResponse response) {
        OAuthFlowService.OAuthContext flow = oauthFlowService.consume(
                "GOOGLE", params.get("state"), request, response);
        String accessToken = socialLoginService.getGoogleAccessToken(
                params.get("code"), flow.codeVerifier());
        GoogleUserResponse userInfo = socialLoginService.getGoogleUserInfo(accessToken);

        SocialLoginService.SocialLoginResult result = socialLoginService.findOrRegisterSocialMember(
                userInfo.email(),
                userInfo.name(),
                "GOOGLE",
                userInfo.id(),
                userInfo.verifiedEmail()
        );

        LoginResponse login = createLoginResponse(result.member(), result.newUser());
        authCookieService.issue(response, login.getRefreshToken());
        return ResponseEntity.ok(ApiResponse.success(login));
    }

    @PutMapping("/complete-social-profile")
    @Transactional
    public ResponseEntity<ApiResponse<LoginResponse>> completeSocialProfile(
            @RequestBody CompleteSocialProfileRequest request,
            Authentication authentication,
            HttpServletResponse response) {
        if (!phoneVerificationService.consumeVerified(
                request.getPhone(), VerificationPurpose.COMPLETE_SOCIAL_PROFILE)) {
            throw new BusinessException(ErrorCode.PHONE_NOT_VERIFIED);
        }

        Member member = memberService.completeSocialProfile(authentication.getName(), request.getNickname(), request.getPhone());
        LoginResponse login = createLoginResponse(member, false);
        authCookieService.issue(response, login.getRefreshToken());
        return ResponseEntity.ok(ApiResponse.success(login, "프로필 등록 완료"));
    }

    private LoginResponse createLoginResponse(Member member, boolean newUser) {
        if (!member.isActive()) {
            throw new BusinessException(ErrorCode.MEMBER_ACCOUNT_INACTIVE);
        }
        // Passwordless 등록 회원은 일반 로그인(비밀번호/소셜)을 차단하고 Passwordless 로그인만 허용한다.
        if (member.isPasswordlessEnabled()) {
            throw new BusinessException(ErrorCode.PASSWORDLESS_LOGIN_REQUIRED);
        }
        String accessToken = jwtTokenProvider.createAccessToken(
                member.getId(), member.getEmail(), member.getRole(), member.currentTokenVersion());
        String refreshToken = jwtTokenProvider.createRefreshToken(
                member.getId(), member.getEmail(), member.getRole(), member.currentTokenVersion());
        refreshTokenService.store(member.getId(), refreshToken, jwtTokenProvider.getRefreshTokenValidity());
        return new LoginResponse(accessToken, refreshToken, member, newUser);
    }
}
