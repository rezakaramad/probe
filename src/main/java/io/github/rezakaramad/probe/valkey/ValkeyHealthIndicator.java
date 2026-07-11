package io.github.rezakaramad.probe.valkey;

import io.lettuce.core.api.StatefulRedisConnection;
import org.springframework.boot.actuate.health.Health;
import org.springframework.boot.actuate.health.HealthIndicator;
import org.springframework.stereotype.Component;

/**
 * Reports Valkey connectivity to Spring Boot Actuator.
 */
@Component("valkey")
public class ValkeyHealthIndicator implements HealthIndicator {

    // Lettuce connection to Valkey Redis server
    private final StatefulRedisConnection<String, String> connection;

    // Constructor injection of the Lettuce connection
    public ValkeyHealthIndicator(StatefulRedisConnection<String, String> connection) {
        this.connection = connection;
    }

    // Spring calls this every time /actuator/health is hit. Everything inside runs fresh each time.
    // @Override -> this is implementing the HealthIndicator interface's health() method
    @Override
    public Health health() {
        try {
            // Send a ping command to Valkey Redis server and check the response
            String reply = connection.sync().ping();
            if ("PONG".equalsIgnoreCase(reply)) {
                return Health.up().withDetail("ping", reply).build();
            }
            return Health.down().withDetail("ping", reply).build();
        } catch (Exception e) {
            return Health.down(e).build();
        }
    }
}
