package com.moida.domain.member;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface MemberRepository extends JpaRepository<Member, Long> {

    Optional<Member> findByEmail(String email);

    Optional<Member> findByMemberNo(String memberNo);

    boolean existsByEmail(String email);

    boolean existsByPhone(String phone);
}
