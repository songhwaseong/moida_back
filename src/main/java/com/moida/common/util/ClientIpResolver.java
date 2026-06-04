package com.moida.common.util;

import jakarta.servlet.http.HttpServletRequest;

/**
 * 클라이언트 실제 IP 추출 유틸.
 * 운영에서 Nginx/로드밸런서 뒤에 있으면 getRemoteAddr() 는 프록시 IP만 잡히므로
 * 프록시가 채워주는 헤더(X-Forwarded-For 등)를 우선 확인한다.
 */
public final class ClientIpResolver {

    private ClientIpResolver() {}

    private static final String[] HEADER_CANDIDATES = {
            "X-Forwarded-For",
            "Proxy-Client-IP",
            "WL-Proxy-Client-IP",
            "HTTP_X_FORWARDED_FOR",
            "X-Real-IP",
    };

    public static String resolve(HttpServletRequest request) {
        if (request == null) return "unknown";
        for (String header : HEADER_CANDIDATES) {
            String value = request.getHeader(header);
            if (value != null && !value.isBlank() && !"unknown".equalsIgnoreCase(value)) {
                // X-Forwarded-For 는 "client, proxy1, proxy2" 형태일 수 있어 맨 앞(최초 클라이언트)을 쓴다.
                int comma = value.indexOf(',');
                return (comma > 0 ? value.substring(0, comma) : value).trim();
            }
        }
        return request.getRemoteAddr();
    }
}
