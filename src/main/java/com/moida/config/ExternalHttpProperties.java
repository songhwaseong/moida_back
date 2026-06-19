package com.moida.config;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;

import java.time.Duration;

@Getter
@Setter
@ConfigurationProperties(prefix = "external-http")
public class ExternalHttpProperties {
    private Duration connectTimeout = Duration.ofSeconds(3);
    private Duration readTimeout = Duration.ofSeconds(5);
    private int readMaxAttempts = 2;
    private Duration retryBackoff = Duration.ofMillis(200);
    private int circuitBreakerFailureThreshold = 5;
    private Duration circuitBreakerOpenDuration = Duration.ofSeconds(30);
}
