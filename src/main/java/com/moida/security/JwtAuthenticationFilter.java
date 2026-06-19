package com.moida.security;

import com.moida.domain.member.MemberRepository;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;

@Component
@RequiredArgsConstructor
public class JwtAuthenticationFilter extends OncePerRequestFilter {

    private static final String AUTH_HEADER = "Authorization";
    private static final String BEARER_PREFIX = "Bearer ";

    private final JwtTokenProvider jwtTokenProvider;
    private final MemberRepository memberRepository;

    @Override
    protected void doFilterInternal(HttpServletRequest request,
                                    HttpServletResponse response,
                                    FilterChain filterChain) throws ServletException, IOException {
        String token = resolveToken(request);

        // access 토큰만 인증에 사용한다. refresh 토큰을 Authorization 헤더로 보내도 거부(타입 미일치).
        if (StringUtils.hasText(token) && jwtTokenProvider.validateToken(token)
                && jwtTokenProvider.isTokenType(token, JwtTokenProvider.TYPE_ACCESS)) {
            Authentication authentication = jwtTokenProvider.getAuthentication(token);
            CustomUserDetails userDetails = (CustomUserDetails) authentication.getPrincipal();
            // 기존 토큰이어도 탈퇴/정지 회원이면 인증 컨텍스트를 세팅하지 않는다.
            memberRepository.findById(userDetails.getMemberId())
                    .filter(member -> member.isActive())
                    .filter(member -> member.currentTokenVersion() == jwtTokenProvider.getTokenVersion(token))
                    .ifPresent(member -> SecurityContextHolder.getContext().setAuthentication(authentication));
        }

        filterChain.doFilter(request, response);
    }

    private String resolveToken(HttpServletRequest request) {
        String bearer = request.getHeader(AUTH_HEADER);
        if (StringUtils.hasText(bearer) && bearer.startsWith(BEARER_PREFIX)) {
            return bearer.substring(BEARER_PREFIX.length());
        }
        return null;
    }
}
