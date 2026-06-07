package com.moida.controller;

import com.moida.common.request.AdminAuditEventRequest;
import com.moida.common.response.ApiResponse;
import com.moida.domain.audit.AdminActionLogService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Set;

@RestController
@RequestMapping("/api/admin/audit-events")
@RequiredArgsConstructor
public class AdminAuditEventController {

    private static final Set<String> ALLOWED_ACTIONS = Set.of(
            "ADMIN_LOGOUT",
            "ADMIN_IDLE_TIMEOUT"
    );

    private final AdminActionLogService adminActionLogService;

    @PostMapping
    public ResponseEntity<ApiResponse<Void>> record(@RequestBody AdminAuditEventRequest request) {
        String actionType = request == null ? null : request.actionType();
        if (!ALLOWED_ACTIONS.contains(actionType)) {
            return ResponseEntity.badRequest()
                    .body(ApiResponse.error("INVALID_ADMIN_AUDIT_EVENT", "지원하지 않는 관리자 감사 이벤트입니다."));
        }

        adminActionLogService.record(
                actionType,
                "ADMIN_SESSION",
                null,
                "ADMIN_SESSION",
                null,
                adminActionLogService.fields("event", actionType),
                request.reason()
        );
        return ResponseEntity.ok(ApiResponse.success(null));
    }
}
