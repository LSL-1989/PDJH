package com.gocent.pdjh.integration.engine;

import org.springframework.stereotype.Component;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * 轻量级全场景熔断器 (Simple Circuit Breaker)
 * 当某个端点连续调用失败达到阈值时触发熔断，拒接流量，避免连环雪崩。
 */
@Component
public class CircuitBreakerManager {

    private static final int FAILURE_THRESHOLD = 10;
    private static final long RESET_TIMEOUT_MS = 60000; // 60 seconds

    private final Map<String, BreakerState> states = new ConcurrentHashMap<>();

    public void checkAndThrow(String endpointCode) {
        BreakerState state = states.computeIfAbsent(endpointCode, k -> new BreakerState());
        if (state.isOpen()) {
            if (System.currentTimeMillis() - state.lastFailureTime > RESET_TIMEOUT_MS) {
                // Half-open: allow one test request
                state.failures.set(0); 
            } else {
                throw new RuntimeException("【CIRCUIT BREAKER OPEN】 端点 " + endpointCode + " 调用频繁失败，当前触发熔断降级！请一分钟后再试。");
            }
        }
    }

    public void recordSuccess(String endpointCode) {
        BreakerState state = states.computeIfAbsent(endpointCode, k -> new BreakerState());
        state.failures.set(0); // Reset on success
    }

    public void recordFailure(String endpointCode) {
        BreakerState state = states.computeIfAbsent(endpointCode, k -> new BreakerState());
        state.failures.incrementAndGet();
        state.lastFailureTime = System.currentTimeMillis();
    }

    private static class BreakerState {
        AtomicInteger failures = new AtomicInteger(0);
        volatile long lastFailureTime = 0;

        boolean isOpen() {
            return failures.get() >= FAILURE_THRESHOLD;
        }
    }
}
