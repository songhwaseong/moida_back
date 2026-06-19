package com.moida.domain.member;

import com.moida.common.exception.BusinessException;
import com.moida.common.exception.ErrorCode;
import com.moida.common.response.GoogleTokenResponse;
import com.moida.common.response.GoogleUserResponse;
import com.moida.common.response.KakaoTokenResponse;
import com.moida.common.response.KakaoUserResponse;
import com.moida.common.response.NaverTokenResponse;
import com.moida.common.response.NaverUserResponse;
import com.moida.common.util.ExternalHttpExecutor;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.client.RestTemplate;

@Service
@RequiredArgsConstructor
public class SocialLoginService {
    private final MemberRepository memberRepository;
    private final MemberSocialAccountRepository socialAccountRepository;
    private final MemberNoGenerator memberNoGenerator;
    private final PasswordEncoder passwordEncoder;
    private final RestTemplate restTemplate;
    private final ExternalHttpExecutor externalHttpExecutor;

    @Value("${social.kakao.client-id}")
    private String kkoClientId;

    @Value("${social.kakao.redirect-uri}")
    private String kkoRedirectUri;

    @Value("${social.naver.client-id}")
    private String navClientId;

    @Value("${social.naver.client-secret}")
    private String navClientSecret;

    @Value("${social.naver.redirect-uri}")
    private String navRedirectUri;

    @Value("${social.google.client-id}")
    private String googleClientId;

    @Value("${social.google.client-secret}")
    private String googleClientSecret;

    @Value("${social.google.redirect-uri}")
    private String googleRedirectUri;

    public record SocialLoginResult(Member member, boolean newUser) {
    }

    @Transactional
    public SocialLoginResult findOrRegisterSocialMember(String email, String name, String socialType,
                                                        String providerUserId, boolean emailVerified) {
        SocialProvider provider = SocialProvider.from(socialType);
        String normalizedProviderUserId = requireProviderUserId(providerUserId);
        if (!emailVerified || email == null || email.isBlank()) {
            throw new BusinessException(ErrorCode.SOCIAL_EMAIL_NOT_VERIFIED);
        }
        String normalizedEmail = normalizeEmail(email, provider, normalizedProviderUserId);
        String displayName = (name == null || name.isBlank()) ? provider.name() + " 회원" : name.trim();

        return socialAccountRepository.findByProviderAndProviderUserId(provider, normalizedProviderUserId)
                .map(account -> new SocialLoginResult(findMember(account.getMember().getId()), false))
                .orElseGet(() -> linkOrCreateMember(normalizedEmail, displayName, provider, normalizedProviderUserId));
    }

    private SocialLoginResult linkOrCreateMember(String email, String name, SocialProvider provider, String providerUserId) {
        return memberRepository.findByEmail(email)
                .map(member -> rejectAutomaticLink())
                .orElseGet(() -> {
                    Member member = memberRepository.save(Member.builder()
                            .memberNo(memberNoGenerator.generate())
                            .email(email)
                            .password(passwordEncoder.encode(provider.name() + "_SOCIAL"))
                            .name(name)
                            .role(MemberRole.USER)
                            .socialLogin(provider.name())
                            .build());
                    saveSocialAccount(member, provider, providerUserId, email);
                    return new SocialLoginResult(member, true);
                });
    }

    private SocialLoginResult rejectAutomaticLink() {
        throw new BusinessException(ErrorCode.SOCIAL_ACCOUNT_LINK_REQUIRED);
    }

    private void saveSocialAccount(Member member, SocialProvider provider, String providerUserId, String email) {
        socialAccountRepository.findByMemberIdAndProvider(member.getId(), provider)
                .orElseGet(() -> socialAccountRepository.save(MemberSocialAccount.builder()
                        .member(member)
                        .provider(provider)
                        .providerUserId(providerUserId)
                        .providerEmail(email)
                        .build()));
    }

