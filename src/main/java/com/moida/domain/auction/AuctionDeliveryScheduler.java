package com.moida.domain.auction;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.util.List;

@Slf4j
@Component
@RequiredArgsConstructor
public class AuctionDeliveryScheduler {

    private static final long FIXED_DELAY_MS = 10_000L;
    private static final long DEMO_STEP_SECONDS = 20L;

    private final AuctionRepository auctionRepository;
    private final AuctionDeliveryService deliveryService;

    @Scheduled(fixedDelay = FIXED_DELAY_MS, initialDelay = 10_000L)
    public void advanceDemoDeliveries() {
        LocalDateTime threshold = LocalDateTime.now().minusSeconds(DEMO_STEP_SECONDS);
        for (DeliveryStatus status : List.of(
                DeliveryStatus.PAYMENT_COMPLETED,
                DeliveryStatus.SHIPMENT_NOTICE,
                DeliveryStatus.SHIPPING
        )) {
            List<Auction> targets = auctionRepository.findAllByStatusAndDeliveryStatusAndDeliveryStatusUpdatedAtBefore(
                    AuctionStatus.SUCCESS, status, threshold);
            if (targets.isEmpty()) {
                continue;
            }
            log.info("[AuctionDeliveryScheduler] status={} targets={}", status, targets.size());
            for (Auction auction : targets) {
                try {
                    deliveryService.advanceDeliveryById(auction.getId(), status);
                } catch (Exception e) {
                    log.error("[AuctionDeliveryScheduler] advance failed auctionId={}", auction.getId(), e);
                }
            }
        }
    }
}
