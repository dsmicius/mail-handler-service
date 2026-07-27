package lt.sba.mailhandler.integration.dispatch.client.impl;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lt.sba.mailhandler.config.MailHandlerProperties;
import lt.sba.mailhandler.integration.dispatch.client.InboundDispatchClient;
import lt.sba.mailhandler.integration.dispatch.dto.InboundDispatchResult;
import lt.sba.mailhandler.integration.dispatch.dto.InboundEmailDispatchRequest;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import org.springframework.web.reactive.function.client.WebClient;

@Component
@RequiredArgsConstructor
public class InboundDispatchWebClient implements InboundDispatchClient {

    private static final String X_API_KEY_HEADER = "x-api-key";

    private final WebClient.Builder webClientBuilder;
    private final MailHandlerProperties properties;
    private final ObjectMapper objectMapper;

    @Override
    public InboundDispatchResult dispatch(String targetUrl, InboundEmailDispatchRequest request) {
        if (!StringUtils.hasText(targetUrl)) {
            throw new IllegalArgumentException("Inbound target URL is not configured");
        }

        return webClientBuilder.build()
                .post()
                .uri(targetUrl)
                .contentType(MediaType.APPLICATION_JSON)
                .accept(MediaType.APPLICATION_JSON)
                .headers(headers -> {
                    if (StringUtils.hasText(properties.getApiKey())) {
                        headers.set(X_API_KEY_HEADER, properties.getApiKey());
                    }
                })
                .bodyValue(request)
                .exchangeToMono(response -> response.bodyToMono(String.class)
                        .defaultIfEmpty("")
                        .map(body -> {
                            int statusCode = response.statusCode().value();
                            JsonNode responseBody = toJsonNode(body);
                            if (!response.statusCode().is2xxSuccessful()) {
                                throw new IllegalStateException("Inbound target returned HTTP " + statusCode + ": " + body);
                            }
                            return new InboundDispatchResult(statusCode, responseBody);
                        }))
                .block();
    }

    private JsonNode toJsonNode(String body) {
        if (!StringUtils.hasText(body)) {
            return objectMapper.getNodeFactory().nullNode();
        }
        try {
            return objectMapper.readTree(body);
        } catch (Exception ex) {
            return objectMapper.getNodeFactory().textNode(body);
        }
    }
}
