package com.moida.domain.wallet;

import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

/**
 * WalletTransaction 엔티티에 대한 데이터 액세스 처리를 담당하는 리포지토리 인터페이스
 */
public interface WalletTransactionRepository extends JpaRepository<WalletTransaction, Long> {

    /**
     * 회원의 거래 내역을 생성일자 기준 내림차순(최신순)으로 페이지네이션하여 조회합니다.
     */
    List<WalletTransaction> findAllByMemberIdOrderByCreatedAtDesc(Long memberId, Pageable pageable);
}
