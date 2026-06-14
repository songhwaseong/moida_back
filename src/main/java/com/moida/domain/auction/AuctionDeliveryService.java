package com.moida.domain.auction;

import com.moida.common.exception.BusinessException;
import com.moida.common.exception.ErrorCode;
import com.moida.common.response.PurchaseHistoryResponse;
import com.moida.domain.member.Member;
import com.moida.domain.notification.Notification;
import com.moida.domain.notification.NotificationService;
import com.moida.domain.product.Product;
import com.moida.domain.product.ProductImageStorageService;
import com.moida.domain.review.ReviewRepository;
import com.moida.domain.settlement.Settlement;
import com.moida.domain.settlement.SettlementRepository;
import com.moida.domain.wallet.WalletService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class AuctionDeliveryService {

    private final AuctionRepository auctionRepository;
    private final SettlementRepository settlementRepository;
    private final NotificationService notificationService;
    private final WalletService walletService;
    private final ProductImageStorageService productImageStorageService;
    private final ReviewRepository reviewRepository;

    @Transactional(readOnly = true)
    public List<PurchaseHistoryResponse> getMyPurchases(Long memberId) {
        List<Auction> auctions = auctionRepository.findSuccessfulPurchasesByWinnerId(memberId);
        if (auctions.isEmpty()) {
            return List.of();
        }

        List<Long> auctionIds = auctions.stream().map(Auction::getId).toList();
        Map<Long, Settlement> settlementByAuctionId = settlementRepository.findAllByAuctionIdIn(auctionIds).stream()
                .collect(Collectors.toMap(s -> s.getAuction().getId(), s -> s));

        List<Long> productIds = auctions.stream().map(auction -> auction.getProduct().getId()).toList();
        Set<Long> reviewedProductIds = Set.copyOf(reviewRepository.findReviewedProductIds(memberId, productIds));

        return auctions.stream()
                .map(auction -> PurchaseHistoryResponse.from(
                        auction,
                        settlementByAuctionId.get(auction.getId()),
                        reviewedProductIds.contains(auction.getProduct().getId()),
                        productImageStorageService::toPublicUrl
                ))
                .toList();
    }

    @Transactional
    public void advanceDeliveryById(Long auctionId) {
        Auction auction = auctionRepository.findById(auctionId)
                .orElseThrow(() -> new BusinessException(ErrorCode.AUCTION_NOT_FOUND));
        DeliveryStatus status = auction.getDeliveryStatus();
        if (auction.getStatus() != AuctionStatus.SUCCESS || status == null) {
            return;
        }

        Product product = auction.getProduct();
        Member buyer = auction.getWinner();
        if (buyer == null) {
            return;
        }

        switch (status) {
            case PAYMENT_COMPLETED -> {
                auction.updateDeliveryStatus(DeliveryStatus.SHIPMENT_NOTICE);
                notifyBuyer(buyer, product, Notification.NotificationType.DELIVERY_SHIPPED,
                        "상품이 발송됐어요",
                        String.format("[%s] 상품이 발송되었습니다. 곧 배송이 시작됩니다.", product.getName()),
                        DeliveryStatus.SHIPMENT_NOTICE);
            }
            case SHIPMENT_NOTICE -> {
                auction.updateDeliveryStatus(DeliveryStatus.SHIPPING);
                notifyBuyer(buyer, product, Notification.NotificationType.DELIVERY_IN_TRANSIT,
                        "상품이 배송중이에요",
                        String.format("[%s] 상품이 배송중입니다.", product.getName()),
                        DeliveryStatus.SHIPPING);
            }
            case SHIPPING -> {
                auction.updateDeliveryStatus(DeliveryStatus.DELIVERED);
                notifyBuyer(buyer, product, Notification.NotificationType.DELIVERY_DELIVERED,
                        "배송이 완료됐어요",
                        String.format("[%s] 배송이 완료되었습니다. 구매내역에서 수령확인을 눌러주세요.", product.getName()),
                        DeliveryStatus.DELIVERED);
            }
            default -> {
            }
        }
        log.info("[AuctionDelivery] advanced auctionId={}, status={}", auctionId, auction.getDeliveryStatus());
    }

    @Transactional
    public void confirmReceipt(Long productId, Long memberId) {
        Auction auction = auctionRepository.findByProductId(productId)
                .orElseThrow(() -> new BusinessException(ErrorCode.AUCTION_NOT_FOUND));
        if (auction.getStatus() != AuctionStatus.SUCCESS) {
            throw new BusinessException(ErrorCode.INVALID_INPUT, "결제 완료된 낙찰 상품만 수령확인할 수 있습니다.");
        }
        Member buyer = auction.getWinner();
        if (buyer == null || !buyer.getId().equals(memberId)) {
            throw new BusinessException(ErrorCode.ACCESS_DENIED, "낙찰자만 수령확인할 수 있습니다.");
        }
        auction.confirmReceipt();

        Settlement settlement = settlementRepository.findByAuctionId(auction.getId())
                .orElseThrow(() -> new BusinessException(ErrorCode.ENTITY_NOT_FOUND, "정산 정보가 없습니다."));
        settlement.payToSeller();

        Product product = auction.getProduct();
        Member seller = product.getSeller();

        // 정산 지급액을 판매자 계좌이력(입금)으로 기록한다. (payToSeller 가 잔액을 이미 올렸으므로 내역만 추가)
        walletService.recordSettlementCredit(
                seller,
                settlement.getSettledAmount(),
                String.format("판매 정산금 입금 - %s", product.getName())
        );
        notificationService.createAndPush(
                seller,
                Notification.NotificationType.SETTLEMENT_PAID,
                "정산이 완료됐어요",
                String.format("[%s] 수령확인이 완료되어 %,d원이 정산되었습니다. 수수료 %,d원이 제외되었습니다.",
                        product.getName(), settlement.getSettledAmount(), settlement.getFeeAmount()),
                "/auctions/" + product.getId()
        );
        notificationService.createAndPush(
                buyer,
                Notification.NotificationType.RECEIPT_CONFIRMED,
                "수령확인이 완료됐어요",
                String.format("[%s] 수령확인이 완료되었습니다.", product.getName()),
                "/my/purchases"
        );
        log.info("[AuctionDelivery] receipt confirmed auctionId={}, buyerId={}, sellerId={}, settledAmount={}",
                auction.getId(), buyer.getId(), seller.getId(), settlement.getSettledAmount());
    }

    private void notifyBuyer(Member buyer, Product product, Notification.NotificationType type,
                             String title, String content, DeliveryStatus status) {
        notificationService.createAndPush(
                buyer,
                type,
                title,
                content,
                trackingLink(product, status)
        );
    }

    private String trackingLink(Product product, DeliveryStatus status) {
        String carrierCode = hasText(product.getCarrierCode()) ? product.getCarrierCode() : "04";
        String trackingNo = hasText(product.getTrackingNo()) ? product.getTrackingNo() : demoTrackingNo(product.getId());
        return "/my/tracking?carrier=" + carrierCode
                + "&invoice=" + trackingNo
                + "&status=" + status.name()
                + "&product=" + URLEncoder.encode(product.getName(), StandardCharsets.UTF_8);
    }

    private String demoTrackingNo(Long productId) {
        long seed = productId == null ? 0L : Math.abs(productId);
        return String.format("900%010d", seed % 10_000_000_000L);
    }

    private boolean hasText(String value) {
        return value != null && !value.isBlank();
    }
}
