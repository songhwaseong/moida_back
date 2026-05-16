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
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
public class MemberController {
    private final MemberService memberService;
    private final AuthenticationManager authenticationManager;
    private final JwtTokenProvider jwtTokenProvider;
    //

    @PostMapping("/login")
    public ResponseEntity<ApiResponse<LoginResponse>> login(@RequestBody LoginRequest request) {

        Authentication authentication = authenticationManager.authenticate(
                new UsernamePasswordAuthenticationToken(request.getEmail(), request.getPassword())
        );

        Member member = memberService.findByEmail(request.getEmail())
                .orElseThrow(() -> new BusinessException(ErrorCode.MEMBER_NOT_FOUND));

        String token = jwtTokenProvider.createAccessToken(
                member.getId(), member.getEmail(), member.getRole()
        );

        return ResponseEntity.ok(ApiResponse.success(
                new LoginResponse(token, member)
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
}
