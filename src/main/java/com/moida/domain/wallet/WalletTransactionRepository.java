package com.moida.domain.wallet;

import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import jakarta.persistence.LockModeType;
import java.util.List;
import java.util.Optional;

/**
 * WalletTransaction 엔티티에 대한 데이터 액세스 처리를 담당하는 리포지토리 인터페이스
 */
public interface WalletTransactionRepository extends JpaRepository<WalletTransaction, Long> {

    /**
     * 회원의 거래 내역을 생성일자 기준 내림차순(최신순)으로 페이지네이션하여 조회합니다.
     */
    List<WalletTransaction> findAllByMemberIdOrderByCreatedAtDesc(Long memberId, Pageable pageable);

    /**
     * 거래 ID로 거래 정보를 조회하며, 처리 중 중복 완료를 막기 위해 비관적 쓰기 락을 획득합니다.
     */
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select wt from WalletTransaction wt join fetch wt.member where wt.id = :id")
    Optional<WalletTransaction> findByIdForUpdate(@Param("id") Long id);

    boolean existsByMemberIdAndStatus(Long memberId, WalletTransaction.TransactionStatus status);

    /**
     * 관리자 화면에서 거래 유형과 상태를 기준으로 지갑 거래 내역을 조회합니다.
     */
    @Query("""
            select wt from WalletTransaction wt
            join fetch wt.member
            where (:type is null or wt.type = :type)
              and (:status is null or wt.status = :status)
            order by wt.createdAt desc
            """)
    List<WalletTransaction> searchForAdmin(
            @Param("type") WalletTransaction.TransactionType type,
            @Param("status") WalletTransaction.TransactionStatus status,
            Pageable pageable
    );
}
