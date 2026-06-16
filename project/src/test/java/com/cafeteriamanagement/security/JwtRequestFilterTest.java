package com.cafeteriamanagement.security;

import com.cafeteriamanagement.service.CustomUserDetailsService;
import io.jsonwebtoken.ExpiredJwtException;
import jakarta.servlet.FilterChain;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;

import java.util.Collections;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class JwtRequestFilterTest {

    @Mock
    private CustomUserDetailsService userDetailsService;
    @Mock
    private JwtTokenUtil jwtTokenUtil;
    @Mock
    private SecurityAuditLogger securityAuditLogger;
    @Mock
    private TokenBlocklist tokenBlocklist;

    @InjectMocks
    private JwtRequestFilter filter;

    @Mock
    private HttpServletRequest request;
    @Mock
    private HttpServletResponse response;
    @Mock
    private FilterChain chain;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);
    }

    @AfterEach
    void tearDown() {
        SecurityContextHolder.clearContext();
    }

    @Test
    void authEndpoint_isSkipped() throws Exception {
        when(request.getRequestURI()).thenReturn("/api/auth/login");
        filter.doFilterInternal(request, response, chain);
        verify(chain).doFilter(request, response);
        verifyNoInteractions(jwtTokenUtil);
    }

    @Test
    void missingBearerHeader_passesThroughUnauthenticated() throws Exception {
        when(request.getRequestURI()).thenReturn("/api/users");
        when(request.getHeader("Authorization")).thenReturn(null);
        filter.doFilterInternal(request, response, chain);
        verify(chain).doFilter(request, response);
        assertNull(SecurityContextHolder.getContext().getAuthentication());
    }

    @Test
    void blockedToken_isRejected() throws Exception {
        when(request.getRequestURI()).thenReturn("/api/users");
        when(request.getHeader("Authorization")).thenReturn("Bearer revoked-token");
        when(tokenBlocklist.isBlocked("revoked-token")).thenReturn(true);

        filter.doFilterInternal(request, response, chain);

        verify(securityAuditLogger).logTokenAuthenticationFailure(eq(request), contains("revoked"));
        verify(chain).doFilter(request, response);
        assertNull(SecurityContextHolder.getContext().getAuthentication());
    }

    @Test
    void validToken_authenticatesRequest() throws Exception {
        when(request.getRequestURI()).thenReturn("/api/users");
        when(request.getHeader("Authorization")).thenReturn("Bearer good-token");
        when(tokenBlocklist.isBlocked("good-token")).thenReturn(false);
        when(jwtTokenUtil.getUsernameFromToken("good-token")).thenReturn("john");
        UserDetails ud = mock(UserDetails.class);
        when(ud.getAuthorities()).thenReturn(Collections.emptyList());
        when(userDetailsService.loadUserByUsername("john")).thenReturn(ud);
        when(jwtTokenUtil.validateToken("good-token", ud)).thenReturn(true);

        filter.doFilterInternal(request, response, chain);

        assertNotNull(SecurityContextHolder.getContext().getAuthentication());
        verify(chain).doFilter(request, response);
    }

    @Test
    void invalidToken_isNotAuthenticated() throws Exception {
        when(request.getRequestURI()).thenReturn("/api/users");
        when(request.getHeader("Authorization")).thenReturn("Bearer bad-token");
        when(tokenBlocklist.isBlocked("bad-token")).thenReturn(false);
        when(jwtTokenUtil.getUsernameFromToken("bad-token")).thenReturn("john");
        UserDetails ud = mock(UserDetails.class);
        when(userDetailsService.loadUserByUsername("john")).thenReturn(ud);
        when(jwtTokenUtil.validateToken("bad-token", ud)).thenReturn(false);

        filter.doFilterInternal(request, response, chain);

        verify(securityAuditLogger).logTokenAuthenticationFailure(eq(request), contains("validation failed"));
        assertNull(SecurityContextHolder.getContext().getAuthentication());
    }

    @Test
    void expiredToken_isLoggedAndNotAuthenticated() throws Exception {
        when(request.getRequestURI()).thenReturn("/api/users");
        when(request.getHeader("Authorization")).thenReturn("Bearer expired-token");
        when(tokenBlocklist.isBlocked("expired-token")).thenReturn(false);
        when(jwtTokenUtil.getUsernameFromToken("expired-token"))
                .thenThrow(new ExpiredJwtException(null, null, "expired"));

        filter.doFilterInternal(request, response, chain);

        verify(securityAuditLogger).logTokenAuthenticationFailure(eq(request), contains("expired"));
        verify(chain).doFilter(request, response);
        assertNull(SecurityContextHolder.getContext().getAuthentication());
    }
}
