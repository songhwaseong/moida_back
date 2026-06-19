package com.moida.security;

import com.moida.common.exception.BusinessException;
import com.moida.common.response.OAuthPrepareResponse;
import com.moida.domain.auth.AuthCookieService;
import com.moida.domain.auth.OAuthFlowService;
import com.moida.domain.member.Member;
import com.moida.domain.member.MemberRole;
import jakarta.servlet.http.Cookie;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;

import java.util.Arrays;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class AuthenticationBoundaryTest {

    private static final String SECRET = "test-secret-key-that-is-at-least-32-bytes-long";

    @Test
    void refreshCookieIsHttpOnlyAndRequiresMatchingCsrfHeader() {
        AuthCookieService service = new AuthCookieService(false, 60_000L);
        MockHttpServletResponse response = new MockHttpServletResponse();
        service.issue(response, "refresh-token");

        String refreshHeader = cookieHeader(response, AuthCookieService.REFRESH_COOKIE);
        String csrfHeader = cookieHeader(response, AuthCookieService.CSRF_COOKIE);
        assertThat(refreshHeader).contains("HttpOnly").contains("SameSite=Strict");
        assertThat(csrfHeader).doesNotContain("HttpOnly");

        String csrf = cookieValue(csrfHeader);
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.setCookies(
                new Cookie(AuthCookieService.REFRESH_COOKIE, "refresh-token"),
                new Cookie(AuthCookieService.CSRF_COOKIE, csrf));
        request.addHeader(AuthCookieService.CSRF_HEADER, csrf);

        assertThat(service.requireRefreshToken(request)).isEqualTo("refresh-token");
    }

    @Test
    void refreshCookieRejectsMissingCsrfHeader() {
        AuthCookieService service = new AuthCookieService(false, 60_000L);
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.setCookies(
                new Cookie(AuthCookieService.REFRESH_COOKIE, "refresh-token"),
                new Cookie(AuthCookieService.CSRF_COOKIE, "csrf"));

        assertThatThrownBy(() -> service.requireRefreshToken(request))
                .isInstanceOf(BusinessException.class);
    }

    @Test
    void oauthFlowIsBoundToProviderStateAndOneCookie() {
        OAuthFlowService service = new OAuthFlowService(SECRET, false);
        MockHttpServletResponse prepareResponse = new MockHttpServletResponse();
        OAuthPrepareResponse prepared = service.prepare("google", prepareResponse);

        assertThat(prepared.pkce()).isTrue();
        assertThat(prepared.codeChallenge()).isNotBlank();

        MockHttpServletRequest callback = new MockHttpServletRequest();
        callback.setCookies(new Cookie("MOIDA_OAUTH_FLOW",
                cookieValue(cookieHeader(prepareResponse, "MOIDA_OAUTH_FLOW"))));
        OAuthFlowService.OAuthContext context = service.consume(
                "google", prepared.state(), callback, new MockHttpServletResponse());

        assertThat(context.provider()).isEqualTo("GOOGLE");
        assertThat(context.codeVerifier()).isNotBlank();
    }

    @Test
    void oauthFlowRejectsDifferentState() {
        OAuthFlowService service = new OAuthFlowService(SECRET, false);
        MockHttpServletResponse prepareResponse = new MockHttpServletResponse();
        service.prepare("kakao", prepareResponse);
        MockHttpServletRequest callback = new MockHttpServletRequest();
        callback.setCookies(new Cookie("MOIDA_OAUTH_FLOW",
                cookieValue(cookieHeader(prepareResponse, "MOIDA_OAUTH_FLOW"))));

        assertThatThrownBy(() -> service.consume(
                "kakao", "different-state", callback, new MockHttpServletResponse()))
                .isInstanceOf(BusinessException.class);
    }

    @Test
    void jwtCarriesMemberTokenVersion() {
        JwtTokenProvider provider = new JwtTokenProvider(SECRET, 60_000L, 120_000L);
        String token = provider.createAccessToken(1L, "member@example.com", MemberRole.USER, 7L);

        assertThat(provider.validateToken(token)).isTrue();
        assertThat(provider.getTokenVersion(token)).isEqualTo(7L);
    }

    @Test
    void memberTokenVersionChangesWhenCredentialsAreInvalidated() {
        Member member = Member.builder()
                .memberNo("2026000000001")
                .email("member@example.com")
                .password("encoded")
                .name("member")
                .role(MemberRole.USER)
                .build();

        assertThat(member.currentTokenVersion()).isZero();
        member.invalidateTokens();
        assertThat(member.currentTokenVersion()).isEqualTo(1L);
    }

    private String cookieHeader(MockHttpServletResponse response, String name) {
        return response.getHeaders("Set-Cookie").stream()
                .filter(value -> value.startsWith(name + "="))
                .findFirst()
                .orElseThrow();
    }

    private String cookieValue(String header) {
        return Arrays.stream(header.split(";", 2))
                .findFirst()
                .orElseThrow()
                .substring(header.indexOf('=') + 1);
    }
}
