package com.cafeteriamanagement.opaque;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.test.context.jdbc.Sql;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@Sql(scripts = "/opaque-test-data.sql", executionPhase = Sql.ExecutionPhase.BEFORE_TEST_METHOD)
@Sql(scripts = "/opaque-test-cleanup.sql", executionPhase = Sql.ExecutionPhase.AFTER_TEST_METHOD)
class OpaqueApiSecurityTest {

    private static final String ADMIN = "opaque_admin";
    private static final String ADMIN_PASSWORD = "StrongPass!23";
    private static final String EMPLOYEE = "opaque_employee";
    private static final String EMPLOYEE_PASSWORD = "EmployeePass!23";
    private static final String CLIENT = "opaque_client";
    private static final String CLIENT_PASSWORD = "ClientPass!23";
    private static final String OTHER_CLIENT = "opaque_other_client";
    private static final String LOGOUT_CLIENT = "opaque_logout_client";
    private static final String LOGOUT_CLIENT_PASSWORD = "ClientPass!23";

    @LocalServerPort
    private int port;

    @Autowired
    private TestRestTemplate restTemplate;

    @Autowired
    private ObjectMapper objectMapper;

    @Test
    void protectedEndpointWithoutJwtIsForbidden() {
        ResponseEntity<String> response = get("/api/dishes");

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.FORBIDDEN);
    }

    @Test
    void publicHealthEndpointIsAvailableButOtherActuatorEndpointsAreDenied() {
        ResponseEntity<String> health = get("/actuator/health");
        ResponseEntity<String> actuator = get("/actuator");

        assertThat(health.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(actuator.getStatusCode()).isEqualTo(HttpStatus.FORBIDDEN);
    }

    @Test
    void securityHeadersArePresentOnHttpResponses() {
        ResponseEntity<String> response = get("/actuator/health");

        assertThat(response.getHeaders().getFirst(HttpHeaders.CACHE_CONTROL)).contains("no-cache");
        assertThat(response.getHeaders().getFirst("X-Frame-Options")).isEqualTo("SAMEORIGIN");
        assertThat(response.getHeaders().getFirst("X-Content-Type-Options")).isEqualTo("nosniff");
    }

    @Test
    void corsPreflightAllowsConfiguredFrontendOrigin() {
        HttpHeaders headers = new HttpHeaders();
        headers.setOrigin("http://localhost:3000");
        headers.setAccessControlRequestMethod(HttpMethod.GET);
        headers.setAccessControlRequestHeaders(List.of(HttpHeaders.AUTHORIZATION, HttpHeaders.CONTENT_TYPE));

        ResponseEntity<String> response = exchange("/api/dishes", HttpMethod.OPTIONS, new HttpEntity<>(headers));

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getHeaders().getAccessControlAllowOrigin()).isEqualTo("http://localhost:3000");
        assertThat(response.getHeaders().getAccessControlAllowMethods()).contains(HttpMethod.GET);
        assertThat(response.getHeaders().getAccessControlAllowHeaders())
                .contains(HttpHeaders.AUTHORIZATION, HttpHeaders.CONTENT_TYPE);
    }

    @Test
    void loginWithValidCredentialsReturnsJwtAndNoPassword() throws Exception {
        ResponseEntity<String> response = loginResponse(CLIENT, CLIENT_PASSWORD);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        JsonNode body = json(response);
        assertThat(body.path("token").asText()).isNotBlank();
        assertThat(body.path("username").asText()).isEqualTo(CLIENT);
        assertThat(body.path("role").asText()).isEqualTo("CLIENT");
        assertThat(body.has("password")).isFalse();
    }

    @Test
    void loginValidationErrorsAreReturnedAsBadRequest() throws Exception {
        ResponseEntity<String> response = post("/api/auth/login", Map.of("username", CLIENT));

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
        assertThat(json(response).path("password").asText()).contains("Password is required");
    }

    @Test
    void loginWithWrongPasswordIsUnauthorized() throws Exception {
        ResponseEntity<String> response = loginResponse(CLIENT, "WrongPassword!23");

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.UNAUTHORIZED);
        assertThat(json(response).path("error").asText()).contains("Invalid credentials");
    }

    @Test
    void repeatedFailedLoginAttemptsAreRateLimited() {
        HttpHeaders headers = jsonHeaders();
        headers.set("X-Forwarded-For", "198.51.100.77");
        HttpEntity<Map<String, Object>> request = new HttpEntity<>(
                Map.of("username", "missing_rate_limited_user", "password", "WrongPassword!23"),
                headers
        );

        ResponseEntity<String> response = null;
        for (int i = 0; i < 5; i++) {
            response = exchange("/api/auth/login", HttpMethod.POST, request);
        }
        assertThat(response).isNotNull();
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.TOO_MANY_REQUESTS);

        ResponseEntity<String> blockedImmediately = exchange("/api/auth/login", HttpMethod.POST, request);
        assertThat(blockedImmediately.getStatusCode()).isEqualTo(HttpStatus.TOO_MANY_REQUESTS);
    }

    @Test
    void revokedJwtCannotAccessProtectedResources() {
        String token = loginToken(LOGOUT_CLIENT, LOGOUT_CLIENT_PASSWORD);
        ResponseEntity<String> beforeLogout = get("/api/users/me", bearer(token));

        ResponseEntity<String> logout = post("/api/auth/logout", null, bearer(token));
        ResponseEntity<String> afterLogout = get("/api/users/me", bearer(token));

        assertThat(beforeLogout.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(logout.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(afterLogout.getStatusCode()).isEqualTo(HttpStatus.FORBIDDEN);
    }

    @Test
    void clientCanReadDishesButCannotCreateDish() {
        String token = loginToken(CLIENT, CLIENT_PASSWORD);
        Map<String, Object> newDish = Map.of(
                "name", "Opaque Client Dish",
                "ingredientNames", List.of("Tomatoes"),
                "price", "4.50"
        );

        ResponseEntity<String> readResponse = get("/api/dishes", bearer(token));
        ResponseEntity<String> writeResponse = post("/api/dishes", newDish, bearer(token));

        assertThat(readResponse.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(writeResponse.getStatusCode()).isEqualTo(HttpStatus.FORBIDDEN);
    }

    @Test
    void employeeCanCreateIngredientButClientCannotDeleteIngredient() throws Exception {
        String employeeToken = loginToken(EMPLOYEE, EMPLOYEE_PASSWORD);
        String clientToken = loginToken(CLIENT, CLIENT_PASSWORD);
        Map<String, Object> ingredient = Map.of(
                "name", "Opaque Herb",
                "type", "VEGETABLES",
                "allergen", "NONE"
        );

        ResponseEntity<String> created = post("/api/ingredients", ingredient, bearer(employeeToken));
        ResponseEntity<String> deleteByClient = exchange(
                "/api/ingredients/" + json(created).path("id").asText(),
                HttpMethod.DELETE,
                new HttpEntity<>(bearer(clientToken))
        );

        assertThat(created.getStatusCode()).isEqualTo(HttpStatus.CREATED);
        assertThat(deleteByClient.getStatusCode()).isEqualTo(HttpStatus.FORBIDDEN);
    }

    @Test
    void adminCanListUsersButClientCannot() {
        String adminToken = loginToken(ADMIN, ADMIN_PASSWORD);
        String clientToken = loginToken(CLIENT, CLIENT_PASSWORD);

        ResponseEntity<String> adminResponse = get("/api/users", bearer(adminToken));
        ResponseEntity<String> clientResponse = get("/api/users", bearer(clientToken));

        assertThat(adminResponse.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(clientResponse.getStatusCode()).isEqualTo(HttpStatus.FORBIDDEN);
        assertThat(adminResponse.getBody()).contains(ADMIN, EMPLOYEE, CLIENT);
        assertThat(adminResponse.getBody()).doesNotContain(ADMIN_PASSWORD, CLIENT_PASSWORD);
    }

    @Test
    void invalidDishPayloadIsRejectedBeforePersistence() throws Exception {
        String employeeToken = loginToken(EMPLOYEE, EMPLOYEE_PASSWORD);
        Map<String, Object> payload = Map.of(
                "name", "Dish 123",
                "ingredientNames", List.of("Tomatoes"),
                "price", "-1.00"
        );

        ResponseEntity<String> response = post("/api/dishes", payload, bearer(employeeToken));

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
        JsonNode body = json(response);
        assertThat(body.path("name").asText()).contains("letters and spaces");
        assertThat(body.path("price").asText()).contains("greater than 0");
    }

    @Test
    void invalidAllergenFilterIsRejected() throws Exception {
        String token = loginToken(CLIENT, CLIENT_PASSWORD);

        ResponseEntity<String> response = get("/api/dishes?allergen=UNKNOWN_ALLERGEN", bearer(token));

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
        assertThat(json(response).path("error").asText()).contains("UNKNOWN_ALLERGEN");
    }

    @Test
    void allergenFilterReturnsOnlyDishesMatchingRequestedAllergen() throws Exception {
        String token = loginToken(CLIENT, CLIENT_PASSWORD);

        ResponseEntity<String> response = get("/api/dishes?allergen=FISH", bearer(token));

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        JsonNode dishes = json(response);
        assertThat(dishes).hasSize(2);
        assertThat(dishes.toString()).contains("Salmon Fillet", "Cod Fillet");
        assertThat(dishes.toString()).doesNotContain("Chicken Breast");
    }

    @Test
    void fileOperationsEnforceRolesAndRejectPathTraversal() throws Exception {
        String employeeToken = loginToken(EMPLOYEE, EMPLOYEE_PASSWORD);
        String clientToken = loginToken(CLIENT, CLIENT_PASSWORD);
        String adminToken = loginToken(ADMIN, ADMIN_PASSWORD);
        Map<String, Object> safeFile = Map.of("path", "opaque/menu.txt", "content", "Secure menu");
        Map<String, Object> traversal = Map.of("path", "../secret.txt", "content", "exfiltrate");

        ResponseEntity<String> writeByEmployee = post("/api/files", safeFile, bearer(employeeToken));
        ResponseEntity<String> readByClient = get("/api/files?path=opaque/menu.txt", bearer(clientToken));
        ResponseEntity<String> writeByClient = post("/api/files", safeFile, bearer(clientToken));
        ResponseEntity<String> traversalByEmployee = post("/api/files", traversal, bearer(employeeToken));
        ResponseEntity<String> deleteByAdmin = exchange(
                "/api/files?path=opaque/menu.txt",
                HttpMethod.DELETE,
                new HttpEntity<>(bearer(adminToken))
        );

        assertThat(writeByEmployee.getStatusCode()).isEqualTo(HttpStatus.CREATED);
        assertThat(readByClient.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(json(readByClient).path("content").asText()).isEqualTo("Secure menu");
        assertThat(writeByClient.getStatusCode()).isEqualTo(HttpStatus.FORBIDDEN);
        assertThat(traversalByEmployee.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
        assertThat(json(traversalByEmployee).path("path").asText()).contains("safe characters");
        assertThat(deleteByAdmin.getStatusCode()).isEqualTo(HttpStatus.NO_CONTENT);
    }

    @Test
    void clientCannotCreatePurchaseForAnotherUser() {
        String token = loginToken(CLIENT, CLIENT_PASSWORD);
        Map<String, Object> payload = purchasePayload(OTHER_CLIENT, "Grilled Chicken with Rice", "2027-12-01");

        ResponseEntity<String> response = post("/api/purchases", payload, bearer(token));

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.FORBIDDEN);
    }

    @Test
    void clientCanCreateOwnFuturePurchaseForDishAvailableInMenu() throws Exception {
        String token = loginToken(CLIENT, CLIENT_PASSWORD);
        Map<String, Object> payload = purchasePayload(CLIENT, "Grilled Chicken with Rice", "2027-12-01");

        ResponseEntity<String> created = post("/api/purchases", payload, bearer(token));
        ResponseEntity<String> visibleToClient = get("/api/purchases", bearer(token));

        assertThat(created.getStatusCode()).isEqualTo(HttpStatus.CREATED);
        assertThat(json(created).path("clientUsername").asText()).isEqualTo(CLIENT);
        assertThat(json(created).path("status").asText()).isEqualTo("PENDING");
        assertThat(visibleToClient.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(visibleToClient.getBody()).contains(CLIENT, "Grilled Chicken with Rice");
    }

    @Test
    void pastPurchaseDateIsRejected() throws Exception {
        String token = loginToken(CLIENT, CLIENT_PASSWORD);
        String yesterday = LocalDate.now().minusDays(1).toString();

        ResponseEntity<String> response = post(
                "/api/purchases",
                purchasePayload(CLIENT, "Grilled Chicken with Rice", yesterday),
                bearer(token)
        );

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
        assertThat(json(response).path("error").asText()).contains("future dates");
    }

    @Test
    void employeeCannotCreatePurchase() {
        String token = loginToken(EMPLOYEE, EMPLOYEE_PASSWORD);
        Map<String, Object> payload = purchasePayload(CLIENT, "Grilled Chicken with Rice", "2027-12-01");

        ResponseEntity<String> response = post("/api/purchases", payload, bearer(token));

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.FORBIDDEN);
    }

    @Test
    void clientCannotReadAnotherClientsPurchaseCollection() {
        String token = loginToken(CLIENT, CLIENT_PASSWORD);

        ResponseEntity<String> response = get("/api/purchases/client/opaque-other-client-id", bearer(token));

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.FORBIDDEN);
    }

    private Map<String, Object> purchasePayload(String username, String dishName, String date) {
        return Map.of("clientUsername", username, "dishName", dishName, "date", date);
    }

    private String loginToken(String username, String password) {
        try {
            ResponseEntity<String> response = loginResponse(username, password);
            assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
            return json(response).path("token").asText();
        } catch (Exception e) {
            throw new AssertionError("Could not log in as " + username, e);
        }
    }

    private ResponseEntity<String> loginResponse(String username, String password) {
        return post("/api/auth/login", Map.of("username", username, "password", password));
    }

    private ResponseEntity<String> get(String path) {
        return restTemplate.getForEntity(url(path), String.class);
    }

    private ResponseEntity<String> get(String path, HttpHeaders headers) {
        return exchange(path, HttpMethod.GET, new HttpEntity<>(headers));
    }

    private ResponseEntity<String> post(String path, Object body) {
        return post(path, body, jsonHeaders());
    }

    private ResponseEntity<String> post(String path, Object body, HttpHeaders headers) {
        return exchange(path, HttpMethod.POST, new HttpEntity<>(body, headers));
    }

    private ResponseEntity<String> exchange(String path, HttpMethod method, HttpEntity<?> entity) {
        return restTemplate.exchange(url(path), method, entity, String.class);
    }

    private HttpHeaders bearer(String token) {
        HttpHeaders headers = jsonHeaders();
        headers.setBearerAuth(token);
        return headers;
    }

    private HttpHeaders jsonHeaders() {
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        return headers;
    }

    private JsonNode json(ResponseEntity<String> response) throws Exception {
        return objectMapper.readTree(response.getBody());
    }

    private String url(String path) {
        return "http://localhost:" + port + path;
    }
}
