package com.moida.domain.product;

import com.moida.common.exception.BusinessException;
import com.moida.common.exception.ErrorCode;
import com.moida.common.request.AdminProductUpdateRequest;
import com.moida.common.response.AdminProductDetailResponse;
import com.moida.common.response.AdminProductResponse;
import com.moida.common.response.AdminProductStatsResponse;
import com.moida.domain.auction.Auction;
import com.moida.domain.auction.AuctionRepository;
import com.moida.domain.category.Category;
import com.moida.domain.category.CategoryRepository;
import com.moida.domain.notification.Notification;
import com.moida.domain.notification.NotificationService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;

/**
 * 관리자 상품 관리 전용 서비스.
 * 일반 상품 서비스와 분리하여 관리자 권한 동작(목록/통계/상태 변경/삭제)만 담당한다.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class AdminProductService {

    private final ProductRepository productRepository;
    private final CategoryRepository categoryRepository;
    private final AuctionRepository auctionRepository;
    private final NotificationService notificationService;

    /** 경매 기간(시작 ~ 종료) — 별도 입력값이 없으므로 일괄 7일로 고정한다. */
    private static final int DEFAULT_AUCTION_DAYS = 7;
    /** 최소 호가 단위 기본값. 등록 시 입력값을 저장하지 않으므로 보수적으로 1,000원으로 둔다. */
    private static final long DEFAULT_MIN_BID_UNIT = 1_000L;
    /** 시연용: 경매예정(SCHEDULED) 진입 후 자동으로 LIVE 로 전환되기까지의 대기 시간(초). */
    private static final int AUTO_GO_LIVE_DELAY_SECONDS = 10;

    /** 전체 상품 목록 (삭제 상태 제외) */
    @Transactional(readOnly = true)
    public List<AdminProductResponse> getProducts() {
        return productRepository.findAllForAdmin().stream()
                .filter(p -> p.getStatus() != ProductStatus.DELETED)
                .map(AdminProductResponse::from)
                .toList();
    }

    /** 상단 통계 카드 (삭제 상태 제외) */
    @Transactional(readOnly = true)
    public AdminProductStatsResponse getStats() {
        List<Product> products = productRepository.findAllForAdmin().stream()
                .filter(p -> p.getStatus() != ProductStatus.DELETED)
                .toList();

        long total = products.size();
        long selling = products.stream().filter(p -> p.getStatus() == ProductStatus.SCHEDULED).count();
        long approving = products.stream().filter(p -> p.getStatus() == ProductStatus.PENDING).count();
        long inBid = products.stream().filter(p -> p.getStatus() == ProductStatus.LIVE).count();
        long hidden = products.stream().filter(p -> p.getStatus() == ProductStatus.HIDDEN).count();

        return new AdminProductStatsResponse(total, selling, approving, inBid, hidden);
    }

    /** 상품 상세 (이미지 전체 포함) */
    @Transactional(readOnly = true)
    public AdminProductDetailResponse getProduct(Long productId) {
        Product product = productRepository.findByIdForAdmin(productId)
                .orElseThrow(() -> new BusinessException(ErrorCode.PRODUCT_NOT_FOUND));
        return AdminProductDetailResponse.from(product);
    }

    /** 상품 정보 수정 (상품명/설명/카테고리/제품상태/가격) */
    @Transactional
    public void updateProduct(Long productId, AdminProductUpdateRequest request) {
        Product product = productRepository.findById(productId)
                .orElseThrow(() -> new BusinessException(ErrorCode.PRODUCT_NOT_FOUND));

        // 카테고리는 이름으로 조회 (null/빈 값이면 변경하지 않음)
        Category category = null;
        if (request.getCategory() != null && !request.getCategory().isBlank()) {
            category = categoryRepository.findByNameAndIsActiveTrue(request.getCategory())
                    .orElseThrow(() -> new BusinessException(ErrorCode.INVALID_INPUT, "존재하지 않는 카테고리입니다."));
        }

        // 제품상태는 enum 이름(S/A/B/C)으로 받는다 (null/빈 값이면 변경하지 않음)
        ProductCondition condition = null;
        if (request.getCondition() != null && !request.getCondition().isBlank()) {
            try {
                condition = ProductCondition.valueOf(request.getCondition().trim().toUpperCase());
            } catch (IllegalArgumentException e) {
                throw new BusinessException(ErrorCode.INVALID_INPUT, "유효하지 않은 제품상태입니다.");
            }
        }

        // Product.update 는 null 필드를 건너뛰므로 변경된 값만 반영된다. (location 은 미수정)
        product.update(request.getName(), request.getDescription(), category, condition, request.getPrice(), null);
    }

    /** 상품 상태 변경 */
    @Transactional
    public void updateStatus(Long productId, ProductStatus status) {
        Product product = productRepository.findById(productId)
                .orElseThrow(() -> new BusinessException(ErrorCode.PRODUCT_NOT_FOUND));
        ProductStatus previousStatus = product.getStatus();
        product.changeStatus(status);

        // 경매예정 진입 시 자동 LIVE 전환 예약 시각(now + 24h)을 기록한다.
        // 관리자가 그 전에 수동으로 LIVE 로 올리면, LIVE 가 된 상품은 스케줄러 조회 대상에서 빠진다.
        if (status == ProductStatus.SCHEDULED) {
            product.scheduleAuctionAt(LocalDateTime.now().plusSeconds(AUTO_GO_LIVE_DELAY_SECONDS));
        }

        if (status == ProductStatus.RETURN_REQUESTED
                || status == ProductStatus.RETURN_SHIPPING
                || status == ProductStatus.RETURN_COMPLETED
                || status == ProductStatus.HIDDEN
                || status == ProductStatus.DELETED) {
            product.scheduleAuctionAt(null);
        }

        // SCHEDULED → LIVE 승인 시점에 Auction row 가 비어 있으면 신규 생성한다.
        // (이미 존재한다면 uk_auction_product 제약으로 중복이 방지되므로 그대로 둔다.)
        if (status == ProductStatus.LIVE) {
            Auction auction = ensureAuctionForLive(product);
            if (previousStatus == ProductStatus.SCHEDULED) {
                notifyAuctionStarted(product, auction);
            }
        }

        if (previousStatus == ProductStatus.PENDING && status == ProductStatus.SCHEDULED) {
            notifyProductApproved(product);
        }
    }

    private void notifyProductApproved(Product product) {
        notificationService.createAndPush(
                product.getSeller(),
                Notification.NotificationType.PRODUCT_APPROVED,
                "상품 승인이 완료됐어요",
                String.format("'%s' 상품이 승인되어 경매예정 상태로 변경됐습니다. 경매 시작 전까지 내 등록 상품에서 상태를 확인할 수 있어요.", product.getName()),
                "/products/" + product.getId()
        );
    }

    /**
     * 스케줄러 전용: 예약 시각이 지난 경매예정 상품을 LIVE 로 자동 전환한다.
     * 이미 LIVE 등 다른 상태로 바뀐 상품은 건너뛰어(idempotent) 중복 전환을 막는다.
     */
    @Transactional
    public void activateScheduledProduct(Long productId) {
        Product product = productRepository.findById(productId)
                .orElseThrow(() -> new BusinessException(ErrorCode.PRODUCT_NOT_FOUND));

        // 관리자가 먼저 수동 전환했거나 숨김/삭제된 경우 → 자동 전환하지 않는다.
        if (product.getStatus() != ProductStatus.SCHEDULED) {
            return;
        }

        product.changeStatus(ProductStatus.LIVE);
        Auction auction = ensureAuctionForLive(product);
        notifyAuctionStarted(product, auction);
        log.info("[AdminProductService] auto activate productId={} → LIVE", productId);
    }

    /** 경매 시작 시점에 Auction 레코드를 생성한다. 이미 있으면 noop. */
    private Auction ensureAuctionForLive(Product product) {
        return auctionRepository.findByProductId(product.getId()).orElseGet(() -> createAuction(product));
    }

    private Auction createAuction(Product product) {
        LocalDateTime now = LocalDateTime.now();
        // 등록 시점에 Product 에 보관해둔 입력값(immediatePrice / minBidUnit)을 사용한다.
        // immediatePrice 는 선택값이라 null 허용, minBidUnit 은 누락 시 DEFAULT 로 폴백한다.
        Long minBidUnit = product.getMinBidUnit() != null ? product.getMinBidUnit() : DEFAULT_MIN_BID_UNIT;
        Auction auction = Auction.builder()
                .auctionNo(generateAuctionNo(now))
                .product(product)
                .startPrice(product.getPrice())
                .immediatePrice(product.getImmediatePrice())
                .minBidUnit(minBidUnit)
                .startAt(now)
                .endAt(now.plusDays(DEFAULT_AUCTION_DAYS))
                .build();
        // 생성 직후 LIVE 로 전환 (Auction 기본 status 는 READY)
        auction.start();
        Auction saved = auctionRepository.save(auction);
        log.info("[AdminProductService] auction created productId={}, auctionNo={}, endAt={}",
                product.getId(), saved.getAuctionNo(), saved.getEndAt());
        return saved;
    }

    private void notifyAuctionStarted(Product product, Auction auction) {
        notificationService.createAndPush(
                product.getSeller(),
                Notification.NotificationType.PRODUCT_AUCTION_STARTED,
                "경매가 시작됐어요",
                String.format("'%s' 상품 경매가 시작됐습니다. 경매 종료 전까지 입찰 현황을 확인해보세요.", product.getName()),
                "/auctions/" + auction.getId()
        );
    }

    /** 경매번호 생성: AUC-yyyyMMdd-NNNN. NNNN 은 전체 카운트+1 을 5자리로 패딩. */
    private String generateAuctionNo(LocalDateTime now) {
        long seq = auctionRepository.count() + 1;
        return String.format("AUC-%s-%05d",
                now.format(DateTimeFormatter.ofPattern("yyyyMMdd")), seq);
    }

    /** 상품 삭제 (soft delete: 상태를 DELETED로 변경) */
    @Transactional
    public void delete(Long productId) {
        Product product = productRepository.findById(productId)
                .orElseThrow(() -> new BusinessException(ErrorCode.PRODUCT_NOT_FOUND));
        product.changeStatus(ProductStatus.DELETED);
    }
}
