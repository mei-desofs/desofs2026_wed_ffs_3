package com.cafeteriamanagement.security;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.time.temporal.ChronoUnit;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Unit tests for {@link TokenBlocklist}.
 *
 * Requirements coverage:
 * - FR03 — users can log out and invalidate their session token
 * - SDR01 — JWT-based stateless authentication with signed tokens
 * - SDR03 — reject expired, malformed or unsigned tokens with HTTP 401
 * - SDR03a — invalidate JWTs on logout (the blocklist is the server-side enforcement point)
 * - ASVS V7.4.1 — logout invalidates session token
 *
 * Test cases addressed:
 * - TC26 — JWT presented after logout must be rejected (logout invalidation)
 *
 * Threats mitigated:
 * - T07 — reuse of a still-unexpired token after the user has logged out
 */
class TokenBlocklistTest {

    private TokenBlocklist blocklist;

    @BeforeEach
    void setUp() {
        blocklist = new TokenBlocklist();
    }

    @Test
    void unknownTokenIsNotBlocked() {
        assertFalse(blocklist.isBlocked("never-seen-token"));
    }

    /** FR03 / SDR03a / TC26: a token added to the blocklist after logout must be rejected. */
    @Test
    void blockedTokenReturnsTrue() {
        String token = "eyJhbGciOiJIUzI1NiJ9.fake-payload";
        blocklist.block(token, Instant.now().plus(1, ChronoUnit.HOURS));
        assertTrue(blocklist.isBlocked(token));
    }

    @Test
    void otherTokensAreUnaffected() {
        String blocked = "blocked-token";
        String other = "other-token";
        blocklist.block(blocked, Instant.now().plus(1, ChronoUnit.HOURS));
        assertFalse(blocklist.isBlocked(other));
    }

    /**
     * SDR03: the blocklist must self-clean once a token's original JWT expiry passes —
     * keeping expired entries indefinitely would leak memory and serve no security purpose
     * because the JWT signature check would already reject the token.
     */
    @Test
    void expiredEntryIsNoLongerBlocked() {
        String token = "expired-token";
        // Block with an instant already in the past
        blocklist.block(token, Instant.now().minus(1, ChronoUnit.SECONDS));
        assertFalse(blocklist.isBlocked(token));
    }

    @Test
    void blockingSameTokenTwiceIsIdempotent() {
        String token = "same-token";
        Instant firstExpiry = Instant.now().plus(1, ChronoUnit.HOURS);
        Instant secondExpiry = Instant.now().plus(2, ChronoUnit.HOURS);

        blocklist.block(token, firstExpiry);
        blocklist.block(token, secondExpiry);

        assertTrue(blocklist.isBlocked(token));
    }
}
