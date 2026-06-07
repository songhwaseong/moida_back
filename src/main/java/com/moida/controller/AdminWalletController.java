package com.moida.controller;

import com.moida.common.exception.BusinessException;
import com.moida.common.exception.ErrorCode;
import com.moida.common.request.AdminReasonRequest;
import com.moida.common.response.AdminWalletTransactionResponse;
import com.moida.common.response.ApiResponse;
import com.moida.common.response.WalletResponse;
import com.moida.domain.audit.AdminActionLogService;
import com.moida.domain.wallet.WalletService;
import com.moida.domain.wallet.WalletTransaction;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
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
    private final AdminActionLogService adminActionLogService;

    /**
     * 관리자 화면에서 입출금 요청 목록을 조회합니다.
     */
    @GetMapping("/transactions")
    public ResponseEntity<ApiResponse<List<AdminWalletTransactionResponse>>> getTransactions(
            @RequestParam(required = false) WalletTransaction.TransactionType type,
            @RequestParam(defaultValue = "PENDING") WalletTransaction.TransactionStatus status,
            @RequestParam(defaultValue = "100") int size
    ) {
        adminActionLogService.recordView(
                "ADMIN_WALLET_TRANSACTION_VIEW",
                "WALLET_TRANSACTION",
                adminActionLogService.fields("type", type, "status", status, "size", size)
        );
        return ResponseEntity.ok(ApiResponse.success(walletService.getAdminTransactions(type, status, size)));
    }

    /**
     * 가상계좌 송금 확인이 끝난 입금 요청을 완료 처리합니다.
     */
    @PostMapping("/deposits/{transactionId}/confirm")
    public ResponseEntity<ApiResponse<WalletResponse>> confirmDeposit(
            @PathVariable Long transactionId,
            @RequestBody AdminReasonRequest request
    ) {
        String reason = requireReason(request);
        try {
            return ResponseEntity.ok(ApiResponse.success(walletService.confirmDeposit(transactionId, reason)));
        } catch (RuntimeException e) {
            recordWalletFailure("WALLET_DEPOSIT_CONFIRM_FAILED", transactionId, reason, e);
            throw e;
        }
    }

    /**
     * 아직 처리되지 않은 입금 요청을 취소합니다.
     */
    @PostMapping("/deposits/{transactionId}/cancel")
    public ResponseEntity<ApiResponse<WalletResponse>> cancelDeposit(
            @PathVariable Long transactionId,
            @RequestBody AdminReasonRequest request
    ) {
        String reason = requireReason(request);
        try {
            return ResponseEntity.ok(ApiResponse.success(walletService.cancelDeposit(transactionId, reason)));
        } catch (RuntimeException e) {
            recordWalletFailure("WALLET_DEPOSIT_CANCEL_FAILED", transactionId, reason, e);
            throw e;
        }
    }

    /**
     * 출금 처리가 끝난 출금 요청을 완료 처리합니다.
     */
    @PostMapping("/withdrawals/{transactionId}/confirm")
    public ResponseEntity<ApiResponse<WalletResponse>> confirmWithdrawal(
            @PathVariable Long transactionId,
            @RequestBody AdminReasonRequest request
    ) {
        String reason = requireReason(request);
        try {
            return ResponseEntity.ok(ApiResponse.success(walletService.confirmWithdrawal(transactionId, reason)));
        } catch (RuntimeException e) {
            recordWalletFailure("WALLET_WITHDRAWAL_CONFIRM_FAILED", transactionId, reason, e);
            throw e;
        }
    }

    /**
     * 아직 처리되지 않은 출금 요청을 취소합니다.
     */
    @PostMapping("/withdrawals/{transactionId}/cancel")
    public ResponseEntity<ApiResponse<WalletResponse>> cancelWithdrawal(
            @PathVariable Long transactionId,
            @RequestBody AdminReasonRequest request
    ) {
        String reason = requireReason(request);
        try {
            return ResponseEntity.ok(ApiResponse.success(walletService.cancelWithdrawal(transactionId, reason)));
        } catch (RuntimeException e) {
            recordWalletFailure("WALLET_WITHDRAWAL_CANCEL_FAILED", transactionId, reason, e);
            throw e;
        }
    }

    private String requireReason(AdminReasonRequest request) {
        if (request == null || request.reason() == null || request.reason().isBlank()) {
            throw new BusinessException(ErrorCode.INVALID_INPUT, "관리자 처리 사유를 입력해야 합니다.");
        }
        return request.reason().trim();
    }

    private void recordWalletFailure(String actionType, Long transactionId, String reason, RuntimeException e) {
        adminActionLogService.recordFailure(
                actionType,
                "WALLET_TRANSACTION",
                transactionId,
                String.valueOf(transactionId),
                adminActionLogService.fields("reason", reason),
                e.getMessage()
        );
    }
}
