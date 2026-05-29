package com.moida.controller;

import com.moida.common.response.ApiResponse;
import com.moida.common.response.FaqResponse;
import com.moida.domain.faq.FaqRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/faqs")
@RequiredArgsConstructor
public class FaqController {

    private final FaqRepository faqRepository;

    @GetMapping
    @Transactional(readOnly = true)
    public ResponseEntity<ApiResponse<List<FaqResponse>>> getFaqs() {
        List<FaqResponse> faqs = faqRepository.findAllByVisibleTrueOrderByDisplayOrderAscIdAsc()
                .stream()
                .map(FaqResponse::from)
                .toList();
        return ResponseEntity.ok(ApiResponse.success(faqs));
    }
}
