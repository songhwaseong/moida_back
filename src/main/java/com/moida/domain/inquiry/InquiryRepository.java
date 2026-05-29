package com.moida.domain.inquiry;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface InquiryRepository extends JpaRepository<Inquiry, Long> {

    List<Inquiry> findAllByProductIdOrderByCreatedAtDesc(Long productId);

    Page<Inquiry> findAllByUserId(Long userId, Pageable pageable);

    @Query("""
            select i
            from Inquiry i
            join fetch i.product p
            join fetch p.category
            join fetch i.user
            join fetch i.seller
            where i.user.id = :userId
            order by i.createdAt desc, i.id desc
            """)
    List<Inquiry> findMyInquiries(@Param("userId") Long userId, Pageable pageable);

    Page<Inquiry> findAllBySellerId(Long sellerId, Pageable pageable);

    long countByAnswerIsNull();
}
