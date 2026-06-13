package com.moida.domain.product;

import com.moida.common.entity.BaseTimeEntity;
import com.moida.domain.category.Category;
import com.moida.domain.member.Member;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Entity
@Getter
@Table(name = "products",
        indexes = {
                @Index(name = "idx_product_status", columnList = "status"),
                @Index(name = "idx_product_seller", columnList = "seller_id"),
                @Index(name = "idx_product_category", columnList = "category_id"),
                @Index(name = "idx_product_type", columnList = "type"),
                @Index(name = "idx_product_no", columnList = "product_no")
        })
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Product extends BaseTimeEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "product_id")
    private Long id;

    @Column(name = "product_no", nullable = false, length = 20)
    private String productNo;            // 상품번호 (e.g. 2600001)

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "seller_id", nullable = false)
    private Member seller;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "category_id", nullable = false)
    private Category category;

    @Column(nullable = false, length = 200)
    private String name;

    @Lob
    @Column(columnDefinition = "TEXT")
    private String description;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private ProductType type;            // TRADE / AUCTION

    @Enumerated(EnumType.STRING)
    @Column(name = "product_condition", nullable = false, length = 10)
    private ProductCondition condition;  // S / A / B / C

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private ProductStatus status;

    @Column(nullable = false)
    private Long price;                  // 표시 가격 / 즉시구매가

    @Column(length = 100)
    private String location;

    @Column(name = "carrier_code", length = 30)
    private String carrierCode;

    @Column(name = "tracking_no", length = 30)
    private String trackingNo;

    @Column(name = "view_count", nullable = false)
    private Long viewCount;

    @Column(name = "like_count", nullable = false)
    private Long likeCount;

    @Lob
    @Column(name = "main_image_url", columnDefinition = "LONGTEXT")
    private String mainImageUrl;

    @Column(name = "auction_scheduled_at")
    private LocalDateTime auctionScheduledAt;

    @Column(name = "return_request_reason", length = 500)
    private String returnRequestReason;

    @Column(name = "return_requested_at")
    private LocalDateTime returnRequestedAt;

    @Column(name = "review_revision_reason", length = 500)
    private String reviewRevisionReason;

    @Column(name = "review_revision_requested_at")
    private LocalDateTime reviewRevisionRequestedAt;

    // 등록 시 입력된 즉시낙찰가/최소 호가단위를 보관한다.
    // SCHEDULED → LIVE 전환 시점에 AdminProductService 가 Auction 엔티티로 옮겨 사용한다.
    // 즉시낙찰가는 선택값이므로 nullable, 최소 호가단위도 누락 시 기본값으로 폴백한다.
    @Column(name = "immediate_price")
    private Long immediatePrice;

    @Column(name = "min_bid_unit")
    private Long minBidUnit;

    @OneToMany(mappedBy = "product", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<ProductImage> images = new ArrayList<>();

    @Builder
    private Product(String productNo, Member seller, Category category, String name,
                    String description, ProductType type, ProductCondition condition,
                    Long price, String location, String carrierCode, String trackingNo, String mainImageUrl,
                    LocalDateTime auctionScheduledAt, Long immediatePrice, Long minBidUnit) {
        this.productNo = productNo;
        this.seller = seller;
        this.category = category;
        this.name = name;
        this.description = description;
        this.type = type;
        this.condition = condition;
        // 신규 등록 상품은 관리자 승인 대기(PENDING)로 시작한다.
        // 관리자가 검토 후 SCHEDULED(경매예정) 또는 LIVE(경매중) 로 전환한다.
        this.status = ProductStatus.PENDING;
        this.price = price;
        this.location = location;
        this.carrierCode = carrierCode;
        this.trackingNo = trackingNo;
        this.mainImageUrl = mainImageUrl;
        this.auctionScheduledAt = auctionScheduledAt;
        this.immediatePrice = immediatePrice;
        this.minBidUnit = minBidUnit;
        this.viewCount = 0L;
        this.likeCount = 0L;
    }

    // ===== Domain Methods =====
    public void update(String name, String description, Category category,
                       ProductCondition condition, Long price, String location) {
        if (name != null) this.name = name;
        if (description != null) this.description = description;
        if (category != null) this.category = category;
        if (condition != null) this.condition = condition;
        if (price != null) this.price = price;
        if (location != null) this.location = location;
    }

    public void changeStatus(ProductStatus status) {
        this.status = status;
    }

    public void requestRevision(String reason) {
        this.status = ProductStatus.NEEDS_REVISION;
        this.reviewRevisionReason = reason;
        this.reviewRevisionRequestedAt = LocalDateTime.now();
        this.auctionScheduledAt = null;
    }

    public void clearRevisionRequest() {
        this.reviewRevisionReason = null;
        this.reviewRevisionRequestedAt = null;
    }

    // 경매예정(SCHEDULED) 진입 시 자동 LIVE 전환 예약 시각을 기록한다.
    // 스케줄러가 이 시각을 지난 SCHEDULED 상품을 LIVE 로 올린다.
    public void scheduleAuctionAt(LocalDateTime goLiveAt) {
        this.auctionScheduledAt = goLiveAt;
    }

    public void requestReturn(String reason) {
        this.status = ProductStatus.RETURN_REQUESTED;
        this.returnRequestReason = reason;
        this.returnRequestedAt = LocalDateTime.now();
        this.auctionScheduledAt = null;
    }

    public void increaseViewCount() { this.viewCount++; }
    public void increaseLikeCount() { this.likeCount++; }
    public void decreaseLikeCount() {
        if (this.likeCount > 0) this.likeCount--;
    }

    public void changeMainImage(String url) { this.mainImageUrl = url; }

    public void addImage(ProductImage image) {
        this.images.add(image);
        image.assignProduct(this);
    }

    // 상품 수정 시 갤러리 전체를 교체한다. orphanRemoval=true 이므로 비우면 기존 행이 삭제된다.
    public void clearImages() {
        this.images.clear();
    }

    // 등록 단계에서 보관한 최소 호가단위를 수정 단계에서 갱신한다.
    public void changeMinBidUnit(Long minBidUnit) {
        if (minBidUnit != null) this.minBidUnit = minBidUnit;
    }

    // 즉시낙찰가는 선택값이라 null 로 설정하면 즉시낙찰을 사용하지 않는 것으로 처리한다.
    public void changeImmediatePrice(Long immediatePrice) {
        this.immediatePrice = immediatePrice;
    }

    public boolean isOwnedBy(Long memberId) {
        return this.seller.getId().equals(memberId);
    }
}
