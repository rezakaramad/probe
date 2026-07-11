package io.github.rezakaramad.probe.echo;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.web.servlet.MockMvc;

import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.not;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Web-layer test for the echo endpoint. Uses {@link WebMvcTest} so only the
 * controller (and the global exception advice) load — no Valkey beans, no
 * Testcontainers — keeping it fast and dependency-free.
 */
@WebMvcTest(EchoController.class)
@EnableConfigurationProperties(EchoProperties.class)
@TestPropertySource(properties = {
        "echo.max-body-bytes=10",
        "echo.sensitive-headers=authorization,cookie",
        "echo.reveal-sensitive-allowed=true",
        "echo.configurable-response-enabled=true",
        "echo.max-delay-ms=50"
})
class EchoIntegrationTest {

    @Autowired
    MockMvc mvc;

    @Test
    void echoReturnsMethodAndPath() throws Exception {
        mvc.perform(get("/api/echo/hello"))
                .andExpect(status().isOk())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.method", is("GET")))
                .andExpect(jsonPath("$.path", is("/api/echo/hello")));
    }

    @Test
    void echoReflectsCustomHeader() throws Exception {
        mvc.perform(get("/api/echo").header("X-Debug", "probe"))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString("X-Debug")))
                .andExpect(content().string(containsString("probe")));
    }

    @Test
    void echoRedactsAuthorizationHeader() throws Exception {
        mvc.perform(get("/api/echo").header("Authorization", "Bearer super-secret-token"))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString("***redacted")))
                .andExpect(content().string(not(containsString("super-secret-token"))));
    }

    @Test
    void echoRevealsSensitiveWhenAllowedAndRequested() throws Exception {
        mvc.perform(get("/api/echo?revealSensitive=true")
                        .header("Authorization", "Bearer super-secret-token"))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString("super-secret-token")));
    }

    @Test
    void echoReflectsQueryParams() throws Exception {
        mvc.perform(get("/api/echo?a=1&b=two"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.queryParams.a[0]", is("1")))
                .andExpect(jsonPath("$.queryParams.b[0]", is("two")));
    }

    @Test
    void echoReflectsBodyForPost() throws Exception {
        mvc.perform(post("/api/echo").contentType(MediaType.TEXT_PLAIN).content("hi"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.method", is("POST")))
                .andExpect(jsonPath("$.body", is("hi")))
                .andExpect(jsonPath("$.bodyTruncated", is(false)));
    }

    @Test
    void echoTruncatesOversizeBody() throws Exception {
        // maxBodyBytes=10; send 20 chars -> truncated to 10.
        mvc.perform(post("/api/echo").contentType(MediaType.TEXT_PLAIN).content("0123456789ABCDEFGHIJ"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.body", is("0123456789")))
                .andExpect(jsonPath("$.bodyTruncated", is(true)));
    }

    @Test
    void echoOverridesStatusCode() throws Exception {
        mvc.perform(get("/api/echo").header("x-set-response-status-code", "503"))
                .andExpect(status().is(503))
                .andExpect(jsonPath("$.method", is("GET")));
    }

    @Test
    void echoClampsDelayToMax() throws Exception {
        // Requested delay far exceeds max-delay-ms=50; must be clamped and still return 200.
        mvc.perform(get("/api/echo").header("x-set-response-delay-ms", "100000"))
                .andExpect(status().isOk());
    }

    @Test
    void echoIgnoresInvalidStatusOverride() throws Exception {
        mvc.perform(get("/api/echo").header("x-set-response-status-code", "9999"))
                .andExpect(status().isOk());
    }
}
