package com.moida.controller;

import com.moida.common.response.ApiResponse;
import com.moida.common.response.InquiryResponse;
import com.moida.domain.inquiry.InquiryService;
import com.moida.security.CustomUserDetails;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/inquiries")
@RequiredArgsConstructor
public class MyInquiryController {

    private final InquiryService inquiryService;

    @GetMapping("/me")
    public ResponseEntity<ApiResponse<List<InquiryResponse>>> getMyInquiries(
            @AuthenticationPrincipal CustomUserDetails userDetails
    ) {
        return ResponseEntity.ok(ApiResponse.success(inquiryService.getMyInquiries(userDetails.getMemberId())));
    }
}
