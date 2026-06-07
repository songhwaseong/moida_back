package com.moida.controller;

import com.moida.common.exception.BusinessException;
import com.moida.common.exception.ErrorCode;
import com.moida.common.request.AdminStatusUpdateRequest;
import com.moida.common.response.AdminAuctionBidResponse;
import com.moida.common.response.AdminAuctionResponse;
import com.moida.common.response.ApiResponse;
import com.moida.domain.audit.AdminActionLogService;
import com.moida.domain.auction.AdminAuctionService;
import com.moida.domain.auction.AuctionStatus;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
/**
 * 관리자 경매 관리 API.
 * SecurityConfig 에서 /api/admin/** 는 ADMIN / MANAGER 만 접근 가능하다.
 */
@RestController
@RequestMapping("/api/admin/auctions")
@RequiredArgsConstructor
public class AdminAuctionController {

    private final AdminAuctionService adminAuctionService;
    private final AdminActionLogService adminActionLogService;

    /** 경매 목록 조회 */
    @GetMapping
    public ResponseEntity<ApiResponse<List<AdminAuctionResponse>>> getAll() {
        adminActionLogService.recordView(
                "ADMIN_AUCTION_VIEW",
                "AUCTION",
                adminActionLogService.fields("view", "list")
        );
        return ResponseEntity.ok(ApiResponse.success(adminAuctionService.getAll()));
    }

    /** 입찰 내역 조회 */
    @GetMapping("/{auctionId}/bids")
    public ResponseEntity<ApiResponse<List<AdminAuctionBidResponse>>> getBids(@PathVariable Long auctionId) {
        adminActionLogService.recordView(
                "ADMIN_AUCTION_BID_VIEW",
                "AUCTION",
                adminActionLogService.fields("auctionId", auctionId)
        );
        return ResponseEntity.ok(ApiResponse.success(adminAuctionService.getBids(auctionId)));
    }

    /** 상태 변경 (body: { "status": "SUCCESS" } 형태의 enum 이름) */
    @PatchMapping("/{auctionId}/status")
    public ResponseEntity<ApiResponse<AdminAuctionResponse>> updateStatus(
            @PathVariable Long auctionId,
            @RequestBody AdminStatusUpdateRequest request) {
        String reason = requireReason(request == null ? null : request.reason());
        try {
            AuctionStatus next = AuctionStatus.valueOf(request.status());
            AdminAuctionResponse updated = adminAuctionService.updateStatus(auctionId, next, reason);
            return ResponseEntity.ok(ApiResponse.success(updated, "상태가 변경되었습니다."));
        } catch (RuntimeException e) {
            adminActionLogService.recordFailure(
                    "AUCTION_STATUS_CHANGE_FAILED",
                    "AUCTION",
                    auctionId,
                    String.valueOf(auctionId),
                    adminActionLogService.fields("requestedStatus", request == null ? null : request.status()),
                    e.getMessage()
            );
            throw e;
        }
    }

    private String requireReason(String reason) {
        if (reason == null || reason.isBlank()) {
            throw new BusinessException(ErrorCode.INVALID_INPUT, "관리자 처리 사유를 입력해야 합니다.");
        }
        return reason.trim();
    }
}
