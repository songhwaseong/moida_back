package com.moida.controller;

import com.moida.common.exception.BusinessException;
import com.moida.common.exception.ErrorCode;
import com.moida.common.request.FaqRequest;
import com.moida.common.response.ApiResponse;
import com.moida.common.response.FaqResponse;
import com.moida.domain.audit.AdminActionLogService;
import com.moida.domain.faq.Faq;
import com.moida.domain.faq.FaqRepository;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/admin/faqs")
@RequiredArgsConstructor
public class AdminFaqController {

    private final FaqRepository faqRepository;
    private final AdminActionLogService adminActionLogService;

    @GetMapping
    @Transactional(readOnly = true)
    public ResponseEntity<ApiResponse<List<FaqResponse>>> getFaqs() {
        List<FaqResponse> faqs = faqRepository.findAllByOrderByDisplayOrderAscIdAsc()
                .stream()
                .map(FaqResponse::from)
                .toList();
        return ResponseEntity.ok(ApiResponse.success(faqs));
    }

    @GetMapping("/{id}")
    @Transactional(readOnly = true)
    public ResponseEntity<ApiResponse<FaqResponse>> getFaq(@PathVariable Long id) {
        return ResponseEntity.ok(ApiResponse.success(FaqResponse.from(findFaq(id))));
    }

    @PostMapping
    @Transactional
    public ResponseEntity<ApiResponse<FaqResponse>> createFaq(
            @Valid @RequestBody FaqRequest request
    ) {
        validateDisplayOrderAvailable(null, request.order());

        Faq faq = faqRepository.save(Faq.builder()
                .category(request.category())
                .question(request.question())
                .answer(request.answer())
                .displayOrder(request.order())
                .visible(request.visible())
                .build());
        adminActionLogService.record(
                "FAQ_CREATE",
                "FAQ",
                faq.getId(),
                faq.getQuestion(),
                null,
                adminActionLogService.fields(
                        "category", faq.getCategory(),
                        "question", faq.getQuestion(),
                        "answer", faq.getAnswer(),
                        "displayOrder", faq.getDisplayOrder(),
                        "visible", faq.getVisible()
                ),
                "FAQ 등록"
        );

        return ResponseEntity
                .status(HttpStatus.CREATED)
                .body(ApiResponse.success(FaqResponse.from(faq), "FAQ가 등록되었습니다."));
    }

    @RequestMapping(value = "/{id}", method = {RequestMethod.PUT, RequestMethod.PATCH})
    @Transactional
    public ResponseEntity<ApiResponse<FaqResponse>> updateFaq(
            @PathVariable Long id,
            @Valid @RequestBody FaqRequest request
    ) {
        Faq faq = findFaq(id);
        validateDisplayOrderAvailable(id, request.order());
        Object beforeValue = adminActionLogService.fields(
                "category", faq.getCategory(),
                "question", faq.getQuestion(),
                "answer", faq.getAnswer(),
                "displayOrder", faq.getDisplayOrder(),
                "visible", faq.getVisible()
        );

        faq.update(
                request.category(),
                request.question(),
                request.answer(),
                request.order(),
                request.visible()
        );
        adminActionLogService.record(
                "FAQ_UPDATE",
                "FAQ",
                faq.getId(),
                faq.getQuestion(),
                beforeValue,
                adminActionLogService.fields(
                        "category", faq.getCategory(),
                        "question", faq.getQuestion(),
                        "answer", faq.getAnswer(),
                        "displayOrder", faq.getDisplayOrder(),
                        "visible", faq.getVisible()
                ),
                "FAQ 수정"
        );

        return ResponseEntity.ok(ApiResponse.success(FaqResponse.from(faq), "FAQ가 수정되었습니다."));
    }

    @DeleteMapping("/{id}")
    @Transactional
    public ResponseEntity<ApiResponse<Void>> deleteFaq(@PathVariable Long id) {
        Faq faq = findFaq(id);
        adminActionLogService.record(
                "FAQ_DELETE",
                "FAQ",
                faq.getId(),
                faq.getQuestion(),
                adminActionLogService.fields(
                        "category", faq.getCategory(),
                        "question", faq.getQuestion(),
                        "answer", faq.getAnswer(),
                        "displayOrder", faq.getDisplayOrder(),
                        "visible", faq.getVisible()
                ),
                null,
                "FAQ 삭제"
        );
        faqRepository.delete(faq);
        return ResponseEntity.ok(ApiResponse.success(null, "FAQ가 삭제되었습니다."));
    }

    private Faq findFaq(Long id) {
        return faqRepository.findById(id)
                .orElseThrow(() -> new BusinessException(ErrorCode.ENTITY_NOT_FOUND, "FAQ를 찾을 수 없습니다."));
    }

    private void validateDisplayOrderAvailable(Long currentId, Integer displayOrder) {
        faqRepository.findFirstByDisplayOrder(displayOrder)
                .filter(faq -> currentId == null || !faq.getId().equals(currentId))
                .ifPresent(faq -> {
                    throw new BusinessException(ErrorCode.INVALID_INPUT, "이미 사용 중인 FAQ 순서입니다.");
                });
    }
}
