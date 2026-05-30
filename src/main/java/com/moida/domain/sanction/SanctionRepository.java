package com.moida.domain.sanction;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.util.List;

public interface SanctionRepository extends JpaRepository<Sanction, Long> {

    Page<Sanction> findAllByMemberId(Long memberId, Pageable pageable);

    // 관리자 제재 관리 화면용 전체 조회 — member 를 fetch join 해 회원번호/이름을 즉시 노출한다.
    @Query("""
            select s
            from Sanction s
            join fetch s.member
            order by s.createdAt desc, s.id desc
            """)
    List<Sanction> findAllForAdmin();
}
