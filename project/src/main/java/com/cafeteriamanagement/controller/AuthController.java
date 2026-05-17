package com.cafeteriamanagement.controller;

import com.cafeteriamanagement.dto.LoginRequestDTO;
import com.cafeteriamanagement.dto.LoginResponseDTO;
import com.cafeteriamanagement.model.entity.User;
import com.cafeteriamanagement.security.JwtTokenUtil;
import com.cafeteriamanagement.security.SecurityAuditLogger;
import com.cafeteriamanagement.service.CustomUserDetailsService;
import com.cafeteriamanagement.service.UserService;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.bind.annotation.*;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.http.HttpStatus;

@RestController
@RequestMapping("/api/auth")
@CrossOrigin(origins = "*")
@Tag(name = "Authentication", description = "Endpoints for JWT based authentication")
public class AuthController {

    @Autowired
    private JwtTokenUtil jwtTokenUtil;

    @Autowired
    private CustomUserDetailsService userDetailsService;

    @Autowired
    private UserService userService;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @Autowired
    private com.cafeteriamanagement.security.SimpleRateLimiter rateLimiter;

    @Autowired
    private SecurityAuditLogger securityAuditLogger;

    @PostMapping("/login")
    @Operation(
        summary = "Authenticate user and issue JWT",
        description = "Validates the provided credentials and returns a JWT token when successful."
    )
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "Authentication successful",
            content = @Content(schema = @Schema(implementation = LoginResponseDTO.class))),
        @ApiResponse(responseCode = "401", description = "Invalid credentials provided")
    })
    public ResponseEntity<LoginResponseDTO> login(@Valid @RequestBody LoginRequestDTO loginRequest, HttpServletRequest request) throws Exception {
        String clientIp = extractClientIp(request);
        String userKey = "login:user:" + loginRequest.getUsername();
        String ipKey = "login:ip:" + clientIp;

        if (rateLimiter.isBlocked(userKey) || rateLimiter.isBlocked(ipKey)) {
            securityAuditLogger.logAuthenticationBlocked(request, loginRequest.getUsername(), "login rate limit already active");
            return ResponseEntity.status(HttpStatus.TOO_MANY_REQUESTS).build();
        }
        
        try {
            UserDetails userDetails = userDetailsService.loadUserByUsername(loginRequest.getUsername());
            
            if (!passwordEncoder.matches(loginRequest.getPassword(), userDetails.getPassword())) {
                throw new BadCredentialsException("Invalid credentials");
            }
            
            User user = userService.findByUsername(loginRequest.getUsername());
            
            final String token = jwtTokenUtil.generateToken(userDetails, user.getType().name());
            // on successful login reset rate limiter for this user/ip
            rateLimiter.reset(userKey);
            rateLimiter.reset(ipKey);
            securityAuditLogger.logAuthenticationSuccess(request, user.getUsername());

            return ResponseEntity.ok(new LoginResponseDTO(token, user.getUsername(), user.getType().name(), "Login successful"));
        } catch (Exception e) {
            securityAuditLogger.logAuthenticationFailure(request, loginRequest.getUsername(), "invalid credentials");
            // record failure against username and IP
            rateLimiter.recordFailure(userKey);
            rateLimiter.recordFailure(ipKey);

            if (rateLimiter.isBlocked(userKey) || rateLimiter.isBlocked(ipKey)) {
                securityAuditLogger.logAuthenticationBlocked(request, loginRequest.getUsername(), "login rate limit triggered");
                return ResponseEntity.status(HttpStatus.TOO_MANY_REQUESTS).build();
            }

            throw new BadCredentialsException("Invalid credentials");
        }
    }

    private String extractClientIp(HttpServletRequest request) {
        String xf = request.getHeader("X-Forwarded-For");
        if (xf != null && !xf.isBlank()) {
            return xf.split(",")[0].trim();
        }
        return request.getRemoteAddr();
    }
}
