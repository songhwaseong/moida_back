package com.moida.domain.auction;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

public interface AuctionRepository extends JpaRepository<Auction, Long> {

    Optional<Auction> findByAuctionNo(String auctionNo);

    Optional<Auction> findByProductId(Long productId);

    Page<Auction> findAllByStatus(AuctionStatus status, Pageable pageable);

    List<Auction> findAllByStatusAndEndAtBefore(AuctionStatus status, LocalDateTime endAt);

    List<Auction> findAllByStatusAndStartAtBefore(AuctionStatus status, LocalDateTime startAt);
}
