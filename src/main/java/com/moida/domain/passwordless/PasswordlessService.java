package com.moida.domain.passwordless;

import com.moida.common.exception.BusinessException;
import com.moida.common.exception.ErrorCode;
import com.moida.common.response.LoginResponse;
import com.moida.common.response.PasswordlessLoginCompleteResponse;
import com.moida.common.response.PasswordlessLoginStartResponse;
import com.moida.common.response.PasswordlessRegistrationStartResponse;
import com.moida.domain.member.Member;
import com.moida.domain.member.MemberRepository;
import com.moida.security.JwtTokenProvider;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

@Service
@RequiredArgsConstructor
public class PasswordlessService {

    private static final int REQUEST_EXPIRES_IN_SECONDS = 300;
    private static final String STATUS_APPROVED = "APPROVED";
    private static final String STATUS_DENIED = "DENIED";
    private static final String STATUS_PENDING = "PENDING";

    private final MemberRepository memberRepository;
    private final PasswordlessClient passwordlessClient;
    private final PasswordlessRequestTokenService requestTokenService;
    private final JwtTokenProvider jwtTokenProvider;

    @Transactional(readOnly = true)
    public boolean isRegistered(Long memberId) {
        Member member = getActiveMember(memberId);
        return passwordlessClient.isRegistered(member.getId());
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

    @Transactional(readOnly = true)
    public void withdraw(Long memberId) {
        Member member = getActiveMember(memberId);
        passwordlessClient.withdraw(member.getId());
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

    private void assertActive(Member member) {
        if (!member.isActive()) {
            throw new BusinessException(ErrorCode.SUSPENDED_MEMBER);
        }
    }
}
