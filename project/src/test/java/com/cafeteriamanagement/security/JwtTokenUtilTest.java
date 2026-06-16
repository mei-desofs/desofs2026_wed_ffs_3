package com.cafeteriamanagement.security;

import io.jsonwebtoken.ExpiredJwtException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.Date;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class JwtTokenUtilTest {

    private JwtTokenUtil jwtTokenUtil;

    private UserDetails userDetails(String username) {
        UserDetails ud = mock(UserDetails.class);
        when(ud.getUsername()).thenReturn(username);
        return ud;
    }

    private JwtTokenUtil build(int validitySeconds) {
        JwtTokenUtil util = new JwtTokenUtil();
        ReflectionTestUtils.setField(util, "secret", "test-secret-key-that-is-at-least-32-bytes-long!!");
        ReflectionTestUtils.setField(util, "jwtTokenValidity", validitySeconds);
        ReflectionTestUtils.invokeMethod(util, "init");
        return util;
    }

    @BeforeEach
    void setUp() {
        jwtTokenUtil = build(3600);
    }

    @Test
    void generateToken_canBeParsedBack() {
        String token = jwtTokenUtil.generateToken(userDetails("john"), "ADMIN");
        assertEquals("john", jwtTokenUtil.getUsernameFromToken(token));
        assertEquals("ADMIN", jwtTokenUtil.getRoleFromToken(token));
        assertTrue(jwtTokenUtil.getExpirationDateFromToken(token).after(new Date()));
    }

    @Test
    void validateToken_trueForMatchingUser() {
        UserDetails ud = userDetails("john");
        String token = jwtTokenUtil.generateToken(ud, "CLIENT");
        assertTrue(jwtTokenUtil.validateToken(token, ud));
    }

    @Test
    void validateToken_falseForDifferentUser() {
        String token = jwtTokenUtil.generateToken(userDetails("john"), "CLIENT");
        assertFalse(jwtTokenUtil.validateToken(token, userDetails("mary")));
    }

    @Test
    void expiredToken_isRejectedOnParse() {
        JwtTokenUtil shortLived = build(-10); // already expired
        String token = shortLived.generateToken(userDetails("john"), "CLIENT");
        assertThrows(ExpiredJwtException.class, () -> shortLived.getUsernameFromToken(token));
    }
}
