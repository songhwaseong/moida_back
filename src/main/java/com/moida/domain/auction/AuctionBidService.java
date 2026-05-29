package com.moida.domain.auction;

import com.moida.common.exception.BusinessException;
import com.moida.common.exception.ErrorCode;
import com.moida.common.request.BidRequest;
import com.moida.common.response.BidResultResponse;
import com.moida.common.response.MyBidResponse;
import com.moida.domain.member.Member;
import com.moida.domain.member.MemberRepository;
import com.moida.domain.product.Product;
import com.moida.domain.product.ProductStatus;
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

    @Transactional(readOnly = true)
    public List<MyBidResponse> getMyBids(Long memberId) {
        Map<Long, Bid> latestBidByAuction = new LinkedHashMap<>();
        bidRepository.findMyBidHistory(memberId, PageRequest.of(0, 100)).forEach(bid ->
                latestBidByAuction.putIfAbsent(bid.getAuction().getId(), bid)
        );

        return latestBidByAuction.values().stream()
                .map(MyBidResponse::from)
                .toList();
    }

    @Transactional
    public BidResultResponse placeProductBid(Long productId, Long memberId, BidRequest request) {
        return placeProductBid(productId, memberId, request.getAmount(), Bid.BidType.NORMAL);
    }

    @Transactional
    public BidResultResponse buyNowProduct(Long productId, Long memberId) {
        Auction auction = auctionRepository.findByProductId(productId)
                .orElseThrow(() -> new BusinessException(ErrorCode.AUCTION_NOT_FOUND));
        if (auction.getImmediatePrice() == null) {
            throw new BusinessException(ErrorCode.INVALID_BID_AMOUNT, "즉시낙찰가가 설정되지 않은 경매입니다.");
        }
        return placeProductBid(productId, memberId, auction.getImmediatePrice(), Bid.BidType.IMMEDIATE);
    }

    private BidResultResponse placeProductBid(Long productId, Long memberId, Long amount, Bid.BidType bidType) {
        Auction auction = auctionRepository.findByProductId(productId)
                .orElseThrow(() -> new BusinessException(ErrorCode.AUCTION_NOT_FOUND));
        Product product = auction.getProduct();

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
            auction.close(bidder, amount);
            product.changeStatus(ProductStatus.SOLD);
        }

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
}
