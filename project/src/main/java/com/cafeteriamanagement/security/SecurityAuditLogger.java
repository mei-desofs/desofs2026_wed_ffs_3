package com.cafeteriamanagement.security;

import jakarta.servlet.http.HttpServletRequest;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.AuthenticationException;
import org.springframework.stereotype.Component;

import java.util.stream.Collectors;

@Component
public class SecurityAuditLogger {

    private static final Logger AUDIT_LOG = LoggerFactory.getLogger("SECURITY_AUDIT");

    public void logAccessDenied(HttpServletRequest request, AccessDeniedException exception) {
        logFailure("ACCESS_DENIED", request, exception.getMessage());
    }

    public void logAuthenticationRequired(HttpServletRequest request, AuthenticationException exception) {
        logFailure("AUTHENTICATION_REQUIRED", request, exception.getMessage());
    }

    public void logAuthenticationSuccess(HttpServletRequest request, String username) {
        logAuthenticationDecision("AUTHENTICATION_SUCCESS", request, username, "credentials accepted", false);
    }

    public void logAuthenticationFailure(HttpServletRequest request, String username, String reason) {
        logAuthenticationDecision("AUTHENTICATION_FAILURE", request, username, reason, true);
    }

    public void logAuthenticationBlocked(HttpServletRequest request, String username, String reason) {
        logAuthenticationDecision("AUTHENTICATION_BLOCKED", request, username, reason, true);
    }

    public void logTokenAuthenticationFailure(HttpServletRequest request, String reason) {
        logAuthenticationDecision("TOKEN_AUTHENTICATION_FAILURE", request, "bearer-token", reason, true);
    }

    private void logFailure(String event, HttpServletRequest request, String reason) {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        String principal = "anonymous";
        String authorities = "none";

        if (authentication != null) {
            principal = sanitize(authentication.getName());
            authorities = authentication.getAuthorities().stream()
                .map(GrantedAuthority::getAuthority)
                .collect(Collectors.joining(","));
            if (authorities.isBlank()) {
                authorities = "none";
            }
        }

        AUDIT_LOG.warn(
            "event={} principal={} authorities={} ip={} method={} path={} reason={}",
            event,
            principal,
            sanitize(authorities),
            sanitize(request.getRemoteAddr()),
            sanitize(request.getMethod()),
            sanitize(request.getRequestURI()),
            sanitize(reason)
        );
    }

    private void logAuthenticationDecision(
            String event,
            HttpServletRequest request,
            String username,
            String reason,
            boolean failure) {
        Logger logger = AUDIT_LOG;
        String message = "event={} username={} ip={} method={} path={} reason={}";
        Object[] args = {
            event,
            sanitize(username),
            sanitize(request.getRemoteAddr()),
            sanitize(request.getMethod()),
            sanitize(request.getRequestURI()),
            sanitize(reason)
        };

        if (failure) {
            logger.warn(message, args);
        } else {
            logger.info(message, args);
        }
    }

    public void logFileOperation(String event, String username, String path) {
        AUDIT_LOG.info("event={} username={} path={}",
            event,
            sanitize(username),
            sanitize(path));
    }

    public void logPurchaseOperation(String event, String username, String purchaseId, String dishName) {
        AUDIT_LOG.info("event={} username={} purchaseId={} dish={}",
            event,
            sanitize(username),
            sanitize(purchaseId),
            sanitize(dishName));
    }

    private String sanitize(String value) {
        if (value == null || value.isBlank()) {
            return "unknown";
        }
        return value.replaceAll("[\\r\\n\\t]", "_");
    }
}
