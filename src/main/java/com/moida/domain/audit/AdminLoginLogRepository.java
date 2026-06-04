package com.moida.domain.audit;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface AdminLoginLogRepository extends JpaRepository<AdminLoginLog, Long> {

    /** 최근 기록 우선(내림차순), 최대 500건. 관리자 조회 화면용. */
    List<AdminLoginLog> findTop500ByOrderByIdDesc();
}
