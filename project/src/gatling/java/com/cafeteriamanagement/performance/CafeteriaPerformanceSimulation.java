package com.cafeteriamanagement.performance;

import io.gatling.javaapi.core.*;
import io.gatling.javaapi.http.*;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.concurrent.atomic.AtomicReference;

import static io.gatling.javaapi.core.CoreDsl.*;
import static io.gatling.javaapi.http.HttpDsl.*;

/**
 * Performance test verifying NFR06 (P95 response time ≤ 500 ms) and
 * NFR07 (100 concurrent requests without degradation).
 *
 * Run with: mvn -f project/pom.xml gatling:test -Pperformance -DbaseUrl=http://localhost:8081
 *
 * Requires a user "perf_admin" / "PerfAdmin@1234" pre-seeded in the database.
 */
public class CafeteriaPerformanceSimulation extends Simulation {

    private static final String BASE_URL =
            System.getProperty("baseUrl", "http://localhost:8081");

    private static final int CONCURRENT_USERS =
            Integer.getInteger("perf.users", 100);

    private static final int P95_MAX_MS =
            Integer.getInteger("perf.p95MaxMs", 500);

    // Shared JWT obtained once in before() so the login endpoint is not
    // hammered by every virtual user (avoids rate-limiter interference).
    private final AtomicReference<String> sharedJwt = new AtomicReference<>("");

    private final HttpProtocolBuilder httpProtocol = http
            .baseUrl(BASE_URL)
            .acceptHeader("application/json")
            .contentTypeHeader("application/json");

    private final ScenarioBuilder menuBrowse =
            scenario("Concurrent menu browse – NFR06 + NFR07")
                    .exec(http("GET /api/dishes")
                            .get("/api/dishes")
                            .header("Authorization", session -> "Bearer " + sharedJwt.get())
                            .check(status().is(200)))
                    .exec(http("GET /api/menus")
                            .get("/api/menus")
                            .header("Authorization", session -> "Bearer " + sharedJwt.get())
                            .check(status().is(200)))
                    .exec(http("GET /api/ingredients")
                            .get("/api/ingredients")
                            .header("Authorization", session -> "Bearer " + sharedJwt.get())
                            .check(status().is(200)));

    // before() is a template method in Gatling's Java Simulation — override it,
    // do NOT call it with a lambda (that is the Scala DSL, not the Java API).
    @Override
    public void before() {
        System.out.println("[Perf] Logging in as perf_admin at " + BASE_URL + " ...");
        try {
            HttpClient client = HttpClient.newHttpClient();
            HttpRequest req = HttpRequest.newBuilder()
                    .uri(URI.create(BASE_URL + "/api/auth/login"))
                    .header("Content-Type", "application/json")
                    .POST(HttpRequest.BodyPublishers.ofString(
                            "{\"username\":\"perf_admin\",\"password\":\"PerfAdmin@1234\"}"))
                    .build();
            HttpResponse<String> resp =
                    client.send(req, HttpResponse.BodyHandlers.ofString());
            if (resp.statusCode() != 200) {
                throw new IllegalStateException(
                        "Login returned HTTP " + resp.statusCode() + ": " + resp.body());
            }
            String body = resp.body();
            int start = body.indexOf("\"token\":\"") + 9;
            int end = body.indexOf('"', start);
            sharedJwt.set(body.substring(start, end));
            System.out.println("[Perf] JWT acquired – injecting "
                    + CONCURRENT_USERS + " users at once (P95 gate: " + P95_MAX_MS + " ms).");
        } catch (Exception e) {
            throw new RuntimeException(
                    "Performance simulation setup failed: " + e.getMessage(), e);
        }
    }

    {
        setUp(
                // NFR06 + NFR07: ramp 100 users over 20 seconds (≈ 5 users/s).
                // Gradual ramp simulates realistic "normal load" growth rather than
                // an instantaneous spike, so the P95 gate reflects sustainable
                // response times rather than queuing artefacts.
                menuBrowse.injectOpen(
                        rampUsers(CONCURRENT_USERS).during(Duration.ofSeconds(20))
                )
        )
        .protocols(httpProtocol)
        .assertions(
                // NFR06: 95th-percentile response time ≤ 500 ms under normal load
                global().responseTime().percentile(95).lte(P95_MAX_MS),
                // NFR07: ≥ 95 % of all requests succeed across 100 users
                global().successfulRequests().percent().gte(95.0)
        );
    }
}
