package com.moida.domain.product;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Optional;
import java.util.List;

public interface ProductRepository extends JpaRepository<Product, Long> {

    Optional<Product> findByProductNo(String productNo);

    Page<Product> findAllByStatus(ProductStatus status, Pageable pageable);

    Page<Product> findAllBySellerId(Long sellerId, Pageable pageable);

    List<Product> findAllBySellerIdOrderByCreatedAtDesc(Long sellerId, Pageable pageable);

    Page<Product> findAllByTypeAndStatus(ProductType type, ProductStatus status, Pageable pageable);

    long countByStatus(ProductStatus status);

    // 홈/검색/인기 화면에서 카드 목록을 채울 때 사용하는 가시(visible) 상품 조회.
    // type/category/status 는 null 허용 동적 필터로, 인자가 비면 해당 조건을 무시한다.
    //   - status=LIVE      → 실시간 경매 섹션
    //   - status=SCHEDULED → 경매 예정 매물 섹션
    // 삭제(DELETED)/숨김(HIDDEN) 상품은 어떤 조건에서도 노출되지 않도록 항상 제외한다.
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
              and p.status not in (com.moida.domain.product.ProductStatus.DELETED, com.moida.domain.product.ProductStatus.HIDDEN)
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
              and p.status not in (com.moida.domain.product.ProductStatus.DELETED, com.moida.domain.product.ProductStatus.HIDDEN)
            """)
    Optional<Product> findVisibleProductDetail(@Param("id") Long id);
}
