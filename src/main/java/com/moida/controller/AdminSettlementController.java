package com.moida.controller;

import com.moida.common.response.AdminSettlementResponse;
import com.moida.common.response.AdminSettlementSummaryResponse;
import com.moida.common.response.ApiResponse;
import com.moida.domain.settlement.AdminSettlementService;
import com.moida.domain.settlement.Settlement;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

/**
 * 관리자 정산 관리 API.
 * SecurityConfig 에서 /api/admin/** 는 ADMIN / MANAGER 만 접근 가능하다.
 */
@RestController
@RequestMapping("/api/admin/settlements")
@RequiredArgsConstructor
public class AdminSettlementController {

    private final AdminSettlementService adminSettlementService;

    @GetMapping
    public ResponseEntity<ApiResponse<List<AdminSettlementResponse>>> getAll() {
        return ResponseEntity.ok(ApiResponse.success(adminSettlementService.getAll()));
    }

    @GetMapping("/summary")
    public ResponseEntity<ApiResponse<AdminSettlementSummaryResponse>> getSummary() {
        return ResponseEntity.ok(ApiResponse.success(adminSettlementService.getSummary()));
    }

    /** 상태 변경 (body: { "status": "PAID" } / "CANCELED") */
    @PatchMapping("/{settlementId}/status")
    public ResponseEntity<ApiResponse<AdminSettlementResponse>> updateStatus(
            @PathVariable Long settlementId,
            @RequestBody Map<String, String> body) {
        Settlement.SettlementStatus next = Settlement.SettlementStatus.valueOf(body.get("status"));
        AdminSettlementResponse updated = adminSettlementService.updateStatus(settlementId, next);
        return ResponseEntity.ok(ApiResponse.success(updated, "상태가 변경되었습니다."));
    }
}
