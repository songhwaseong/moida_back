package com.moida.controller;

import com.moida.common.request.AuctionPolicyUpdateRequest;
import com.moida.common.response.ApiResponse;
import com.moida.common.response.AuctionPolicyResponse;
import com.moida.domain.auction.AuctionPolicyService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

/**
 * 관리자 경매 정책 API (경매 기본 진행 기간).
 * SecurityConfig 의 /api/admin/** 규칙에 의해 ADMIN/MANAGER 만 접근 가능.
 */
@RestController
@RequestMapping("/api/admin/auction-policy")
@RequiredArgsConstructor
public class AdminAuctionPolicyController {

    private final AuctionPolicyService auctionPolicyService;

    @GetMapping
    public ResponseEntity<ApiResponse<AuctionPolicyResponse>> get() {
        return ResponseEntity.ok(ApiResponse.success(auctionPolicyService.get()));
    }

    @PatchMapping
    public ResponseEntity<ApiResponse<AuctionPolicyResponse>> update(
            @RequestBody AuctionPolicyUpdateRequest request) {
        AuctionPolicyResponse updated = auctionPolicyService.update(request);
        return ResponseEntity.ok(ApiResponse.success(updated, "경매 기본 기간이 수정되었습니다."));
    }
}
