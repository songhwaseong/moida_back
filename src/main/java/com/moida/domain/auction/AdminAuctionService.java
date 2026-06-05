package com.moida.domain.auction;

import com.moida.common.exception.BusinessException;
import com.moida.common.exception.ErrorCode;
import com.moida.common.response.AdminAuctionBidResponse;
import com.moida.common.response.AdminAuctionResponse;
import com.moida.domain.audit.AdminActionLogService;
import com.moida.domain.member.Member;
import com.moida.domain.notification.Notification;
import com.moida.domain.notification.NotificationService;
import com.moida.domain.product.Product;
import com.moida.domain.product.ProductStatus;
import com.moida.domain.settlement.FeeRule;
import com.moida.domain.settlement.FeeRuleRepository;
import com.moida.domain.settlement.Settlement;
import com.moida.domain.settlement.SettlementRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

/**
 * 관리자 경매 관리 전용 서비스.
 * 일반 경매/입찰 흐름과 분리해 목록 조회·입찰 내역·관리자 권한 상태 전이만 담당한다.
 *
 * 상태 전이 규칙:
 *   LIVE        → SUCCESS  : 최고가 입찰을 winner 로 지정 (입찰이 없으면 거부)
 *   LIVE/READY  → FAILED   : 유찰 처리
 *   any         → CANCELED : 강제 취소
 *   READY       → LIVE     : 시작 (이미 LIVE 면 noop)
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class AdminAuctionService {

    private final AuctionRepository auctionRepository;
    private final BidRepository bidRepository;
    private final SettlementRepository settlementRepository;
    private final FeeRuleRepository feeRuleRepository;
    private final NotificationService notificationService;
    private final AuctionCompletionService completionService;
    private final AdminActionLogService adminActionLogService;

    /** 전체 경매 목록 (최신순) */
    @Transactional(readOnly = true)
    public List<AdminAuctionResponse> getAll() {
        return auctionRepository.findAllForAdmin().stream()
                .map(AdminAuctionResponse::from)
                .toList();
    }

    /** 입찰 내역 (금액 내림차순) */
    @Transactional(readOnly = true)
    public List<AdminAuctionBidResponse> getBids(Long auctionId) {
        if (!auctionRepository.existsById(auctionId)) {
            throw new BusinessException(ErrorCode.AUCTION_NOT_FOUND);
        }
        return bidRepository.findHistoryByAuctionId(auctionId).stream()
                .map(AdminAuctionBidResponse::from)
                .toList();
    }

    /** 관리자 상태 변경 */
    @Transactional
    public AdminAuctionResponse updateStatus(Long auctionId, AuctionStatus next) {
        Auction auction = auctionRepository.findById(auctionId)
                .orElseThrow(() -> new BusinessException(ErrorCode.AUCTION_NOT_FOUND));
        AuctionStatus previousStatus = auction.getStatus();
        ProductStatus previousProductStatus = auction.getProduct().getStatus();

        switch (next) {
            case LIVE -> {
                if (auction.getStatus() != AuctionStatus.LIVE) {
                    auction.start();
                    auction.getProduct().changeStatus(ProductStatus.LIVE);
                    notifySeller(
                            auction.getProduct(),
                            Notification.NotificationType.PRODUCT_AUCTION_STARTED,
                            "경매가 시작됐어요",
                            String.format("'%s' 상품 경매가 시작됐습니다. 경매 종료 전까지 입찰 현황을 확인해보세요.",
                                    auction.getProduct().getName()),
                            "/auctions/" + auction.getId()
                    );
                }
            }
            case SUCCESS -> {
                Bid top = bidRepository.findFirstByAuctionIdOrderByAmountDesc(auctionId)
                        .orElseThrow(() -> new BusinessException(ErrorCode.INVALID_INPUT,
                                "입찰 내역이 없어 낙찰 처리할 수 없습니다."));
                completionService.finalizeWinner(auction, top.getBidder(), top.getAmount());
            }
            case FAILED -> {
                auction.close(null, null);
                Product product = auction.getProduct();
                product.changeStatus(ProductStatus.FAILED);
                if (previousStatus != AuctionStatus.FAILED) {
                    notifySeller(
                            product,
                            Notification.NotificationType.PRODUCT_AUCTION_FAILED,
                            "경매가 유찰됐어요",
                            String.format("[%s] 경매가 유찰 처리되었습니다. 필요 시 재등록해주세요.", product.getName()),
                            "/auctions/" + auction.getId()
                    );
                }
            }
            case CANCELED -> auction.cancel();
            case READY -> throw new BusinessException(ErrorCode.INVALID_INPUT,
                    "READY 상태로의 변경은 허용되지 않습니다.");
        }
        adminActionLogService.record(
                "AUCTION_STATUS_CHANGE",
                "AUCTION",
                auction.getId(),
                auction.getProduct().getName(),
                adminActionLogService.fields(
                        "auctionStatus", previousStatus,
                        "productStatus", previousProductStatus
                ),
                adminActionLogService.fields(
                        "auctionStatus", auction.getStatus(),
                        "productStatus", auction.getProduct().getStatus(),
                        "winnerId", auction.getWinner() == null ? null : auction.getWinner().getId(),
                        "winningPrice", auction.getWinningPrice()
                ),
                "경매 상태 변경"
        );
        return AdminAuctionResponse.from(auction);
    }

    private void notifySeller(Product product, Notification.NotificationType type,
                              String title, String content, String linkUrl) {
        Member seller = product.getSeller();
        if (seller == null) return;
        notificationService.createAndPush(seller, type, title, content, linkUrl);
    }

    /**
     * 낙찰 시 정산 row 자동 생성.
     * 이미 같은 경매에 정산이 있으면 중복 생성하지 않는다(낙찰 → 보류 → 다시 낙찰 시도 등의 시나리오 대비).
     */
    private void ensureSettlement(Auction auction, Member buyer, long salesAmount) {
        if (settlementRepository.existsByAuctionId(auction.getId())) {
            return;
        }
        Member seller = auction.getProduct().getSeller();
        double feeRate = calcFeeRate(salesAmount);
        long feeAmount = Math.round(salesAmount * feeRate / 100.0);

        Settlement settlement = Settlement.builder()
                .auction(auction)
                .seller(seller)
                .buyer(buyer)
                .salesAmount(salesAmount)
                .feeAmount(feeAmount)
                .feeRate(feeRate)
                .build();
        settlementRepository.save(settlement);
        log.info("[AdminAuctionService] settlement created auctionId={}, sales={}, feeRate={}%, fee={}",
                auction.getId(), salesAmount, feeRate, feeAmount);
    }

    /**
     * 낙찰가에 적용할 수수료율(%) 산정.
     * fee_rules 테이블에서 minAmount ≤ salesAmount 인 정책 중 minAmount 가 가장 큰 것을 사용한다.
     * 정책이 비어 있는 환경(시드 실패 등)에서는 안전한 기본값 5% 로 폴백한다.
     */
    private double calcFeeRate(long salesAmount) {
        List<FeeRule> rules = feeRuleRepository.findAllByOrderByMinAmountAsc();
        double applied = 5.0;
        for (FeeRule rule : rules) {
            if (rule.getMinAmount() <= salesAmount) {
                applied = rule.getFeeRate();
            } else {
                break; // minAmount ASC 이므로 더 볼 필요 없음
            }
        }
        return applied;
    }
}
