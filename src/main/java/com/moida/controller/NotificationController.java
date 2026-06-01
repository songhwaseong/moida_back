package com.moida.controller;

import com.moida.common.exception.BusinessException;
import com.moida.common.exception.ErrorCode;
import com.moida.common.request.NotificationSettingRequest;
import com.moida.common.response.ApiResponse;
import com.moida.common.response.NotificationResponse;
import com.moida.common.response.NotificationSettingResponse;
import com.moida.common.response.UnreadNotificationCountResponse;
import com.moida.domain.notification.Notification;
import com.moida.domain.notification.NotificationRepository;
import com.moida.security.CustomUserDetails;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/notifications")
@RequiredArgsConstructor
public class NotificationController {

    private final NotificationRepository notificationRepository;

    @GetMapping
    @Transactional(readOnly = true)
    public ResponseEntity<ApiResponse<List<NotificationResponse>>> getNotifications(
            @AuthenticationPrincipal CustomUserDetails userDetails,
            @RequestParam(defaultValue = "50") int size
    ) {
        int pageSize = Math.min(Math.max(size, 1), 100);
        List<NotificationResponse> notifications = notificationRepository
                .findAllByMemberIdOrderByCreatedAtDesc(userDetails.getMemberId(), PageRequest.of(0, pageSize))
                .stream()
                .map(NotificationResponse::from)
                .toList();

        return ResponseEntity.ok(ApiResponse.success(notifications));
    }

    @GetMapping("/unread-count")
    @Transactional(readOnly = true)
    public ResponseEntity<ApiResponse<UnreadNotificationCountResponse>> getUnreadCount(
            @AuthenticationPrincipal CustomUserDetails userDetails
    ) {
        long count = notificationRepository.countByMemberIdAndIsReadFalse(userDetails.getMemberId());
        return ResponseEntity.ok(ApiResponse.success(new UnreadNotificationCountResponse(count)));
    }

    @PatchMapping("/{id}/read")
    @Transactional
    public ResponseEntity<ApiResponse<NotificationResponse>> markAsRead(
            @AuthenticationPrincipal CustomUserDetails userDetails,
            @PathVariable Long id
    ) {
        Notification notification = notificationRepository.findByIdAndMemberId(id, userDetails.getMemberId())
                .orElseThrow(() -> new BusinessException(ErrorCode.ENTITY_NOT_FOUND, "Notification not found."));

        notification.markAsRead();
        return ResponseEntity.ok(ApiResponse.success(NotificationResponse.from(notification)));
    }

    @PatchMapping("/read-all")
    @Transactional
    public ResponseEntity<ApiResponse<Void>> markAllAsRead(
            @AuthenticationPrincipal CustomUserDetails userDetails
    ) {
        notificationRepository.findAllByMemberIdAndIsReadFalse(userDetails.getMemberId())
                .forEach(Notification::markAsRead);

        return ResponseEntity.ok(ApiResponse.success(null, "All notifications have been marked as read."));
    }

    @GetMapping("/settings")
    public ResponseEntity<ApiResponse<NotificationSettingResponse>> getSettings() {
        return ResponseEntity.ok(ApiResponse.success(NotificationSettingResponse.defaults()));
    }

    @PutMapping("/settings")
    public ResponseEntity<ApiResponse<NotificationSettingResponse>> updateSettings(
            @RequestBody NotificationSettingRequest request
    ) {
        return ResponseEntity.ok(ApiResponse.success(NotificationSettingResponse.from(request)));
    }
}
