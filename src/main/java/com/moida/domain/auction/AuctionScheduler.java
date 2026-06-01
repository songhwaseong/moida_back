package com.moida.domain.auction;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.util.List;

/**
 * 경매 상태 자동 전이를 담당하는 스케줄러.
 *
 * 1) closeEndedAuctions   — endAt 이 지난 LIVE 경매를 종료 처리. (낙찰자 분기는 AuctionCompletionService 위임)
 * 2) expireUnpaidAuctions — paymentDeadline 이 지난 AWAITING_PAYMENT 경매를 유찰 처리.
 *
 * 각 경매 1건씩을 AuctionCompletionService 의 @Transactional 메서드로 호출해
 * 한 건 실패가 다른 건 처리를 막지 않도록 한다.
 * (같은 클래스의 self-invocation 은 Spring AOP 프록시를 통과하지 않으므로
 *  실제 트랜잭션 경계는 반드시 별도 빈 메서드를 호출해야 한다 — 그래서 위임 구조를 채택.)
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class AuctionScheduler {

    /** 스케줄 주기: 1분. 결제 기한이 시간 단위라 분 정확도면 충분. */
    private static final long FIXED_DELAY_MS = 60_000L;

    private final AuctionRepository auctionRepository;
    private final AuctionCompletionService completionService;

    @Scheduled(fixedDelay = FIXED_DELAY_MS, initialDelay = 30_000L)
    public void closeEndedAuctions() {
        LocalDateTime now = LocalDateTime.now();
        List<Auction> targets = auctionRepository.findAllByStatusAndEndAtBefore(AuctionStatus.LIVE, now);
        if (targets.isEmpty()) return;

        log.info("[AuctionScheduler] closeEndedAuctions targets={}", targets.size());
        for (Auction a : targets) {
            try {
                completionService.closeEndedAuctionById(a.getId());
            } catch (Exception e) {
                log.error("[AuctionScheduler] closeEndedAuctions failed auctionId={}", a.getId(), e);
            }
        }
    }

    @Scheduled(fixedDelay = FIXED_DELAY_MS, initialDelay = 45_000L)
    public void expireUnpaidAuctions() {
        LocalDateTime now = LocalDateTime.now();
        List<Auction> targets = auctionRepository.findAllByStatusAndPaymentDeadlineBefore(
                AuctionStatus.AWAITING_PAYMENT, now);
        if (targets.isEmpty()) return;

        log.info("[AuctionScheduler] expireUnpaidAuctions targets={}", targets.size());
        for (Auction a : targets) {
            try {
                completionService.expireUnpaidAuction(a);
            } catch (Exception e) {
                log.error("[AuctionScheduler] expireUnpaidAuctions failed auctionId={}", a.getId(), e);
            }
        }
    }
}
