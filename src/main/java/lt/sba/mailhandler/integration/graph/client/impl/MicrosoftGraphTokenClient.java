package lt.sba.mailhandler.integration.graph.client.impl;

import lombok.RequiredArgsConstructor;
import lt.sba.mailhandler.config.GraphProperties;
import lt.sba.mailhandler.integration.graph.client.GraphTokenClient;
import lt.sba.mailhandler.integration.graph.dto.GraphTokenResponse;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import org.springframework.web.reactive.function.BodyInserters;
import org.springframework.web.reactive.function.client.WebClient;

import java.time.Instant;

@Component
@RequiredArgsConstructor
public class MicrosoftGraphTokenClient implements GraphTokenClient {

    private final GraphProperties properties;
    private final WebClient.Builder webClientBuilder;

    private String cachedToken;
    private Instant expiresAt = Instant.EPOCH;

    @Override
    public synchronized String getAccessToken() {
        if (StringUtils.hasText(cachedToken) && Instant.now().isBefore(expiresAt.minusSeconds(60))) {
            return cachedToken;
        }

        validateConfig();

        GraphTokenResponse response = webClientBuilder.build()
                .post()
                .uri(properties.getTokenUrl() + "/{tenantId}/oauth2/v2.0/token", properties.getTenantId())
                .contentType(MediaType.APPLICATION_FORM_URLENCODED)
                .body(BodyInserters.fromFormData("client_id", properties.getClientId())
                        .with("client_secret", properties.getClientSecret())
                        .with("scope", properties.getScope())
                        .with("grant_type", "client_credentials"))
                .retrieve()
                .bodyToMono(GraphTokenResponse.class)
                .block();

        if (response == null || !StringUtils.hasText(response.accessToken())) {
            throw new IllegalStateException("Microsoft Graph token response did not contain access_token");
        }

        cachedToken = response.accessToken();
        expiresAt = Instant.now().plusSeconds(Math.max(response.expiresIn(), 300));
        return cachedToken;
    }

    private void validateConfig() {
        if (!StringUtils.hasText(properties.getTenantId())) {
            throw new IllegalStateException("microsoft.graph.tenant-id is not configured");
        }
        if (!StringUtils.hasText(properties.getClientId())) {
            throw new IllegalStateException("microsoft.graph.client-id is not configured");
        }
        if (!StringUtils.hasText(properties.getClientSecret())) {
            throw new IllegalStateException("microsoft.graph.client-secret is not configured");
        }
    }
}
