package com.moida.config;

import com.moida.domain.member.Member;
import com.moida.domain.member.MemberRepository;
import com.moida.domain.settlement.FeeRule;
import com.moida.domain.settlement.FeeRuleRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;
import jakarta.transaction.Transactional;
import java.util.List;


@Slf4j
@Component
@RequiredArgsConstructor
public class DataInitializer implements CommandLineRunner {

    private final MemberRepository memberRepository;
    private final FeeRuleRepository feeRuleRepository;

    // 관리자/매니저 목업 계정 시드는 모두 제거되었다.
    // 권한이 필요한 계정은 운영자가 DB에서 직접 role 컬럼을 수정하거나 별도 절차로 처리한다.

    /** 기본 수수료 정책. fee_rules 테이블이 비어 있을 때만 시드한다. */
    private static final long[][] DEFAULT_FEE_RULES = {
            // {minAmount, feeRate%}
            {0L,          5},
            {500_000L,    4},
            {1_000_000L,  3},
            {10_000_000L, 2},
    };

    @Override
    @Transactional
    public void run(String... args) {
        migrateNicknames();
        seedFeeRules();
    }

    private void migrateNicknames() {
        List<Member> members = memberRepository.findAllByNicknameIsNull();
        for (Member member : members) {
            member.updateNickname(member.getName());
        }
    }

    private void seedFeeRules() {
        if (feeRuleRepository.count() > 0) return;
        for (long[] row : DEFAULT_FEE_RULES) {
            feeRuleRepository.save(FeeRule.builder()
                    .minAmount(row[0])
                    .feeRate((double) row[1])
                    .build());
        }
        log.info("기본 수수료 정책 {}건 시드 완료", DEFAULT_FEE_RULES.length);
    }
}
