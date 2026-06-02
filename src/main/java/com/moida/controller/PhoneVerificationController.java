package com.moida.controller;

import com.moida.common.request.PhoneCodeSendRequest;
import com.moida.common.request.PhoneCodeVerifyRequest;
import com.moida.common.response.ApiResponse;
import com.moida.domain.auth.PhoneVerificationService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * 휴대폰 인증 — 인증번호 발송/검증.
 * /api/auth/** 는 SecurityConfig 에서 permitAll 이므로 비로그인(회원가입 단계)에서 호출 가능.
 */
@RestController
@RequestMapping("/api/auth/phone")
@RequiredArgsConstructor
public class PhoneVerificationController {

    private final PhoneVerificationService phoneVerificationService;

    @PostMapping("/send")
    public ResponseEntity<ApiResponse<Void>> send(@Valid @RequestBody PhoneCodeSendRequest request) {
        phoneVerificationService.sendCode(request.getPhone());
        return ResponseEntity.ok(ApiResponse.success(null, "인증번호를 전송했습니다."));
    }

    @PostMapping("/verify")
    public ResponseEntity<ApiResponse<Void>> verify(@Valid @RequestBody PhoneCodeVerifyRequest request) {
        phoneVerificationService.verifyCode(request.getPhone(), request.getCode());
        return ResponseEntity.ok(ApiResponse.success(null, "휴대폰 인증이 완료되었습니다."));
    }
}
