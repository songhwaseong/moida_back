package com.moida.domain.auth;

import com.moida.common.exception.BusinessException;
import com.moida.common.exception.ErrorCode;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.security.SecureRandom;
import java.time.Duration;
import java.time.LocalDateTime;

@Slf4j
@Service
@RequiredArgsConstructor
public class EmailVerificationService {


    private static final Duration CODE_TTL = Duration.ofMinutes(5);
    private static final Duration VERIFIED_TTL = Duration.ofMinutes(30);
    private static final Duration RESEND_COOLDOWN = Duration.ofSeconds(30);
    private static final int MAX_ATTEMPTS = 5;
    private static final SecureRandom RANDOM = new SecureRandom();

    private final EmailVerificationRepository repository;
    private final JavaMailSender mailSender;

    @Transactional
    public void sendCode(String email) {
        LocalDateTime now = LocalDateTime.now();
        String code = String.format("%06d", RANDOM.nextInt(1_000_000));

        EmailVerification verification = repository.findByEmail(email).orElse(null);
        if (verification == null) {
            repository.save(EmailVerification.issue(email, code, now.plus(CODE_TTL)));
        } else {
            if (verification.getUpdatedAt() != null
                    && Duration.between(verification.getUpdatedAt(), now).compareTo(RESEND_COOLDOWN) < 0) {
                throw new BusinessException(ErrorCode.EMAIL_VERIFICATION_RESEND_COOLDOWN);
            }
            verification.renew(code, now.plus(CODE_TTL));
        }

        sendMail(email, code);
    }

    @Transactional
    public void verifyCode(String email, String code) {
        EmailVerification verification = repository.findByEmail(email)
                .orElseThrow(() -> new BusinessException(ErrorCode.EMAIL_VERIFICATION_NOT_FOUND));

        LocalDateTime now = LocalDateTime.now();
        if (verification.isExpired(now)) {
            throw new BusinessException(ErrorCode.EMAIL_VERIFICATION_CODE_EXPIRED);
        }
        if (verification.getAttemptCount() >= MAX_ATTEMPTS) {
            throw new BusinessException(ErrorCode.EMAIL_VERIFICATION_TOO_MANY_ATTEMPTS);
        }
        if (code == null || !code.equals(verification.getCode())) {
            verification.increaseAttempt();
            throw new BusinessException(ErrorCode.EMAIL_VERIFICATION_CODE_MISMATCH);
        }
        verification.markVerified(now);
    }

    @Transactional(readOnly = true)
    public boolean isVerified(String email) {
        return repository.findByEmail(email)
                .map(v -> v.isVerifiedWithin(LocalDateTime.now(), VERIFIED_TTL))
                .orElse(false);
    }

    private void sendMail(String to, String code) {
        try {
            SimpleMailMessage message = new SimpleMailMessage();
            message.setTo(to);
            message.setSubject("[모이다] 이메일 인증 코드");
            message.setText("인증 코드: " + code + "\n\n5분 이내에 입력해주세요.");
            mailSender.send(message);
        } catch (Exception e) {
            log.error("[EmailVerificationService] 메일 발송 실패 to={}", to, e);
            throw new BusinessException(ErrorCode.EMAIL_SEND_FAILED);
        }
    }
}
