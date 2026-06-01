package com.moida.controller;

import com.moida.common.request.AdminSanctionRequest;
import com.moida.common.response.AdminSanctionResponse;
import com.moida.common.response.ApiResponse;
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

    @GetMapping
    public ResponseEntity<ApiResponse<List<AdminSanctionResponse>>> getAll() {
        return ResponseEntity.ok(ApiResponse.success(adminSanctionService.getAll()));
    }

    /** 누적 제재 건수 (대시보드용) */
    @GetMapping("/count")
    public ResponseEntity<ApiResponse<Map<String, Long>>> count() {
        return ResponseEntity.ok(ApiResponse.success(Map.of("total", adminSanctionService.countAll())));
    }

    /** 제재 등록 */
    @PostMapping
    public ResponseEntity<ApiResponse<AdminSanctionResponse>> create(
            @RequestBody AdminSanctionRequest request,
            @AuthenticationPrincipal CustomUserDetails admin) {
        return ResponseEntity.ok(ApiResponse.success(
                adminSanctionService.create(request, admin),
                "제재가 등록되었습니다."));
    }
}
