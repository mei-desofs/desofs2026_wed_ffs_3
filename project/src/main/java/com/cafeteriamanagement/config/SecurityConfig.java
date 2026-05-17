package com.cafeteriamanagement.config;

import com.cafeteriamanagement.security.JwtRequestFilter;
import com.cafeteriamanagement.security.SecurityAuditLogger;
import com.cafeteriamanagement.service.CustomUserDetailsService;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.web.servlet.FilterRegistrationBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.Ordered;
import org.springframework.http.HttpMethod;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.dao.DaoAuthenticationProvider;
import org.springframework.security.config.Customizer;
import org.springframework.security.config.annotation.authentication.builders.AuthenticationManagerBuilder;
import org.springframework.security.config.annotation.authentication.configuration.AuthenticationConfiguration;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.web.filter.ForwardedHeaderFilter;

@Configuration
@EnableWebSecurity
@EnableMethodSecurity(prePostEnabled = true)
public class SecurityConfig {

    @Autowired
    private JwtRequestFilter jwtRequestFilter;

    @Autowired
    private CustomUserDetailsService customUserDetailsService;

    @Autowired
    private SecurityAuditLogger securityAuditLogger;

    @Value("${app.security.require-https:true}")
    private boolean requireHttps;

    @Bean
    public FilterRegistrationBean<ForwardedHeaderFilter> forwardedHeaderFilter() {
        FilterRegistrationBean<ForwardedHeaderFilter> registration =
            new FilterRegistrationBean<>(new ForwardedHeaderFilter());
        registration.setOrder(Ordered.HIGHEST_PRECEDENCE);
        return registration;
    }

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

    @Bean
    public DaoAuthenticationProvider authenticationProvider() {
        DaoAuthenticationProvider authProvider = new DaoAuthenticationProvider(customUserDetailsService);
        authProvider.setPasswordEncoder(passwordEncoder());
        return authProvider;
    }

    @Bean
    public AuthenticationManager authenticationManager(AuthenticationConfiguration config) throws Exception {
        return config.getAuthenticationManager();
    }

    @Bean 
    public AuthenticationManager authManager(HttpSecurity http) throws Exception {
        AuthenticationManagerBuilder authenticationManagerBuilder = 
            http.getSharedObject(AuthenticationManagerBuilder.class);
        authenticationManagerBuilder.authenticationProvider(authenticationProvider());
        return authenticationManagerBuilder.build();
    }

    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
        http
            .authenticationProvider(authenticationProvider())
            // CSRF disabled: stateless REST API uses JWT in Authorization header, not cookies
            .csrf(csrf -> csrf.disable())
            .sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
            .exceptionHandling(exceptions -> exceptions
                .accessDeniedHandler((request, response, accessDeniedException) -> {
                    securityAuditLogger.logAccessDenied(request, accessDeniedException);
                    response.sendError(HttpServletResponse.SC_FORBIDDEN);
                })
                .authenticationEntryPoint((request, response, authException) -> {
                    securityAuditLogger.logAuthenticationRequired(request, authException);
                    response.sendError(HttpServletResponse.SC_FORBIDDEN);
                })
            )
            .authorizeHttpRequests(auth -> auth
                .requestMatchers("/swagger-ui/**").permitAll()
                .requestMatchers("/v3/api-docs/**").permitAll()
                .requestMatchers("/api", "/api/").permitAll()
                .requestMatchers("/api/auth/**").permitAll()
                .requestMatchers("/actuator/health").permitAll()
                .requestMatchers("/actuator/**").denyAll()
                .requestMatchers(HttpMethod.GET, "/api/ingredients/**").hasAnyRole("ADMIN", "EMPLOYEE", "CLIENT")
                .requestMatchers(HttpMethod.POST, "/api/ingredients").hasAnyRole("ADMIN", "EMPLOYEE")
                .requestMatchers(HttpMethod.PUT, "/api/ingredients/**").hasAnyRole("ADMIN", "EMPLOYEE")
                .requestMatchers(HttpMethod.DELETE, "/api/ingredients/**").hasAnyRole("ADMIN", "EMPLOYEE")
                .requestMatchers(HttpMethod.GET, "/api/dishes/**").hasAnyRole("ADMIN", "EMPLOYEE", "CLIENT")
                .requestMatchers(HttpMethod.POST, "/api/dishes").hasAnyRole("ADMIN", "EMPLOYEE")
                .requestMatchers(HttpMethod.PUT, "/api/dishes/**").hasAnyRole("ADMIN", "EMPLOYEE")
                .requestMatchers(HttpMethod.DELETE, "/api/dishes/**").hasAnyRole("ADMIN", "EMPLOYEE")
                .requestMatchers(HttpMethod.GET, "/api/menus/**").hasAnyRole("ADMIN", "EMPLOYEE", "CLIENT")
                .requestMatchers(HttpMethod.POST, "/api/menus").hasAnyRole("ADMIN", "EMPLOYEE")
                .requestMatchers(HttpMethod.PUT, "/api/menus/**").hasAnyRole("ADMIN", "EMPLOYEE")
                .requestMatchers(HttpMethod.DELETE, "/api/menus/**").hasAnyRole("ADMIN", "EMPLOYEE")
                .requestMatchers(HttpMethod.GET, "/api/users/me").hasAnyRole("ADMIN", "CLIENT")
                .requestMatchers(HttpMethod.PUT, "/api/users/me").hasAnyRole("ADMIN", "CLIENT")
                .requestMatchers(HttpMethod.GET, "/api/users/**").hasRole("ADMIN")
                .requestMatchers(HttpMethod.POST, "/api/users").hasRole("ADMIN")
                .requestMatchers(HttpMethod.PUT, "/api/users/**").hasRole("ADMIN")
                .requestMatchers(HttpMethod.DELETE, "/api/users/**").hasRole("ADMIN")
                .requestMatchers(HttpMethod.GET, "/api/purchases/**").hasAnyRole("ADMIN", "CLIENT", "EMPLOYEE")
                .requestMatchers(HttpMethod.POST, "/api/purchases").hasAnyRole("ADMIN", "CLIENT")
                .requestMatchers(HttpMethod.PUT, "/api/purchases/**").hasAnyRole("ADMIN", "CLIENT")
                .requestMatchers(HttpMethod.DELETE, "/api/purchases/**").hasAnyRole("ADMIN", "CLIENT")
                .requestMatchers(HttpMethod.GET, "/api/files/list").hasAnyRole("ADMIN", "EMPLOYEE", "CLIENT")
                .requestMatchers(HttpMethod.GET, "/api/files").hasAnyRole("ADMIN", "EMPLOYEE", "CLIENT")
                .requestMatchers(HttpMethod.POST, "/api/files").hasAnyRole("ADMIN", "EMPLOYEE")
                .requestMatchers(HttpMethod.POST, "/api/files/directory").hasAnyRole("ADMIN", "EMPLOYEE")
                .requestMatchers(HttpMethod.DELETE, "/api/files").hasRole("ADMIN")
                .anyRequest().authenticated()
            )
            .headers(headers -> {
                headers.cacheControl(Customizer.withDefaults());
                headers.frameOptions(frameOptions -> frameOptions.sameOrigin());
                if (requireHttps) {
                    headers.httpStrictTransportSecurity(hsts -> hsts
                        .includeSubDomains(true)
                        .maxAgeInSeconds(31536000));
                }
            });
        if (requireHttps) {
            http.redirectToHttps(Customizer.withDefaults());
        }
        http.addFilterBefore(jwtRequestFilter, UsernamePasswordAuthenticationFilter.class);
        return http.build();
    }
}
