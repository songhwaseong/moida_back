package com.moida.domain.auction;

import com.moida.common.exception.BusinessException;
import com.moida.common.exception.ErrorCode;
import com.moida.domain.member.Member;
import com.moida.domain.member.MemberRepository;
import com.moida.domain.notification.Notification;
import com.moida.domain.notification.NotificationService;
import com.moida.domain.product.Product;
import com.moida.domain.product.ProductStatus;
import com.moida.domain.sanction.Sanction;
import com.moida.domain.sanction.SanctionRepository;
import com.moida.domain.settlement.FeeRule;
import com.moida.domain.settlement.FeeRuleRepository;
import com.moida.domain.settlement.Settlement;
import com.moida.domain.settlement.SettlementRepository;
import com.moida.domain.wallet.WalletService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

/**
 * 경매 종료 → 낙찰 확정 → 결제 → 정산 의 전체 사이클을 담당하는 서비스.
 *
 * 핵심 흐름:
 *   1) finalizeWinner(auction, winner, amount)
 *      - winner.balance ≥ amount  → 즉시 차감 + Settlement 생성 + SOLD + AUCTION_WON 알림
 *      - winner.balance < amount  → AWAITING_PAYMENT 전환 + paymentDeadline 설정 + AUCTION_WON_PAYMENT_REQUIRED 알림
 *   2) payForWinningAuction(auctionId, memberId)  (사용자가 충전 후 결제 트리거)
 *      - 잔액 검증 → 차감 + Settlement 생성 + SUCCESS + PAYMENT_COMPLETED 알림
 *   3) expireUnpaidAuction(auction)  (스케줄러가 호출)
 *      - 유찰 처리 + 낙찰자 nonPaymentCount++ + (3회 누적 시 Sanction SUSPEND_7 자동 발급)
 *      - 낙찰자/판매자 양쪽에 AUCTION_FAILED_BY_NONPAYMENT 알림
 *
 * 정책 상수는 한 곳에 모아두어 차후 운영 정책 변경 시 한 줄만 바꾸면 되도록 한다.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class AuctionCompletionService {

    /** 낙찰 후 결제 대기 기한 (시간). */
    public static final int PAYMENT_DEADLINE_HOURS = 48;

    /** 미결제 누적 임계치. 도달 시 SUSPEND_7 자동 발급. */
    public static final int NON_PAYMENT_SANCTION_THRESHOLD = 3;

    /** 자동 제재 정지 기간 (일). */
    public static final int AUTO_SUSPEND_DAYS = 7;

    /** FeeRule 이 비어 있을 때 안전 폴백 수수료율(%). */
    private static final double DEFAULT_FEE_RATE = 5.0;

    private final AuctionRepository auctionRepository;
    private final BidRepository bidRepository;
    private final MemberRepository memberRepository;
    private final SettlementRepository settlementRepository;
    private final FeeRuleRepository feeRuleRepository;
    private final NotificationService notificationService;
    private final SanctionRepository sanctionRepository;
    private final WalletService walletService;

    // ================================================================
    // 1) 낙찰 확정
    // ================================================================

    /**
     * 경매 종료 시점에 호출. 잔액에 따라 즉시 정산 또는 결제 대기로 분기한다.
     * 호출 책임: 이미 winner 와 winningAmount 가 결정된 상태(=최고가 입찰자)에서 호출되어야 한다.
     *
     * @return 즉시 결제 분기로 진행됐는지 여부. true 면 SUCCESS, false 면 AWAITING_PAYMENT.
     */
    @Transactional
    public boolean finalizeWinner(Auction auction, Member winner, long winningAmount) {
        Product product = auction.getProduct();

        // 낙찰 확정 시점에 판매자에게 1회 알림. (즉시낙찰/일반낙찰 공통 경로이며, 잔액 충분/부족 분기 이전)
        // 결제 완료 여부와 무관하게 "낙찰됐다"는 사실 자체를 판매자에게 전달한다.
        Member seller = product.getSeller();
        if (seller != null) {
            createNotification(
                    seller,
                    Notification.NotificationType.PRODUCT_SOLD,
                    "상품이 낙찰됐어요",
                    String.format("[%s] 상품이 %s원에 낙찰되었습니다.",
                            product.getName(), formatAmount(winningAmount)),
                    "/my/products"
            );
        }

        if (winner.getBalance() >= winningAmount) {
            // 잔액 충분 → 즉시 결제 처리
            settleImmediately(auction, winner, winningAmount, product);
            return true;
        } else {
            // 잔액 부족 → 결제 대기로 전환
            LocalDateTime deadline = LocalDateTime.now().plusHours(PAYMENT_DEADLINE_HOURS);
            auction.markAwaitingPayment(winner, winningAmount, deadline);
            // product 상태는 SOLD 가 아니라 그대로 둔다(결제 완료 시점에 SOLD).
            // 단, 다른 사용자에게 LIVE 로 보이지는 않아야 하므로 HIDDEN 으로 처리.
            product.changeStatus(ProductStatus.HIDDEN);

            createNotification(
                    winner,
                    Notification.NotificationType.AUCTION_WON_PAYMENT_REQUIRED,
                    "낙찰되었어요! 결제를 진행해주세요",
                    String.format("[%s] %s원에 낙찰되었습니다. %d시간 내(%s까지) 잔액을 충전하고 결제해주세요. 미결제 시 유찰 처리됩니다.",
                            product.getName(), formatAmount(winningAmount), PAYMENT_DEADLINE_HOURS, deadline),
                    "/auctions/" + auction.getId()
            );

            log.info("[AuctionCompletion] AWAITING_PAYMENT auctionId={}, winnerId={}, amount={}, deadline={}",
                    auction.getId(), winner.getId(), winningAmount, deadline);
            return false;
        }
    }

    // ================================================================
    // 2) 결제 트리거 (사용자가 충전 후 호출)
    // ================================================================

    /**
     * 결제 대기 상태인 경매에 대해 본인이 잔액 차감하여 결제를 완료한다.
     * 비-AWAITING_PAYMENT 상태이거나 본인이 낙찰자가 아니면 거부.
     */
    @Transactional
    public void payForWinningAuction(Long auctionId, Long memberId) {
        Auction auction = auctionRepository.findById(auctionId)
                .orElseThrow(() -> new BusinessException(ErrorCode.AUCTION_NOT_FOUND));

        if (auction.getStatus() != AuctionStatus.AWAITING_PAYMENT) {
            throw new BusinessException(ErrorCode.INVALID_INPUT, "결제 대기 상태가 아닌 경매입니다.");
        }
        if (auction.getWinner() == null || !auction.getWinner().getId().equals(memberId)) {
            throw new BusinessException(ErrorCode.ACCESS_DENIED, "낙찰자만 결제할 수 있습니다.");
        }
        if (LocalDateTime.now().isAfter(auction.getPaymentDeadline())) {
            // 기한 만료. 스케줄러가 처리하기 전에 사용자가 클릭한 경우 — 명시적 에러로 안내.
            throw new BusinessException(ErrorCode.INVALID_INPUT, "결제 기한이 만료되었습니다.");
        }

        // pessimistic lock 으로 잔액 재조회 — 충전/출금/타 결제와의 동시성 보호.
        Member winner = memberRepository.findByIdForUpdate(memberId)
                .orElseThrow(() -> new BusinessException(ErrorCode.MEMBER_NOT_FOUND));

        long amount = auction.getWinningPrice();
        if (winner.getBalance() < amount) {
            throw new BusinessException(
                    ErrorCode.INSUFFICIENT_BALANCE,
                    "잔액이 부족합니다. (보유: " + winner.getBalance() + "원, 결제: " + amount + "원)"
            );
        }

        Product product = auction.getProduct();
        settleImmediately(auction, winner, amount, product);
        log.info("[AuctionCompletion] payment completed auctionId={}, memberId={}, amount={}",
                auctionId, memberId, amount);
    }

    // ================================================================
    // 2-1) 경매 시간 종료 → 낙찰자 결정 (스케줄러가 1건씩 호출)
    // ================================================================

    /**
     * endAt 이 지난 LIVE 경매 1건을 처리한다.
     * 입찰 있음 → finalizeWinner() 로 위임 (잔액 분기)
     * 입찰 없음 → 그 자리에서 FAILED.
     * 별도 트랜잭션으로 두어 다중 건 배치 중 한 건 실패가 다른 건을 막지 않게 한다.
     */
    @Transactional
    public void closeEndedAuctionById(Long auctionId) {
        Auction auction = auctionRepository.findById(auctionId).orElse(null);
        if (auction == null || auction.getStatus() != AuctionStatus.LIVE) return;
        if (auction.getEndAt() != null && LocalDateTime.now().isBefore(auction.getEndAt())) return;

        bidRepository.findFirstByAuctionIdOrderByAmountDesc(auctionId)
                .ifPresentOrElse(
                        top -> finalizeWinner(auction, top.getBidder(), top.getAmount()),
                        () -> {
                            auction.close(null, null);
                            Product product = auction.getProduct();
                            product.changeStatus(ProductStatus.FAILED);
                            notifyAuctionFailed(product, auction, "입찰 없이 유찰됐어요",
                                    String.format("[%s] 경매가 입찰 없이 종료되어 유찰 처리되었습니다. 필요 시 재등록해주세요.",
                                            product.getName()));
                            log.info("[AuctionCompletion] no-bid failed auctionId={}", auctionId);
                        }
                );
    }

    // ================================================================
    // 3) 결제 기한 만료 처리 (스케줄러가 호출)
    // ================================================================

    /**
     * 결제 기한이 지난 AWAITING_PAYMENT 경매를 FAILED 로 전환하고 페널티를 누적시킨다.
     * 별도 트랜잭션으로 처리하여 다른 경매 만료 처리 실패가 전체를 막지 않게 한다.
     */
    @Transactional
    public void expireUnpaidAuction(Auction auction) {
        if (auction.getStatus() != AuctionStatus.AWAITING_PAYMENT) return;
        if (auction.getPaymentDeadline() == null || LocalDateTime.now().isBefore(auction.getPaymentDeadline())) return;

        Member winner = auction.getWinner();
        Product product = auction.getProduct();

        auction.expirePayment();          // AWAITING_PAYMENT → FAILED
        product.changeStatus(ProductStatus.FAILED); // 상품도 유찰로

        // 낙찰자 페널티 누적
        if (winner != null) {
            Member managedWinner = memberRepository.findById(winner.getId()).orElse(null);
            if (managedWinner != null) {
                managedWinner.increaseNonPaymentCount();

                if (managedWinner.getNonPaymentCount() >= NON_PAYMENT_SANCTION_THRESHOLD) {
                    issueAutoSuspendSanction(managedWinner);
                    managedWinner.resetNonPaymentCount();
                }
            }

            createNotification(
                    winner,
                    Notification.NotificationType.AUCTION_FAILED_BY_NONPAYMENT,
                    "결제 기한 만료로 유찰되었습니다",
                    String.format("[%s] 결제 기한 내 결제가 이루어지지 않아 유찰 처리되었습니다. (누적 미결제: %d/%d)",
                            product.getName(),
                            winner.getNonPaymentCount() == null ? 0 : winner.getNonPaymentCount(),
                            NON_PAYMENT_SANCTION_THRESHOLD),
                    "/my/bids"
            );
        }

        // 판매자에게도 알림
        Member seller = product.getSeller();
        if (seller != null) {
            createNotification(
                    seller,
                    Notification.NotificationType.AUCTION_FAILED_BY_NONPAYMENT,
                    "낙찰자 미결제로 유찰되었습니다",
                    String.format("[%s] 낙찰자가 기한 내 결제하지 않아 유찰 처리되었습니다. 필요 시 재등록해주세요.",
                            product.getName()),
                    "/my/products"
            );
        }

        log.info("[AuctionCompletion] expire unpaid auctionId={}, winnerId={}",
                auction.getId(), winner == null ? null : winner.getId());
    }

    // ================================================================
    // 내부 헬퍼
    // ================================================================

    /** 즉시 결제 분기. 잔액 차감 → Settlement 생성 → 경매/상품 상태 SUCCESS/SOLD → 알림. */
    private void settleImmediately(Auction auction, Member winner, long amount, Product product) {
        winner.deductBalance(amount);

        // 구매자 결제(잔액 차감)를 계좌이력(출금)으로 기록한다. (deductBalance 가 잔액을 이미 차감했으므로 내역만 추가)
        walletService.recordPaymentDebit(
                winner,
                amount,
                String.format("경매 낙찰 결제 - %s", product.getName())
        );

        // 이미 결제 완료된 경매가 다시 호출되는 경우(중복 호출)를 위해 가드.
        if (auction.getStatus() == AuctionStatus.AWAITING_PAYMENT) {
            auction.completePayment();
        } else {
            // LIVE → SUCCESS (즉시낙찰/일반 종료 즉시 결제 케이스)
            auction.close(winner, amount);
        }
        product.changeStatus(ProductStatus.SOLD);

        ensureSettlement(auction, winner, amount);
        auction.startDeliveryDemo();

        createNotification(
                winner,
                auction.getStatus() == AuctionStatus.SUCCESS
                        ? Notification.NotificationType.PAYMENT_COMPLETED
                        : Notification.NotificationType.AUCTION_WON,
                "결제가 완료되었습니다",
                String.format("[%s] %s원 결제 완료. 곧 발송 준비됩니다.",
                        product.getName(), formatAmount(amount)),
                "/auctions/" + auction.getId()
        );
    }

    /** 낙찰 정산 row 자동 생성 (이미 있으면 noop). AdminAuctionService 의 동일 로직과 일관되게 유지. */
    private void ensureSettlement(Auction auction, Member buyer, long salesAmount) {
        if (settlementRepository.existsByAuctionId(auction.getId())) return;
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
        log.info("[AuctionCompletion] settlement created auctionId={}, sales={}, feeRate={}%, fee={}",
                auction.getId(), salesAmount, feeRate, feeAmount);
    }

    /** fee_rules 에서 salesAmount 이하 minAmount 중 가장 큰 정책 사용. 비어 있으면 DEFAULT_FEE_RATE. */
    private double calcFeeRate(long salesAmount) {
        List<FeeRule> rules = feeRuleRepository.findAllByOrderByMinAmountAsc();
        double applied = DEFAULT_FEE_RATE;
        for (FeeRule rule : rules) {
            if (rule.getMinAmount() <= salesAmount) {
                applied = rule.getFeeRate();
            } else {
                break;
            }
        }
        return applied;
    }

    /** 시스템 자동 발급 Sanction (admin = null). Member 도 suspend 처리. */
    private void issueAutoSuspendSanction(Member member) {
        LocalDateTime expiresAt = LocalDateTime.now().plusDays(AUTO_SUSPEND_DAYS);
        Sanction sanction = Sanction.builder()
                .member(member)
                .admin(null)                     // 시스템 자동 발급
                .type(Sanction.SanctionType.SUSPEND_7)
                .reason(String.format("결제 기한 미이행 %d회 누적 (자동 발급)", NON_PAYMENT_SANCTION_THRESHOLD))
                .adminNote("auto-issued by AuctionCompletionService")
                .expiresAt(expiresAt)
                .build();
        sanctionRepository.save(sanction);
        member.suspend(expiresAt);

        createNotification(
                member,
                Notification.NotificationType.SANCTION,
                "입찰이 일시 정지되었습니다",
                String.format("결제 미이행 %d회 누적으로 %d일간 입찰이 제한됩니다. (해제: %s)",
                        NON_PAYMENT_SANCTION_THRESHOLD, AUTO_SUSPEND_DAYS, expiresAt),
                "/my"
        );

        log.warn("[AuctionCompletion] auto SUSPEND_7 issued memberId={}, until={}", member.getId(), expiresAt);
    }

    /** DB 저장 + WebSocket(STOMP) 실시간 push 를 함께 수행. push 실패는 무시(NotificationService 내부에서 로깅). */
    private void createNotification(Member to, Notification.NotificationType type,
                                     String title, String content, String linkUrl) {
        notificationService.createAndPush(to, type, title, content, linkUrl);
    }

    private void notifyAuctionFailed(Product product, Auction auction, String title, String content) {
        Member seller = product.getSeller();
        if (seller == null) return;
        createNotification(
                seller,
                Notification.NotificationType.PRODUCT_AUCTION_FAILED,
                title,
                content,
                "/auctions/" + auction.getId()
        );
    }

    private String formatAmount(long amount) {
        return String.format("%,d", amount);
    }
}
