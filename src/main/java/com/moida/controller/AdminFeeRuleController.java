package com.moida.controller;

import com.moida.common.request.FeeRuleUpdateRequest;
import com.moida.common.response.ApiResponse;
import com.moida.common.response.FeeRuleResponse;
import com.moida.domain.settlement.AdminFeeRuleService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * 관리자 수수료 정책 API.
 * SecurityConfig 의 /api/admin/** 규칙에 의해 ADMIN/MANAGER 만 접근 가능.
 */
@RestController
@RequestMapping("/api/admin/fee-rules")
@RequiredArgsConstructor
public class AdminFeeRuleController {

    private final AdminFeeRuleService adminFeeRuleService;

    @GetMapping
    public ResponseEntity<ApiResponse<List<FeeRuleResponse>>> getAll() {
        return ResponseEntity.ok(ApiResponse.success(adminFeeRuleService.getAll()));
    }

    /** 수수료 정책 수정 (minAmount / feeRate, 모두 선택적) */
    @PatchMapping("/{id}")
    public ResponseEntity<ApiResponse<FeeRuleResponse>> update(
            @PathVariable Long id,
            @RequestBody FeeRuleUpdateRequest request) {
        FeeRuleResponse updated = adminFeeRuleService.update(id, request);
        return ResponseEntity.ok(ApiResponse.success(updated, "수수료 정책이 수정되었습니다."));
    }
}
