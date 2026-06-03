package com.moida.domain.auction;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

public interface AuctionRepository extends JpaRepository<Auction, Long> {

    Optional<Auction> findByAuctionNo(String auctionNo);

    Optional<Auction> findByProductId(Long productId);

    Page<Auction> findAllByStatus(AuctionStatus status, Pageable pageable);

    List<Auction> findAllByStatusAndEndAtBefore(AuctionStatus status, LocalDateTime endAt);

    List<Auction> findAllByStatusAndStartAtBefore(AuctionStatus status, LocalDateTime startAt);

    /** 결제 기한이 지난 AWAITING_PAYMENT 경매 조회 (스케줄러에서 유찰 처리 대상). */
    List<Auction> findAllByStatusAndPaymentDeadlineBefore(AuctionStatus status, LocalDateTime now);

    List<Auction> findAllByStatusAndDeliveryStatusAndDeliveryStatusUpdatedAtBefore(
            AuctionStatus status,
            DeliveryStatus deliveryStatus,
            LocalDateTime now
    );

    @Query("""
            select a
            from Auction a
            join fetch a.product p
            join fetch p.category
            join fetch p.seller
            where a.winner.id = :memberId
              and a.status = com.moida.domain.auction.AuctionStatus.SUCCESS
            order by a.updatedAt desc, a.id desc
            """)
    List<Auction> findSuccessfulPurchasesByWinnerId(@Param("memberId") Long memberId);

    @Query("select a from Auction a where a.product.id in :productIds")
    List<Auction> findAllByProductIdIn(@Param("productIds") List<Long> productIds);

    long countByWinnerId(Long winnerId);

    // 관리자 경매 관리 화면용 전체 조회.
    // product/category 까지 fetch join 해 테이블 표시 시 추가 쿼리가 발생하지 않게 한다.
    // 최신 생성순으로 정렬해 최근 시작한 경매가 상단에 노출되도록 한다.
    @Query("""
            select a
            from Auction a
            join fetch a.product p
            join fetch p.category
            order by a.createdAt desc, a.id desc
            """)
    List<Auction> findAllForAdmin();
}
