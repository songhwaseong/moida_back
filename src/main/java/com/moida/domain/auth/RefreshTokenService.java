package com.moida.domain.auth;

import com.moida.common.exception.BusinessException;
import com.moida.common.exception.ErrorCode;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.HexFormat;

/**
 * 서버측 refresh 토큰 저장소 관리.
 *
 * 회원당 1개의 refresh 토큰(해시)만 보관하며, 발급/갱신 시 교체(store)·검증(validate)·폐기(revoke)를 제공한다.
 */
@Service
@RequiredArgsConstructor
public class RefreshTokenService {

    private final RefreshTokenRepository refreshTokenRepository;

    /** 발급/갱신된 refresh 토큰을 회원별로 저장(교체)한다. 같은 회원의 이전 토큰은 무효화된다. */
    @Transactional
    public void store(Long memberId, String rawToken, long validityMs) {
        String hash = hash(rawToken);
        LocalDateTime expiresAt = LocalDateTime.now().plus(validityMs, ChronoUnit.MILLIS);
        refreshTokenRepository.findByMemberId(memberId)
                .ifPresentOrElse(
                        token -> token.rotate(hash, expiresAt),
                        () -> refreshTokenRepository.save(RefreshToken.create(memberId, hash, expiresAt))
                );
    }

    /** 제시된 refresh 토큰이 현재 저장된(= 가장 최근 발급된) 토큰과 일치하고 만료되지 않았는지 검증. */
    @Transactional(readOnly = true)
    public void validate(Long memberId, String rawToken) {
        RefreshToken stored = refreshTokenRepository.findByMemberId(memberId)
                .orElseThrow(() -> new BusinessException(ErrorCode.INVALID_TOKEN,
                        "로그아웃되었거나 만료된 세션입니다. 다시 로그인해주세요."));
        if (!stored.matches(hash(rawToken))) {
            // 이미 회전된(이전) 토큰이거나 위조 → 거부.
            // 현재 유효 토큰은 그대로 두어, 옛 토큰 제시만으로 정상 세션이 끊기지 않게 한다.
            throw new BusinessException(ErrorCode.INVALID_TOKEN,
                    "이미 갱신되었거나 유효하지 않은 refresh 토큰입니다.");
        }
        if (stored.isExpired(LocalDateTime.now())) {
            throw new BusinessException(ErrorCode.EXPIRED_TOKEN);
        }
    }

    /** 로그아웃/강제 만료: 회원의 저장된 refresh 토큰을 폐기해 더 이상 갱신할 수 없게 한다. */
    @Transactional
    public void revoke(Long memberId) {
        refreshTokenRepository.deleteByMemberId(memberId);
    }

    private String hash(String raw) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] hashed = digest.digest(raw.getBytes(StandardCharsets.UTF_8));
            return HexFormat.of().formatHex(hashed);
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException("SHA-256 not available", e);
        }
    }
}
