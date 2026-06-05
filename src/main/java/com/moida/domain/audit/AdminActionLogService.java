package com.moida.domain.audit;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.moida.common.response.AdminActionLogResponse;
import com.moida.security.CustomUserDetails;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Service
@RequiredArgsConstructor
public class AdminActionLogService {

    private static final String UNKNOWN_ADMIN = "unknown";

    private final AdminActionLogRepository adminActionLogRepository;
    private final ObjectMapper objectMapper;

    public void record(String actionType, String targetType, Long targetId, String targetName,
                       Object beforeValue, Object afterValue, String reason) {
        AdminActor actor = resolveActor();
        HttpServletRequest request = currentRequest();

        adminActionLogRepository.save(AdminActionLog.builder()
                .adminId(actor.adminId())
                .adminEmail(truncate(actor.email(), 255))
                .actionType(truncate(actionType, 60))
                .targetType(truncate(targetType, 40))
                .targetId(targetId)
                .targetName(truncate(targetName, 200))
                .beforeValue(toJson(beforeValue))
                .afterValue(toJson(afterValue))
                .reason(truncate(reason, 500))
                .ip(truncate(resolveIp(request), 64))
                .userAgent(truncate(request == null ? null : request.getHeader("User-Agent"), 512))
                .build());
    }

    public Map<String, Object> fields(Object... pairs) {
        Map<String, Object> values = new LinkedHashMap<>();
        if (pairs == null) {
            return values;
        }
        for (int i = 0; i + 1 < pairs.length; i += 2) {
            values.put(String.valueOf(pairs[i]), pairs[i + 1]);
        }
        return values;
    }

    @Transactional(readOnly = true)
    public List<AdminActionLogResponse> getRecent() {
        return adminActionLogRepository.findTop500ByOrderByIdDesc().stream()
                .map(AdminActionLogResponse::from)
                .toList();
    }

    private AdminActor resolveActor() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null) {
            return new AdminActor(null, UNKNOWN_ADMIN);
        }
        Object principal = authentication.getPrincipal();
        if (principal instanceof CustomUserDetails userDetails) {
            return new AdminActor(userDetails.getMemberId(), userDetails.getUsername());
        }
        String name = authentication.getName();
        return new AdminActor(null, name == null || name.isBlank() ? UNKNOWN_ADMIN : name);
    }

    private HttpServletRequest currentRequest() {
        if (RequestContextHolder.getRequestAttributes() instanceof ServletRequestAttributes attributes) {
            return attributes.getRequest();
        }
        return null;
    }

    private String resolveIp(HttpServletRequest request) {
        if (request == null) {
            return null;
        }
        String forwarded = request.getHeader("X-Forwarded-For");
        if (forwarded != null && !forwarded.isBlank()) {
            return forwarded.split(",")[0].trim();
        }
        return request.getRemoteAddr();
    }

    private String toJson(Object value) {
        if (value == null) {
            return null;
        }
        if (value instanceof String text) {
            return text;
        }
        try {
            return objectMapper.writeValueAsString(value);
        } catch (JsonProcessingException e) {
            return String.valueOf(value);
        }
    }

    private String truncate(String value, int maxLength) {
        if (value == null) {
            return null;
        }
        return value.length() > maxLength ? value.substring(0, maxLength) : value;
    }

    private record AdminActor(Long adminId, String email) {
    }
}
