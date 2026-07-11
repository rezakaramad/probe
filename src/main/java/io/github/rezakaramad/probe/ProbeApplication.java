package io.github.rezakaramad.probe;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.autoconfigure.data.redis.RedisAutoConfiguration;
import org.springframework.boot.context.properties.EnableConfigurationProperties;

/**
 * Probe is a debugging tool for testing connectivity to cloud services and
 * other network endpoints. It verifies that an application can reach and
 * interact with services such as PostgreSQL and Valkey, and can probe IP
 * addresses, DNS names, and arbitrary endpoints across on-premises and GCP
 * environments.
 */

// RedisAutoConfiguration only supports static credentials via spring.data.redis.password.
// GCP Memorystore with IAM_AUTH requires a short-lived OAuth2 token that must be
// fetched and refreshed on every reconnect, which needs a custom RedisCredentialsProvider
// attached at client-build time. Excluding auto-config lets ValkeyConfig own the client.
@SpringBootApplication(exclude = RedisAutoConfiguration.class)

// Read application.yml properties and env vars with prefix "valkey" into a ValkeyProperties object.
@EnableConfigurationProperties(ValkeyProperties.class)

public class ProbeApplication {

    public static void main(String[] args) {
        SpringApplication.run(ProbeApplication.class, args);
    }
}
