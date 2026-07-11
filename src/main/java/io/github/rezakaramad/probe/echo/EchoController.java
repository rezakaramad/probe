package io.github.rezakaramad.probe.echo;

import jakarta.servlet.http.HttpServletRequest;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RestController;

import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

/**
 * Reflects the incoming HTTP request back to the caller as JSON so clients,
 * proxies and ingress rewrites can be debugged ("what did the server actually
 * receive?").
 *
 * <p>Responses are always JSON to avoid reflected-XSS: request input is never
 * echoed as {@code text/html}. Sensitive headers are redacted by default.
 *
 * <p>When {@code echo.configurable-response-enabled} is set, callers may also
 * shape the response (status code, delay, content-type) to exercise client
 * retry/timeout behaviour. The delay is hard-capped to prevent abuse.
 */
@RestController
@RequestMapping("/api/echo")
public class EchoController {

    private static final Logger log = LoggerFactory.getLogger(EchoController.class);

    // Control inputs for the configurable-response feature.
    private static final String HDR_STATUS = "x-set-response-status-code";
    private static final String HDR_DELAY = "x-set-response-delay-ms";
    private static final String HDR_CONTENT_TYPE = "x-set-response-content-type";
    private static final String PARAM_STATUS = "status";
    private static final String PARAM_DELAY = "delay";
    private static final String PARAM_REVEAL = "revealSensitive";

    private final EchoProperties props;

    public EchoController(EchoProperties props) {
        this.props = props;
    }

    @RequestMapping(value = {"", "/**"}, method = {
            RequestMethod.GET, RequestMethod.POST, RequestMethod.PUT,
            RequestMethod.DELETE, RequestMethod.PATCH
    })
    public ResponseEntity<EchoDtos.EchoResponse> echo(
            HttpServletRequest request,
            @RequestBody(required = false) byte[] body) {
        boolean reveal = props.revealSensitiveAllowed()
                && "true".equalsIgnoreCase(request.getParameter(PARAM_REVEAL));

        EchoDtos.EchoResponse payload = buildResponse(request, body, reveal);

        ResponseEntity.BodyBuilder builder = ResponseEntity.status(HttpStatus.OK);
        MediaType contentType = MediaType.APPLICATION_JSON;

        if (props.configurableResponseEnabled()) {
            HttpStatus status = resolveStatus(request);
            if (status != null) {
                builder = ResponseEntity.status(status);
            }
            contentType = resolveContentType(request);
            applyDelay(request);
        }

        return builder.contentType(contentType).body(payload);
    }

    private EchoDtos.EchoResponse buildResponse(HttpServletRequest request, byte[] body, boolean reveal) {
        Map<String, List<String>> headers = new LinkedHashMap<>();
        var names = request.getHeaderNames();
        while (names.hasMoreElements()) {
            String name = names.nextElement();
            List<String> values = Collections.list(request.getHeaders(name));
            if (!reveal && props.isSensitive(name)) {
                headers.put(name, values.stream().map(EchoController::redact).toList());
            } else {
                headers.put(name, values);
            }
        }

        Map<String, List<String>> queryParams = new LinkedHashMap<>();
        request.getParameterMap().forEach((k, v) -> queryParams.put(k, List.of(v)));

        String bodyText = null;
        boolean truncated = false;
        if (body != null && body.length > 0) {
            int limit = Math.min(body.length, props.maxBodyBytes());
            truncated = body.length > props.maxBodyBytes();
            bodyText = new String(body, 0, limit, StandardCharsets.UTF_8);
        }

        return new EchoDtos.EchoResponse(
                request.getMethod(),
                request.getRequestURI(),
                request.getQueryString(),
                headers,
                queryParams,
                bodyText,
                truncated,
                request.getRemoteAddr(),
                clientIp(request),
                request.getScheme(),
                request.isSecure(),
                Instant.now());
    }

    /** Replaces a secret value with a length-preserving marker for safe debugging. */
    private static String redact(String value) {
        return "***redacted (len=" + (value == null ? 0 : value.length()) + ")***";
    }

    /** First hop of X-Forwarded-For if present (the original client), else the socket address. */
    private static String clientIp(HttpServletRequest request) {
        String xff = request.getHeader("x-forwarded-for");
        if (xff != null && !xff.isBlank()) {
            return xff.split(",")[0].trim();
        }
        return request.getRemoteAddr();
    }

    /** Reads a status override from header or query param; ignored if absent or out of range. */
    private HttpStatus resolveStatus(HttpServletRequest request) {
        String raw = firstNonBlank(request.getHeader(HDR_STATUS), request.getParameter(PARAM_STATUS));
        if (raw == null) {
            return null;
        }
        try {
            int code = Integer.parseInt(raw.trim());
            if (code >= 100 && code <= 599) {
                HttpStatus resolved = HttpStatus.resolve(code);
                if (resolved != null) {
                    return resolved;
                }
            }
        } catch (NumberFormatException ignored) {
            // fall through -> no override
        }
        log.debug("Ignoring invalid response-status override: {}", raw);
        return null;
    }

    /**
     * Reads a content-type override. Defaults to JSON and refuses HTML so reflected
     * request input can never be served as an executable document (XSS guard).
     */
    private MediaType resolveContentType(HttpServletRequest request) {
        String raw = request.getHeader(HDR_CONTENT_TYPE);
        if (raw == null || raw.isBlank()) {
            return MediaType.APPLICATION_JSON;
        }
        try {
            MediaType requested = MediaType.parseMediaType(raw.trim());
            String type = requested.toString().toLowerCase(Locale.ROOT);
            if (type.contains("html") || type.contains("xhtml")) {
                log.debug("Refusing HTML content-type override: {}", raw);
                return MediaType.APPLICATION_JSON;
            }
            return requested;
        } catch (org.springframework.http.InvalidMediaTypeException e) {
            return MediaType.APPLICATION_JSON;
        }
    }

    /** Sleeps up to the configured cap to simulate a slow dependency, never unbounded. */
    private void applyDelay(HttpServletRequest request) {
        String raw = firstNonBlank(request.getHeader(HDR_DELAY), request.getParameter(PARAM_DELAY));
        if (raw == null) {
            return;
        }
        try {
            long requested = Long.parseLong(raw.trim());
            long delay = Math.max(0, Math.min(requested, props.maxDelayMs()));
            if (delay > 0) {
                Thread.sleep(delay);
            }
        } catch (NumberFormatException ignored) {
            // no delay on bad input
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }

    private static String firstNonBlank(String a, String b) {
        if (a != null && !a.isBlank()) {
            return a;
        }
        if (b != null && !b.isBlank()) {
            return b;
        }
        return null;
    }
}
