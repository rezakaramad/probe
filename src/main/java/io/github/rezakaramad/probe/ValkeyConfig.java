package io.github.rezakaramad.probe;

import io.lettuce.core.ClientOptions;
import io.lettuce.core.RedisClient;
import io.lettuce.core.RedisURI;
import io.lettuce.core.SocketOptions;
import io.lettuce.core.SslVerifyMode;
import io.lettuce.core.TimeoutOptions;
import io.lettuce.core.api.StatefulRedisConnection;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.io.IOException;
import java.time.Duration;

/**
 * This class tells Spring how to create and manage:
 * - A Lettuce RedisClient for connecting to Valkey.
 * - A StatefulRedisConnection for executing commands on Valkey.
 */

@Configuration
public class ValkeyConfig {

    // Create a logger for this class so we can log messages to the console and/or log files.
    private static final Logger log = LoggerFactory.getLogger(ValkeyConfig.class);

    // Inject the ValkeyProperties object that Spring created from application.yml and environment variables.
    private final ValkeyProperties props;

    // Constructor for ValkeyConfig that takes a ValkeyProperties object as a parameter.
    // Spring will automatically call this constructor and provide the ValkeyProperties 
    // object when it creates an instance of ValkeyConfig. This allows us to access the 
    // configuration properties (host, port, sslEnabled, iamAuthEnabled, timeoutSeconds)
    // in this class.
    public ValkeyConfig(ValkeyProperties props) {
        this.props = props;
    }

    // This is a bean method which is called when the application starts up.
    // It creates a RedisClient configured to connect to Valkey using the properties defined in ValkeyProperties.
    @Bean(destroyMethod = "shutdown")
    public RedisClient valkeyClient() throws IOException {
        // How long to wait for a connection or command to complete before timing out.
        Duration timeout = Duration.ofSeconds(props.timeoutSeconds());

        // Build the RedisURI with the host, port, and timeout from the properties.
        RedisURI.Builder uri = RedisURI.builder()
                .withHost(props.host())
                .withPort(props.port())
                .withTimeout(timeout);

        // If SSL is enabled, configure the URI to use SSL and verify the peer's certificate.
        if (props.sslEnabled()) {
            uri.withSsl(true).withVerifyPeer(SslVerifyMode.FULL);
        }

        // If IAM authentication is enabled, configure the URI to use the IamValkeyCredentialsProvider
        // which will fetch a short-lived OAuth2 token from GCP for authentication.
        if (props.iamAuthEnabled()) {
            uri.withAuthentication(new IamValkeyCredentialsProvider());
        }

        log.info("Connecting to Valkey {}:{} (ssl={}, iamAuth={})",
                props.host(), props.port(), props.sslEnabled(), props.iamAuthEnabled());

        // Create the RedisClient with the configured URI and set options
        // for reconnecting, timeouts, and socket behavior.
        RedisClient client = RedisClient.create(uri.build());
        client.setOptions(ClientOptions.builder()
                // Reconnect (and re-auth with a fresh IAM token) after drops.
                .autoReconnect(true)
                // Fail fast instead of queueing commands while disconnected.
                .disconnectedBehavior(ClientOptions.DisconnectedBehavior.REJECT_COMMANDS)
                // Validate the connection is usable before handing it out.
                .pingBeforeActivateConnection(true)
                .socketOptions(SocketOptions.builder()
                        .connectTimeout(timeout)
                        .keepAlive(true)
                        .build())
                .timeoutOptions(TimeoutOptions.enabled(timeout))
                .build());
        return client;
    }

    // Opens a real TCP connection to Valkey using the RedisClient.
    // This connection is used to execute commands on Valkey.
    // The connection is closed when the application shuts down.
    @Bean(destroyMethod = "close")
    public StatefulRedisConnection<String, String> valkeyConnection(RedisClient client) {
        return client.connect();
    }
}
