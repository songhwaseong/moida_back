package com.moida.common.response;

import com.moida.domain.auction.Auction;
import com.moida.domain.auction.DeliveryStatus;
import com.moida.domain.product.Product;
import com.moida.domain.settlement.Settlement;

import java.time.format.DateTimeFormatter;

public record PurchaseHistoryResponse(
        Long productId,
        Long auctionId,
        String productNo,
        String auctionNo,
        String name,
        String image,
        String category,
        Long winningPrice,
        String auctionStatus,
        String deliveryStatus,
        String deliveryStatusLabel,
        String settlementStatus,
        Long feeAmount,
        Long settledAmount,
        Boolean canConfirmReceipt,
        String purchasedAt,
        String deliveredAt,
        String receivedAt
) {
    private static final DateTimeFormatter FORMATTER = DateTimeFormatter.ofPattern("yyyy.MM.dd HH:mm");

    public static PurchaseHistoryResponse from(Auction auction, Settlement settlement) {
        Product product = auction.getProduct();
        DeliveryStatus deliveryStatus = auction.getDeliveryStatus();
        boolean canConfirmReceipt = deliveryStatus == DeliveryStatus.DELIVERED
                && settlement != null
                && settlement.getStatus() == Settlement.SettlementStatus.PENDING;

        return new PurchaseHistoryResponse(
                product.getId(),
                auction.getId(),
                product.getProductNo(),
                auction.getAuctionNo(),
                product.getName(),
                product.getMainImageUrl(),
                product.getCategory().getName(),
                auction.getWinningPrice(),
                auction.getStatus().name(),
                deliveryStatus == null ? null : deliveryStatus.name(),
                deliveryStatusLabel(deliveryStatus),
                settlement == null ? null : settlement.getStatus().name(),
                settlement == null ? null : settlement.getFeeAmount(),
                settlement == null ? null : settlement.getSettledAmount(),
                canConfirmReceipt,
                auction.getUpdatedAt() == null ? null : auction.getUpdatedAt().format(FORMATTER),
                auction.getDeliveryStatusUpdatedAt() == null ? null : auction.getDeliveryStatusUpdatedAt().format(FORMATTER),
                auction.getReceivedAt() == null ? null : auction.getReceivedAt().format(FORMATTER)
        );
    }

    private static String deliveryStatusLabel(DeliveryStatus status) {
        if (status == null) {
            return "결제완료";
        }
        return switch (status) {
            case PAYMENT_COMPLETED -> "결제완료";
            case SHIPMENT_NOTICE -> "발송알림";
            case SHIPPING -> "배송중";
            case DELIVERED -> "배송완료";
            case RECEIVED -> "수령확인";
        };
    }
}
