package com.moida.domain.product;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface ProductLikeRepository extends JpaRepository<ProductLike, Long> {

    Optional<ProductLike> findByProductIdAndMemberId(Long productId, Long memberId);

    boolean existsByProductIdAndMemberId(Long productId, Long memberId);

    void deleteByProductIdAndMemberId(Long productId, Long memberId);

    Page<ProductLike> findAllByMemberId(Long memberId, Pageable pageable);
}
