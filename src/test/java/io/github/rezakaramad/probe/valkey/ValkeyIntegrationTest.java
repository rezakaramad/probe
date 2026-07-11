package io.github.rezakaramad.probe.valkey;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.web.servlet.MockMvc;
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.utility.DockerImageName;

import static org.hamcrest.Matchers.is;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Full-stack integration test against a real Valkey container (no TLS/IAM,
 * which are GCP-only). Verifies the config, service, controller, validation
 * and health wiring end to end.
 */
@Testcontainers
@AutoConfigureMockMvc
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.MOCK)
class ValkeyIntegrationTest {

    @Container
    static final GenericContainer<?> valkey =
            new GenericContainer<>(DockerImageName.parse("valkey/valkey:8"))
                    .withExposedPorts(6379);

    @DynamicPropertySource
    static void valkeyProperties(DynamicPropertyRegistry registry) {
        registry.add("valkey.host", valkey::getHost);
        registry.add("valkey.port", () -> valkey.getMappedPort(6379));
        registry.add("valkey.ssl-enabled", () -> false);
        registry.add("valkey.iam-auth-enabled", () -> false);
    }

    @Autowired
    MockMvc mvc;

    @Test
    void pingReturnsPong() throws Exception {
        mvc.perform(get("/api/valkey/ping"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.ok", is(true)))
                .andExpect(jsonPath("$.reply", is("PONG")));
    }

    @Test
    void setThenGetRoundTrips() throws Exception {
        mvc.perform(post("/api/valkey/set")
                        .contentType("application/json")
                        .content("{\"key\":\"it:greeting\",\"value\":\"hello\",\"ttlSeconds\":60}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.ok", is(true)))
                .andExpect(jsonPath("$.key", is("it:greeting")));

        mvc.perform(get("/api/valkey/get").param("key", "it:greeting"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.found", is(true)))
                .andExpect(jsonPath("$.value", is("hello")));
    }

    @Test
    void getMissingKeyReportsNotFound() throws Exception {
        mvc.perform(get("/api/valkey/get").param("key", "it:absent"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.found", is(false)));
    }

    @Test
    void invalidSetBodyReturnsBadRequest() throws Exception {
        mvc.perform(post("/api/valkey/set")
                        .contentType("application/json")
                        .content("{\"key\":\"\",\"value\":\"x\",\"ttlSeconds\":0}"))
                .andExpect(status().isBadRequest());
    }

    @Test
    void healthReadinessIsUp() throws Exception {
        mvc.perform(get("/actuator/health/readiness"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status", is("UP")));
    }
}
