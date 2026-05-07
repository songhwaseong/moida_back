package com.moida.domain.auction;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface BidRepository extends JpaRepository<Bid, Long> {

    List<Bid> findAllByAuctionIdOrderByAmountDesc(Long auctionId);

    Page<Bid> findAllByBidderId(Long bidderId, Pageable pageable);

    Optional<Bid> findFirstByAuctionIdOrderByAmountDesc(Long auctionId);

    long countByAuctionId(Long auctionId);
}
