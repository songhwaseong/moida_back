package com.moida.controller;

import com.moida.common.response.ApiResponse;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

@RestController
@RequestMapping("/api/public")
public class HealthController {

    @GetMapping("/health")
    public ApiResponse<Map<String, Object>> health() {
        // 공개 엔드포인트라 인스턴스 식별 정보(호스트명)는 노출하지 않는다.
        // 인스턴스 구분이 필요하면 내부망/인증 경로에서 X-Instance-Name 응답 헤더를 사용한다.
        return ApiResponse.success(Map.of(
                "service", "moida-backend",
                "status", "UP"
        ));
    }
}
