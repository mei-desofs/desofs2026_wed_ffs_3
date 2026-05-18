package com.cafeteriamanagement.security;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

/**
 * Unit tests for {@link PasswordPolicyService}.
 *
 * Requirements coverage:
 * - FR01 — users register with a unique username and password
 * - SDR05 — minimum length 12, maximum at least 64 characters
 * - ASVS V6.2.1 — passwords >= 12 chars
 * - ASVS V6.2.9 — no maximum length truncation; service rejects rather than silently truncates
 *
 * Threats mitigated:
 * - Weak-credential exploitation via short or empty passwords
 *
 * Note: SDR05a (generic error messages) is covered at controller level by {@code AuthController}.
 * SDR05b (breached password rejection) is delegated to {@link HaveIBeenPwnedClient}.
 */
class PasswordPolicyServiceTest {

    private static final int MIN = 12;
    private static final int MAX = 128;

    private final PasswordPolicyService policy = new PasswordPolicyService();

    @BeforeEach
    @SuppressWarnings("null") // policy is final and initialised at declaration
    void setUp() {
        // The @Value fields are not injected outside of Spring context — set them via reflection
        ReflectionTestUtils.setField(policy, "minLength", MIN);
        ReflectionTestUtils.setField(policy, "maxLength", MAX);
    }

    @Test
    void rejectsNullPassword() {
        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class,
            () -> policy.validate(null));
        assertEquals("Password is required", ex.getMessage());
    }

    /** SDR05: passwords shorter than 12 characters must be rejected. */
    @Test
    void rejectsPasswordShorterThanMinimum() {
        String tooShort = "a".repeat(MIN - 1);
        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class,
            () -> policy.validate(tooShort));
        assertEquals("Password must be at least " + MIN + " characters", ex.getMessage());
    }

    /** SDR05: boundary check — the minimum length is inclusive (12 chars accepted). */
    @Test
    void acceptsPasswordExactlyAtMinimum() {
        String atMin = "a".repeat(MIN);
        assertDoesNotThrow(() -> policy.validate(atMin));
    }

    /** ASVS V6.2.9: maximum length must be enforced — no silent truncation. */
    @Test
    void acceptsPasswordExactlyAtMaximum() {
        String atMax = "a".repeat(MAX);
        assertDoesNotThrow(() -> policy.validate(atMax));
    }

    /** ASVS V6.2.9: passwords above the configured maximum must be rejected explicitly. */
    @Test
    void rejectsPasswordLongerThanMaximum() {
        String tooLong = "a".repeat(MAX + 1);
        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class,
            () -> policy.validate(tooLong));
        assertEquals("Password must be at most " + MAX + " characters", ex.getMessage());
    }

    @Test
    void acceptsTypicalStrongPassword() {
        assertDoesNotThrow(() -> policy.validate("Correct-Horse-Battery-Staple-2026!"));
    }
}
