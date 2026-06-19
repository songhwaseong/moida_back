package com.moida.domain.auth;

import com.moida.common.exception.BusinessException;
import com.moida.common.exception.ErrorCode;
import com.moida.common.response.OAuthPrepareResponse;
import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseCookie;
import org.springframework.stereotype.Service;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.SecureRandom;
import java.time.Duration;
import java.time.Instant;
import java.util.Base64;
import java.util.Locale;

@Service
public class OAuthFlowService {

    private static final String COOKIE_NAME = "MOIDA_OAUTH_FLOW";
    private static final Duration FLOW_TTL = Duration.ofMinutes(5);
    private static final SecureRandom RANDOM = new SecureRandom();

    private final byte[] signingKey;
    private final boolean secure;

    public OAuthFlowService(
            @Value("${jwt.secret}") String secret,
            @Value("${auth.cookie.secure:false}") boolean secure
    ) {
        this.signingKey = secret.getBytes(StandardCharsets.UTF_8);
        this.secure = secure;
    }

    public OAuthPrepareResponse prepare(String rawProvider, HttpServletResponse response) {
        String provider = normalizeProvider(rawProvider);
        String state = randomToken(32);
        String verifier = randomToken(64);
        long expiresAt = Instant.now().plus(FLOW_TTL).getEpochSecond();
        String payload = String.join(".", provider, state, verifier, String.valueOf(expiresAt));
        String value = payload + "." + sign(payload);
        addCookie(response, value, FLOW_TTL);

        boolean pkce = "GOOGLE".equals(provider);
        return new OAuthPrepareResponse(state, pkce ? challenge(verifier) : null, pkce);
    }

    public OAuthContext consume(String rawProvider, String returnedState,
                                HttpServletRequest request, HttpServletResponse response) {
        String provider = normalizeProvider(rawProvider);
        String value = findCookie(request);
        clear(response);
        if (value == null) throw invalidFlow();

        String[] parts = value.split("\\.");
        if (parts.length != 5) throw invalidFlow();
        String payload = String.join(".", parts[0], parts[1], parts[2], parts[3]);
        if (!MessageDigest.isEqual(
                sign(payload).getBytes(StandardCharsets.UTF_8),
                parts[4].getBytes(StandardCharsets.UTF_8))) {
            throw invalidFlow();
        }
        long expiresAt;
        try {
            expiresAt = Long.parseLong(parts[3]);
        } catch (NumberFormatException ex) {
            throw invalidFlow();
        }
        if (!provider.equals(parts[0])
                || returnedState == null
                || !MessageDigest.isEqual(
                        parts[1].getBytes(StandardCharsets.UTF_8),
                        returnedState.getBytes(StandardCharsets.UTF_8))
                || Instant.now().getEpochSecond() > expiresAt) {
            throw invalidFlow();
        }
        return new OAuthContext(provider, parts[2]);
    }

    private String normalizeProvider(String provider) {
        String normalized = provider == null ? "" : provider.trim().toUpperCase(Locale.ROOT);
        if (!normalized.equals("KAKAO") && !normalized.equals("NAVER") && !normalized.equals("GOOGLE")) {
            throw invalidFlow();
        }
        return normalized;
    }

    private String randomToken(int bytesLength) {
        byte[] bytes = new byte[bytesLength];
        RANDOM.nextBytes(bytes);
        return Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);
    }

    private String challenge(String verifier) {
        try {
            byte[] digest = MessageDigest.getInstance("SHA-256")
                    .digest(verifier.getBytes(StandardCharsets.US_ASCII));
            return Base64.getUrlEncoder().withoutPadding().encodeToString(digest);
        } catch (Exception ex) {
            throw new IllegalStateException("SHA-256 is unavailable.", ex);
        }
    }

    private String sign(String payload) {
        try {
            Mac mac = Mac.getInstance("HmacSHA256");
            mac.init(new SecretKeySpec(signingKey, "HmacSHA256"));
            return Base64.getUrlEncoder().withoutPadding()
                    .encodeToString(mac.doFinal(payload.getBytes(StandardCharsets.UTF_8)));
        } catch (Exception ex) {
            throw new IllegalStateException("OAuth flow signing failed.", ex);
        }
    }

    private String findCookie(HttpServletRequest request) {
        if (request.getCookies() == null) return null;
        for (Cookie cookie : request.getCookies()) {
            if (COOKIE_NAME.equals(cookie.getName())) return cookie.getValue();
        }
        return null;
    }

    private void addCookie(HttpServletResponse response, String value, Duration maxAge) {
        ResponseCookie cookie = ResponseCookie.from(COOKIE_NAME, value)
                .httpOnly(true)
                .secure(secure)
                .sameSite("Lax")
                .path("/api/auth")
                .maxAge(maxAge)
                .build();
        response.addHeader(HttpHeaders.SET_COOKIE, cookie.toString());
    }

    private void clear(HttpServletResponse response) {
        addCookie(response, "", Duration.ZERO);
    }

    private BusinessException invalidFlow() {
        return new BusinessException(ErrorCode.INVALID_TOKEN, "Invalid or expired OAuth flow.");
    }

    public record OAuthContext(String provider, String codeVerifier) {
    }
}
