package com.moida.domain.auth;

import com.moida.common.exception.BusinessException;
import com.moida.common.exception.ErrorCode;
import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseCookie;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.SecureRandom;
import java.time.Duration;
import java.util.Base64;

@Service
public class AuthCookieService {

    public static final String REFRESH_COOKIE = "MOIDA_REFRESH";
    public static final String CSRF_COOKIE = "XSRF-TOKEN";
    public static final String CSRF_HEADER = "X-XSRF-TOKEN";

    private static final SecureRandom RANDOM = new SecureRandom();

    private final boolean secure;
    private final Duration refreshTtl;

    public AuthCookieService(
            @Value("${auth.cookie.secure:false}") boolean secure,
            @Value("${jwt.refresh-token-validity}") long refreshTokenValidityMs
    ) {
        this.secure = secure;
        this.refreshTtl = Duration.ofMillis(refreshTokenValidityMs);
    }

    public void issue(HttpServletResponse response, String refreshToken) {
        addCookie(response, REFRESH_COOKIE, refreshToken, true, "/api/auth", refreshTtl);
        byte[] csrfBytes = new byte[32];
        RANDOM.nextBytes(csrfBytes);
        String csrfToken = Base64.getUrlEncoder().withoutPadding().encodeToString(csrfBytes);
        addCookie(response, CSRF_COOKIE, csrfToken, false, "/", refreshTtl);
    }

    public String requireRefreshToken(HttpServletRequest request) {
        validateCsrf(request);
        String token = findCookie(request, REFRESH_COOKIE);
        if (!StringUtils.hasText(token)) {
            throw new BusinessException(ErrorCode.INVALID_TOKEN);
        }
        return token;
    }

    public String getRefreshToken(HttpServletRequest request) {
        return findCookie(request, REFRESH_COOKIE);
    }

    public void validateCsrf(HttpServletRequest request) {
        String cookieToken = findCookie(request, CSRF_COOKIE);
        String headerToken = request.getHeader(CSRF_HEADER);
        if (!StringUtils.hasText(cookieToken) || !StringUtils.hasText(headerToken)
                || !MessageDigest.isEqual(
                        cookieToken.getBytes(StandardCharsets.UTF_8),
                        headerToken.getBytes(StandardCharsets.UTF_8))) {
            throw new BusinessException(ErrorCode.ACCESS_DENIED, "Invalid CSRF token.");
        }
    }

    public void clear(HttpServletResponse response) {
        addCookie(response, REFRESH_COOKIE, "", true, "/api/auth", Duration.ZERO);
        addCookie(response, CSRF_COOKIE, "", false, "/", Duration.ZERO);
    }

    private String findCookie(HttpServletRequest request, String name) {
        Cookie[] cookies = request.getCookies();
        if (cookies == null) return null;
        for (Cookie cookie : cookies) {
            if (name.equals(cookie.getName())) return cookie.getValue();
        }
        return null;
    }

    private void addCookie(HttpServletResponse response, String name, String value, boolean httpOnly,
                           String path, Duration maxAge) {
        ResponseCookie cookie = ResponseCookie.from(name, value)
                .httpOnly(httpOnly)
                .secure(secure)
                .sameSite("Strict")
                .path(path)
                .maxAge(maxAge)
                .build();
        response.addHeader(HttpHeaders.SET_COOKIE, cookie.toString());
    }
}
