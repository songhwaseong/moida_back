package com.moida.domain.auction;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface AuctionPolicyRepository extends JpaRepository<AuctionPolicy, Long> {

    /** 단일 정책 행 조회 (가장 먼저 생성된 1건). */
    Optional<AuctionPolicy> findTopByOrderByIdAsc();
}
