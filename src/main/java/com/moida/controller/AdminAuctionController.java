package com.moida.controller;

import com.moida.common.response.AdminAuctionBidResponse;
import com.moida.common.response.AdminAuctionResponse;
import com.moida.common.response.ApiResponse;
import com.moida.domain.auction.AdminAuctionService;
import com.moida.domain.auction.AuctionStatus;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

/**
 * 관리자 경매 관리 API.
 * SecurityConfig 에서 /api/admin/** 는 ADMIN / MANAGER 만 접근 가능하다.
 */
@RestController
@RequestMapping("/api/admin/auctions")
@RequiredArgsConstructor
public class AdminAuctionController {

    private final AdminAuctionService adminAuctionService;

    /** 경매 목록 조회 */
    @GetMapping
    public ResponseEntity<ApiResponse<List<AdminAuctionResponse>>> getAll() {
        return ResponseEntity.ok(ApiResponse.success(adminAuctionService.getAll()));
    }

    /** 입찰 내역 조회 */
    @GetMapping("/{auctionId}/bids")
    public ResponseEntity<ApiResponse<List<AdminAuctionBidResponse>>> getBids(@PathVariable Long auctionId) {
        return ResponseEntity.ok(ApiResponse.success(adminAuctionService.getBids(auctionId)));
    }

    /** 상태 변경 (body: { "status": "SUCCESS" } 형태의 enum 이름) */
    @PatchMapping("/{auctionId}/status")
    public ResponseEntity<ApiResponse<AdminAuctionResponse>> updateStatus(
            @PathVariable Long auctionId,
            @RequestBody Map<String, String> body) {
        AuctionStatus next = AuctionStatus.valueOf(body.get("status"));
        AdminAuctionResponse updated = adminAuctionService.updateStatus(auctionId, next);
        return ResponseEntity.ok(ApiResponse.success(updated, "상태가 변경되었습니다."));
    }
}
