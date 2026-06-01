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

    // 관리자 문의 관리 화면용 전체 조회.
    // product/user/seller 를 fetch join 해 카드에 표시할 정보를 한 번에 가져온다.
    // 최신순 정렬로 답변 대기 문의가 자연스럽게 상단에 노출되도록 한다.
    @Query("""
            select i
            from Inquiry i
            join fetch i.product p
            join fetch p.category
            join fetch i.user
            join fetch i.seller
            order by i.createdAt desc, i.id desc
            """)
    List<Inquiry> findAllForAdmin();
}
