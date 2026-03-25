package com.cafeteriamanagement.util;

import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;

public class PasswordTest {
    public static void main(String[] args) {
        BCryptPasswordEncoder encoder = new BCryptPasswordEncoder();
        
        String storedHash = "$2a$10$N9qo8uLOickgx2ZMRZoMye5JFYPqJqlF3dQU8Q2O8T6T6vQZxW.ky";
        String password = "123";
        
        System.out.println("Testing password: " + password);
        System.out.println("Against hash: " + storedHash);
        System.out.println("Match result: " + encoder.matches(password, storedHash));
        
        String newHash = encoder.encode(password);
        System.out.println("New hash for same password: " + newHash);
        System.out.println("New hash matches: " + encoder.matches(password, newHash));
    }
}