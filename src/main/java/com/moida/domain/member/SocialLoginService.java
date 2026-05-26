package com.moida.domain.member;

import com.moida.common.response.*;                                     // 소셜 로그인 DTO (Kakao, Naver, Google)
import jakarta.transaction.Transactional;                               // DB 작업 실패 시 자동 롤백
import lombok.RequiredArgsConstructor;                                  // final 필드 자동 생성자 주입
import org.springframework.beans.factory.annotation.Value;              // yml 값 읽어오는 어노테이션
import org.springframework.http.*;                                      // HttpHeaders, HttpMethod, HttpEntity, MediaType
import org.springframework.security.crypto.password.PasswordEncoder;    // 신규 소셜 회원 임시 비밀번호 암호화
import org.springframework.stereotype.Service;                          // Spring 서비스 컴포넌트 등록
import org.springframework.util.LinkedMultiValueMap;                    // POST body 파라미터 담는 Map
import org.springframework.util.MultiValueMap;                          // LinkedMultiValueMap 인터페이스
import org.springframework.web.client.RestTemplate;                     // 소셜 API 서버에 HTTP 요청 보내는 클라이언트

import java.time.LocalDateTime;                                         // memberNo 생성 시 현재 날짜
import java.time.format.DateTimeFormatter;                              // 날짜를 yyyyMMdd 형식으로 포맷

@Service
@RequiredArgsConstructor
public class SocialLoginService {
    private final MemberRepository memberRepository;    // 회원 조회 및 저장
    private final PasswordEncoder passwordEncoder;       // 임시 비밀번호 암호화
    private final RestTemplate restTemplate = new RestTemplate(); // 소셜 API HTTP 클라이언트

    @Value("${social.kakao.client-id}")
    private String kkoClientId;         // application.yml → 환경변수 KAKAO_CLIENT_ID

    @Value("${social.kakao.redirect-uri}")
    private String kkoRedirectUri;      // application.yml → 환경변수 KAKAO_REDIRECT_URI

    @Value("${social.naver.client-id}")
    private String navClientId;         // application.yml → 환경변수 NAVER_CLIENT_ID

    @Value("${social.naver.client-secret}")
    private String navClientSecret;     // application.yml → 환경변수 NAVER_CLIENT_SECRET

    @Value("${social.naver.redirect-uri}")
    private String navRedirectUri;      // application.yml → 환경변수 NAVER_REDIRECT_URI

    @Value("${social.google.client-id}")
    private String googleClientId;      // application.yml → 환경변수 GOOGLE_CLIENT_ID

    @Value("${social.google.client-secret}")
    private String googleClientSecret;  // application.yml → 환경변수 GOOGLE_CLIENT_SECRET

    @Value("${social.google.redirect-uri}")
    private String googleRedirectUri;   // application.yml → 환경변수 GOOGLE_REDIRECT_URI

    // ===== 공통 =====

    // 이메일로 기존 회원 조회 → 없으면 자동 가입 후 반환
    @Transactional
    public Member findOrRegisterSocialMember(String email, String name, String socialType) {
        return memberRepository.findByEmail(email)
                .orElseGet(() -> {
                    String memberNo = LocalDateTime.now()
                            .format(DateTimeFormatter.ofPattern("yyyyMMdd"))
                            + String.format("%05d", memberRepository.count() + 1);
                    return memberRepository.save(Member.builder()
                            .memberNo(memberNo)
                            .email(email)
                            .password(passwordEncoder.encode(socialType + "_SOCIAL")) // 소셜 회원 임시 비밀번호
                            .name(name)
                            .role(MemberRole.USER)
                            .socialLogin(socialType) // "KAKAO" / "NAVER" / "GOOGLE"
                            .build());
                });
    }
    // ===== 카카오 =====

    // 인가 코드 → 액세스 토큰 교환
    public String getKkoAccessToken(String code) {
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_FORM_URLENCODED);
        MultiValueMap<String, String> params = new LinkedMultiValueMap<>();
        params.add("grant_type", "authorization_code");
        params.add("client_id", kkoClientId);
        params.add("redirect_uri", kkoRedirectUri);
        params.add("code", code);
        KakaoTokenResponse response = restTemplate.postForObject(
                "https://kauth.kakao.com/oauth/token",
                new HttpEntity<>(params, headers),
                KakaoTokenResponse.class
        );
        return response.getAccessToken();
    }

    // 액세스 토큰 → 카카오 사용자 정보 조회
    public KakaoUserResponse getKakaoUserInfo(String accessToken) {
        HttpHeaders headers = new HttpHeaders();
        headers.setBearerAuth(accessToken);
        return restTemplate.exchange(
                "https://kapi.kakao.com/v2/user/me",
                HttpMethod.GET,
                new HttpEntity<>(headers),
                KakaoUserResponse.class
        ).getBody();
    }
    // ===== 네이버 =====

    // 인가 코드 + state → 액세스 토큰 교환 (네이버는 GET 방식)
    public String getNavAccessToken(String code, String state) {
        String url = "https://nid.naver.com/oauth2.0/token"
                + "?grant_type=authorization_code"
                + "&client_id=" + navClientId
                + "&client_secret=" + navClientSecret
                + "&code=" + code
                + "&state=" + state;
        NaverTokenResponse response = restTemplate.getForObject(url, NaverTokenResponse.class);
        return response.getAccessToken();
    }

    // 액세스 토큰 → 네이버 사용자 정보 조회
    public NaverUserResponse getNaverUserInfo(String accessToken) {
        HttpHeaders headers = new HttpHeaders();
        headers.setBearerAuth(accessToken);
        return restTemplate.exchange(
                "https://openapi.naver.com/v1/nid/me",
                HttpMethod.GET,
                new HttpEntity<>(headers),
                NaverUserResponse.class
        ).getBody();
    }

    // ===== 구글 =====

    // 인가 코드 → 액세스 토큰 교환
    public String getGoogleAccessToken(String code) {
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_FORM_URLENCODED);
        MultiValueMap<String, String> params = new LinkedMultiValueMap<>();
        params.add("grant_type", "authorization_code");
        params.add("client_id", googleClientId);
        params.add("client_secret", googleClientSecret);
        params.add("redirect_uri", googleRedirectUri);
        params.add("code", code);
        GoogleTokenResponse response = restTemplate.postForObject(
                "https://oauth2.googleapis.com/token",
                new HttpEntity<>(params, headers),
                GoogleTokenResponse.class
        );
        return response.getAccessToken();
    }

    // 액세스 토큰 → 구글 사용자 정보 조회
    public GoogleUserResponse getGoogleUserInfo(String accessToken) {
        HttpHeaders headers = new HttpHeaders();
        headers.setBearerAuth(accessToken);
        return restTemplate.exchange(
                "https://www.googleapis.com/oauth2/v2/userinfo",
                HttpMethod.GET,
                new HttpEntity<>(headers),
                GoogleUserResponse.class
        ).getBody();
    }
}
