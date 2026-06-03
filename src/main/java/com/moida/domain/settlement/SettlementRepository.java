package com.moida.domain.settlement;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.util.List;
import java.util.Optional;

public interface SettlementRepository extends JpaRepository<Settlement, Long> {

    Page<Settlement> findAllByStatus(Settlement.SettlementStatus status, Pageable pageable);

    Page<Settlement> findAllBySellerId(Long sellerId, Pageable pageable);

    // 한 경매에 정산 row 가 이미 존재하는지 확인 (낙찰 처리 중복 방지)
    boolean existsByAuctionId(Long auctionId);

    Optional<Settlement> findByAuctionId(Long auctionId);

    List<Settlement> findAllByAuctionIdIn(List<Long> auctionIds);

    // 관리자 정산 관리 화면용 전체 조회.
    // seller/buyer/auction.product 까지 fetch join 해 테이블 표시 시 N+1 을 막는다.
    // 최신 생성순으로 정렬해 최근 거래가 상단에 보이도록 한다.
    @Query("""
            select s
            from Settlement s
            join fetch s.seller
            join fetch s.buyer
            join fetch s.auction a
            join fetch a.product
            order by s.createdAt desc, s.id desc
            """)
    List<Settlement> findAllForAdmin();
}
