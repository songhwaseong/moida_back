package com.moida.domain.sanction;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

public interface SanctionRepository extends JpaRepository<Sanction, Long> {

    Page<Sanction> findAllByMemberId(Long memberId, Pageable pageable);
}
