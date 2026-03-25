package com.cafeteriamanagement.dto;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "Authentication response returning JWT and user metadata")
public class LoginResponseDTO {

    @Schema(description = "JWT bearer token to authorize subsequent requests", example = "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9...")
    private String token;

    @Schema(description = "Authenticated username", example = "john_doe")
    private String username;

    @Schema(description = "Role granted to the authenticated user", example = "CLIENT")
    private String role;

    @Schema(description = "Additional authentication message", example = "Login successful")
    private String message;

    public LoginResponseDTO() {}

    public LoginResponseDTO(String token, String username, String role, String message) {
        this.token = token;
        this.username = username;
        this.role = role;
        this.message = message;
    }

    public String getToken() {
        return token;
    }

    public void setToken(String token) {
        this.token = token;
    }

    public String getUsername() {
        return username;
    }

    public void setUsername(String username) {
        this.username = username;
    }

    public String getRole() {
        return role;
    }

    public void setRole(String role) {
        this.role = role;
    }

    public String getMessage() {
        return message;
    }

    public void setMessage(String message) {
        this.message = message;
    }
}