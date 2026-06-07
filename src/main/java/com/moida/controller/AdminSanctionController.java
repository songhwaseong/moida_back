package com.moida.controller;

import com.moida.common.exception.BusinessException;
import com.moida.common.exception.ErrorCode;
import com.moida.common.request.AdminSanctionRequest;
import com.moida.common.response.AdminSanctionResponse;
import com.moida.common.response.ApiResponse;
import com.moida.domain.audit.AdminActionLogService;
import com.moida.domain.sanction.AdminSanctionService;
import com.moida.security.CustomUserDetails;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

/**
 * 관리자 제재 관리 API.
 * SecurityConfig 의 /api/admin/** 규칙에 의해 ADMIN/MANAGER 만 접근 가능.
 */
@RestController
@RequestMapping("/api/admin/sanctions")
@RequiredArgsConstructor
public class AdminSanctionController {

    private final AdminSanctionService adminSanctionService;
    private final AdminActionLogService adminActionLogService;

    @GetMapping
    public ResponseEntity<ApiResponse<List<AdminSanctionResponse>>> getAll() {
        adminActionLogService.recordView(
                "ADMIN_SANCTION_VIEW",
                "SANCTION",
                adminActionLogService.fields("view", "list")
        );
        return ResponseEntity.ok(ApiResponse.success(adminSanctionService.getAll()));
    }

    /** 누적 제재 건수 (대시보드용) */
    @GetMapping("/count")
    public ResponseEntity<ApiResponse<Map<String, Long>>> count() {
        adminActionLogService.recordView(
                "ADMIN_SANCTION_COUNT_VIEW",
                "SANCTION",
                adminActionLogService.fields("view", "count")
        );
        return ResponseEntity.ok(ApiResponse.success(Map.of("total", adminSanctionService.countAll())));
    }

    /** 제재 등록 */
    @PostMapping
    public ResponseEntity<ApiResponse<AdminSanctionResponse>> create(
            @RequestBody AdminSanctionRequest request,
            @AuthenticationPrincipal CustomUserDetails admin) {
        requireReason(request);
        try {
            return ResponseEntity.ok(ApiResponse.success(
                    adminSanctionService.create(request, admin),
                    "제재가 등록되었습니다."));
        } catch (RuntimeException e) {
            adminActionLogService.recordFailure(
                    "SANCTION_CREATE_FAILED",
                    "MEMBER",
                    null,
                    request == null ? null : request.getMemberNo(),
                    adminActionLogService.fields(
                            "memberNo", request == null ? null : request.getMemberNo(),
                            "type", request == null ? null : request.getType(),
                            "reason", request == null ? null : request.getReason()
                    ),
                    e.getMessage()
            );
            throw e;
        }
    }

    private void requireReason(AdminSanctionRequest request) {
        if (request == null || request.getReason() == null || request.getReason().isBlank()) {
            throw new BusinessException(ErrorCode.INVALID_INPUT, "관리자 처리 사유를 입력해야 합니다.");
        }
    }
}
