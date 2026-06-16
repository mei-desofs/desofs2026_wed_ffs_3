package com.cafeteriamanagement.exception;

import com.cafeteriamanagement.security.SecurityAuditLogger;
import jakarta.servlet.http.HttpServletRequest;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.validation.BindingResult;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;

import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class GlobalExceptionHandlerTest {

    @Mock
    private SecurityAuditLogger securityAuditLogger;

    @InjectMocks
    private GlobalExceptionHandler handler;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);
    }

    @Test
    void validationException_returns400WithFieldErrors() {
        MethodArgumentNotValidException ex = mock(MethodArgumentNotValidException.class);
        BindingResult br = mock(BindingResult.class);
        when(ex.getBindingResult()).thenReturn(br);
        when(br.getAllErrors()).thenReturn(List.of(new FieldError("user", "username", "must not be blank")));

        ResponseEntity<Map<String, String>> response = handler.handleValidationExceptions(ex);

        assertEquals(HttpStatus.BAD_REQUEST, response.getStatusCode());
        assertEquals("must not be blank", response.getBody().get("username"));
    }

    @Test
    void illegalArgument_returns400() {
        ResponseEntity<Map<String, String>> response =
                handler.handleIllegalArgumentException(new IllegalArgumentException("bad input"));
        assertEquals(HttpStatus.BAD_REQUEST, response.getStatusCode());
        assertEquals("bad input", response.getBody().get("error"));
    }

    @Test
    void illegalState_returns400() {
        ResponseEntity<Map<String, String>> response =
                handler.handleIllegalStateException(new IllegalStateException("bad state"));
        assertEquals(HttpStatus.BAD_REQUEST, response.getStatusCode());
        assertEquals("bad state", response.getBody().get("error"));
    }

    @Test
    void badCredentials_returns401() {
        ResponseEntity<Map<String, String>> response =
                handler.handleBadCredentialsException(new BadCredentialsException("nope"));
        assertEquals(HttpStatus.UNAUTHORIZED, response.getStatusCode());
    }

    @Test
    void accessDenied_returns403AndIsLogged() {
        HttpServletRequest request = mock(HttpServletRequest.class);
        AccessDeniedException ex = new AccessDeniedException("denied");
        ResponseEntity<Map<String, String>> response = handler.handleAccessDeniedException(ex, request);
        assertEquals(HttpStatus.FORBIDDEN, response.getStatusCode());
        assertEquals("Forbidden", response.getBody().get("error"));
        verify(securityAuditLogger).logAccessDenied(request, ex);
    }

    @Test
    void dataIntegrity_ingredientConstraint() {
        String msg = "Unique index CONSTRAINT_INDEX_2 ON PUBLIC.INGREDIENTS(NAME) VALUES ( /* 1 */ 'Tomato' )";
        ResponseEntity<Map<String, String>> response =
                handler.handleDataIntegrityViolationException(new DataIntegrityViolationException(msg));
        assertEquals(HttpStatus.BAD_REQUEST, response.getStatusCode());
        assertTrue(response.getBody().get("error").contains("Tomato"));
    }

    @Test
    void dataIntegrity_userConstraint() {
        String msg = "CONSTRAINT_INDEX ON PUBLIC.USERS(USERNAME) VALUES ( /* 1 */ 'john' )";
        ResponseEntity<Map<String, String>> response =
                handler.handleDataIntegrityViolationException(new DataIntegrityViolationException(msg));
        assertTrue(response.getBody().get("error").contains("john"));
    }

    @Test
    void dataIntegrity_unknownConstraint_genericMessage() {
        String msg = "CONSTRAINT_INDEX on some other table";
        ResponseEntity<Map<String, String>> response =
                handler.handleDataIntegrityViolationException(new DataIntegrityViolationException(msg));
        assertTrue(response.getBody().get("error").contains("already exists"));
    }

    @Test
    void dataIntegrity_nonConstraint_genericMessage() {
        ResponseEntity<Map<String, String>> response =
                handler.handleDataIntegrityViolationException(new DataIntegrityViolationException("some other DB error"));
        assertEquals("A record with this information already exists", response.getBody().get("error"));
    }

    @Test
    void genericException_returns500WithSafeMessage() {
        ResponseEntity<Map<String, String>> response =
                handler.handleGenericException(new RuntimeException("internal detail that must not leak"));
        assertEquals(HttpStatus.INTERNAL_SERVER_ERROR, response.getStatusCode());
        assertEquals("An unexpected error occurred", response.getBody().get("error"));
    }
}
