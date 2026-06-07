package com.moida.controller;

import com.moida.common.exception.BusinessException;
import com.moida.common.exception.ErrorCode;
import com.moida.common.request.AdminSettlementStatusUpdateRequest;
import com.moida.common.response.AdminSettlementResponse;
import com.moida.common.response.AdminSettlementSummaryResponse;
import com.moida.common.response.ApiResponse;
import com.moida.domain.audit.AdminActionLogService;
import com.moida.domain.settlement.AdminSettlementService;
import com.moida.domain.settlement.Settlement;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/admin/settlements")
@RequiredArgsConstructor
public class AdminSettlementController {

    private final AdminSettlementService adminSettlementService;
    private final AdminActionLogService adminActionLogService;

    @GetMapping
    public ResponseEntity<ApiResponse<List<AdminSettlementResponse>>> getAll() {
        adminActionLogService.recordView(
                "ADMIN_SETTLEMENT_VIEW",
                "SETTLEMENT",
                adminActionLogService.fields("view", "list")
        );
        return ResponseEntity.ok(ApiResponse.success(adminSettlementService.getAll()));
    }

    @GetMapping("/summary")
    public ResponseEntity<ApiResponse<AdminSettlementSummaryResponse>> getSummary() {
        adminActionLogService.recordView(
                "ADMIN_SETTLEMENT_SUMMARY_VIEW",
                "SETTLEMENT",
                adminActionLogService.fields("view", "summary")
        );
        return ResponseEntity.ok(ApiResponse.success(adminSettlementService.getSummary()));
    }

    @PatchMapping("/{settlementId}/status")
    public ResponseEntity<ApiResponse<AdminSettlementResponse>> updateStatus(
            @PathVariable Long settlementId,
            @RequestBody AdminSettlementStatusUpdateRequest request
    ) {
        try {
            String reason = requireReason(request == null ? null : request.reason());
            Settlement.SettlementStatus next = Settlement.SettlementStatus.valueOf(request.status());
            AdminSettlementResponse updated = adminSettlementService.updateStatus(settlementId, next, reason);
            return ResponseEntity.ok(ApiResponse.success(updated, "상태가 변경되었습니다."));
        } catch (RuntimeException e) {
            adminActionLogService.recordFailure(
                    "SETTLEMENT_STATUS_CHANGE_FAILED",
                    "SETTLEMENT",
                    settlementId,
                    String.valueOf(settlementId),
                    adminActionLogService.fields("requestedStatus", request == null ? null : request.status()),
                    e.getMessage()
            );
            throw e;
        }
    }

    private String requireReason(String reason) {
        if (reason == null || reason.isBlank()) {
            throw new BusinessException(ErrorCode.INVALID_INPUT, "관리자 처리 사유를 입력해야 합니다.");
        }
        return reason.trim();
    }
}
