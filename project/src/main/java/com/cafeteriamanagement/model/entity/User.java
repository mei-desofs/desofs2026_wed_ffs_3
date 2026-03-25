package com.cafeteriamanagement.model.entity;

import com.cafeteriamanagement.model.enums.UserType;
import jakarta.persistence.*;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import java.math.BigDecimal;
import java.util.Objects;
import java.util.UUID;

@Entity
@Table(name = "users")
public class User {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "external_id", unique = true, nullable = false)
    private String externalId;

    @Column(unique = true, nullable = false)
    @NotBlank(message = "Username is required")
    private String username;

    @Column(nullable = false)
    @NotBlank(message = "Password is required")
    private String password;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    @NotNull(message = "User type is required")
    private UserType type;

    @Column(precision = 10, scale = 2)
    @DecimalMin(value = "0.0", message = "Balance must be non-negative")
    private BigDecimal balance;

    protected User() {}

    public User(String username, String password, UserType type, BigDecimal balance) {
        this.externalId = UUID.randomUUID().toString();
        this.username = username;
        this.password = password;
        this.type = type;
        this.balance = (type == UserType.EMPLOYEE) ? null : balance;
    }

    public Long getId() {
        return id;
    }

    public String getExternalId() {
        return externalId;
    }

    public String getUsername() {
        return username;
    }

    public String getPassword() {
        return password;
    }

    public UserType getType() {
        return type;
    }

    public BigDecimal getBalance() {
        return balance;
    }

    public void updateDetails(String username, String password, UserType type, BigDecimal balance) {
        this.username = username;
        this.password = password; 
        this.type = type;
        this.balance = (type == UserType.EMPLOYEE) ? null : balance;
    }

    public void deductBalance(BigDecimal amount) {
        if (type == UserType.EMPLOYEE) {
            throw new IllegalStateException("Employees cannot make purchases");
        }
        if (balance == null || balance.compareTo(amount) < 0) {
            throw new IllegalStateException("Insufficient balance");
        }
        this.balance = this.balance.subtract(amount);
    }

    public void addBalance(BigDecimal amount) {
        if (type == UserType.EMPLOYEE) {
            throw new IllegalStateException("Employees cannot have balance");
        }
        this.balance = (this.balance == null ? BigDecimal.ZERO : this.balance).add(amount);
    }

    public boolean hasEnoughBalance(BigDecimal amount) {
        if (type == UserType.EMPLOYEE) {
            return false;
        }
        return balance != null && balance.compareTo(amount) >= 0;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        User user = (User) o;
        return Objects.equals(id, user.id);
    }

    @Override
    public int hashCode() {
        return Objects.hash(id);
    }

    @Override
    public String toString() {
        return "User{" +
                "externalId='" + externalId + '\'' +
                ", username='" + username + '\'' +
                ", type=" + type +
                ", balance=" + balance +
                '}';
    }
}