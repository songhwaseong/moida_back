package com.moida.domain.auth;

import com.moida.common.exception.BusinessException;
import com.moida.common.exception.ErrorCode;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.security.SecureRandom;
import java.time.Duration;
import java.time.LocalDate;
import java.time.LocalDateTime;

@Slf4j
@Service
@RequiredArgsConstructor
public class PhoneVerificationService {

    /** 인증번호 유효시간(발송 후). */
    private static final Duration CODE_TTL = Duration.ofMinutes(3);
    /** 인증 완료 후 가입에 사용할 수 있는 유효시간. */
    private static final Duration VERIFIED_TTL = Duration.ofMinutes(30);
    /** 재전송 쿨다운. */
    private static final Duration RESEND_COOLDOWN = Duration.ofSeconds(30);
    /** 코드 입력 최대 시도 횟수. */
    private static final int MAX_ATTEMPTS = 5;
    /** 번호당 하루 최대 발송 횟수(문자 폭탄/비용 방어). */
    private static final int MAX_DAILY_SENDS = 5;

    private static final SecureRandom RANDOM = new SecureRandom();

    private final PhoneVerificationRepository repository;
    private final SmsService smsService;

    /** 인증번호 발급 + SMS 발송. 번호당 1행을 재사용한다. */
    @Transactional
    public void sendCode(String rawPhone, VerificationPurpose purpose) {
        requirePhonePurpose(purpose);
        String phone = normalize(rawPhone);
        if (phone.isBlank()) {
            throw new BusinessException(ErrorCode.INVALID_INPUT, "휴대폰 번호를 입력해주세요.");
        }

        LocalDateTime now = LocalDateTime.now();
        LocalDate today = now.toLocalDate();
        String code = generateCode();

        PhoneVerification verification = repository.findByPhone(phone).orElse(null);
        if (verification == null) {
            PhoneVerification created = PhoneVerification.issue(phone, code, now.plus(CODE_TTL), purpose);
            created.recordSend(today);
            repository.save(created);
        } else {
            // 하루 총량 초과 차단 (연타 쿨다운만으로는 막지 못하는 누적 발송 폭탄/비용 방어).
            if (verification.hasReachedDailyLimit(today, MAX_DAILY_SENDS)) {
                throw new BusinessException(ErrorCode.VERIFICATION_DAILY_LIMIT);
            }
            // 직전 발송 직후 연타 방지.
            if (verification.getUpdatedAt() != null
                    && Duration.between(verification.getUpdatedAt(), now).compareTo(RESEND_COOLDOWN) < 0) {
                throw new BusinessException(ErrorCode.VERIFICATION_RESEND_COOLDOWN);
            }
            verification.renew(code, now.plus(CODE_TTL), purpose);
            verification.recordSend(today);
        }

        smsService.sendVerificationCode(phone, code);
    }

    /** 인증번호 검증. 성공 시 verified 로 표시한다. */
    @Transactional
    public void verifyCode(String rawPhone, String code, VerificationPurpose purpose) {
        requirePhonePurpose(purpose);
        String phone = normalize(rawPhone);
        PhoneVerification verification = repository.findByPhone(phone)
                .orElseThrow(() -> new BusinessException(ErrorCode.VERIFICATION_NOT_FOUND));

        if (!verification.isFor(purpose)) {
            throw new BusinessException(ErrorCode.VERIFICATION_NOT_FOUND);
        }

        LocalDateTime now = LocalDateTime.now();
        if (verification.isExpired(now)) {
            throw new BusinessException(ErrorCode.VERIFICATION_CODE_EXPIRED);
        }
        if (verification.getAttemptCount() >= MAX_ATTEMPTS) {
            throw new BusinessException(ErrorCode.VERIFICATION_TOO_MANY_ATTEMPTS);
        }
        if (code == null || !code.equals(verification.getCode())) {
            verification.increaseAttempt();
            throw new BusinessException(ErrorCode.VERIFICATION_CODE_MISMATCH);
        }
        verification.markVerified(now);
    }

    /** 가입 등에서 사용: 해당 번호가 최근에 인증 완료되었는지. */
    @Transactional
    public boolean consumeVerified(String rawPhone, VerificationPurpose purpose) {
        requirePhonePurpose(purpose);
        String phone = normalize(rawPhone);
        PhoneVerification verification = repository.findByPhoneForUpdate(phone)
                .orElseThrow(() -> new BusinessException(ErrorCode.PHONE_NOT_VERIFIED));
        if (!verification.isFor(purpose)
                || !verification.isVerifiedWithin(LocalDateTime.now(), VERIFIED_TTL)) {
            throw new BusinessException(ErrorCode.PHONE_NOT_VERIFIED);
        }
        verification.consume();
        return true;
    }

    private String generateCode() {
        return String.format("%06d", RANDOM.nextInt(1_000_000));
    }

    private String normalize(String phone) {
        return phone == null ? "" : phone.replaceAll("[^0-9]", "");
    }

    private void requirePhonePurpose(VerificationPurpose purpose) {
        if (purpose == null || !purpose.supportsPhone()) {
            throw new BusinessException(ErrorCode.INVALID_INPUT, "Invalid phone verification purpose.");
        }
    }
}
