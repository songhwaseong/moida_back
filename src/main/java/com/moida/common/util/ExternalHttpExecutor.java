package com.moida.common.util;

import com.moida.config.ExternalHttpProperties;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.web.client.HttpServerErrorException;
import org.springframework.web.client.ResourceAccessException;

import java.time.Duration;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;
import java.util.function.Supplier;

@Slf4j
@Component
@RequiredArgsConstructor
public class ExternalHttpExecutor {
    private final ExternalHttpProperties properties;
    private final Map<String, CircuitState> circuits = new ConcurrentHashMap<>();

    public <T> T executeRead(String service, Supplier<T> request) {
        return execute(service, Math.max(1, properties.getReadMaxAttempts()), request);
    }

    public <T> T executeOnce(String service, Supplier<T> request) {
        return execute(service, 1, request);
    }

    private <T> T execute(String service, int maxAttempts, Supplier<T> request) {
        CircuitState state = circuits.computeIfAbsent(service, ignored -> new CircuitState());
        ensureCircuitAvailable(service, state);

        RuntimeException lastFailure = null;
        for (int attempt = 1; attempt <= maxAttempts; attempt++) {
            try {
                T result = request.get();
                state.reset();
                return result;
            } catch (RuntimeException ex) {
                lastFailure = ex;
                if (!isInfrastructureFailure(ex)) {
                    throw ex;
                }
                if (attempt < maxAttempts) {
                    pauseBeforeRetry();
                }
            }
        }

        recordFailure(service, state);
        throw lastFailure;
    }

    private void ensureCircuitAvailable(String service, CircuitState state) {
        long openUntil = state.openUntil.get();
        long now = System.currentTimeMillis();
        if (openUntil > now) {
            throw new CircuitOpenException(service);
        }
        if (openUntil != 0) {
            state.openUntil.compareAndSet(openUntil, 0);
        }
    }

    private void recordFailure(String service, CircuitState state) {
        int threshold = Math.max(1, properties.getCircuitBreakerFailureThreshold());
        if (state.consecutiveFailures.incrementAndGet() < threshold) {
            return;
        }

        long openMillis = Math.max(1, properties.getCircuitBreakerOpenDuration().toMillis());
        state.openUntil.set(System.currentTimeMillis() + openMillis);
        state.consecutiveFailures.set(0);
        log.warn("external_http_circuit_opened service={} openDuration={}",
                service, properties.getCircuitBreakerOpenDuration());
    }

    private boolean isInfrastructureFailure(Throwable throwable) {
        Throwable current = throwable;
        while (current != null) {
            if (current instanceof ResourceAccessException || current instanceof HttpServerErrorException) {
                return true;
            }
            current = current.getCause();
        }
        return false;
    }

    private void pauseBeforeRetry() {
        Duration backoff = properties.getRetryBackoff();
        if (backoff == null || backoff.isZero() || backoff.isNegative()) {
            return;
        }
        try {
            Thread.sleep(backoff.toMillis());
        } catch (InterruptedException ex) {
            Thread.currentThread().interrupt();
            throw new IllegalStateException("Interrupted while retrying an external HTTP call.", ex);
        }
    }

    private static final class CircuitState {
        private final AtomicInteger consecutiveFailures = new AtomicInteger();
        private final AtomicLong openUntil = new AtomicLong();

        private void reset() {
            consecutiveFailures.set(0);
            openUntil.set(0);
        }
    }

    public static final class CircuitOpenException extends RuntimeException {
        public CircuitOpenException(String service) {
            super("External HTTP circuit is open: " + service);
        }
    }
}
