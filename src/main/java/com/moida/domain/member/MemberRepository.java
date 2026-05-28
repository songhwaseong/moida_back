package com.moida.domain.member;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import jakarta.persistence.LockModeType;

import java.util.List;
import java.util.Optional;

public interface MemberRepository extends JpaRepository<Member, Long> {

    Optional<Member> findByEmail(String email);

    Optional<Member> findByMemberNo(String memberNo);

    /**
     * 회원 ID로 회원 정보를 조회하며, 비관적 쓰기 락(Pessimistic Write Lock)을 획득합니다.
     * 동시성 이슈를 방지해야 하는 비즈니스 로직(예: 잔액 변경)에서 사용합니다.
     */
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select m from Member m where m.id = :id")
    Optional<Member> findByIdForUpdate(@Param("id") Long id);

    List<Member> findAllByStatusOrderByWithdrawnAtDesc(MemberStatus status);

    boolean existsByEmail(String email);

    boolean existsByPhone(String phone);
}
