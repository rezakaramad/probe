package io.github.rezakaramad.probe.valkey;

// The validation annotations used to reject bad inputs before they reach Valkey.
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import org.springframework.validation.annotation.Validated;

// The Spring annotations that map HTTP verbs and paths to methods
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

// Used to timestamp each response
import java.time.Instant;

/**
 * REST API backing the demo UI. Each endpoint performs a real round-trip
 * against the Valkey instance so the page proves TLS + IAM connectivity.
 */

// The @Validated annotation on the class enables validation of method parameters
// annotated with @Valid, @NotBlank, etc. Spring will return a 400 Bad Request
// response if the request fails validation, rather than passing bad data to Valkey.
// The @RestController annotation marks this class as a Spring MVC controller where
// every method's return value is automatically serialized to JSON and sent as the
// HTTP response body. 
// The @RequestMapping annotation sets the base path for all methods 
// in this controller to "/api/valkey" which means that all endpoints will be prefixed with this path.
@Validated
@RestController
@RequestMapping("/api/valkey")
public class ValkeyController {

    // final --> these fields are immutable after construction, ensuring thread safety and consistency
    // private --> only this can access these fields
    private final ValkeyService valkey;
    private final ValkeyProperties props;

    // Constructor
    public ValkeyController(ValkeyService valkey, ValkeyProperties props) {
        this.valkey = valkey;
        this.props = props;
    }

    /** Connection metadata for the info panel. 
     * GET /api/valkey/info - reads connection config from ValkeyProperties and returns it as JSON.
     * No Valkey round-trip.
     * Used by the UI to populate the connection panel. No secrets - just host, port, 
     * and which security features are on.
    */
    @GetMapping("/info")
    public ValkeyDtos.ConnectionInfo info() {
        return new ValkeyDtos.ConnectionInfo(
                props.host(), props.port(), props.sslEnabled(), props.iamAuthEnabled(),
                props.sslInsecure());
    }

    /** PING the instance.
     * Records the time, sends PING to Valkey, records how long it took.
     * Returns whether it succeeded (ok = true if reply was PONG), the raw reply,
     * and the latency in milliseconds.
    */
    @GetMapping("/ping")
    public ValkeyDtos.PingResult ping() {
        long start = System.nanoTime();
        String reply = valkey.ping();
        return new ValkeyDtos.PingResult(
                "PONG".equalsIgnoreCase(reply), reply, elapsedMs(start), Instant.now());
    }

    /** SET a key with a TTL.
     *  Receives a JSON body, deserialises it into a SetRequest.
     * @Valid triggers constraint checks on that object (key must not be blank, TTL must be 1–86400)
     * before the method body even runs.
     * @RequestBody tells Spring to read the request body and convert it from JSON into a SetRequest object.
    */
    @PostMapping("/set")
    public ValkeyDtos.SetResult set(@Valid @RequestBody ValkeyDtos.SetRequest req) {
        long start = System.nanoTime();
        valkey.set(req.key(), req.value(), req.ttlSeconds());
        return new ValkeyDtos.SetResult(
                true, req.key(), req.ttlSeconds(), elapsedMs(start), Instant.now());
    }

    /** GET a key.
     * @RequestParam tells Spring to read the "key" query parameter from the URL and pass it to this method.
     */
    @GetMapping("/get")
    public ValkeyDtos.GetResult get(@RequestParam @NotBlank String key) {
        long start = System.nanoTime();
        String value = valkey.get(key);
        return new ValkeyDtos.GetResult(
                value != null, key, value, elapsedMs(start), Instant.now());
    }

    // Helper method to calculate elapsed time in milliseconds, rounded to two decimal places.
    private static double elapsedMs(long startNanos) {
        return Math.round((System.nanoTime() - startNanos) / 10_000.0) / 100.0;
    }
}
