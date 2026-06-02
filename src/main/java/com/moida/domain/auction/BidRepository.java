package com.moida.domain.auction;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface BidRepository extends JpaRepository<Bid, Long> {

    List<Bid> findAllByAuctionIdOrderByAmountDesc(Long auctionId);

    // 경매 상세 입찰 이력에서 bidder 정보까지 바로 필요하므로 fetch join으로 N+1을 피한다.
    @Query("""
            select b
            from Bid b
            join fetch b.bidder
            where b.auction.id = :auctionId
            order by b.amount desc
            """)
    List<Bid> findHistoryByAuctionId(@Param("auctionId") Long auctionId);

    Page<Bid> findAllByBidderId(Long bidderId, Pageable pageable);

    @Query("""
            select b
            from Bid b
            join fetch b.bidder
            join fetch b.auction a
            join fetch a.product p
            join fetch p.category
            where b.bidder.id = :bidderId
            order by b.createdAt desc, b.id desc
            """)
    List<Bid> findMyBidHistory(@Param("bidderId") Long bidderId, Pageable pageable);

    Optional<Bid> findFirstByAuctionIdOrderByAmountDesc(Long auctionId);

    long countByAuctionId(Long auctionId);

    long countByBidderId(Long bidderId);
}
