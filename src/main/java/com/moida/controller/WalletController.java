package com.moida.controller;

import com.moida.common.request.BankAccountRequest;
import com.moida.common.request.WalletAmountRequest;
import com.moida.common.response.ApiResponse;
import com.moida.common.response.WalletResponse;
import com.moida.domain.wallet.WalletService;
import com.moida.security.CustomUserDetails;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * 지갑 및 출금 계좌 관련 API를 제공하는 컨트롤러
 */
@RestController
@RequestMapping("/api/wallet")
@RequiredArgsConstructor
public class WalletController {

    private final WalletService walletService;

    /**
     * 회원의 지갑 정보(잔액, 계좌, 거래 내역)를 조회합니다.
     */
    @GetMapping
    public ResponseEntity<ApiResponse<WalletResponse>> getWallet(
            @AuthenticationPrincipal CustomUserDetails userDetails
    ) {
        return ResponseEntity.ok(ApiResponse.success(walletService.getWallet(userDetails.getMemberId())));
    }

    /**
     * 회원의 출금 계좌 정보를 등록하거나 수정(변경)합니다.
     */
    @PutMapping("/account")
    public ResponseEntity<ApiResponse<WalletResponse>> saveAccount(
            @AuthenticationPrincipal CustomUserDetails userDetails,
            @Valid @RequestBody BankAccountRequest request
    ) {
        return ResponseEntity.ok(ApiResponse.success(walletService.saveAccount(userDetails.getMemberId(), request)));
    }

    /**
     * 회원의 등록된 출금 계좌 정보를 삭제합니다.
     */
    @DeleteMapping("/account")
    public ResponseEntity<ApiResponse<WalletResponse>> deleteAccount(
            @AuthenticationPrincipal CustomUserDetails userDetails
    ) {
        return ResponseEntity.ok(ApiResponse.success(walletService.deleteAccount(userDetails.getMemberId())));
    }

    /**
     * 지갑 잔액 충전을 요청(가상계좌 입금 대기 생성)합니다.
     */
    @PostMapping("/deposits")
    public ResponseEntity<ApiResponse<WalletResponse>> deposit(
            @AuthenticationPrincipal CustomUserDetails userDetails,
            @Valid @RequestBody WalletAmountRequest request
    ) {
        return ResponseEntity.ok(ApiResponse.success(walletService.deposit(userDetails.getMemberId(), request)));
    }

    /**
     * 지갑 잔액 출금을 신청합니다.
     */
    @PostMapping("/withdrawals")
    public ResponseEntity<ApiResponse<WalletResponse>> withdraw(
            @AuthenticationPrincipal CustomUserDetails userDetails,
            @Valid @RequestBody WalletAmountRequest request
    ) {
        return ResponseEntity.ok(ApiResponse.success(walletService.withdraw(userDetails.getMemberId(), request)));
    }
}