    private String requireProviderUserId(String providerUserId) {
        if (providerUserId == null || providerUserId.isBlank()) {
            throw new IllegalArgumentException("Social provider user id is required.");
        }
        return providerUserId.trim();
    }

    private String normalizeEmail(String email, SocialProvider provider, String providerUserId) {
        if (email != null && !email.isBlank()) {
            return email.trim();
        }
        return provider.name().toLowerCase() + "-" + providerUserId + "@social.moida.local";
    }

    private Member findMember(Long memberId) {
        return memberRepository.findById(memberId)
                .orElseThrow(() -> new IllegalStateException("Linked social member not found."));
    }

    public String getKkoAccessToken(String code) {
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_FORM_URLENCODED);
        MultiValueMap<String, String> params = new LinkedMultiValueMap<>();
        params.add("grant_type", "authorization_code");
        params.add("client_id", kkoClientId);
        params.add("redirect_uri", kkoRedirectUri);
        params.add("code", code);
        KakaoTokenResponse response = externalHttpExecutor.executeOnce("oauth-kakao", () ->
                restTemplate.postForObject(
                        "https://kauth.kakao.com/oauth/token",
                        new HttpEntity<>(params, headers),
                        KakaoTokenResponse.class
                ));
        return response.getAccessToken();
    }

    public KakaoUserResponse getKakaoUserInfo(String accessToken) {
        HttpHeaders headers = new HttpHeaders();
        headers.setBearerAuth(accessToken);
        return externalHttpExecutor.executeOnce("oauth-kakao", () ->
                restTemplate.exchange(
                        "https://kapi.kakao.com/v2/user/me",
                        HttpMethod.GET,
                        new HttpEntity<>(headers),
                        KakaoUserResponse.class
                ).getBody());
    }

    public String getNavAccessToken(String code, String state) {
        String url = "https://nid.naver.com/oauth2.0/token"
                + "?grant_type=authorization_code"
                + "&client_id=" + navClientId
                + "&client_secret=" + navClientSecret
                + "&code=" + code
                + "&state=" + state;
        NaverTokenResponse response = externalHttpExecutor.executeOnce("oauth-naver", () ->
                restTemplate.getForObject(url, NaverTokenResponse.class));
        return response.getAccessToken();
    }

    public NaverUserResponse getNaverUserInfo(String accessToken) {
        HttpHeaders headers = new HttpHeaders();
        headers.setBearerAuth(accessToken);
        return externalHttpExecutor.executeOnce("oauth-naver", () ->
                restTemplate.exchange(
                        "https://openapi.naver.com/v1/nid/me",
                        HttpMethod.GET,
                        new HttpEntity<>(headers),
                        NaverUserResponse.class
                ).getBody());
    }

    public String getGoogleAccessToken(String code, String codeVerifier) {
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_FORM_URLENCODED);
        MultiValueMap<String, String> params = new LinkedMultiValueMap<>();
        params.add("grant_type", "authorization_code");
        params.add("client_id", googleClientId);
        params.add("client_secret", googleClientSecret);
        params.add("redirect_uri", googleRedirectUri);
        params.add("code", code);
        params.add("code_verifier", codeVerifier);
        GoogleTokenResponse response = externalHttpExecutor.executeOnce("oauth-google", () ->
                restTemplate.postForObject(
                        "https://oauth2.googleapis.com/token",
                        new HttpEntity<>(params, headers),
                        GoogleTokenResponse.class
                ));
        return response.getAccessToken();
    }

    public GoogleUserResponse getGoogleUserInfo(String accessToken) {
        HttpHeaders headers = new HttpHeaders();
        headers.setBearerAuth(accessToken);
        return externalHttpExecutor.executeOnce("oauth-google", () ->
                restTemplate.exchange(
                        "https://www.googleapis.com/oauth2/v2/userinfo",
                        HttpMethod.GET,
                        new HttpEntity<>(headers),
                        GoogleUserResponse.class
                ).getBody());
    }
}
