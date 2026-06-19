package com.moida.security;

import com.moida.domain.member.MemberRole;
import com.moida.domain.member.Member;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.stereotype.Component;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.util.Date;
import java.util.List;

@Slf4j
@Component
public class JwtTokenProvider {

    private final SecretKey key;
    private final long accessTokenValidity;
    private final long refreshTokenValidity;

    public JwtTokenProvider(
            @Value("${jwt.secret}") String secret,
            @Value("${jwt.access-token-validity}") long accessTokenValidity,
            @Value("${jwt.refresh-token-validity}") long refreshTokenValidity
    ) {
        this.key = Keys.hmacShaKeyFor(secret.getBytes(StandardCharsets.UTF_8));
        this.accessTokenValidity = accessTokenValidity;
        this.refreshTokenValidity = refreshTokenValidity;
    }

    /** 토큰 종류 구분 클레임. access 를 refresh 로(또는 그 반대로) 오용하는 것을 막는다. */
    public static final String CLAIM_TYPE = "type";
    public static final String TYPE_ACCESS = "access";
    public static final String TYPE_REFRESH = "refresh";

    public String createAccessToken(Long memberId, String email, MemberRole role, long tokenVersion) {
        return createToken(memberId, email, role, tokenVersion, accessTokenValidity, TYPE_ACCESS);
    }

    public String createRefreshToken(Long memberId, String email, MemberRole role, long tokenVersion) {
        return createToken(memberId, email, role, tokenVersion, refreshTokenValidity, TYPE_REFRESH);
    }

    /** refresh 토큰 유효기간(ms). 서버측 refresh 토큰 저장 시 만료시각 산정에 사용한다. */
    public long getRefreshTokenValidity() {
        return refreshTokenValidity;
    }

    private String createToken(Long memberId, String email, MemberRole role, long tokenVersion,
                               long validity, String type) {
        Date now = new Date();
        Date expiry = new Date(now.getTime() + validity);

        return Jwts.builder()
                .subject(String.valueOf(memberId))
                .claim("email", email)
                .claim("role", role.name())
                .claim("ver", tokenVersion)
                .claim(CLAIM_TYPE, type)
                .issuedAt(now)
                .expiration(expiry)
                .signWith(key)
                .compact();
    }

    public Authentication getAuthentication(String token) {
        Claims claims = parseClaims(token);
        Long memberId = Long.parseLong(claims.getSubject());
        String email = claims.get("email", String.class);
        String role = claims.get("role", String.class);

        List<SimpleGrantedAuthority> authorities = List.of(new SimpleGrantedAuthority("ROLE_" + role));
        CustomUserDetails principal = new CustomUserDetails(memberId, email, "", authorities);
        return new UsernamePasswordAuthenticationToken(principal, "", authorities);
    }

    /** 토큰의 type 클레임이 기대한 종류인지 확인. (서명/만료 검증은 validateToken 으로 별도 수행) */
    public boolean isTokenType(String token, String expectedType) {
        try {
            return expectedType.equals(parseClaims(token).get(CLAIM_TYPE, String.class));
        } catch (Exception e) {
            return false;
        }
    }

    public Authentication createAuthentication(Member member) {
        List<SimpleGrantedAuthority> authorities = List.of(
                new SimpleGrantedAuthority("ROLE_" + member.getRole().name()));
        CustomUserDetails principal = new CustomUserDetails(
                member.getId(), member.getEmail(), "", authorities);
        return new UsernamePasswordAuthenticationToken(principal, "", authorities);
    }

    public long getTokenVersion(String token) {
        Number version = parseClaims(token).get("ver", Number.class);
        return version == null ? 0L : version.longValue();
    }

    public boolean validateToken(String token) {
        try {
            parseClaims(token);
            return true;
        } catch (io.jsonwebtoken.security.SecurityException | io.jsonwebtoken.MalformedJwtException e) {
            log.warn("Invalid JWT signature");
        } catch (io.jsonwebtoken.ExpiredJwtException e) {
            log.warn("Expired JWT token");
        } catch (io.jsonwebtoken.UnsupportedJwtException e) {
            log.warn("Unsupported JWT token");
        } catch (IllegalArgumentException e) {
            log.warn("JWT claims string is empty");
        }
        return false;
    }

    private Claims parseClaims(String token) {
        return Jwts.parser()
                .verifyWith(key)
                .build()
                .parseSignedClaims(token)
                .getPayload();
    }
}
