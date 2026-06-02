package com.moida.controller;

import com.moida.common.request.InquiryRequest;
import com.moida.common.response.ApiResponse;
import com.moida.common.response.InquiryResponse;
import com.moida.domain.inquiry.InquiryService;
import com.moida.security.CustomUserDetails;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/products/{productId}/inquiries")
@RequiredArgsConstructor
public class InquiryController {

    private final InquiryService inquiryService;

    @GetMapping
    public ResponseEntity<ApiResponse<List<InquiryResponse>>> getProductInquiries(
            @PathVariable Long productId,
            @AuthenticationPrincipal CustomUserDetails userDetails
    ) {
        Long memberId = userDetails != null ? userDetails.getMemberId() : null;
        return ResponseEntity.ok(ApiResponse.success(inquiryService.getProductInquiries(productId, memberId)));
    }

    @PostMapping
    public ResponseEntity<ApiResponse<InquiryResponse>> createProductInquiry(
            @PathVariable Long productId,
            @Valid @RequestBody InquiryRequest request,
            @AuthenticationPrincipal CustomUserDetails userDetails
    ) {
        InquiryResponse inquiry = inquiryService.createProductInquiry(
                productId,
                userDetails.getMemberId(),
                request
        );
        return ResponseEntity.ok(ApiResponse.success(inquiry, "Inquiry has been created."));
    }
}
