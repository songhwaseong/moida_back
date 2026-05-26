package com.moida.controller;

import com.moida.common.response.AdminWalletTransactionResponse;
import com.moida.common.response.ApiResponse;
import com.moida.common.response.WalletResponse;
import com.moida.domain.wallet.WalletService;
import com.moida.domain.wallet.WalletTransaction;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * 내부 입출금 확인 처리를 위한 관리자 지갑 API를 제공합니다.
 */
@RestController
@RequestMapping("/api/admin/wallet")
@RequiredArgsConstructor
public class AdminWalletController {

    private final WalletService walletService;

    /**
     * 관리자 화면에서 입출금 요청 목록을 조회합니다.
     */
    @GetMapping("/transactions")
    public ResponseEntity<ApiResponse<List<AdminWalletTransactionResponse>>> getTransactions(
            @RequestParam(required = false) WalletTransaction.TransactionType type,
            @RequestParam(defaultValue = "PENDING") WalletTransaction.TransactionStatus status,
            @RequestParam(defaultValue = "100") int size
    ) {
        return ResponseEntity.ok(ApiResponse.success(walletService.getAdminTransactions(type, status, size)));
    }

    /**
     * 가상계좌 송금 확인이 끝난 입금 요청을 완료 처리합니다.
     */
    @PostMapping("/deposits/{transactionId}/confirm")
    public ResponseEntity<ApiResponse<WalletResponse>> confirmDeposit(
            @PathVariable Long transactionId
    ) {
        return ResponseEntity.ok(ApiResponse.success(walletService.confirmDeposit(transactionId)));
    }

    /**
     * 아직 처리되지 않은 입금 요청을 취소합니다.
     */
    @PostMapping("/deposits/{transactionId}/cancel")
    public ResponseEntity<ApiResponse<WalletResponse>> cancelDeposit(
            @PathVariable Long transactionId
    ) {
        return ResponseEntity.ok(ApiResponse.success(walletService.cancelDeposit(transactionId)));
    }

    /**
     * 출금 처리가 끝난 출금 요청을 완료 처리합니다.
     */
    @PostMapping("/withdrawals/{transactionId}/confirm")
    public ResponseEntity<ApiResponse<WalletResponse>> confirmWithdrawal(
            @PathVariable Long transactionId
    ) {
        return ResponseEntity.ok(ApiResponse.success(walletService.confirmWithdrawal(transactionId)));
    }

    /**
     * 아직 처리되지 않은 출금 요청을 취소합니다.
     */
    @PostMapping("/withdrawals/{transactionId}/cancel")
    public ResponseEntity<ApiResponse<WalletResponse>> cancelWithdrawal(
            @PathVariable Long transactionId
    ) {
        return ResponseEntity.ok(ApiResponse.success(walletService.cancelWithdrawal(transactionId)));
    }
}
