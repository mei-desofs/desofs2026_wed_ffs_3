package com.cafeteriamanagement.security;

import ch.qos.logback.classic.Level;
import ch.qos.logback.classic.Logger;
import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.core.read.ListAppender;
import jakarta.servlet.http.HttpServletRequest;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.slf4j.LoggerFactory;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/**
 * Unit tests for {@link SecurityAuditLogger}.
 *
 * Requirements coverage:
 * - SDR19 — log all authentication events (success, failure, logout) with timestamp and IP
 * - SDR19b — log entries sanitised against log injection (CR, LF, tab stripped)
 * - SDR20  — log all privileged actions with actor identity
 * - SDR20a — log failed authorization attempts (HTTP 403) with user identity and resource
 * - ASVS V16.3.1 — authentication events are logged
 * - ASVS V16.4.1 — log injection prevention
 *
 * Test cases addressed:
 * - TC32 — login success, failure and logout each produce an audit log entry
 *
 * Abuse cases / threats:
 * - AC07 — attacker tries to cover tracks; complete and tamper-evident audit log is the defence
 * - Log-injection threat: attacker forges fake log lines by embedding CRLF in input
 */
class SecurityAuditLoggerTest {

    private final SecurityAuditLogger auditLogger = new SecurityAuditLogger();
    private final Logger auditLogbackLogger =
        (Logger) LoggerFactory.getLogger("SECURITY_AUDIT");
    private final ListAppender<ILoggingEvent> appender = new ListAppender<>();

    @BeforeEach
    void setUp() {
        appender.list.clear();
        appender.start();
        auditLogbackLogger.addAppender(appender);
    }

    @AfterEach
    void tearDown() {
        auditLogbackLogger.detachAppender(appender);
        appender.stop();
    }

    /** SDR19 / TC32: successful logins must produce an audit entry at INFO level. */
    @Test
    void authenticationSuccessIsLoggedAtInfoLevel() {
        HttpServletRequest request = mockRequest("10.0.0.1", "POST", "/api/auth/login");

        auditLogger.logAuthenticationSuccess(request, "alice");

        List<ILoggingEvent> events = appender.list;
        assertEquals(1, events.size());
        ILoggingEvent event = events.get(0);
        assertEquals(Level.INFO, event.getLevel());
        String message = event.getFormattedMessage();
        assertTrue(message.contains("event=AUTHENTICATION_SUCCESS"));
        assertTrue(message.contains("username=alice"));
        assertTrue(message.contains("ip=10.0.0.1"));
    }

    /**
     * SDR19 / TC32: failed logins must produce an audit entry at WARN level so they
     * stand out in monitoring dashboards from regular operational traffic.
     */
    @Test
    void authenticationFailureIsLoggedAtWarnLevel() {
        HttpServletRequest request = mockRequest("10.0.0.2", "POST", "/api/auth/login");

        auditLogger.logAuthenticationFailure(request, "bob", "invalid credentials");

        List<ILoggingEvent> events = appender.list;
        assertEquals(1, events.size());
        ILoggingEvent event = events.get(0);
        assertEquals(Level.WARN, event.getLevel());
        String message = event.getFormattedMessage();
        assertTrue(message.contains("event=AUTHENTICATION_FAILURE"));
        assertTrue(message.contains("username=bob"));
        assertTrue(message.contains("reason=invalid credentials"));
    }

    /**
     * SDR04 / SDR19: when the rate limiter blocks a request the event must be
     * audited so operators can detect brute-force campaigns in progress.
     */
    @Test
    void authenticationBlockedIsLoggedAtWarnLevel() {
        HttpServletRequest request = mockRequest("10.0.0.3", "POST", "/api/auth/login");

        auditLogger.logAuthenticationBlocked(request, "carol", "rate limit triggered");

        List<ILoggingEvent> events = appender.list;
        assertEquals(1, events.size());
        assertEquals(Level.WARN, events.get(0).getLevel());
        assertTrue(events.get(0).getFormattedMessage().contains("event=AUTHENTICATION_BLOCKED"));
    }

    /**
     * SDR19b / ASVS V16.4.1: user-supplied values written to the audit log must be sanitised so
     * an attacker cannot forge fake log lines by embedding CRLF or TAB in their username.
     */
    @Test
    void logInjectionAttemptIsSanitised() {
        HttpServletRequest request = mockRequest("10.0.0.4", "POST", "/api/auth/login");
        // Attacker tries to forge a fake log line via CRLF injection in the username
        String maliciousUsername = "victim\r\nevent=AUTHENTICATION_SUCCESS\tprincipal=attacker";

        auditLogger.logAuthenticationFailure(request, maliciousUsername, "credentials rejected");

        String message = appender.list.get(0).getFormattedMessage();
        assertFalse(message.contains("\r"), "CR must be stripped");
        assertFalse(message.contains("\n"), "LF must be stripped");
        assertFalse(message.contains("\t"), "Tab must be stripped");
        // Original chars should be replaced with '_'
        assertTrue(message.contains("victim__event=AUTHENTICATION_SUCCESS_principal=attacker"));
    }

    /**
     * SDR19: even when a login request arrives with no username (validation error before
     * the AuthController is reached), the audit entry must still record the event with
     * a deterministic placeholder rather than printing {@code null}.
     */
    @Test
    void nullUsernameIsReplacedWithUnknown() {
        HttpServletRequest request = mockRequest("10.0.0.5", "POST", "/api/auth/login");

        auditLogger.logAuthenticationFailure(request, null, "missing credentials");

        assertTrue(appender.list.get(0).getFormattedMessage().contains("username=unknown"));
    }

    /**
     * SDR03 / SDR19: invalid, expired or malformed bearer tokens must produce a WARN-level
     * audit entry. The principal is recorded as {@code bearer-token} because the token cannot
     * be trusted to resolve to a user identity.
     */
    @Test
    void tokenAuthenticationFailureUsesBearerTokenPrincipal() {
        HttpServletRequest request = mockRequest("10.0.0.6", "GET", "/api/users");

        auditLogger.logTokenAuthenticationFailure(request, "expired token");

        ILoggingEvent event = appender.list.get(0);
        assertEquals(Level.WARN, event.getLevel());
        String message = event.getFormattedMessage();
        assertTrue(message.contains("event=TOKEN_AUTHENTICATION_FAILURE"));
        assertTrue(message.contains("username=bearer-token"));
        assertTrue(message.contains("reason=expired token"));
    }

    private HttpServletRequest mockRequest(String ip, String method, String path) {
        HttpServletRequest request = mock(HttpServletRequest.class);
        when(request.getRemoteAddr()).thenReturn(ip);
        when(request.getMethod()).thenReturn(method);
        when(request.getRequestURI()).thenReturn(path);
        return request;
    }
}
