package io.github.rezakaramad.probe.echo;

import java.time.Instant;
import java.util.List;
import java.util.Map;

/**
 * API response DTOs for the echo endpoint. Records keep the JSON contract
 * explicit and typed rather than relying on ad-hoc maps.
 */
public final class EchoDtos {

    private EchoDtos() {
    }

    /**
     * Reflection of the incoming request, echoed back for debugging. Sensitive
     * headers are redacted by default (see {@link EchoProperties}); the body is
     * capped and {@code bodyTruncated} signals when it was cut.
     */
    public record EchoResponse(
            String method,
            String path,
            String query,
            Map<String, List<String>> headers,
            Map<String, List<String>> queryParams,
            String body,
            boolean bodyTruncated,
            String remoteAddr,
            String clientIp,
            String scheme,
            boolean secure,
            Instant timestamp) {
    }
}
