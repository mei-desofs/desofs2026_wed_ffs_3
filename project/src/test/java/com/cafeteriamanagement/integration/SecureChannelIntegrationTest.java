package com.cafeteriamanagement.integration;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import static org.hamcrest.Matchers.startsWith;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest(properties = "app.security.require-https=true")
@AutoConfigureMockMvc
class SecureChannelIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Test
    void redirectsPlainHttpRequestsToHttps() throws Exception {
        mockMvc.perform(post("/api/auth/login")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{}"))
            .andExpect(status().is3xxRedirection())
            .andExpect(header().string("Location", startsWith("https://")));
    }

    @Test
    void acceptsRequestsMarkedSecureByReverseProxy() throws Exception {
        mockMvc.perform(post("/api/auth/login")
                .header("X-Forwarded-Proto", "https")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{}"))
            .andExpect(status().isBadRequest());
    }
}
