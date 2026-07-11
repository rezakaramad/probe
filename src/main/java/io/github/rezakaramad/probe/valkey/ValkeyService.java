package io.github.rezakaramad.probe.valkey;

import io.lettuce.core.api.StatefulRedisConnection;
import io.lettuce.core.api.sync.RedisCommands;
import org.springframework.stereotype.Service;

/**
 * Sends commands to Valkey and returns the results.
 */

@Service
public class ValkeyService {

    private final RedisCommands<String, String> commands;

    public ValkeyService(StatefulRedisConnection<String, String> connection) {
        // Synchronous -> send command, wait for answer, then continue
        this.commands = connection.sync();
    }

    /** Returns the server's reply to PING, typically {@code PONG}. */
    public String ping() {
        return commands.ping();
    }

    /** Stores a value with a TTL (seconds) and returns the status reply. */
    public String set(String key, String value, long ttlSeconds) {
        return commands.setex(key, ttlSeconds, value);
    }

    /** Reads a value, or {@code null} if the key is absent/expired. */
    public String get(String key) {
        return commands.get(key);
    }
}
