package io.github.rezakaramad.probe;

import java.time.Instant;

/**
 * API response DTOs for the Valkey operations. Records keep the JSON contract
 * explicit and typed rather than relying on ad-hoc maps.
 */
public final class ValkeyDtos {

    private ValkeyDtos() {
    }

    /** Connection metadata surfaced to the UI.
     * Tells the UI what the connection is and what features are enabled.
     * ConnectionInfo
     * @param host
     * @param port
     * @param sslEnabled
     * @param iamAuthEnabled
    */
    public record ConnectionInfo(String host, int port, boolean sslEnabled, boolean iamAuthEnabled) {
    }

    /** Result of a PING.
     * The result of pinging the Valkey server, did it succeed, what was the reply,
     * how fast was it, and when.
    */
    public record PingResult(boolean ok, String reply, double latencyMs, Instant timestamp) {
    }

    /** Request body for SET.
     * The request body for the SET operation, containing the key, value, and TTL.
    */
    public record SetRequest(
            @jakarta.validation.constraints.NotBlank String key,
            @jakarta.validation.constraints.NotNull String value,
            @jakarta.validation.constraints.Min(1) @jakarta.validation.constraints.Max(86_400) long ttlSeconds) {
    }

    /** Result of a SET.
     * The result of the SET operation, indicating success, the key set, the TTL, latency, and timestamp.
    */
    public record SetResult(boolean ok, String key, long ttlSeconds, double latencyMs, Instant timestamp) {
    }

    /** Result of a GET.
     * The result of fetching a key — was it found, what's the value, how fast.
    */
    public record GetResult(boolean found, String key, String value, double latencyMs, Instant timestamp) {
    }
}
