package com.moida.domain.product;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface ProductRepository extends JpaRepository<Product, Long> {

    Optional<Product> findByProductNo(String productNo);

    Page<Product> findAllByStatus(ProductStatus status, Pageable pageable);

    Page<Product> findAllBySellerId(Long sellerId, Pageable pageable);

    Page<Product> findAllByTypeAndStatus(ProductType type, ProductStatus status, Pageable pageable);

    long countByStatus(ProductStatus status);
}
