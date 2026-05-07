package com.moida.domain.inquiry;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface InquiryRepository extends JpaRepository<Inquiry, Long> {

    List<Inquiry> findAllByProductIdOrderByCreatedAtDesc(Long productId);

    Page<Inquiry> findAllByUserId(Long userId, Pageable pageable);

    Page<Inquiry> findAllBySellerId(Long sellerId, Pageable pageable);

    long countByAnswerIsNull();
}
