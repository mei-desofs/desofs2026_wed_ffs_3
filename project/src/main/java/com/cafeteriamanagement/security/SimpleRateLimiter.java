package com.cafeteriamanagement.security;

import org.springframework.stereotype.Component;

import java.time.Instant;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Component
public class SimpleRateLimiter {

    private static class Counter {
        int count;
        Instant expiresAt;
    }

    private final Map<String, Counter> attempts = new ConcurrentHashMap<>();

    // default: 5 attempts per 15 minutes
    private final int maxAttempts = 5;
    private final long windowSeconds = 15 * 60;

    public synchronized boolean isBlocked(String key) {
        Counter c = attempts.get(key);
        if (c == null) return false;
        if (Instant.now().isAfter(c.expiresAt)) {
            attempts.remove(key);
            return false;
        }
        return c.count >= maxAttempts;
    }

    public synchronized void recordFailure(String key) {
        Counter c = attempts.get(key);
        if (c == null || Instant.now().isAfter(c.expiresAt)) {
            c = new Counter();
            c.count = 1;
            c.expiresAt = Instant.now().plusSeconds(windowSeconds);
            attempts.put(key, c);
            return;
        }
        c.count++;
    }

    public synchronized void reset(String key) {
        attempts.remove(key);
    }
}
