package com.moida.domain.product;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.util.List;

/**
 * 상품 상태 자동 전이 스케줄러.
 *
 * activateScheduledProducts — 경매예정(SCHEDULED) 상품이 예약 시각(auctionScheduledAt, 승인 후 24h)을
 *                             넘기면 자동으로 LIVE(경매중) 로 전환한다.
 *
 * 관리자가 미리 수동으로 LIVE 로 올린 상품은 SCHEDULED 가 아니므로 조회되지 않아 중복 전환되지 않는다.
 * 한 건 실패가 다른 건 처리를 막지 않도록 AdminProductService 의 @Transactional 메서드에 건별로 위임한다.
 * (같은 클래스 self-invocation 은 프록시를 통과하지 않으므로 별도 빈 메서드 호출이 필요하다.)
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class ProductScheduler {

    /** 스케줄 주기: 1분. 자동 전환이 분 단위 정확도면 충분. */
    private static final long FIXED_DELAY_MS = 60_000L;

    private final ProductRepository productRepository;
    private final AdminProductService adminProductService;

    @Scheduled(fixedDelay = FIXED_DELAY_MS, initialDelay = 60_000L)
    public void activateScheduledProducts() {
        LocalDateTime now = LocalDateTime.now();
        List<Product> targets = productRepository.findAllByStatusAndAuctionScheduledAtBefore(
                ProductStatus.SCHEDULED, now);
        if (targets.isEmpty()) return;

        log.info("[ProductScheduler] activateScheduledProducts targets={}", targets.size());
        for (Product p : targets) {
            try {
                adminProductService.activateScheduledProduct(p.getId());
            } catch (Exception e) {
                log.error("[ProductScheduler] activate failed productId={}", p.getId(), e);
            }
        }
    }
}
