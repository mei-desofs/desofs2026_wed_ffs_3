
package com.cafeteriamanagement.integration;

import com.cafeteriamanagement.dto.LoginRequestDTO;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.http.*;
import org.springframework.boot.test.web.client.TestRestTemplate;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
public class AuthenticationIntegrationTest {

    @LocalServerPort
    private int port;

    @Autowired
    private TestRestTemplate restTemplate;

    private String getBaseUrl() {
        return "http://localhost:" + port + "/api";
    }

    private <T> ResponseEntity<T> postForEntityWithRetry(String url, Object request, Class<T> responseType) {
        int attempts = 0;
        while (attempts < 5) {
            try {
                return restTemplate.postForEntity(url, request, responseType);
            } catch (Exception ex) {
                attempts++;
                try { Thread.sleep(500); } catch (InterruptedException ignored) {}
            }
        }
        throw new RuntimeException("Failed to connect after retries");
    }

    @Test
    void testProtectedEndpointRequiresJwt() {
        ResponseEntity<String> response = restTemplate.getForEntity(
            getBaseUrl() + "/users",
            String.class
        );
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.FORBIDDEN);
    }

    @Test
    void testLoginValidationErrors() {
        LoginRequestDTO loginRequest = new LoginRequestDTO();
        loginRequest.setPassword("password1");
        ResponseEntity<String> response = postForEntityWithRetry(
            getBaseUrl() + "/auth/login",
            loginRequest,
            String.class
        );
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
        assertThat(response.getBody()).contains("username");

        loginRequest = new LoginRequestDTO();
        loginRequest.setUsername("mary_client");
        response = postForEntityWithRetry(
            getBaseUrl() + "/auth/login",
            loginRequest,
            String.class
        );
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
        assertThat(response.getBody()).contains("password");
    }
}
