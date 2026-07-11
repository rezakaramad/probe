package io.github.rezakaramad.probe.valkey;

import io.lettuce.core.RedisConnectionException;
import io.lettuce.core.api.StatefulRedisConnection;
import org.springframework.beans.factory.BeanCreationException;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Service;

/**
 * Sends commands to Valkey and returns the results.
 *
 * <p>The connection bean is {@link Lazy} so the app starts successfully even when Valkey is
 * unreachable. The first command triggers bean initialisation; if the connection fails at that
 * point Spring wraps the Lettuce exception in {@link BeanCreationException}. {@link #exec}
 * unwraps it back to a {@link RedisConnectionException} so the existing
 * {@code GlobalExceptionHandler} can return a clean 503 — keeping Spring plumbing out of the
 * HTTP error-handling layer.
 */
@Service
public class ValkeyService {

    private final StatefulRedisConnection<String, String> connection;

    public ValkeyService(@Lazy StatefulRedisConnection<String, String> connection) {
        this.connection = connection;
    }

    /** Returns the server's reply to PING, typically {@code PONG}. */
    public String ping() {
        return exec(() -> connection.sync().ping());
    }

    /** Stores a value with a TTL (seconds) and returns the status reply. */
    public String set(String key, String value, long ttlSeconds) {
        return exec(() -> connection.sync().setex(key, ttlSeconds, value));
    }

    /** Reads a value, or {@code null} if the key is absent/expired. */
    public String get(String key) {
        return exec(() -> connection.sync().get(key));
    }

    /**
     * Runs a Valkey command and converts any {@link BeanCreationException} (thrown when the
     * {@code @Lazy} connection proxy fails to initialise) into a {@link RedisConnectionException}
     * so callers see a consistent Lettuce exception type regardless of whether the failure
     * happened on the first use or a later reconnect attempt.
     */
    private <T> T exec(java.util.concurrent.Callable<T> command) {
        try {
            return command.call();
        } catch (BeanCreationException e) {
            Throwable cause = e;
            while (cause != null) {
                if (cause instanceof RedisConnectionException rce) throw rce;
                cause = cause.getCause();
            }
            throw new RedisConnectionException("Valkey connection unavailable", e);
        } catch (RuntimeException e) {
            throw e;
        } catch (Exception e) {
            throw new RedisConnectionException("Valkey command failed", e);
        }
    }
}
