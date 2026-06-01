package com.moida.controller;

import com.moida.common.response.ApiResponse;
import com.moida.common.response.InquiryResponse;
import com.moida.domain.inquiry.AdminInquiryService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

/**
 * 관리자 상품 문의 관리 API.
 * SecurityConfig 에서 /api/admin/** 는 ADMIN / MANAGER 만 접근 가능하다.
 */
@RestController
@RequestMapping("/api/admin/inquiries")
@RequiredArgsConstructor
public class AdminInquiryController {

    private final AdminInquiryService adminInquiryService;

    /** 문의 목록 조회 */
    @GetMapping
    public ResponseEntity<ApiResponse<List<InquiryResponse>>> getAll() {
        return ResponseEntity.ok(ApiResponse.success(adminInquiryService.getAll()));
    }

    /** 답변 작성/수정 (body: { "text": "..." }) */
    @PatchMapping("/{inquiryId}/answer")
    public ResponseEntity<ApiResponse<InquiryResponse>> answer(
            @PathVariable Long inquiryId,
            @RequestBody Map<String, String> body) {
        InquiryResponse updated = adminInquiryService.answer(inquiryId, body.get("text"));
        return ResponseEntity.ok(ApiResponse.success(updated, "답변이 등록되었습니다."));
    }

    /** 답변 삭제 */
    @DeleteMapping("/{inquiryId}/answer")
    public ResponseEntity<ApiResponse<String>> removeAnswer(@PathVariable Long inquiryId) {
        adminInquiryService.removeAnswer(inquiryId);
        return ResponseEntity.ok(ApiResponse.success("답변이 삭제되었습니다."));
    }

    /** 문의 자체 삭제 */
    @DeleteMapping("/{inquiryId}")
    public ResponseEntity<ApiResponse<String>> delete(@PathVariable Long inquiryId) {
        adminInquiryService.delete(inquiryId);
        return ResponseEntity.ok(ApiResponse.success("문의가 삭제되었습니다."));
    }
}
