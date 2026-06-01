package com.moida.domain.settlement;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface FeeRuleRepository extends JpaRepository<FeeRule, Long> {

    /** 모든 정책을 적용 시작 금액 오름차순으로 반환 (정산 계산/관리자 화면 공통) */
    List<FeeRule> findAllByOrderByMinAmountAsc();
}
