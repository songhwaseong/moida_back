package com.moida.domain.passwordless;

import com.moida.common.exception.BusinessException;
import com.moida.common.exception.ErrorCode;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.time.Clock;
import java.util.Base64;

@Component
public class PasswordlessRequestTokenService {

    private static final String HMAC_ALGORITHM = "HmacSHA256";

    private final byte[] signingKey;
    private final Clock clock;

    public PasswordlessRequestTokenService(@Value("${jwt.secret}") String jwtSecret) {
        this.signingKey = jwtSecret.getBytes(StandardCharsets.UTF_8);
        this.clock = Clock.systemUTC();
    }

    public String create(Long memberId, String sessionId, String random, int expiresInSeconds) {
        long expiresAt = clock.millis() + expiresInSeconds * 1000L;
        String payload = memberId + "."
                + encode(sessionId) + "."
                + encode(random) + "."
                + expiresAt;
        return payload + "." + sign(payload);
    }

    public PasswordlessRequestContext parse(String token) {
        String[] parts = token == null ? new String[0] : token.split("\\.");
        if (parts.length != 5) {
            throw new BusinessException(ErrorCode.INVALID_TOKEN, "Passwordless 인증 요청 정보가 올바르지 않습니다.");
        }

        String payload = parts[0] + "." + parts[1] + "." + parts[2] + "." + parts[3];
        if (!constantTimeEquals(sign(payload), parts[4])) {
            throw new BusinessException(ErrorCode.INVALID_TOKEN, "Passwordless 인증 요청 정보가 올바르지 않습니다.");
        }

        long expiresAt;
        try {
            expiresAt = Long.parseLong(parts[3]);
        } catch (NumberFormatException ex) {
            throw new BusinessException(ErrorCode.INVALID_TOKEN, "Passwordless 인증 요청 정보가 올바르지 않습니다.");
        }

        if (expiresAt < clock.millis()) {
            throw new BusinessException(ErrorCode.EXPIRED_TOKEN, "Passwordless 인증 요청 시간이 만료되었습니다.");
        }

        try {
            return new PasswordlessRequestContext(
                    Long.parseLong(parts[0]),
                    decode(parts[1]),
                    decode(parts[2]),
                    expiresAt
            );
        } catch (IllegalArgumentException ex) {
            throw new BusinessException(ErrorCode.INVALID_TOKEN, "Passwordless 인증 요청 정보가 올바르지 않습니다.");
        }
    }

    private String sign(String payload) {
        try {
            Mac mac = Mac.getInstance(HMAC_ALGORITHM);
            mac.init(new SecretKeySpec(signingKey, HMAC_ALGORITHM));
            return Base64.getUrlEncoder().withoutPadding()
                    .encodeToString(mac.doFinal(payload.getBytes(StandardCharsets.UTF_8)));
        } catch (Exception ex) {
            throw new BusinessException(ErrorCode.INTERNAL_SERVER_ERROR, "Passwordless 인증 요청 정보를 생성하지 못했습니다.");
        }
    }

    private String encode(String value) {
        return Base64.getUrlEncoder().withoutPadding()
                .encodeToString(value.getBytes(StandardCharsets.UTF_8));
    }

    private String decode(String value) {
        return new String(Base64.getUrlDecoder().decode(value), StandardCharsets.UTF_8);
    }

    private boolean constantTimeEquals(String expected, String actual) {
        return MessageDigestUtil.isEqual(expected.getBytes(StandardCharsets.UTF_8), actual.getBytes(StandardCharsets.UTF_8));
    }
}
