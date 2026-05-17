package com.cafeteriamanagement.security;

import org.springframework.stereotype.Component;

import java.time.Instant;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Component
public class TokenBlocklist {

    private final Map<String, Instant> blocked = new ConcurrentHashMap<>();

    public void block(String token, Instant until) {
        blocked.put(token, until);
    }

    public boolean isBlocked(String token) {
        Instant exp = blocked.get(token);
        if (exp == null) return false;
        if (Instant.now().isAfter(exp)) {
            blocked.remove(token);
            return false;
        }
        return true;
    }
}
