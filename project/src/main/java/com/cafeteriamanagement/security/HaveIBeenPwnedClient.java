package com.cafeteriamanagement.security;

import org.springframework.stereotype.Component;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.security.MessageDigest;

@Component
public class HaveIBeenPwnedClient {

    private static final String HIBP_API_PREFIX = "https://api.pwnedpasswords.com/range/";

    public boolean isBreached(String password) {
        try {
            MessageDigest md = MessageDigest.getInstance("SHA-1");
            byte[] digest = md.digest(password.getBytes("UTF-8"));
            StringBuilder sb = new StringBuilder();
            for (byte b : digest) {
                sb.append(String.format("%02X", b));
            }
            String sha1 = sb.toString();
            String prefix = sha1.substring(0, 5);
            String suffix = sha1.substring(5);

            URL url = new URL(HIBP_API_PREFIX + prefix);
            HttpURLConnection conn = (HttpURLConnection) url.openConnection();
            conn.setRequestMethod("GET");
            conn.setConnectTimeout(3000);
            conn.setReadTimeout(3000);
            conn.setRequestProperty("User-Agent", "cafeteria-management/1.0");

            int code = conn.getResponseCode();
            if (code != 200) {
                // Treat non-200 as unknown (fail open) to avoid blocking users if HIBP is unreachable
                return false;
            }

            try (BufferedReader in = new BufferedReader(new InputStreamReader(conn.getInputStream()))) {
                String line;
                while ((line = in.readLine()) != null) {
                    String[] parts = line.split(":");
                    if (parts.length >= 2) {
                        String returnedSuffix = parts[0].trim();
                        if (returnedSuffix.equalsIgnoreCase(suffix)) {
                            return true;
                        }
                    }
                }
            }
        } catch (Exception e) {
            // On errors, log minimally and allow (fail open). In production, consider fail-closed depending on policy.
            System.out.println("HIBP check failed: " + e.getMessage());
        }
        return false;
    }
}
