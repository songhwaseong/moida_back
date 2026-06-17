package com.moida.domain.passwordless;

import com.moida.common.exception.BusinessException;
import com.moida.common.exception.ErrorCode;
import com.moida.common.response.LoginResponse;
import com.moida.common.response.PasswordlessLoginCompleteResponse;
import com.moida.common.response.PasswordlessLoginStartResponse;
import com.moida.common.response.PasswordlessRegistrationStartResponse;
import com.moida.domain.auth.EmailVerificationService;
import com.moida.domain.auth.RefreshTokenService;
import com.moida.domain.member.Member;
import com.moida.domain.member.MemberRepository;
import com.moida.security.JwtTokenProvider;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

@Service
@RequiredArgsConstructor
public class PasswordlessService {

    private static final int REQUEST_EXPIRES_IN_SECONDS = 60;
    private static final String STATUS_APPROVED = "APPROVED";
    private static final String STATUS_DENIED = "DENIED";
    private static final String STATUS_PENDING = "PENDING";

    private final MemberRepository memberRepository;
    private final PasswordlessClient passwordlessClient;
    private final PasswordlessRequestTokenService requestTokenService;
    private final JwtTokenProvider jwtTokenProvider;
    private final PasswordEncoder passwordEncoder;
    private final EmailVerificationService emailVerificationService;
    private final RefreshTokenService refreshTokenService;

    @Transactional(readOnly = true)
    public boolean isRegistered(Long memberId) {
        Member member = getActiveMember(memberId);
        return passwordlessClient.isRegistered(member.getId());
    }

    /**
     * 등록 확정. 외부 Passwordless 서버에 실제로 등록됐는지 확인하고,
     * 등록됐으면 로컬 플래그를 켜서 이후 일반 로그인을 차단한다.
     */
    @Transactional
    public boolean confirmRegistration(Long memberId) {
        Member member = getActiveMember(memberId);
        boolean registered = passwordlessClient.isRegistered(member.getId());
        if (registered) {
            member.enablePasswordless();
        }
        return registered;
    }

    @Transactional(readOnly = true)
    public PasswordlessRegistrationStartResponse startRegistration(Long memberId) {
        Member member = getActiveMember(memberId);
        PasswordlessRegistrationData registration = passwordlessClient.join(member.getId());
        return new PasswordlessRegistrationStartResponse(
                registration.qr(),
                registration.corpId(),
                registration.registerKey(),
                registration.terms(),
                registration.serverUrl(),
                registration.userId(),
                registration.pushConnectorUrl(),
                registration.pushConnectorToken(),
                REQUEST_EXPIRES_IN_SECONDS
        );
    }

    /** 로그인 세션이 있는 회원이 직접 해지하는 경로. */
    @Transactional
    public void withdraw(Long memberId) {
        Member member = getActiveMember(memberId);
        passwordlessClient.withdraw(member.getId());
        member.disablePasswordless();
    }

    /**
     * 평상시 해지 — 비밀번호 확인.
     * 일반 로그인(/auth/login)이 차단된 상태에서도 해지가 가능하도록
     * 로그인 세션 발급 없이 이메일+비밀번호만 직접 검증한다.
     */
    @Transactional
    public void withdrawByPassword(String email, String password) {
        Member member = getActiveMemberByEmail(email);
        if (!passwordEncoder.matches(password, member.getPassword())) {
            throw new BusinessException(ErrorCode.INVALID_PASSWORD);
        }
        passwordlessClient.withdraw(member.getId());
        member.disablePasswordless();
    }

    /**
     * 분실 복구 해지 — 이메일 인증.
     * 휴대폰/앱 분실로 Passwordless 인증도 비밀번호도 쓸 수 없을 때
     * 이메일 인증 코드만으로 해지해 락아웃을 방지한다.
     */
    @Transactional
    public void withdrawByEmail(String email) {
        Member member = getActiveMemberByEmail(email);
        if (!emailVerificationService.isVerified(email)) {
            throw new BusinessException(ErrorCode.EMAIL_VERIFICATION_NOT_FOUND, "이메일 인증을 먼저 완료해주세요.");
        }
        passwordlessClient.withdraw(member.getId());
        member.disablePasswordless();
    }

    @Transactional(readOnly = true)
    public PasswordlessLoginStartResponse startLogin(String email, String clientIp) {
        Member member = memberRepository.findByEmail(email)
                .orElseThrow(() -> new BusinessException(ErrorCode.MEMBER_NOT_FOUND));
        assertActive(member);

        if (!passwordlessClient.isRegistered(member.getId())) {
            throw new BusinessException(ErrorCode.INVALID_INPUT, "Passwordless가 등록되지 않은 회원입니다.");
        }

        String sessionId = System.currentTimeMillis() + "_sessionId";
        String random = UUID.randomUUID().toString();
        String oneTimeToken = passwordlessClient.issueOneTimeToken(member.getId());
        PasswordlessAuthenticationData authData = passwordlessClient.requestAuthentication(member.getId(), oneTimeToken, clientIp, sessionId, random);
        String requestToken = requestTokenService.create(member.getId(), sessionId, random, REQUEST_EXPIRES_IN_SECONDS);

        return new PasswordlessLoginStartResponse(
                requestToken,
                sessionId,
                authData.servicePassword(),
                passwordlessClient.getPushConnectorUrl(),
                authData.pushConnectorToken(),
                REQUEST_EXPIRES_IN_SECONDS
        );
    }

    @Transactional
    public PasswordlessLoginCompleteResponse completeLogin(String requestToken) {
        PasswordlessRequestContext context = requestTokenService.parse(requestToken);
        Member member = getActiveMember(context.memberId());

        String auth = passwordlessClient.getResult(member.getId(), context.sessionId(), context.random());
        if ("Y".equalsIgnoreCase(auth)) {
            member.updateLastLogin();
            String accessToken = jwtTokenProvider.createAccessToken(member.getId(), member.getEmail(), member.getRole());
            String refreshToken = jwtTokenProvider.createRefreshToken(member.getId(), member.getEmail(), member.getRole());
            refreshTokenService.store(member.getId(), refreshToken, jwtTokenProvider.getRefreshTokenValidity());
            return new PasswordlessLoginCompleteResponse(
                    STATUS_APPROVED,
                    new LoginResponse(accessToken, refreshToken, member, false)
            );
        }

        if ("N".equalsIgnoreCase(auth)) {
            return new PasswordlessLoginCompleteResponse(STATUS_DENIED, null);
        }

        return new PasswordlessLoginCompleteResponse(STATUS_PENDING, null);
    }

    @Transactional(readOnly = true)
    public void cancelLogin(String requestToken) {
        PasswordlessRequestContext context = requestTokenService.parse(requestToken);
        Member member = getActiveMember(context.memberId());
        passwordlessClient.cancel(member.getId(), context.sessionId(), context.random());
    }

    private Member getActiveMember(Long memberId) {
        Member member = memberRepository.findById(memberId)
                .orElseThrow(() -> new BusinessException(ErrorCode.MEMBER_NOT_FOUND));
        assertActive(member);
        return member;
    }

    private Member getActiveMemberByEmail(String email) {
        Member member = memberRepository.findByEmail(email)
                .orElseThrow(() -> new BusinessException(ErrorCode.MEMBER_NOT_FOUND));
        assertActive(member);
        return member;
    }

    private void assertActive(Member member) {
        if (!member.isActive()) {
            throw new BusinessException(ErrorCode.SUSPENDED_MEMBER);
        }
    }
}
