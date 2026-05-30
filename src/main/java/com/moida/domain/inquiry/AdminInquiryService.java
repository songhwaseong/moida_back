package com.moida.domain.inquiry;

import com.moida.common.exception.BusinessException;
import com.moida.common.exception.ErrorCode;
import com.moida.common.response.InquiryResponse;
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

    /** 전체 문의 목록 (최신순) */
    @Transactional(readOnly = true)
    public List<InquiryResponse> getAll() {
        return inquiryRepository.findAllForAdmin().stream()
                .map(InquiryResponse::from)
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
        inquiry.answer(text.trim());
        return InquiryResponse.from(inquiry);
    }

    /** 답변 삭제 (문의 자체는 남김) */
    @Transactional
    public void removeAnswer(Long inquiryId) {
        Inquiry inquiry = inquiryRepository.findById(inquiryId)
                .orElseThrow(() -> new BusinessException(ErrorCode.ENTITY_NOT_FOUND));
        inquiry.removeAnswer();
    }

    /** 문의 자체 삭제 (hard delete) */
    @Transactional
    public void delete(Long inquiryId) {
        if (!inquiryRepository.existsById(inquiryId)) {
            throw new BusinessException(ErrorCode.ENTITY_NOT_FOUND);
        }
        inquiryRepository.deleteById(inquiryId);
    }
}
