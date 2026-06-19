package com.moida.domain.auth;

import com.moida.common.exception.BusinessException;
import com.moida.common.exception.ErrorCode;
import com.moida.domain.member.Member;
import com.moida.domain.member.MemberRepository;
import com.moida.security.JwtTokenProvider;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.SecureRandom;
import java.time.LocalDateTime;
import java.util.Base64;
import java.util.HexFormat;

@Service
@RequiredArgsConstructor
public class WebSocketTicketService {

    private static final int TTL_SECONDS = 30;
    private static final SecureRandom RANDOM = new SecureRandom();

    private final WebSocketTicketRepository ticketRepository;
    private final MemberRepository memberRepository;
    private final JwtTokenProvider jwtTokenProvider;

    @Transactional
    public IssuedTicket issue(Long memberId) {
        ticketRepository.deleteByExpiresAtBefore(LocalDateTime.now());
        Member member = memberRepository.findById(memberId)
                .filter(Member::isActive)
                .orElseThrow(() -> new BusinessException(ErrorCode.UNAUTHORIZED));
        byte[] bytes = new byte[32];
        RANDOM.nextBytes(bytes);
        String rawTicket = Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);
        ticketRepository.save(WebSocketTicket.create(
                hash(rawTicket), member.getId(), LocalDateTime.now().plusSeconds(TTL_SECONDS)));
        return new IssuedTicket(rawTicket, TTL_SECONDS);
    }

    @Transactional
    public Authentication consume(String rawTicket) {
        if (rawTicket == null || rawTicket.isBlank()) {
            throw new BusinessException(ErrorCode.UNAUTHORIZED);
        }
        WebSocketTicket ticket = ticketRepository.findByHashForUpdate(hash(rawTicket))
                .orElseThrow(() -> new BusinessException(ErrorCode.UNAUTHORIZED));
        if (!ticket.canConsume(LocalDateTime.now())) {
            throw new BusinessException(ErrorCode.UNAUTHORIZED);
        }
        Member member = memberRepository.findById(ticket.getMemberId())
                .filter(Member::isActive)
                .orElseThrow(() -> new BusinessException(ErrorCode.UNAUTHORIZED));
        ticket.consume();
        return jwtTokenProvider.createAuthentication(member);
    }

    private String hash(String value) {
        try {
            byte[] digest = MessageDigest.getInstance("SHA-256")
                    .digest(value.getBytes(StandardCharsets.UTF_8));
            return HexFormat.of().formatHex(digest);
        } catch (Exception ex) {
            throw new IllegalStateException("SHA-256 is unavailable.", ex);
        }
    }

    public record IssuedTicket(String ticket, int expiresInSeconds) {
    }
}
