package com.moida.domain.review;

import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface ReviewRepository extends JpaRepository<Review, Long> {

    @EntityGraph(attributePaths = {"product", "reviewer"})
    List<Review> findAllByTargetMemberIdOrderByCreatedAtDesc(Long targetMemberId, Pageable pageable);

    boolean existsByProductIdAndReviewerId(Long productId, Long reviewerId);

    @Query("select r.product.id from Review r "
            + "where r.reviewer.id = :reviewerId and r.product.id in :productIds")
    List<Long> findReviewedProductIds(@Param("reviewerId") Long reviewerId,
                                      @Param("productIds") List<Long> productIds);
}
