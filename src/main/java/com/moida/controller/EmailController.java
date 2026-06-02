package com.moida.controller;

import com.moida.common.exception.BusinessException;
import com.moida.common.exception.ErrorCode;
import com.moida.common.request.ResetPasswordRequest;
import com.moida.common.request.SendEmailCodeRequest;
import com.moida.common.request.VerifyEmailCodeRequest;
import com.moida.common.response.ApiResponse;
import com.moida.domain.auth.EmailVerificationService;
import com.moida.domain.member.MemberService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/auth/email")
@RequiredArgsConstructor
public class EmailController {

    private final EmailVerificationService emailVerificationService;
    private final MemberService memberService;

    @PostMapping("/send-code")
    public ResponseEntity<ApiResponse<String>> sendCode(
            @Valid @RequestBody SendEmailCodeRequest request) {
        emailVerificationService.sendCode(request.getEmail());
        return ResponseEntity.ok(ApiResponse.success("인증 코드가 발송됐습니다."));
    }

    @PostMapping("/verify-code")
    public ResponseEntity<ApiResponse<String>> verifyCode(
            @Valid @RequestBody VerifyEmailCodeRequest request) {
        emailVerificationService.verifyCode(request.getEmail(), request.getCode());
        return ResponseEntity.ok(ApiResponse.success("인증이 완료됐습니다."));
    }

    @PostMapping("/reset-password")
    public ResponseEntity<ApiResponse<String>> resetPassword(
            @Valid @RequestBody ResetPasswordRequest request) {
        if (!emailVerificationService.isVerified(request.getEmail())) {
            throw new BusinessException(ErrorCode.EMAIL_VERIFICATION_NOT_FOUND, "이메일 인증을 먼저 완료해주세요.");
        }
        memberService.resetPassword(request.getEmail(), request.getNewPassword());
        return ResponseEntity.ok(ApiResponse.success("비밀번호가 변경됐습니다."));
    }
}
