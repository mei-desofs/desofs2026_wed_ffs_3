package com.cafeteriamanagement.controller;

import com.cafeteriamanagement.dto.LoginRequestDTO;
import com.cafeteriamanagement.dto.LoginResponseDTO;
import com.cafeteriamanagement.model.entity.User;
import com.cafeteriamanagement.model.enums.UserType;
import com.cafeteriamanagement.security.JwtTokenUtil;
import com.cafeteriamanagement.security.SecurityAuditLogger;
import com.cafeteriamanagement.security.SimpleRateLimiter;
import com.cafeteriamanagement.security.TokenBlocklist;
import com.cafeteriamanagement.service.CustomUserDetailsService;
import com.cafeteriamanagement.service.UserService;
import jakarta.servlet.http.HttpServletRequest;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.math.BigDecimal;
import java.util.Date;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class AuthControllerTest {

    @Mock private JwtTokenUtil jwtTokenUtil;
    @Mock private CustomUserDetailsService userDetailsService;
    @Mock private UserService userService;
    @Mock private PasswordEncoder passwordEncoder;
    @Mock private SimpleRateLimiter rateLimiter;
    @Mock private SecurityAuditLogger securityAuditLogger;
    @Mock private TokenBlocklist tokenBlocklist;
    @Mock private HttpServletRequest request;

    @InjectMocks
    private AuthController authController;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);
    }

    private LoginRequestDTO loginRequest() {
        LoginRequestDTO dto = mock(LoginRequestDTO.class);
        when(dto.getUsername()).thenReturn("john");
        when(dto.getPassword()).thenReturn("secret-password");
        return dto;
    }

    @Test
    void login_success_returnsToken() throws Exception {
        when(request.getRemoteAddr()).thenReturn("1.2.3.4");
        when(rateLimiter.isBlocked(anyString())).thenReturn(false);
        UserDetails ud = mock(UserDetails.class);
        when(ud.getPassword()).thenReturn("hashed");
        when(userDetailsService.loadUserByUsername("john")).thenReturn(ud);
        when(passwordEncoder.matches("secret-password", "hashed")).thenReturn(true);
        when(userService.findByUsername("john"))
                .thenReturn(new User("john", "hashed", UserType.CLIENT, new BigDecimal("10.00")));
        when(jwtTokenUtil.generateToken(eq(ud), eq("CLIENT"))).thenReturn("jwt-token");

        ResponseEntity<LoginResponseDTO> response = authController.login(loginRequest(), request);

        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertEquals("jwt-token", response.getBody().getToken());
        verify(rateLimiter).reset("login:user:john");
        verify(securityAuditLogger).logAuthenticationSuccess(request, "john");
    }

    @Test
    void login_usesXForwardedForWhenPresent() throws Exception {
        when(request.getHeader("X-Forwarded-For")).thenReturn("9.9.9.9, 1.1.1.1");
        when(rateLimiter.isBlocked(anyString())).thenReturn(false);
        UserDetails ud = mock(UserDetails.class);
        when(ud.getPassword()).thenReturn("hashed");
        when(userDetailsService.loadUserByUsername("john")).thenReturn(ud);
        when(passwordEncoder.matches(anyString(), anyString())).thenReturn(true);
        when(userService.findByUsername("john"))
                .thenReturn(new User("john", "hashed", UserType.ADMIN, BigDecimal.ZERO));
        when(jwtTokenUtil.generateToken(any(), anyString())).thenReturn("jwt");

        assertEquals(HttpStatus.OK, authController.login(loginRequest(), request).getStatusCode());
        verify(rateLimiter).isBlocked("login:ip:9.9.9.9");
    }

    @Test
    void login_alreadyRateLimited_returns429() throws Exception {
        when(request.getRemoteAddr()).thenReturn("1.2.3.4");
        when(rateLimiter.isBlocked("login:user:john")).thenReturn(true);

        ResponseEntity<LoginResponseDTO> response = authController.login(loginRequest(), request);

        assertEquals(HttpStatus.TOO_MANY_REQUESTS, response.getStatusCode());
        verify(securityAuditLogger).logAuthenticationBlocked(eq(request), eq("john"), anyString());
        verifyNoInteractions(jwtTokenUtil);
    }

    @Test
    void login_badPassword_throwsBadCredentials() throws Exception {
        when(request.getRemoteAddr()).thenReturn("1.2.3.4");
        when(rateLimiter.isBlocked(anyString())).thenReturn(false);
        UserDetails ud = mock(UserDetails.class);
        when(ud.getPassword()).thenReturn("hashed");
        when(userDetailsService.loadUserByUsername("john")).thenReturn(ud);
        when(passwordEncoder.matches(anyString(), anyString())).thenReturn(false);

        assertThrows(BadCredentialsException.class, () -> authController.login(loginRequest(), request));
        verify(rateLimiter).recordFailure("login:user:john");
        verify(securityAuditLogger).logAuthenticationFailure(eq(request), eq("john"), anyString());
    }

    @Test
    void login_failureTriggersRateLimit_returns429() throws Exception {
        when(request.getRemoteAddr()).thenReturn("1.2.3.4");
        // not blocked initially, then blocked after the recorded failure
        when(rateLimiter.isBlocked("login:user:john")).thenReturn(false, true);
        when(rateLimiter.isBlocked("login:ip:1.2.3.4")).thenReturn(false);
        when(userDetailsService.loadUserByUsername("john"))
                .thenThrow(new RuntimeException("boom"));

        ResponseEntity<LoginResponseDTO> response = authController.login(loginRequest(), request);

        assertEquals(HttpStatus.TOO_MANY_REQUESTS, response.getStatusCode());
        verify(rateLimiter).recordFailure("login:user:john");
    }

    @Test
    void logout_success_blocksToken() {
        when(request.getHeader("Authorization")).thenReturn("Bearer some-token");
        when(jwtTokenUtil.getExpirationDateFromToken("some-token")).thenReturn(new Date());

        ResponseEntity<Map<String, String>> response = authController.logout(request);

        assertEquals(HttpStatus.OK, response.getStatusCode());
        verify(tokenBlocklist).block(eq("some-token"), any());
    }

    @Test
    void logout_noToken_returns401() {
        when(request.getHeader("Authorization")).thenReturn(null);
        ResponseEntity<Map<String, String>> response = authController.logout(request);
        assertEquals(HttpStatus.UNAUTHORIZED, response.getStatusCode());
        verifyNoInteractions(tokenBlocklist);
    }

    @Test
    void logout_invalidToken_stillReturns200() {
        when(request.getHeader("Authorization")).thenReturn("Bearer bad");
        when(jwtTokenUtil.getExpirationDateFromToken("bad")).thenThrow(new RuntimeException("invalid"));
        ResponseEntity<Map<String, String>> response = authController.logout(request);
        assertEquals(HttpStatus.OK, response.getStatusCode());
    }
}
