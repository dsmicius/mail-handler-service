package lt.sba.mailhandler.integration.complaints.client.impl;

import lombok.RequiredArgsConstructor;
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

    private final WebClient.Builder webClientBuilder;

    @Override
    public ProcessInboundEmailResponse process(String targetUrl, ProcessInboundEmailRequest request) {
        if (!StringUtils.hasText(targetUrl)) {
            throw new IllegalArgumentException("Inbound target URL is not configured");
        }

        return webClientBuilder.build()
                .post()
                .uri(targetUrl)
                .contentType(MediaType.APPLICATION_JSON)
                .accept(MediaType.APPLICATION_JSON)
                .bodyValue(request)
                .retrieve()
                .bodyToMono(ProcessInboundEmailResponse.class)
                .block();
    }
}
