package com.moida.domain.auth;

import com.moida.common.exception.BusinessException;
import com.moida.common.exception.ErrorCode;
import com.solapi.sdk.SolapiClient;
import com.solapi.sdk.message.exception.SolapiMessageNotReceivedException;
import com.solapi.sdk.message.model.Message;
import com.solapi.sdk.message.service.DefaultMessageService;
import jakarta.annotation.PostConstruct;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

/**
 * 솔라피(Solapi) SMS 발송 래퍼.
 *
 * 키/발신번호(application.yml → 환경변수 SOLAPI_*)가 비어 있으면 "개발 모드"로 동작하여
 * 실제 발송 없이 인증번호를 로그로만 남긴다. (로컬 개발/테스트에서 키 없이도 인증 흐름 검증 가능)
 *
 * ※ SDK 버전(1.0.3)에 따라 패키지 경로가 다를 수 있다. 컴파일 오류 시 import 만 조정하면 된다.
 */
@Slf4j
@Service
public class SmsService {

    @Value("${solapi.api-key:}")
    private String apiKey;

    @Value("${solapi.api-secret:}")
    private String apiSecret;

    @Value("${solapi.sender:}")
    private String sender;

    private DefaultMessageService messageService;

    @PostConstruct
    void init() {
        if (isConfigured()) {
            this.messageService = SolapiClient.INSTANCE.createInstance(apiKey, apiSecret);
            log.info("[SmsService] 솔라피 초기화 완료 (발신번호={})", sender);
        } else {
            log.warn("[SmsService] 솔라피 설정이 비어 있어 개발 모드로 동작합니다. (실제 발송 없이 인증번호를 로그로 출력)");
        }
    }

    private boolean isConfigured() {
        return notBlank(apiKey) && notBlank(apiSecret) && notBlank(sender);
    }

    private boolean notBlank(String v) {
        return v != null && !v.isBlank();
    }

    /** 인증번호 SMS 발송. to 는 숫자만(하이픈 없이) 권장. */
    public void sendVerificationCode(String to, String code) {
        String text = "[모이다] 인증번호 [" + code + "] 를 입력해주세요.";

        // 개발 모드: 실제 발송하지 않고 코드만 로깅.
        if (!isConfigured() || messageService == null) {
            log.warn("[SmsService] (개발모드) SMS 미발송 — to={}, code={}", to, code);
            return;
        }

        try {
            Message message = new Message();
            message.setFrom(sender);
            message.setTo(to);
            message.setText(text);
            messageService.send(message);
        } catch (SolapiMessageNotReceivedException e) {
            log.error("[SmsService] 발송 실패 목록={}, message={}", e.getFailedMessageList(), e.getMessage());
            throw new BusinessException(ErrorCode.SMS_SEND_FAILED);
        } catch (Exception e) {
            log.error("[SmsService] SMS 발송 중 오류", e);
            throw new BusinessException(ErrorCode.SMS_SEND_FAILED);
        }
    }
}
