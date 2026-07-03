package lt.sba.mailhandler.integration.graph.client.impl;

import lombok.RequiredArgsConstructor;
import lt.sba.mailhandler.config.GraphProperties;
import lt.sba.mailhandler.config.MailHandlerProperties;
import lt.sba.mailhandler.integration.graph.client.GraphMailboxClient;
import lt.sba.mailhandler.integration.graph.client.GraphTokenClient;
import lt.sba.mailhandler.integration.graph.dto.GraphAttachmentDto;
import lt.sba.mailhandler.integration.graph.dto.GraphAttachmentListResponse;
import lt.sba.mailhandler.integration.graph.dto.GraphMessageDto;
import lt.sba.mailhandler.integration.graph.dto.GraphMessageListResponse;
import org.springframework.http.HttpHeaders;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import org.springframework.web.reactive.function.client.WebClient;

import java.util.List;
import java.util.Map;

@Component
@RequiredArgsConstructor
public class MicrosoftGraphMailboxClient implements GraphMailboxClient {

    private static final String MESSAGE_SELECT = String.join(",",
            "id",
            "internetMessageId",
            "conversationId",
            "subject",
            "body",
            "bodyPreview",
            "from",
            "toRecipients",
            "ccRecipients",
            "bccRecipients",
            "receivedDateTime",
            "internetMessageHeaders",
            "hasAttachments"
    );

    private final GraphProperties graphProperties;
    private final GraphTokenClient tokenClient;
    private final WebClient.Builder webClientBuilder;

    @Override
    public List<GraphMessageDto> listUnreadMessages(MailHandlerProperties.Mailbox mailbox, int maxMessages) {
        validateMailbox(mailbox);

        GraphMessageListResponse response = graphClient()
                .get()
                .uri(uriBuilder -> uriBuilder
                        .path("/users/{mailbox}/mailFolders/{folder}/messages")
                        .queryParam("$top", Math.max(1, maxMessages))
                        .queryParam("$filter", "isRead eq false")
                        .queryParam("$select", MESSAGE_SELECT)
                        .build(mailbox.getMailbox(), mailbox.getFolder()))
                .header(HttpHeaders.AUTHORIZATION, bearer())
                .retrieve()
                .bodyToMono(GraphMessageListResponse.class)
                .block();

        return response == null || response.value() == null ? List.of() : response.value();
    }

    @Override
    public List<GraphAttachmentDto> listAttachments(MailHandlerProperties.Mailbox mailbox, String messageId) {
        if (!StringUtils.hasText(messageId)) {
            return List.of();
        }

        GraphAttachmentListResponse response = graphClient()
                .get()
                .uri(uriBuilder -> uriBuilder
                        .path("/users/{mailbox}/messages/{messageId}/attachments")
                        .queryParam("$select", "id,name,contentType,size,isInline")
                        .build(mailbox.getMailbox(), messageId))
                .header(HttpHeaders.AUTHORIZATION, bearer())
                .retrieve()
                .bodyToMono(GraphAttachmentListResponse.class)
                .block();

        return response == null || response.value() == null ? List.of() : response.value();
    }

    @Override
    public void markAsRead(MailHandlerProperties.Mailbox mailbox, String messageId) {
        if (!StringUtils.hasText(messageId)) {
            return;
        }

        graphClient()
                .patch()
                .uri("/users/{mailbox}/messages/{messageId}", mailbox.getMailbox(), messageId)
                .header(HttpHeaders.AUTHORIZATION, bearer())
                .bodyValue(Map.of("isRead", true))
                .retrieve()
                .toBodilessEntity()
                .block();
    }

    private WebClient graphClient() {
        return webClientBuilder.baseUrl(graphProperties.getApiUrl()).build();
    }

    private String bearer() {
        return "Bearer " + tokenClient.getAccessToken();
    }

    private void validateMailbox(MailHandlerProperties.Mailbox mailbox) {
        if (mailbox == null) {
            throw new IllegalArgumentException("Mailbox config is missing");
        }
        if (!StringUtils.hasText(mailbox.getMailbox())) {
            throw new IllegalArgumentException("Mailbox email/address is not configured");
        }
        if (!StringUtils.hasText(mailbox.getFolder())) {
            throw new IllegalArgumentException("Mailbox folder is not configured");
        }
    }
}
