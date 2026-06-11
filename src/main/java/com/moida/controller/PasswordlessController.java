package com.moida.controller;

import com.moida.common.request.PasswordlessEmailRequest;
import com.moida.common.request.PasswordlessRequestTokenRequest;
import com.moida.common.request.PasswordlessWithdrawRequest;
import com.moida.common.response.ApiResponse;
import com.moida.common.response.PasswordlessLoginCompleteResponse;
import com.moida.common.response.PasswordlessLoginStartResponse;
import com.moida.common.response.PasswordlessRegistrationStartResponse;
import com.moida.common.response.PasswordlessStatusResponse;
import com.moida.common.util.ClientIpResolver;
import com.moida.domain.passwordless.PasswordlessService;
import com.moida.security.CustomUserDetails;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api")
public class PasswordlessController {

    private final PasswordlessService passwordlessService;

    @PostMapping("/auth/passwordless/login/start")
    public ResponseEntity<ApiResponse<PasswordlessLoginStartResponse>> startLogin(
            @Valid @RequestBody PasswordlessEmailRequest request,
            HttpServletRequest servletRequest
    ) {
        String clientIp = ClientIpResolver.resolve(servletRequest);
        return ResponseEntity.ok(ApiResponse.success(passwordlessService.startLogin(request.email(), clientIp)));
    }

    @PostMapping("/auth/passwordless/login/complete")
    public ResponseEntity<ApiResponse<PasswordlessLoginCompleteResponse>> completeLogin(
            @Valid @RequestBody PasswordlessRequestTokenRequest request
    ) {
        return ResponseEntity.ok(ApiResponse.success(passwordlessService.completeLogin(request.requestToken())));
    }

    @PostMapping("/auth/passwordless/login/cancel")
    public ResponseEntity<ApiResponse<Void>> cancelLogin(
            @Valid @RequestBody PasswordlessRequestTokenRequest request
    ) {
        passwordlessService.cancelLogin(request.requestToken());
        return ResponseEntity.ok(ApiResponse.success(null));
    }

    @GetMapping("/members/me/passwordless/status")
    public ResponseEntity<ApiResponse<PasswordlessStatusResponse>> getStatus(
            @AuthenticationPrincipal CustomUserDetails userDetails
    ) {
        boolean registered = passwordlessService.isRegistered(userDetails.getMemberId());
        return ResponseEntity.ok(ApiResponse.success(new PasswordlessStatusResponse(registered)));
    }

    @PostMapping("/members/me/passwordless/registration/start")
    public ResponseEntity<ApiResponse<PasswordlessRegistrationStartResponse>> startRegistration(
            @AuthenticationPrincipal CustomUserDetails userDetails
    ) {
        return ResponseEntity.ok(ApiResponse.success(passwordlessService.startRegistration(userDetails.getMemberId())));
    }

    @PostMapping("/members/me/passwordless/registration/confirm")
    public ResponseEntity<ApiResponse<PasswordlessStatusResponse>> confirmRegistration(
            @AuthenticationPrincipal CustomUserDetails userDetails
    ) {
        boolean registered = passwordlessService.confirmRegistration(userDetails.getMemberId());
        return ResponseEntity.ok(ApiResponse.success(new PasswordlessStatusResponse(registered)));
    }

    @DeleteMapping("/members/me/passwordless")
    public ResponseEntity<ApiResponse<Void>> withdraw(
            @AuthenticationPrincipal CustomUserDetails userDetails
    ) {
        passwordlessService.withdraw(userDetails.getMemberId());
        return ResponseEntity.ok(ApiResponse.success(null));
    }

    /**
     * 평상시 해지 — 이메일+비밀번호 확인.
     * Passwordless 등록 후에는 /auth/login 이 차단되므로, 로그인 세션 없이 해지할 수 있도록
     * 공개 엔드포인트(/api/auth/**)로 두고 비밀번호를 내부에서 직접 검증한다.
     */
    @PostMapping("/auth/passwordless/withdraw")
    public ResponseEntity<ApiResponse<Void>> withdrawByPassword(
            @Valid @RequestBody PasswordlessWithdrawRequest request
    ) {
        passwordlessService.withdrawByPassword(request.email(), request.password());
        return ResponseEntity.ok(ApiResponse.success(null));
    }

    /**
     * 분실 복구 해지 — 이메일 인증 코드 확인.
     * 휴대폰/앱 분실 시 비밀번호도 Passwordless 인증도 못 쓰는 락아웃을 방지한다.
     * 사전에 /api/auth/email/send-code, verify-code 로 이메일 인증을 마쳐야 한다.
     */
    @PostMapping("/auth/passwordless/withdraw-by-email")
    public ResponseEntity<ApiResponse<Void>> withdrawByEmail(
            @Valid @RequestBody PasswordlessEmailRequest request
    ) {
        passwordlessService.withdrawByEmail(request.email());
        return ResponseEntity.ok(ApiResponse.success(null));
    }
}
