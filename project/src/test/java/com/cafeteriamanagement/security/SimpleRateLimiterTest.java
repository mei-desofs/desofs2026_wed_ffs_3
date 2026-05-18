package com.cafeteriamanagement.security;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Unit tests for {@link SimpleRateLimiter}.
 *
 * Requirements coverage:
 * - SDR04 — rate limiting / account lockout after N failed login attempts
 * - ASVS V6.3.1 — anti-automation controls against brute force
 *
 * Test cases addressed:
 * - TC01 (credential stuffing) — verifies threshold blocks at the 5th failed attempt
 * - TC04 (high-rate login flood) — verifies blocking persists beyond the threshold
 *
 * Abuse cases / threats:
 * - AC01 (brute-force login)
 * - AC08 (DoS via request flooding) — partial; per-key limiter mitigates single-source floods
 *
 * Default policy under test: 5 failed attempts per 15-minute window per key.
 */
class SimpleRateLimiterTest {

    private SimpleRateLimiter limiter;

    @BeforeEach
    void setUp() {
        limiter = new SimpleRateLimiter();
    }

    @Test
    void unknownKeyIsNotBlocked() {
        assertFalse(limiter.isBlocked("login:user:nobody"));
    }

    @Test
    void singleFailureDoesNotBlock() {
        limiter.recordFailure("login:user:alice");
        assertFalse(limiter.isBlocked("login:user:alice"));
    }

    @Test
    void fourFailuresDoNotBlock() {
        String key = "login:user:bob";
        for (int i = 0; i < 4; i++) {
            limiter.recordFailure(key);
        }
        assertFalse(limiter.isBlocked(key));
    }

    /** SDR04 / TC01: threshold reached at the 5th failure must block subsequent attempts. */
    @Test
    void fifthFailureBlocksKey() {
        String key = "login:user:carol";
        for (int i = 0; i < 5; i++) {
            limiter.recordFailure(key);
        }
        assertTrue(limiter.isBlocked(key));
    }

    /** TC04: sustained flood beyond the threshold must keep the key blocked. */
    @Test
    void additionalFailuresKeepKeyBlocked() {
        String key = "login:user:dave";
        for (int i = 0; i < 10; i++) {
            limiter.recordFailure(key);
        }
        assertTrue(limiter.isBlocked(key));
    }

    /**
     * SDR04: a successful authentication must clear the counter for the same key
     * (the {@code AuthController} invokes {@link SimpleRateLimiter#reset(String)} on login success).
     */
    @Test
    void resetUnblocksKey() {
        String key = "login:user:erin";
        for (int i = 0; i < 5; i++) {
            limiter.recordFailure(key);
        }
        assertTrue(limiter.isBlocked(key));

        limiter.reset(key);
        assertFalse(limiter.isBlocked(key));
    }

    /**
     * SDR04: per-user and per-IP counters must be independent so that one attacker
     * does not lock out a legitimate user, and one user does not exhaust the IP quota for everyone.
     */
    @Test
    void differentKeysAreTrackedIndependently() {
        String userKey = "login:user:frank";
        String ipKey = "login:ip:10.0.0.1";

        for (int i = 0; i < 5; i++) {
            limiter.recordFailure(userKey);
        }

        assertTrue(limiter.isBlocked(userKey));
        assertFalse(limiter.isBlocked(ipKey));
    }
}
