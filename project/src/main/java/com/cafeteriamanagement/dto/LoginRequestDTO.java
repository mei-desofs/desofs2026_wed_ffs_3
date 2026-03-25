package com.cafeteriamanagement.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;

@Schema(description = "Credentials payload used to authenticate users")
public class LoginRequestDTO {

    @NotBlank(message = "Username is required")
    @Schema(description = "Registered username", example = "john_doe")
    private String username;

    @NotBlank(message = "Password is required")
    @Schema(description = "User password", example = "StrongPass!23")
    private String password;

    public LoginRequestDTO() {}

    public LoginRequestDTO(String username, String password) {
        this.username = username;
        this.password = password;
    }

    public String getUsername() {
        return username;
    }

    public void setUsername(String username) {
        this.username = username;
    }

    public String getPassword() {
        return password;
    }

    public void setPassword(String password) {
        this.password = password;
    }
}