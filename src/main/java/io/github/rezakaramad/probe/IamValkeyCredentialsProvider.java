package io.github.rezakaramad.probe;

import com.google.auth.oauth2.AccessToken;
import com.google.auth.oauth2.GoogleCredentials;
import io.lettuce.core.RedisCredentials;
import io.lettuce.core.RedisCredentialsProvider;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import reactor.core.publisher.Mono;

import java.io.IOException;
import java.util.List;

/**
 * Supplies GCP IAM access tokens as Valkey credentials.
 *
 * <p>Memorystore instances configured with {@code IAM_AUTH} expect a short-lived
 * OAuth2 access token as the connection password (no username). Lettuce invokes
 * {@link #resolveCredentials()} on every (re)connect, so returning a freshly
 * refreshed token here keeps connections valid past the ~1h token lifetime.
 *
 * <p>{@link GoogleCredentials#getApplicationDefault()} resolves the pod's
 * Workload Identity via the GKE metadata server when running in-cluster, or a
 * local gcloud/ADC credential when running on a developer machine.
 *
 * <p>Note: this implements Lettuce's {@link RedisCredentialsProvider}. Lettuce
 * is a Redis client and Valkey is a protocol-compatible Redis fork, so the
 * library interface keeps the {@code Redis} name while this class is named for
 * Valkey.
 */
public class IamValkeyCredentialsProvider implements RedisCredentialsProvider {

    private static final Logger log = LoggerFactory.getLogger(IamValkeyCredentialsProvider.class);
    private static final List<String> SCOPES =
            List.of("https://www.googleapis.com/auth/cloud-platform");

    private final GoogleCredentials credentials;

    public IamValkeyCredentialsProvider() throws IOException {
        this.credentials = GoogleCredentials.getApplicationDefault().createScoped(SCOPES);
    }

    @Override
    public Mono<RedisCredentials> resolveCredentials() {
        return Mono.fromCallable(() -> {
            credentials.refreshIfExpired();
            AccessToken token = credentials.getAccessToken();
            if (token == null || token.getTokenValue() == null) {
                throw new IllegalStateException("No GCP access token available from ADC");
            }
            log.debug("Resolved IAM access token, expires at {}", token.getExpirationTime());
            // IAM_AUTH uses the access token as the password; username is null.
            return RedisCredentials.just(null, token.getTokenValue());
        });
    }
}
