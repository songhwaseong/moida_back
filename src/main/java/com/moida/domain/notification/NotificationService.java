package com.moida.domain.notification;

import com.moida.common.exception.BusinessException;
import com.moida.common.exception.ErrorCode;
import com.moida.common.request.NotificationSettingRequest;
import com.moida.common.response.NotificationResponse;
import com.moida.common.response.NotificationSettingResponse;
import com.moida.domain.member.Member;
import com.moida.domain.member.MemberRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
public class NotificationService {

    private static final int DEFAULT_NOTIFICATION_SIZE = 50;
    private static final int MAX_NOTIFICATION_SIZE = 100;

    private final MemberRepository memberRepository;
    private final NotificationRepository notificationRepository;
    private final NotificationSettingRepository settingRepository;

    @Transactional
    public NotificationSettingResponse getSettings(Long memberId) {
        // 설정 row가 아직 없는 회원도 동일한 API로 바로 설정 화면을 열 수 있게 합니다.
        NotificationSetting setting = settingRepository.findByMemberId(memberId)
                .orElseGet(() -> settingRepository.save(NotificationSetting.defaultFor(findMember(memberId))));
        return NotificationSettingResponse.from(setting);
    }

    @Transactional
    public NotificationSettingResponse updateSettings(Long memberId, NotificationSettingRequest request) {
        NotificationSetting setting = settingRepository.findByMemberId(memberId)
                .orElseGet(() -> settingRepository.save(NotificationSetting.defaultFor(findMember(memberId))));
        setting.update(
                request.bidEnabled(),
                request.priceEnabled(),
                request.chatEnabled(),
                request.tradeEnabled(),
                request.marketingEnabled()
        );
        return NotificationSettingResponse.from(setting);
    }

    @Transactional(readOnly = true)
    public List<NotificationResponse> getNotifications(Long memberId, Integer size) {
        int pageSize = normalizeSize(size);
        // 알림 목록은 항상 로그인한 회원 본인 데이터만 반환합니다.
        return notificationRepository.findAllByMemberIdOrderByCreatedAtDesc(
                        memberId,
                        PageRequest.of(0, pageSize, Sort.by(Sort.Direction.DESC, "createdAt"))
                )
                .stream()
                .map(NotificationResponse::from)
                .toList();
    }

    @Transactional
    public NotificationResponse markAsRead(Long memberId, Long notificationId) {
        // 다른 회원의 알림 ID를 직접 호출해도 읽음 처리되지 않도록 소유자를 함께 확인합니다.
        Notification notification = notificationRepository.findByIdAndMemberId(notificationId, memberId)
                .orElseThrow(() -> new BusinessException(ErrorCode.ENTITY_NOT_FOUND));
        notification.markAsRead();
        return NotificationResponse.from(notification);
    }

    @Transactional
    public void markAllAsRead(Long memberId) {
        notificationRepository.markAllAsRead(memberId);
    }

    @Transactional(readOnly = true)
    public long countUnread(Long memberId) {
        return notificationRepository.countByMemberIdAndIsReadFalse(memberId);
    }

    private int normalizeSize(Integer size) {
        if (size == null) return DEFAULT_NOTIFICATION_SIZE;
        return Math.max(1, Math.min(size, MAX_NOTIFICATION_SIZE));
    }

    private Member findMember(Long memberId) {
        return memberRepository.findById(memberId)
                .orElseThrow(() -> new BusinessException(ErrorCode.MEMBER_NOT_FOUND));
    }
}
