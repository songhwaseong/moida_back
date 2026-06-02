package com.moida.config;

import com.moida.domain.terms.TermsDocument;
import com.moida.domain.terms.TermsDocumentRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;

@Component
@RequiredArgsConstructor
public class TermsDataInitializer implements ApplicationRunner {

    private final TermsDocumentRepository termsDocumentRepository;

    @Override
    @Transactional
    public void run(ApplicationArguments args) {
        createIfAbsent(TermsDocument.TermsType.TERMS, "이용약관", termsContent());
        createIfAbsent(TermsDocument.TermsType.PRIVACY, "개인정보처리방침", privacyContent());
    }

    private void createIfAbsent(TermsDocument.TermsType type, String title, String content) {
        if (termsDocumentRepository.existsByType(type)) {
            return;
        }

        termsDocumentRepository.save(TermsDocument.builder()
                .type(type)
                .title(title)
                .content(content)
                .effectiveDate(LocalDate.of(2026, 6, 1))
                .active(true)
                .build());
    }

    private String termsContent() {
        return """
                제1조 (목적)
                이 약관은 MOIDA가 제공하는 중고거래 및 경매 서비스의 이용과 관련하여 회사와 이용자의 권리, 의무 및 책임 사항을 규정함을 목적으로 합니다.

                제2조 (정의)
                "이용자"란 이 약관에 따라 회사가 제공하는 서비스를 이용하는 회원 및 비회원을 말합니다. "회원"이란 회사와 서비스 이용계약을 체결하고 계정을 부여받은 자를 말합니다.

                제3조 (약관의 효력 및 변경)
                이 약관은 서비스 화면에 게시하거나 기타의 방법으로 이용자에게 공지함으로써 효력이 발생합니다. 회사는 관련 법령을 위배하지 않는 범위에서 약관을 개정할 수 있습니다.

                제4조 (서비스의 제공)
                회사는 중고물품 거래 중개, 온라인 경매, 안전결제, 알림 및 고객지원 서비스를 제공합니다. 서비스의 구체적인 내용은 운영 정책에 따라 변경될 수 있습니다.

                제5조 (이용자의 의무)
                이용자는 타인의 정보를 도용하거나 허위 정보를 등록해서는 안 되며, 불법 물품 거래, 외부 직거래 유도, 서비스 운영을 방해하는 행위를 해서는 안 됩니다.

                제6조 (책임 제한)
                회사는 이용자 간 거래의 중개자로서 거래 당사자가 아니며, 이용자의 귀책 사유로 발생한 손해에 대하여 관련 법령에서 정한 범위를 초과하여 책임지지 않습니다.
                """;
    }

    private String privacyContent() {
        return """
                1. 수집하는 개인정보 항목
                회사는 회원가입, 거래 진행, 고객지원 제공을 위해 이메일, 비밀번호, 이름, 닉네임, 휴대폰 번호, 주소, 거래 및 결제 관련 정보를 수집할 수 있습니다.

                2. 개인정보의 수집 및 이용 목적
                수집한 개인정보는 회원 식별, 서비스 제공, 거래 및 배송 처리, 문의 응대, 부정 이용 방지, 서비스 개선을 위해 이용됩니다.

                3. 보유 및 이용 기간
                개인정보는 회원 탈퇴 시까지 보유하며, 탈퇴 후에는 지체 없이 파기합니다. 다만 관계 법령에 따라 보관이 필요한 정보는 해당 기간 동안 보관할 수 있습니다.

                4. 개인정보의 제3자 제공
                회사는 이용자의 동의 없이 개인정보를 외부에 제공하지 않습니다. 단, 법령에 근거가 있거나 거래 이행에 필요한 범위에서는 예외가 적용될 수 있습니다.

                5. 개인정보 보호 책임자
                개인정보 처리와 관련한 문의는 고객센터를 통해 접수할 수 있으며, 회사는 관련 법령에 따라 신속하게 답변하고 처리합니다.
                """;
    }
}
