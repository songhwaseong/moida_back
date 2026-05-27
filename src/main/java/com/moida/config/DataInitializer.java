package com.moida.config;

import com.moida.domain.member.Member;
import com.moida.domain.member.MemberRepository;
import com.moida.domain.member.MemberRole;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.CommandLineRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
public class DataInitializer implements CommandLineRunner {

    private final MemberRepository memberRepository;
    private final PasswordEncoder passwordEncoder;

    // LoginPage.tsx의 ADMIN_CREDENTIALS를 DB로 이전
    private static final String[][] ADMIN_ACCOUNTS = {
            {"admin@admin.com", "admin", "관리자1"},
    };

    private static final String[][] MANAGER_ACCOUNTS = {
            {"admin@bazar.kr",        "admin1234",   "관리자2"},
            {"kmikyung761@gmail.com", "1",           "김미경"},
            {"manager@bazar.kr",      "manager5678", "매니저"},
            {"yalejong96@gmail.com",  "2",           "관리자3"},
            {"zes1357@outlook.com",   "1",           "관리자4"},
            {"supyoungsun@gmail.com", "2",           "관리자5"},
    };

    @Override
    public void run(String... args) {
        createAccounts(ADMIN_ACCOUNTS, MemberRole.ADMIN, "ADMIN");
        createAccounts(MANAGER_ACCOUNTS, MemberRole.MANAGER, "MANAGER");
    }

    private void createAccounts(String[][] accounts, MemberRole role, String prefix) {
        for (int i = 0; i < accounts.length; i++) {
            String email    = accounts[i][0];
            String password = accounts[i][1];
            String name     = accounts[i][2];
            if (memberRepository.existsByEmail(email)) continue;
            Member member = Member.builder()
                    .memberNo(String.format(prefix + "%05d", i + 1))
                    .email(email)
                    .password(passwordEncoder.encode(password))
                    .name(name)
                    .role(role)
                    .build();
            memberRepository.save(member);
            log.info("계정 생성: {} ({})", email, role);
        }
    }
}
