package com.moida.controller;

import com.moida.common.response.AdminActionLogResponse;
import com.moida.common.response.ApiResponse;
import com.moida.domain.audit.AdminActionLogService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/admin/action-logs")
@RequiredArgsConstructor
public class AdminActionLogController {

    private final AdminActionLogService adminActionLogService;

    @GetMapping
    public ResponseEntity<ApiResponse<List<AdminActionLogResponse>>> getRecent() {
        adminActionLogService.recordView(
                "ADMIN_ACTION_LOG_VIEW",
                "ADMIN_ACTION_LOG",
                adminActionLogService.fields("limit", 500)
        );
        return ResponseEntity.ok(ApiResponse.success(adminActionLogService.getRecent()));
    }
}
