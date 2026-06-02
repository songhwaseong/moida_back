package com.moida.controller;

import com.moida.common.exception.BusinessException;
import com.moida.common.exception.ErrorCode;
import com.moida.common.response.ApiResponse;
import com.moida.common.response.TermsResponse;
import com.moida.domain.terms.TermsDocument;
import com.moida.domain.terms.TermsDocumentRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/terms")
@RequiredArgsConstructor
public class TermsController {

    private final TermsDocumentRepository termsDocumentRepository;

    @GetMapping
    @Transactional(readOnly = true)
    public ResponseEntity<ApiResponse<List<TermsResponse>>> getTerms() {
        List<TermsResponse> terms = List.of(
                findTerms(TermsDocument.TermsType.TERMS),
                findTerms(TermsDocument.TermsType.PRIVACY)
        );
        return ResponseEntity.ok(ApiResponse.success(terms));
    }

    @GetMapping("/{type}")
    @Transactional(readOnly = true)
    public ResponseEntity<ApiResponse<TermsResponse>> getTermsByType(@PathVariable String type) {
        return ResponseEntity.ok(ApiResponse.success(findTerms(parseType(type))));
    }

    private TermsResponse findTerms(TermsDocument.TermsType type) {
        TermsDocument document = termsDocumentRepository.findByTypeAndActiveTrue(type)
                .orElseThrow(() -> new BusinessException(ErrorCode.ENTITY_NOT_FOUND, "Terms document not found."));
        return TermsResponse.from(document);
    }

    private TermsDocument.TermsType parseType(String type) {
        try {
            return TermsDocument.TermsType.valueOf(type.toUpperCase());
        } catch (IllegalArgumentException e) {
            throw new BusinessException(ErrorCode.INVALID_INPUT, "Invalid terms type.");
        }
    }
}
