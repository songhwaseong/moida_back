package com.moida.domain.inquiry;

import com.moida.common.exception.BusinessException;
import com.moida.common.exception.ErrorCode;
import com.moida.common.response.InquiryResponse;
import com.moida.domain.audit.AdminActionLogService;
import com.moida.domain.notification.Notification;
import com.moida.domain.notification.NotificationService;
import com.moida.domain.product.ProductImageStorageService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

/**
 * 관리자 상품 문의 관리 전용 서비스.
 * 일반 사용자/판매자 흐름과 분리해, 답변 작성·수정·삭제·문의 삭제 권한 동작만 담당한다.
 */
@Service
@RequiredArgsConstructor
public class AdminInquiryService {

    private final InquiryRepository inquiryRepository;
    private final NotificationService notificationService;
    private final AdminActionLogService adminActionLogService;
    private final ProductImageStorageService productImageStorageService;

    /** 전체 문의 목록 (최신순) */
    @Transactional(readOnly = true)
    public List<InquiryResponse> getAll() {
        return inquiryRepository.findAllForAdmin().stream()
                .map(inquiry -> InquiryResponse.from(inquiry, productImageStorageService::toPublicUrl))
                .toList();
    }

    /** 답변 작성/수정 */
    @Transactional
    public InquiryResponse answer(Long inquiryId, String text) {
        if (text == null || text.isBlank()) {
            throw new BusinessException(ErrorCode.INVALID_INPUT, "답변 내용을 입력해주세요.");
        }
        Inquiry inquiry = inquiryRepository.findById(inquiryId)
                .orElseThrow(() -> new BusinessException(ErrorCode.ENTITY_NOT_FOUND));
        String previousAnswer = inquiry.getAnswer();
        inquiry.answer(text.trim());
        adminActionLogService.record(
                "INQUIRY_ANSWER",
                "INQUIRY",
                inquiry.getId(),
                inquiry.getProduct().getName(),
                adminActionLogService.fields("answer", previousAnswer),
                adminActionLogService.fields("answer", inquiry.getAnswer()),
                "상품 문의 답변 등록/수정"
        );
        notificationService.createAndPush(
                inquiry.getUser(),
                Notification.NotificationType.INQUIRY_ANSWERED,
                "상품 문의에 답변이 등록됐어요",
                String.format("'%s' 상품 문의에 답변이 등록되었습니다.", inquiry.getProduct().getName()),
                "/products/" + inquiry.getProduct().getId()
        );
        return InquiryResponse.from(inquiry, productImageStorageService::toPublicUrl);
    }

    /** 답변 삭제 (문의 자체는 남김) */
    @Transactional
    public void removeAnswer(Long inquiryId) {
        Inquiry inquiry = inquiryRepository.findById(inquiryId)
                .orElseThrow(() -> new BusinessException(ErrorCode.ENTITY_NOT_FOUND));
        String previousAnswer = inquiry.getAnswer();
        inquiry.removeAnswer();
        adminActionLogService.record(
                "INQUIRY_ANSWER_DELETE",
                "INQUIRY",
                inquiry.getId(),
                inquiry.getProduct().getName(),
                adminActionLogService.fields("answer", previousAnswer),
                adminActionLogService.fields("answer", inquiry.getAnswer()),
                "상품 문의 답변 삭제"
        );
    }

    /** 문의 자체 삭제 (hard delete) */
    @Transactional
    public void delete(Long inquiryId) {
        Inquiry inquiry = inquiryRepository.findById(inquiryId)
                .orElseThrow(() -> new BusinessException(ErrorCode.ENTITY_NOT_FOUND));
        adminActionLogService.record(
                "INQUIRY_DELETE",
                "INQUIRY",
                inquiry.getId(),
                inquiry.getProduct().getName(),
                adminActionLogService.fields(
                        "question", inquiry.getQuestion(),
                        "answer", inquiry.getAnswer(),
                        "userId", inquiry.getUser().getId()
                ),
                null,
                "상품 문의 삭제"
        );
        inquiryRepository.delete(inquiry);
    }
}
