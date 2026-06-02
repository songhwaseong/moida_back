package com.moida.controller;

import com.moida.common.exception.BusinessException;
import com.moida.common.exception.ErrorCode;
import com.moida.common.response.ApiResponse;
import com.moida.common.response.NoticeResponse;
import com.moida.domain.notice.Notice;
import com.moida.domain.notice.NoticeRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/notices")
@RequiredArgsConstructor
public class NoticeController {

    private final NoticeRepository noticeRepository;

    @GetMapping
    @Transactional(readOnly = true)
    public ResponseEntity<ApiResponse<List<NoticeResponse>>> getNotices() {
        List<NoticeResponse> notices = noticeRepository
                .findAllByStatusOrderByIsPinnedDescCreatedAtDesc(Notice.NoticeStatus.PUBLISHED)
                .stream()
                .map(NoticeResponse::from)
                .toList();
        return ResponseEntity.ok(ApiResponse.success(notices));
    }

    @GetMapping("/{id}")
    @Transactional
    public ResponseEntity<ApiResponse<NoticeResponse>> getNotice(@PathVariable Long id) {
        Notice notice = noticeRepository.findById(id)
                .filter(item -> item.getStatus() == Notice.NoticeStatus.PUBLISHED)
                .orElseThrow(() -> new BusinessException(ErrorCode.ENTITY_NOT_FOUND, "공지사항을 찾을 수 없습니다."));
        notice.increaseViewCount();
        return ResponseEntity.ok(ApiResponse.success(NoticeResponse.from(notice)));
    }
}
