package com.cafeteriamanagement.config;

import com.cafeteriamanagement.security.JwtRequestFilter;
import com.cafeteriamanagement.service.CustomUserDetailsService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.dao.DaoAuthenticationProvider;
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
import org.springframework.security.web.util.matcher.AntPathRequestMatcher;

@Configuration
@EnableWebSecurity
@EnableMethodSecurity(prePostEnabled = true)
public class SecurityConfig {

    @Autowired
    private JwtRequestFilter jwtRequestFilter;

    @Autowired
    private CustomUserDetailsService customUserDetailsService;

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

    @Bean
    public DaoAuthenticationProvider authenticationProvider() {
        DaoAuthenticationProvider authProvider = new DaoAuthenticationProvider();
        authProvider.setUserDetailsService(customUserDetailsService);
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
            .csrf(csrf -> csrf.disable())
            .sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
            .authorizeHttpRequests(auth -> auth
                .requestMatchers(new AntPathRequestMatcher("/swagger-ui/**")).permitAll()
                .requestMatchers(new AntPathRequestMatcher("/v3/api-docs/**")).permitAll()
                .requestMatchers(new AntPathRequestMatcher("/api"), new AntPathRequestMatcher("/api/")).permitAll()
                .requestMatchers(new AntPathRequestMatcher("/api/auth/**")).permitAll()
                .requestMatchers(new AntPathRequestMatcher("/h2-console/**")).permitAll()
                .requestMatchers(new AntPathRequestMatcher("/api/ingredients/**", HttpMethod.GET.name())).hasAnyRole("ADMIN", "EMPLOYEE", "CLIENT")
                .requestMatchers(new AntPathRequestMatcher("/api/ingredients", HttpMethod.POST.name())).hasAnyRole("ADMIN", "EMPLOYEE")
                .requestMatchers(new AntPathRequestMatcher("/api/ingredients/**", HttpMethod.PUT.name())).hasAnyRole("ADMIN", "EMPLOYEE")
                .requestMatchers(new AntPathRequestMatcher("/api/ingredients/**", HttpMethod.DELETE.name())).hasAnyRole("ADMIN", "EMPLOYEE")
                .requestMatchers(new AntPathRequestMatcher("/api/dishes/**", HttpMethod.GET.name())).hasAnyRole("ADMIN", "EMPLOYEE", "CLIENT")
                .requestMatchers(new AntPathRequestMatcher("/api/dishes", HttpMethod.POST.name())).hasAnyRole("ADMIN", "EMPLOYEE")
                .requestMatchers(new AntPathRequestMatcher("/api/dishes/**", HttpMethod.PUT.name())).hasAnyRole("ADMIN", "EMPLOYEE")
                .requestMatchers(new AntPathRequestMatcher("/api/dishes/**", HttpMethod.DELETE.name())).hasAnyRole("ADMIN", "EMPLOYEE")
                .requestMatchers(new AntPathRequestMatcher("/api/menus/**", HttpMethod.GET.name())).hasAnyRole("ADMIN", "EMPLOYEE", "CLIENT")
                .requestMatchers(new AntPathRequestMatcher("/api/menus", HttpMethod.POST.name())).hasAnyRole("ADMIN", "EMPLOYEE")
                .requestMatchers(new AntPathRequestMatcher("/api/menus/**", HttpMethod.PUT.name())).hasAnyRole("ADMIN", "EMPLOYEE")
                .requestMatchers(new AntPathRequestMatcher("/api/menus/**", HttpMethod.DELETE.name())).hasAnyRole("ADMIN", "EMPLOYEE")
                .requestMatchers(new AntPathRequestMatcher("/api/users/me", HttpMethod.GET.name())).hasAnyRole("ADMIN", "CLIENT")
                .requestMatchers(new AntPathRequestMatcher("/api/users/me", HttpMethod.PUT.name())).hasAnyRole("ADMIN", "CLIENT")
                .requestMatchers(new AntPathRequestMatcher("/api/users/**", HttpMethod.GET.name())).hasRole("ADMIN")
                .requestMatchers(new AntPathRequestMatcher("/api/users", HttpMethod.POST.name())).hasRole("ADMIN")
                .requestMatchers(new AntPathRequestMatcher("/api/users/**", HttpMethod.PUT.name())).hasRole("ADMIN")
                .requestMatchers(new AntPathRequestMatcher("/api/users/**", HttpMethod.DELETE.name())).hasRole("ADMIN")
                .requestMatchers(new AntPathRequestMatcher("/api/purchases/**", HttpMethod.GET.name())).hasAnyRole("ADMIN", "CLIENT")
                .requestMatchers(new AntPathRequestMatcher("/api/purchases", HttpMethod.POST.name())).hasAnyRole("ADMIN", "CLIENT")
                .requestMatchers(new AntPathRequestMatcher("/api/purchases/**", HttpMethod.PUT.name())).hasAnyRole("ADMIN", "CLIENT")
                .requestMatchers(new AntPathRequestMatcher("/api/purchases/**", HttpMethod.DELETE.name())).hasAnyRole("ADMIN", "CLIENT")
                .anyRequest().authenticated()
            )
            .headers(headers -> headers.frameOptions().sameOrigin());
        http.addFilterBefore(jwtRequestFilter, UsernamePasswordAuthenticationFilter.class);
        return http.build();
    }
}