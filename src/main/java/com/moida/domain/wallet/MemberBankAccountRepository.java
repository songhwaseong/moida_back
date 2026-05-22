package com.moida.domain.wallet;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

/**
 * MemberBankAccount 엔티티에 대한 데이터 액세스 처리를 담당하는 리포지토리 인터페이스
 */
public interface MemberBankAccountRepository extends JpaRepository<MemberBankAccount, Long> {

    /**
     * 회원 식별자로 출금 계좌를 조회합니다.
     */
    Optional<MemberBankAccount> findByMemberId(Long memberId);

    /**
     * 해당 회원에게 등록된 계좌가 존재하는지 여부를 확인합니다.
     */
    boolean existsByMemberId(Long memberId);

    /**
     * 회원 식별자로 등록된 출금 계좌를 삭제합니다.
     */
    void deleteByMemberId(Long memberId);
}
