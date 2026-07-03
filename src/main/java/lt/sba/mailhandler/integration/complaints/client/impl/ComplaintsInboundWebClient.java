package lt.sba.mailhandler.integration.complaints.client.impl;

import lombok.RequiredArgsConstructor;
import lt.sba.mailhandler.config.ComplaintsServiceProperties;
import lt.sba.mailhandler.integration.complaints.client.ComplaintsInboundClient;
import lt.sba.mailhandler.integration.complaints.dto.ProcessInboundEmailRequest;
import lt.sba.mailhandler.integration.complaints.dto.ProcessInboundEmailResponse;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import org.springframework.web.reactive.function.client.WebClient;

@Component
@RequiredArgsConstructor
public class ComplaintsInboundWebClient implements ComplaintsInboundClient {

    private static final String X_API_KEY_HEADER = "x-api-key";

    private final WebClient.Builder webClientBuilder;
    private final ComplaintsServiceProperties complaintsServiceProperties;

    @Override
    public ProcessInboundEmailResponse process(String targetUrl, ProcessInboundEmailRequest request) {
        String resolvedTargetUrl = resolveTargetUrl(targetUrl);
        if (!StringUtils.hasText(resolvedTargetUrl)) {
            throw new IllegalArgumentException("Complaints inbound target URL is not configured");
        }

        return webClientBuilder.build()
                .post()
                .uri(resolvedTargetUrl)
                .contentType(MediaType.APPLICATION_JSON)
                .accept(MediaType.APPLICATION_JSON)
                .headers(headers -> {
                    if (StringUtils.hasText(complaintsServiceProperties.getXApiKey())) {
                        headers.set(X_API_KEY_HEADER, complaintsServiceProperties.getXApiKey());
                    }
                })
                .bodyValue(request)
                .retrieve()
                .bodyToMono(ProcessInboundEmailResponse.class)
                .block();
    }

    private String resolveTargetUrl(String targetUrl) {
        if (StringUtils.hasText(targetUrl)) {
            return targetUrl;
        }
        return complaintsServiceProperties.inboundProcessUrl();
    }
}
