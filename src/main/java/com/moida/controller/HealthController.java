package com.moida.controller;

import com.moida.common.response.ApiResponse;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

@RestController
@RequestMapping("/api/public")
public class HealthController {

    @Value("${INSTANCE_NAME:${HOSTNAME:unknown}}")
    private String instanceName;

    @GetMapping("/health")
    public ApiResponse<Map<String, Object>> health() {
        return ApiResponse.success(Map.of(
                "service", "moida-backend",
                "status", "UP",
                "instance", instanceName
        ));
    }
}
