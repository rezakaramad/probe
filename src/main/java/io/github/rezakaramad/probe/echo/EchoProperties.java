package io.github.rezakaramad.probe.echo;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.validation.annotation.Validated;

import java.util.List;

/**
 * Reads application.yml / env vars (ECHO_*) into a typed Java object controlling
 * the echo endpoint's behaviour.
 *
 * @param maxBodyBytes                 max request-body bytes echoed back; larger bodies are
 *                                     truncated and flagged.
 * @param sensitiveHeaders             header names whose values are redacted by default.
 * @param revealSensitiveAllowed       whether {@code ?revealSensitive=true} may un-redact
 *                                     sensitive headers (set false in sensitive environments).
 * @param configurableResponseEnabled  whether callers may override status/delay/content-type.
 * @param maxDelayMs                   hard cap on the artificial response delay (DoS guard).
 */

@Validated
@ConfigurationProperties(prefix = "echo")
public record EchoProperties(

        @Min(1) @Max(10_485_760)
        int maxBodyBytes,

        List<String> sensitiveHeaders,

        boolean revealSensitiveAllowed,

        boolean configurableResponseEnabled,

        @Min(0) @Max(60_000)
        long maxDelayMs) {

    /** Default sensitive headers when none are configured. */
    private static final List<String> DEFAULT_SENSITIVE_HEADERS =
            List.of("authorization", "cookie", "set-cookie", "proxy-authorization");

    public EchoProperties {
        if (sensitiveHeaders == null || sensitiveHeaders.isEmpty()) {
            sensitiveHeaders = DEFAULT_SENSITIVE_HEADERS;
        } else {
            sensitiveHeaders = sensitiveHeaders.stream()
                    .map(h -> h.toLowerCase(java.util.Locale.ROOT))
                    .toList();
        }
    }

    /** Case-insensitive check for whether a header should be redacted. */
    public boolean isSensitive(String headerName) {
        return sensitiveHeaders.contains(headerName.toLowerCase(java.util.Locale.ROOT));
    }
}
