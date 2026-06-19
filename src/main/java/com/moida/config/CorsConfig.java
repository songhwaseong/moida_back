package com.moida.config;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

@Configuration
@ConfigurationProperties(prefix = "cors")
public class CorsConfig {

    private List<String> allowedOrigins = new ArrayList<>();

    public List<String> getAllowedOrigins() { return allowedOrigins; }

    public void setAllowedOrigins(List<String> allowedOrigins) {
        this.allowedOrigins = allowedOrigins;
    }

    /**
     * CORS 와 WebSocket 핸드셰이크가 공유하는 허용 origin 목록(정규화 적용).
     * 비어 있으면 로컬 개발 편의를 위해 dev origin 으로 fallback 한다.
     * 주의: 운영(prod)에서는 FRONTEND_ORIGIN 을 반드시 설정해야 한다 —
     *       미설정 시 dev origin 으로 fallback 되어 의도치 않게 localhost 가 허용된다.
     */
    public List<String> resolvedAllowedOrigins() {
        List<String> base = (allowedOrigins == null || allowedOrigins.isEmpty())
                ? List.of(
                        "http://localhost:5173",
                        "http://127.0.0.1:5173",
                        "http://localhost:3000",
                        "http://localhost:4173")
                : allowedOrigins;
        return normalizeOrigins(base);
    }

    @Bean
    public CorsConfigurationSource corsConfigurationSource() {
        CorsConfiguration config = new CorsConfiguration();
        config.setAllowedOrigins(resolvedAllowedOrigins());
        config.setAllowedMethods(List.of("GET", "POST", "PUT", "PATCH", "DELETE", "OPTIONS"));
        config.setAllowedHeaders(List.of("*"));
        config.setExposedHeaders(List.of("Authorization", "Content-Disposition", "X-Instance-Name"));
        config.setAllowCredentials(true);
        config.setMaxAge(3600L);

        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/**", config);
        return source;
    }

    private List<String> normalizeOrigins(List<String> origins) {
        return origins.stream()
                .flatMap(origin -> Arrays.stream(origin.split(",")))
                .map(String::trim)
                .filter(origin -> !origin.isEmpty())
                .toList();
    }
}
