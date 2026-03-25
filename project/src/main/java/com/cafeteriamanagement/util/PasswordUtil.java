package com.cafeteriamanagement.util;

import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;

public class PasswordUtil {
    public static void main(String[] args) {
        BCryptPasswordEncoder encoder = new BCryptPasswordEncoder();
        
        System.out.println("admin123: " + encoder.encode("admin123"));
        System.out.println("emp123: " + encoder.encode("emp123"));
        System.out.println("client123: " + encoder.encode("client123"));
    }
}