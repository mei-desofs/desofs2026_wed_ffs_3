package com.cafeteriamanagement.dto;

import com.cafeteriamanagement.model.enums.UserType;
import com.fasterxml.jackson.annotation.JsonProperty;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import java.math.BigDecimal;

@Schema(description = "User payload exchanging information between client and server")
public class UserDTO {

    @Schema(description = "External identifier exposed via API", example = "550e8400-e29b-41d4-a716-446655440000", accessMode = Schema.AccessMode.READ_ONLY)
    private String id;

    @NotBlank(message = "Username is required")
    @Schema(description = "Unique username used for authentication", example = "john_doe")
    private String username;

    @NotBlank(message = "Password is required")
    @JsonProperty(access = JsonProperty.Access.WRITE_ONLY)
    @Schema(description = "Plain text password supplied on create/update", example = "StrongPass!23", accessMode = Schema.AccessMode.WRITE_ONLY)
    private String password;

    @NotNull(message = "User type is required")
    @Schema(description = "Type of user in the platform", example = "CLIENT")
    private UserType type;

    @DecimalMin(value = "0.0", message = "Balance must be non-negative")
    @Schema(description = "Wallet balance available for purchases", example = "25.50")
    private BigDecimal balance;

    public UserDTO() {}

    public UserDTO(String id, String username, String password, UserType type, BigDecimal balance) {
        this.id = id;
        this.username = username;
        this.password = password;
        this.type = type;
        this.balance = balance;
    }

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
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

    public UserType getType() {
        return type;
    }

    public void setType(UserType type) {
        this.type = type;
    }

    public BigDecimal getBalance() {
        return balance;
    }

    public void setBalance(BigDecimal balance) {
        this.balance = balance;
    }
}