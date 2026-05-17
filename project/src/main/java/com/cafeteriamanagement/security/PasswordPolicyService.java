package com.cafeteriamanagement.security;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

@Service
public class PasswordPolicyService {

    @Value("${app.password.min:12}")
    private int minLength;

    @Value("${app.password.max:128}")
    private int maxLength;

    public void validate(String password) {
        if (password == null) throw new IllegalArgumentException("Password is required");
        if (password.length() < minLength) {
            throw new IllegalArgumentException("Password must be at least " + minLength + " characters");
        }
        if (password.length() > maxLength) {
            throw new IllegalArgumentException("Password must be at most " + maxLength + " characters");
        }
        // Additional checks (complexity, blacklist, etc.) can be added here
    }
}
