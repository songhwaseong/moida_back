package com.moida.security;

import com.moida.common.exception.BusinessException;
import com.moida.common.response.InquiryResponse;
import com.moida.common.util.ExternalHttpExecutor;
import com.moida.domain.auth.EmailVerification;
import com.moida.domain.auth.EmailVerificationRepository;
import com.moida.domain.auth.EmailVerificationService;
import com.moida.domain.auth.VerificationPurpose;
import com.moida.domain.inquiry.Inquiry;
import com.moida.domain.member.Member;
import com.moida.domain.member.MemberNoGenerator;
import com.moida.domain.member.MemberRepository;
import com.moida.domain.member.MemberSocialAccountRepository;
import com.moida.domain.member.SocialLoginService;
import com.moida.domain.product.Product;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.client.RestTemplate;

import java.time.LocalDateTime;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class SecurityHardeningTest {

    @Mock
    private EmailVerificationRepository emailVerificationRepository;

    @Mock
    private JavaMailSender mailSender;

    @Mock
    private MemberRepository memberRepository;

    @Mock
    private MemberSocialAccountRepository socialAccountRepository;

    @Mock
    private MemberNoGenerator memberNoGenerator;

    @Mock
    private PasswordEncoder passwordEncoder;

    @Mock
    private RestTemplate restTemplate;

    @Mock
    private ExternalHttpExecutor externalHttpExecutor;

    @InjectMocks
    private EmailVerificationService emailVerificationService;

    @InjectMocks
    private SocialLoginService socialLoginService;

    @Test
    void privateInquiryIsMaskedForUnauthorizedViewer() {
        Inquiry inquiry = privateInquiry();

        InquiryResponse response = InquiryResponse.from(inquiry, false);

        assertThat(response.user()).isEqualTo("Private");
        assertThat(response.question()).isEqualTo("This is a private inquiry.");
        assertThat(response.answer()).isNull();
        assertThat(response.answerDate()).isNull();
    }

    @Test
    void privateInquiryRemainsVisibleToAuthorizedViewer() {
        Inquiry inquiry = privateInquiry();

        InquiryResponse response = InquiryResponse.from(inquiry, true);

        assertThat(response.user()).isEqualTo("buyer");
        assertThat(response.question()).isEqualTo("secret question");
        assertThat(response.answer()).isEqualTo("secret answer");
    }

    @Test
    void verifiedEmailCanOnlyBeConsumedOnceForItsPurpose() {
        EmailVerification verification = EmailVerification.issue(
                "member@example.com",
                "123456",
                LocalDateTime.now().plusMinutes(5),
                VerificationPurpose.RESET_PASSWORD
        );
        verification.markVerified(LocalDateTime.now());
        when(emailVerificationRepository.findByEmailForUpdate("member@example.com"))
                .thenReturn(Optional.of(verification));

        assertThat(emailVerificationService.consumeVerified(
                "member@example.com", VerificationPurpose.RESET_PASSWORD)).isTrue();
        assertThatThrownBy(() -> emailVerificationService.consumeVerified(
                "member@example.com", VerificationPurpose.RESET_PASSWORD))
                .isInstanceOf(BusinessException.class);
    }

    @Test
    void verifiedEmailCannotBeUsedForAnotherPurpose() {
        EmailVerification verification = EmailVerification.issue(
                "member@example.com",
                "123456",
                LocalDateTime.now().plusMinutes(5),
                VerificationPurpose.RESET_PASSWORD
        );
        verification.markVerified(LocalDateTime.now());
        when(emailVerificationRepository.findByEmailForUpdate("member@example.com"))
                .thenReturn(Optional.of(verification));

        assertThatThrownBy(() -> emailVerificationService.consumeVerified(
                "member@example.com", VerificationPurpose.PASSWORDLESS_WITHDRAW))
                .isInstanceOf(BusinessException.class);
    }

    @Test
    void unverifiedSocialEmailIsRejectedBeforeAccountLookup() {
        assertThatThrownBy(() -> socialLoginService.findOrRegisterSocialMember(
                "member@example.com", "member", "GOOGLE", "provider-id", false))
                .isInstanceOf(BusinessException.class);

        verifyNoInteractions(socialAccountRepository, memberRepository);
    }

    @Test
    void existingEmailIsNotAutomaticallyLinkedToSocialAccount() {
        Member existingMember = mock(Member.class);
        when(socialAccountRepository.findByProviderAndProviderUserId(
                com.moida.domain.member.SocialProvider.GOOGLE, "provider-id"))
                .thenReturn(Optional.empty());
        when(memberRepository.findByEmail("member@example.com"))
                .thenReturn(Optional.of(existingMember));

        assertThatThrownBy(() -> socialLoginService.findOrRegisterSocialMember(
                "member@example.com", "member", "GOOGLE", "provider-id", true))
                .isInstanceOf(BusinessException.class);
    }

    private Inquiry privateInquiry() {
        Product product = mock(Product.class);
        Member seller = mock(Member.class);
        Member buyer = mock(Member.class);
        when(product.getName()).thenReturn("product");
        when(seller.getName()).thenReturn("seller");
        lenient().when(buyer.getName()).thenReturn("buyer");

        Inquiry inquiry = Inquiry.builder()
                .product(product)
                .user(buyer)
                .seller(seller)
                .question("secret question")
                .isSecret(true)
                .build();
        inquiry.answer("secret answer");
        return inquiry;
    }
}
