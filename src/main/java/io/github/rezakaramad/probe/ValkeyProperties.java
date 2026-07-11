package io.github.rezakaramad.probe;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.validation.annotation.Validated;

/**
 * Reads application.yml / env vars (VALKEY_HOST, VALKEY_PORT, etc.) into a typed Java object.
 *
 * @param host           Valkey host (injected as VALKEY_HOST on the Platform).
 * @param port           Valkey port (injected as VALKEY_PORT on the Platform).
 * @param sslEnabled     whether to use TLS (true for SERVER_AUTHENTICATION instances).
 * @param iamAuthEnabled whether to authenticate with a rotating GCP IAM token.
 * @param timeoutSeconds command/connect timeout in seconds.
 */

@Validated
@ConfigurationProperties(prefix = "valkey")
public record ValkeyProperties(

        @NotBlank
        String host,

        @Min(1) @Max(65535)
        int port,

        boolean sslEnabled,

        boolean iamAuthEnabled,

        @Min(1) @Max(120)
        int timeoutSeconds) {
}
