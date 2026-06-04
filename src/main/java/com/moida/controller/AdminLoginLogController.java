package com.moida.controller;

import com.moida.common.response.AdminLoginLogResponse;
import com.moida.common.response.ApiResponse;
import com.moida.domain.audit.AdminLoginLogService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * 관리자 로그인 기록 조회 API.
 * SecurityConfig 에서 /api/admin/login-logs/** 는 ADMIN 전용으로 제한된다(MANAGER 불가).
 */
@RestController
@RequestMapping("/api/admin/login-logs")
@RequiredArgsConstructor
public class AdminLoginLogController {

    private final AdminLoginLogService adminLoginLogService;

    @GetMapping
    public ResponseEntity<ApiResponse<List<AdminLoginLogResponse>>> getRecent() {
        return ResponseEntity.ok(ApiResponse.success(adminLoginLogService.getRecent()));
    }
}
