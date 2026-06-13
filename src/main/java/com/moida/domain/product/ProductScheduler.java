package com.moida.domain.product;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.util.List;

/**
 * 상품 상태 자동 전이 스케줄러.
 *
 * activateScheduledProducts — 기본 비활성화된 자동 시작 스케줄러.
 *                             운영 정책상 경매 시작은 관리자 화면에서 수동 처리한다.
 *
 * 관리자가 미리 수동으로 LIVE 로 올린 상품은 SCHEDULED 가 아니므로 조회되지 않아 중복 전환되지 않는다.
 * 한 건 실패가 다른 건 처리를 막지 않도록 AdminProductService 의 @Transactional 메서드에 건별로 위임한다.
 * (같은 클래스 self-invocation 은 프록시를 통과하지 않으므로 별도 빈 메서드 호출이 필요하다.)
 */
@Slf4j
@Component
@ConditionalOnProperty(prefix = "moida.auctions.auto-start", name = "enabled", havingValue = "true")
@RequiredArgsConstructor
public class ProductScheduler {

    /** 스케줄 주기: 10초. 테스트 환경에서 명시적으로 켠 경우에만 사용한다. */
    private static final long FIXED_DELAY_MS = 10_000L;

    private final ProductRepository productRepository;
    private final AdminProductService adminProductService;

    @Scheduled(fixedDelay = FIXED_DELAY_MS, initialDelay = 10_000L)
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
