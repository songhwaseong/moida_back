package com.moida.domain.auction;

import com.moida.common.exception.BusinessException;
import com.moida.common.exception.ErrorCode;
import com.moida.common.request.BidRequest;
import com.moida.common.response.BidResultResponse;
import com.moida.common.response.MyBidResponse;
import com.moida.domain.member.Member;
import com.moida.domain.member.MemberRepository;
import com.moida.domain.member.MemberStatus;
import com.moida.domain.notification.Notification;
import com.moida.domain.notification.NotificationService;
import com.moida.domain.product.Product;
import com.moida.domain.product.ProductImageStorageService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.data.domain.PageRequest;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Service
@RequiredArgsConstructor
@Slf4j
public class AuctionBidService {

    private final AuctionRepository auctionRepository;
    private final BidRepository bidRepository;
    private final MemberRepository memberRepository;
    private final AuctionCompletionService completionService;
    private final NotificationService notificationService;
    private final ProductImageStorageService productImageStorageService;

    @Transactional(readOnly = true)
    public List<MyBidResponse> getMyBids(Long memberId) {
        Map<Long, Bid> latestBidByAuction = new LinkedHashMap<>();
        bidRepository.findMyBidHistory(memberId, PageRequest.of(0, 100)).forEach(bid ->
                latestBidByAuction.putIfAbsent(bid.getAuction().getId(), bid)
        );

        return latestBidByAuction.values().stream()
                .map(bid -> MyBidResponse.from(bid, productImageStorageService::toPublicUrl))
                .toList();
    }

    @Transactional
    public BidResultResponse placeProductBid(Long productId, Long memberId, BidRequest request) {
        return placeProductBid(productId, memberId, request.getAmount(), Bid.BidType.NORMAL);
    }

    @Transactional
    public BidResultResponse buyNowProduct(Long productId, Long memberId) {
        return placeProductBid(productId, memberId, null, Bid.BidType.IMMEDIATE);
    }

    private BidResultResponse placeProductBid(Long productId, Long memberId, Long amount, Bid.BidType bidType) {
        Auction auction = auctionRepository.findByProductIdForUpdate(productId)
                .orElseThrow(() -> new BusinessException(ErrorCode.AUCTION_NOT_FOUND));
        Product product = auction.getProduct();

        if (bidType == Bid.BidType.IMMEDIATE) {
            if (auction.getImmediatePrice() == null) {
                throw new BusinessException(ErrorCode.INVALID_BID_AMOUNT, "즉시낙찰가가 설정되지 않은 경매입니다.");
            }
            amount = auction.getImmediatePrice();
        }
        if (amount == null) {
            throw new BusinessException(ErrorCode.INVALID_BID_AMOUNT);
        }

        if (product.isOwnedBy(memberId)) {
            throw new BusinessException(ErrorCode.SELF_BID_NOT_ALLOWED);
        }
        if (!auction.isLive()) {
            throw new BusinessException(ErrorCode.AUCTION_ALREADY_ENDED);
        }

        long minimumBid = auction.getCurrentPrice() + auction.getMinBidUnit();
        boolean immediateBid = bidType == Bid.BidType.IMMEDIATE || isImmediateBid(amount, auction.getImmediatePrice());

        if (!immediateBid && amount < minimumBid) {
            throw new BusinessException(
                    ErrorCode.INVALID_BID_AMOUNT,
                    "최소 입찰가는 " + minimumBid + "원입니다."
            );
        }
        if (immediateBid && auction.getImmediatePrice() != null && amount < auction.getImmediatePrice()) {
            throw new BusinessException(ErrorCode.INVALID_BID_AMOUNT, "즉시낙찰가보다 낮게 입찰할 수 없습니다.");
        }

        Member bidder = memberRepository.findById(memberId)
                .orElseThrow(() -> new BusinessException(ErrorCode.MEMBER_NOT_FOUND));

        // 정지된 회원은 입찰 불가 (결제 미이행 누적으로 자동 정지된 케이스 포함).
        // suspendedUntil 이 지났는데도 status 가 SUSPENDED 인 경우 — 별도 해제 잡이 돌기 전에 입찰 시도 — 는
        // suspendedUntil 시각으로 한 번 더 비교해 자연 해제분은 허용한다.
        if (bidder.getStatus() == MemberStatus.SUSPENDED
                && (bidder.getSuspendedUntil() == null
                    || java.time.LocalDateTime.now().isBefore(bidder.getSuspendedUntil()))) {
            throw new BusinessException(
                    ErrorCode.ACCESS_DENIED,
                    "입찰이 정지된 계정입니다." + (bidder.getSuspendedUntil() != null
                            ? " (해제: " + bidder.getSuspendedUntil() + ")" : "")
            );
        }

        // 지갑 예치금 잔액 검증.
        // 프론트(AuctionDetailPage)에서 모달/버튼 단계에서 1차 차단하지만,
        // API 를 직접 호출해 우회하는 경우를 막기 위해 서버에서도 동일 조건으로 재검증한다.
        // 정책: 보유 잔액보다 큰 금액은 입찰 불가 (frontend: amount > userBalance 와 동일).
        if (bidder.getBalance() < amount) {
            throw new BusinessException(
                    ErrorCode.INSUFFICIENT_BALANCE,
                    "잔액이 부족합니다. (보유: " + bidder.getBalance() + "원, 입찰: " + amount + "원)"
            );
        }

        bidRepository.findAllByAuctionIdOrderByAmountDesc(auction.getId())
                .forEach(Bid::unmarkWinning);

        auction.placeBid(amount);

        Bid savedBid = bidRepository.saveAndFlush(Bid.builder()
                .auction(auction)
                .bidder(bidder)
                .amount(amount)
                .bidType(immediateBid ? Bid.BidType.IMMEDIATE : Bid.BidType.NORMAL)
                .build());

        if (immediateBid) {
            // 잔액 충분 → 즉시 차감 + Settlement + SOLD,
            // 잔액 부족 → AWAITING_PAYMENT 전환 (입찰 단계 검증을 통과했으므로 보통은 충분하지만,
            //   동시 출금 등 극단 케이스 대비). AuctionCompletionService 가 분기를 모두 처리한다.
            completionService.finalizeWinner(auction, bidder, amount);
        }

        notifySellerBidPlaced(product, auction, bidder, amount);

        List<Bid> bidHistory = bidRepository.findHistoryByAuctionId(auction.getId());
        log.info("[AuctionBidService] bid saved productId={}, auctionId={}, bidId={}, bidderId={}, amount={}",
                productId, auction.getId(), savedBid.getId(), memberId, amount);

        return BidResultResponse.from(
                auction.getCurrentPrice(),
                auction.getBidCount(),
                auction.isLive(),
                bidHistory
        );
    }

    private boolean isImmediateBid(long amount, Long immediatePrice) {
        return immediatePrice != null && amount >= immediatePrice;
    }

    private void notifySellerBidPlaced(Product product, Auction auction, Member bidder, long amount) {
        Member seller = product.getSeller();
        if (seller == null) return;

        notificationService.createAndPush(
                seller,
                Notification.NotificationType.BID_PLACED,
                "새 입찰이 들어왔어요",
                String.format("'%s' 상품에 %s님이 %,d원으로 입찰했습니다.",
                        product.getName(), bidder.getName(), amount),
                "/auctions/" + auction.getId()
        );
    }
}
