package com.moida.domain.product;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDateTime;
import java.util.Optional;
import java.util.List;

public interface ProductRepository extends JpaRepository<Product, Long> {

    Optional<Product> findByProductNo(String productNo);

    Page<Product> findAllByStatus(ProductStatus status, Pageable pageable);

    // 자동 LIVE 전환 대상: 예약 시각(auctionScheduledAt)이 지난 경매예정 상품.
    // auctionScheduledAt 이 null 인 행은 조건에 걸리지 않아 자연히 제외된다.
    List<Product> findAllByStatusAndAuctionScheduledAtBefore(ProductStatus status, LocalDateTime time);

    Page<Product> findAllBySellerId(Long sellerId, Pageable pageable);

    List<Product> findAllBySellerIdOrderByCreatedAtDesc(Long sellerId, Pageable pageable);

    Page<Product> findAllByTypeAndStatus(ProductType type, ProductStatus status, Pageable pageable);

    long countByStatus(ProductStatus status);

    // 관리자 상품 관리 화면용 전체 조회.
    // 일반 조회와 달리 HIDDEN 까지 포함하며(삭제 제외는 서비스에서 처리),
    // 테이블에 표시할 seller/category 를 fetch join 해 N+1 을 막는다. 최신 등록순.
    @Query("""
            select p
            from Product p
            join fetch p.seller
            join fetch p.category
            order by p.createdAt desc
            """)
    List<Product> findAllForAdmin();

    // 관리자 상품 상세용 단건 조회. 일반 상세와 달리 상태(HIDDEN/DELETED) 제한 없이
    // seller/category/images 를 한 번에 fetch 한다.
    @Query("""
            select distinct p
            from Product p
            join fetch p.seller
            join fetch p.category
            left join fetch p.images
            where p.id = :id
            """)
    Optional<Product> findByIdForAdmin(@Param("id") Long id);

    // 홈/검색/인기 화면에서 카드 목록을 채울 때 사용하는 가시(visible) 상품 조회.
    // type/category/status 는 null 허용 동적 필터로, 인자가 비면 해당 조건을 무시한다.
    //   - status=LIVE      → 실시간 경매 섹션
    //   - status=SCHEDULED → 경매 예정 매물 섹션
    // 삭제(DELETED)/숨김(HIDDEN)/승인대기(PENDING)/보완요청(NEEDS_REVISION) 상품은 어떤 조건에서도 노출되지 않도록 항상 제외한다.
    // 정렬은 호출자가 Pageable.sort 로 주입한다(기본: 최신순, 인기: viewCount 우선).
    // seller/category 를 fetch join 해 카드 표시에 필요한 연관을 미리 로딩한다.
    @Query("""
            select p
            from Product p
            join fetch p.seller
            join fetch p.category
            where (:type is null or p.type = :type)
              and (:category is null or p.category.name = :category)
              and (:status is null or p.status = :status)
              and p.status not in (com.moida.domain.product.ProductStatus.DELETED,
                                   com.moida.domain.product.ProductStatus.HIDDEN,
                                   com.moida.domain.product.ProductStatus.PENDING,
                                   com.moida.domain.product.ProductStatus.NEEDS_REVISION,
                                   com.moida.domain.product.ProductStatus.RETURN_REQUESTED,
                                   com.moida.domain.product.ProductStatus.RETURN_SHIPPING,
                                   com.moida.domain.product.ProductStatus.RETURN_COMPLETED)
            """)
    List<Product> findVisibleProducts(
            @Param("type") ProductType type,
            @Param("category") String category,
            @Param("status") ProductStatus status,
            Pageable pageable
    );

    // 상품 상세 화면 진입 시 호출. seller/category/images 까지 한 번에 fetch 해
    // 상세 뷰가 추가 쿼리 없이 그려질 수 있도록 한다. 숨김/삭제 상품은 응답하지 않는다.
    @Query("""
            select distinct p
            from Product p
            join fetch p.seller
            join fetch p.category
            left join fetch p.images
            where p.id = :id
              and p.status not in (com.moida.domain.product.ProductStatus.DELETED,
                                   com.moida.domain.product.ProductStatus.HIDDEN,
                                   com.moida.domain.product.ProductStatus.PENDING,
                                   com.moida.domain.product.ProductStatus.NEEDS_REVISION,
                                   com.moida.domain.product.ProductStatus.RETURN_REQUESTED,
                                   com.moida.domain.product.ProductStatus.RETURN_SHIPPING,
                                   com.moida.domain.product.ProductStatus.RETURN_COMPLETED)
            """)
    Optional<Product> findVisibleProductDetail(@Param("id") Long id);

    // 판매자 본인의 상세 조회용. 본인 상품은 PENDING/NEEDS_REVISION/HIDDEN 도 볼 수 있어야 하므로 상태 필터 없이 가져온다.
    // (DELETED 만 호출자에서 걸러낸다.)
    @Query("""
            select distinct p
            from Product p
            join fetch p.seller
            join fetch p.category
            left join fetch p.images
            where p.id = :id
            """)
    Optional<Product> findOwnProductDetail(@Param("id") Long id);
}
