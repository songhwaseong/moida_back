package com.moida.controller;

import com.moida.common.exception.BusinessException;
import com.moida.common.exception.ErrorCode;
import com.moida.common.request.NoticeRequest;
import com.moida.common.response.ApiResponse;
import com.moida.common.response.NoticeResponse;
import com.moida.domain.audit.AdminActionLogService;
import com.moida.domain.member.Member;
import com.moida.domain.member.MemberRepository;
import com.moida.domain.member.MemberStatus;
import com.moida.domain.notification.Notification;
import com.moida.domain.notification.NotificationService;
import com.moida.domain.notice.Notice;
import com.moida.domain.notice.NoticeRepository;
import com.moida.security.CustomUserDetails;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/admin/notices")
@RequiredArgsConstructor
public class AdminNoticeController {

    private final NoticeRepository noticeRepository;
    private final MemberRepository memberRepository;
    private final NotificationService notificationService;
    private final AdminActionLogService adminActionLogService;

    @GetMapping
    @Transactional(readOnly = true)
    public ResponseEntity<ApiResponse<List<NoticeResponse>>> getNotices() {
        List<NoticeResponse> notices = noticeRepository.findAllByOrderByIsPinnedDescCreatedAtDesc()
                .stream()
                .map(NoticeResponse::from)
                .toList();
        return ResponseEntity.ok(ApiResponse.success(notices));
    }

    @GetMapping("/{id}")
    @Transactional(readOnly = true)
    public ResponseEntity<ApiResponse<NoticeResponse>> getNotice(@PathVariable Long id) {
        return ResponseEntity.ok(ApiResponse.success(NoticeResponse.from(findNotice(id))));
    }

    @PostMapping
    @Transactional
    public ResponseEntity<ApiResponse<NoticeResponse>> createNotice(
            @AuthenticationPrincipal CustomUserDetails userDetails,
            @Valid @RequestBody NoticeRequest request
    ) {
        Member author = memberRepository.findById(userDetails.getMemberId())
                .orElseThrow(() -> new BusinessException(ErrorCode.ENTITY_NOT_FOUND, "작성자를 찾을 수 없습니다."));

        Notice notice = noticeRepository.save(Notice.builder()
                .author(author)
                .title(request.title())
                .content(request.content())
                .category(parseCategory(request.category()))
                .status(parseStatus(request.status()))
                .isPinned(request.isPinned())
                .build());
        adminActionLogService.record(
                "NOTICE_CREATE",
                "NOTICE",
                notice.getId(),
                notice.getTitle(),
                null,
                adminActionLogService.fields(
                        "title", notice.getTitle(),
                        "content", notice.getContent(),
                        "category", notice.getCategory(),
                        "status", notice.getStatus(),
                        "isPinned", notice.getIsPinned()
                ),
                "공지사항 등록"
        );

        if (notice.getStatus() == Notice.NoticeStatus.PUBLISHED) {
            notifyPublishedNotice(notice);
        }

        return ResponseEntity
                .status(HttpStatus.CREATED)
                .body(ApiResponse.success(NoticeResponse.from(notice), "공지사항이 등록되었습니다."));
    }

    @PutMapping("/{id}")
    @Transactional
    public ResponseEntity<ApiResponse<NoticeResponse>> updateNotice(
            @PathVariable Long id,
            @Valid @RequestBody NoticeRequest request
    ) {
        Notice notice = findNotice(id);
        Object beforeValue = adminActionLogService.fields(
                "title", notice.getTitle(),
                "content", notice.getContent(),
                "category", notice.getCategory(),
                "status", notice.getStatus(),
                "isPinned", notice.getIsPinned()
        );
        notice.update(
                request.title(),
                request.content(),
                parseCategory(request.category()),
                parseStatus(request.status()),
                request.isPinned()
        );
        adminActionLogService.record(
                "NOTICE_UPDATE",
                "NOTICE",
                notice.getId(),
                notice.getTitle(),
                beforeValue,
                adminActionLogService.fields(
                        "title", notice.getTitle(),
                        "content", notice.getContent(),
                        "category", notice.getCategory(),
                        "status", notice.getStatus(),
                        "isPinned", notice.getIsPinned()
                ),
                "공지사항 수정"
        );
        return ResponseEntity.ok(ApiResponse.success(NoticeResponse.from(notice), "공지사항이 수정되었습니다."));
    }

    @DeleteMapping("/{id}")
    @Transactional
    public ResponseEntity<ApiResponse<Void>> deleteNotice(@PathVariable Long id) {
        Notice notice = findNotice(id);
        adminActionLogService.record(
                "NOTICE_DELETE",
                "NOTICE",
                notice.getId(),
                notice.getTitle(),
                adminActionLogService.fields(
                        "title", notice.getTitle(),
                        "content", notice.getContent(),
                        "category", notice.getCategory(),
                        "status", notice.getStatus(),
                        "isPinned", notice.getIsPinned()
                ),
                null,
                "공지사항 삭제"
        );
        noticeRepository.delete(notice);
        return ResponseEntity.ok(ApiResponse.success(null, "공지사항이 삭제되었습니다."));
    }

    private Notice findNotice(Long id) {
        return noticeRepository.findById(id)
                .orElseThrow(() -> new BusinessException(ErrorCode.ENTITY_NOT_FOUND, "공지사항을 찾을 수 없습니다."));
    }

    private Notice.NoticeCategory parseCategory(String category) {
        if ("이벤트".equals(category)) return Notice.NoticeCategory.EVENT;
        if ("점검".equals(category)) return Notice.NoticeCategory.MAINTENANCE;
        if ("정책".equals(category)) return Notice.NoticeCategory.POLICY;
        return Notice.NoticeCategory.GENERAL;
    }

    private Notice.NoticeStatus parseStatus(String status) {
        if ("예약".equals(status)) return Notice.NoticeStatus.SCHEDULED;
        if ("숨김".equals(status)) return Notice.NoticeStatus.HIDDEN;
        return Notice.NoticeStatus.PUBLISHED;
    }

    private void notifyPublishedNotice(Notice notice) {
        memberRepository.findAllByStatusOrderByWithdrawnAtDesc(MemberStatus.ACTIVE)
                .forEach(member -> notificationService.createAndPush(
                        member,
                        Notification.NotificationType.NOTICE,
                        "새 공지사항이 등록됐어요",
                        String.format("[%s] %s", noticeCategoryLabel(notice.getCategory()), notice.getTitle()),
                        "/notices/" + notice.getId()
                ));
    }

    private String noticeCategoryLabel(Notice.NoticeCategory category) {
        return switch (category) {
            case EVENT -> "이벤트";
            case MAINTENANCE -> "점검";
            case POLICY -> "정책";
            case UPDATE -> "업데이트";
            case URGENT -> "긴급";
            case GENERAL -> "서비스";
        };
    }
}
