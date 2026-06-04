package com.moida.domain.audit;

import com.moida.common.response.AdminLoginLogResponse;
import com.moida.domain.member.Member;
import com.moida.domain.member.MemberRepository;
import com.moida.domain.member.MemberRole;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

/**
 * 관리자 로그인 기록 서비스.
 * 관리자(ADMIN/MANAGER) 계정의 로그인 성공/실패를 남기고, 조회를 제공한다.
 * 기록 실패가 로그인 자체를 막지 않도록 예외는 삼킨다.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class AdminLoginLogService {

    private final AdminLoginLogRepository adminLoginLogRepository;
    private final MemberRepository memberRepository;

    private static boolean isAdminRole(MemberRole role) {
        return role == MemberRole.ADMIN || role == MemberRole.MANAGER;
    }

    /** 로그인 성공 기록. 관리자 권한 계정일 때만 남긴다. */
    @Transactional
    public void recordSuccess(Member member, String ip, String userAgent) {
        if (member == null || !isAdminRole(member.getRole())) return;
        save(member.getId(), member.getEmail(), member.getRole(), ip, userAgent, LoginResult.SUCCESS);
    }

    /**
     * 로그인 실패 기록. 대상 이메일이 관리자 권한 계정일 때만 남긴다.
     * (없는 이메일/일반회원에 대한 실패는 관리자 감사 대상이 아니므로 무시)
     */
    @Transactional
    public void recordFailure(String email, String ip, String userAgent) {
        if (email == null || email.isBlank()) return;
        memberRepository.findByEmail(email)
                .filter(m -> isAdminRole(m.getRole()))
                .ifPresent(m -> save(m.getId(), m.getEmail(), m.getRole(), ip, userAgent, LoginResult.FAIL));
    }

    private void save(Long memberId, String email, MemberRole role,
                      String ip, String userAgent, LoginResult result) {
        try {
            adminLoginLogRepository.save(AdminLoginLog.builder()
                    .memberId(memberId)
                    .email(email)
                    .role(role)
                    .ip(ip)
                    .userAgent(truncate(userAgent, 512))
                    .result(result)
                    .build());
        } catch (Exception e) {
            // 감사 로그 적재 실패가 로그인 흐름을 막지 않도록 한다.
            log.warn("[AdminLoginLogService] 기록 실패 email={}, result={}", email, result, e);
        }
    }

    private static String truncate(String s, int max) {
        if (s == null) return null;
        return s.length() <= max ? s : s.substring(0, max);
    }

    @Transactional(readOnly = true)
    public List<AdminLoginLogResponse> getRecent() {
        return adminLoginLogRepository.findTop500ByOrderByIdDesc().stream()
                .map(AdminLoginLogResponse::from)
                .toList();
    }
}
