package com.moida.domain.notification;

import com.moida.common.exception.BusinessException;
import com.moida.common.exception.ErrorCode;
import com.moida.common.request.NotificationSettingRequest;
import com.moida.common.response.NotificationResponse;
import com.moida.common.response.NotificationSettingResponse;
import com.moida.domain.member.Member;
import com.moida.domain.member.MemberRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class NotificationService {

    private static final int DEFAULT_NOTIFICATION_SIZE = 50;
    private static final int MAX_NOTIFICATION_SIZE = 100;

    /**
     * STOMP user 목적지 경로. 클라이언트는 /user/queue/notifications 를 구독한다.
     * (SimpMessagingTemplate.convertAndSendToUser 가 /queue/notifications → /user/queue/notifications 로 라우팅)
     */
    public static final String USER_NOTIFICATION_DESTINATION = "/queue/notifications";

    private final MemberRepository memberRepository;
    private final NotificationRepository notificationRepository;
    private final NotificationSettingRepository settingRepository;
    private final SimpMessagingTemplate messagingTemplate;

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
                request.marketingEnabled(),
                request.productStatusEnabled(),
                request.inquiryEnabled(),
                request.auctionResultEnabled()
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

    /**
     * 알림을 DB 에 저장하고 동시에 해당 사용자에게 실시간 STOMP 메시지로 push 한다.
     * push 라우팅 키는 STOMP Principal.getName() = {@code Member.email} (JwtTokenProvider/CustomUserDetails 기준).
     * 따라서 호출자는 알림 대상 Member 의 email 이 채워져 있다는 점만 보장하면 된다.
     *
     * push 실패(WebSocket 연결 없음 등)는 DB 저장을 롤백하지 않는다 —
     * 사용자가 다음번 알림 페이지 진입 시 정상적으로 보이는 것이 더 중요하기 때문.
     */
    @Transactional
    public Notification createAndPush(Member to, Notification.NotificationType type,
                                       String title, String content, String linkUrl) {
        if (!isEnabledFor(to, type)) {
            log.debug("[NotificationService] skipped by setting memberId={}, type={}", to.getId(), type);
            return null;
        }

        Notification saved = notificationRepository.save(Notification.builder()
                .member(to)
                .type(type)
                .title(title)
                .content(content)
                .linkUrl(linkUrl)
                .build());

        try {
            // STOMP user destination 으로 즉시 push. 수신 클라이언트는 NotificationResponse 형태로 처리.
            messagingTemplate.convertAndSendToUser(
                    to.getEmail(),
                    USER_NOTIFICATION_DESTINATION,
                    NotificationResponse.from(saved)
            );
        } catch (Exception e) {
            // 실시간 push 가 실패해도(클라이언트 미연결 등) 정상 흐름으로 간주.
            // 사용자는 알림 페이지에서 곧 확인할 수 있다.
            log.warn("[NotificationService] WebSocket push failed memberId={}, type={}: {}",
                    to.getId(), type, e.getMessage());
        }
        return saved;
    }

    private boolean isEnabledFor(Member member, Notification.NotificationType type) {
        NotificationSetting setting = settingRepository.findByMemberId(member.getId())
                .orElse(null);
        if (setting == null) {
            return true;
        }

        return switch (type) {
            case BID_PLACED, BID_OUTBID -> setting.isBidEnabled();
            case CHAT_MESSAGE -> setting.isChatEnabled();
            case PRODUCT_APPROVED, PRODUCT_AUCTION_STARTED, PRODUCT_AUCTION_FAILED -> setting.isProductStatusEnabled();
            case INQUIRY_NEW, INQUIRY_ANSWERED -> setting.isInquiryEnabled();
            case AUCTION_WON,
                    AUCTION_WON_PAYMENT_REQUIRED,
                    PRODUCT_SOLD,
                    PAYMENT_COMPLETED,
                    AUCTION_FAILED_BY_NONPAYMENT,
                    AUCTION_LOST,
                    AUCTION_ENDED,
                    DELIVERY_SHIPPED,
                    DELIVERY_IN_TRANSIT,
                    DELIVERY_DELIVERED,
                    RECEIPT_CONFIRMED,
                    SETTLEMENT_PAID -> setting.isAuctionResultEnabled();
            // 제재/공지 등 운영상 중요한 알림은 설정과 무관하게 보냅니다.
            case SANCTION, NOTICE -> true;
        };
    }
}
