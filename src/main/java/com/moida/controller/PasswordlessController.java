package com.moida.controller;

import com.moida.common.request.PasswordlessEmailRequest;
import com.moida.common.request.PasswordlessRequestTokenRequest;
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
        boolean registered = passwordlessService.isRegistered(userDetails.getMemberId());
        return ResponseEntity.ok(ApiResponse.success(new PasswordlessStatusResponse(registered)));
    }

    @DeleteMapping("/members/me/passwordless")
    public ResponseEntity<ApiResponse<Void>> withdraw(
            @AuthenticationPrincipal CustomUserDetails userDetails
    ) {
        passwordlessService.withdraw(userDetails.getMemberId());
        return ResponseEntity.ok(ApiResponse.success(null));
    }
}
