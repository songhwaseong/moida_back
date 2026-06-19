package com.moida.common.util;

import com.moida.config.ExternalHttpProperties;
import org.junit.jupiter.api.Test;
import org.springframework.web.client.ResourceAccessException;

import java.time.Duration;
import java.util.concurrent.atomic.AtomicInteger;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class ExternalHttpExecutorTest {

    @Test
    void readCallRetriesTemporaryConnectionFailure() {
        ExternalHttpProperties properties = properties();
        properties.setReadMaxAttempts(2);
        ExternalHttpExecutor executor = new ExternalHttpExecutor(properties);
        AtomicInteger attempts = new AtomicInteger();

        String result = executor.executeRead("tracking", () -> {
            if (attempts.incrementAndGet() == 1) {
                throw new ResourceAccessException("temporary timeout");
            }
            return "ok";
        });

        assertThat(result).isEqualTo("ok");
        assertThat(attempts).hasValue(2);
    }

    @Test
    void circuitOpensAfterConfiguredNumberOfInfrastructureFailures() {
        ExternalHttpProperties properties = properties();
        properties.setReadMaxAttempts(1);
        properties.setCircuitBreakerFailureThreshold(1);
        ExternalHttpExecutor executor = new ExternalHttpExecutor(properties);
        AtomicInteger calls = new AtomicInteger();

        assertThatThrownBy(() -> executor.executeRead("tracking", () -> {
            calls.incrementAndGet();
            throw new ResourceAccessException("down");
        })).isInstanceOf(ResourceAccessException.class);

        assertThatThrownBy(() -> executor.executeRead("tracking", () -> {
            calls.incrementAndGet();
            return "unexpected";
        })).isInstanceOf(ExternalHttpExecutor.CircuitOpenException.class);
        assertThat(calls).hasValue(1);
    }

    @Test
    void clientErrorDoesNotOpenCircuit() {
        ExternalHttpProperties properties = properties();
        properties.setCircuitBreakerFailureThreshold(1);
        ExternalHttpExecutor executor = new ExternalHttpExecutor(properties);

        assertThatThrownBy(() -> executor.executeOnce("oauth", () -> {
            throw new IllegalArgumentException("invalid authorization code");
        })).isInstanceOf(IllegalArgumentException.class);

        assertThat(executor.executeOnce("oauth", () -> "ok")).isEqualTo("ok");
    }

    private ExternalHttpProperties properties() {
        ExternalHttpProperties properties = new ExternalHttpProperties();
        properties.setRetryBackoff(Duration.ZERO);
        properties.setCircuitBreakerOpenDuration(Duration.ofSeconds(30));
        return properties;
    }
}
