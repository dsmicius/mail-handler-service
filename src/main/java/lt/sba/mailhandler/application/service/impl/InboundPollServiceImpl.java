package lt.sba.mailhandler.application.service.impl;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import lt.sba.mailhandler.api.dto.MailboxPollResponse;
import lt.sba.mailhandler.api.dto.MessageProcessResult;
import lt.sba.mailhandler.api.dto.PollAllResponse;
import lt.sba.mailhandler.application.service.InboundPollService;
import lt.sba.mailhandler.config.MailHandlerProperties;
import lt.sba.mailhandler.integration.dispatch.client.InboundDispatchClient;
import lt.sba.mailhandler.integration.dispatch.dto.InboundDispatchResult;
import lt.sba.mailhandler.integration.dispatch.dto.InboundEmailAddressDto;
import lt.sba.mailhandler.integration.dispatch.dto.InboundEmailAttachmentDto;
import lt.sba.mailhandler.integration.dispatch.dto.InboundEmailDispatchRequest;
import lt.sba.mailhandler.integration.graph.client.GraphMailboxClient;
import lt.sba.mailhandler.integration.graph.dto.GraphAttachmentDto;
import lt.sba.mailhandler.integration.graph.dto.GraphBodyDto;
import lt.sba.mailhandler.integration.graph.dto.GraphInternetMessageHeaderDto;
import lt.sba.mailhandler.integration.graph.dto.GraphMessageDto;
import lt.sba.mailhandler.integration.graph.dto.GraphRecipientDto;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Base64;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

@Slf4j
@Service
@RequiredArgsConstructor
public class InboundPollServiceImpl implements InboundPollService {

    private static final String HEADER_IN_REPLY_TO = "in-reply-to";
    private static final String HEADER_REFERENCES = "references";

    private final MailHandlerProperties properties;
    private final GraphMailboxClient graphMailboxClient;
    private final InboundDispatchClient dispatchClient;
    private final ObjectMapper objectMapper;

    @Override
    public PollAllResponse pollAllEnabledMailboxes() {
        List<MailboxPollResponse> responses = new ArrayList<>();
        Map<String, MailHandlerProperties.Mailbox> mailboxes = configuredMailboxes();

        for (Map.Entry<String, MailHandlerProperties.Mailbox> entry : mailboxes.entrySet()) {
            if (!entry.getValue().isEnabled()) {
                continue;
            }
            responses.add(pollMailbox(entry.getKey()));
        }

        int fetched = responses.stream().mapToInt(MailboxPollResponse::fetched).sum();
        int dispatched = responses.stream().mapToInt(MailboxPollResponse::dispatched).sum();
        int failed = responses.stream().mapToInt(MailboxPollResponse::failed).sum();

        return new PollAllResponse(responses.size(), fetched, dispatched, failed, responses);
    }

    @Override
    public MailboxPollResponse pollMailbox(String mailboxKey) {
        Map<String, MailHandlerProperties.Mailbox> mailboxes = configuredMailboxes();
        MailHandlerProperties.Mailbox mailbox = mailboxes.get(mailboxKey);
        if (mailbox == null) {
            throw new IllegalArgumentException("Mailbox config not found: " + mailboxKey);
        }
        if (!mailbox.isEnabled()) {
            return emptyResponse(mailboxKey, mailbox, "Mailbox is disabled");
        }

        List<GraphMessageDto> messages = graphMailboxClient.listUnreadMessages(mailbox, properties.getPoll().getMaxMessages());
        List<MessageProcessResult> results = new ArrayList<>();
        List<String> errors = new ArrayList<>();

        for (GraphMessageDto message : messages) {
            MessageProcessResult result = processMessage(mailboxKey, mailbox, message);
            results.add(result);
            if (StringUtils.hasText(result.error())) {
                errors.add(result.error());
            }
        }

        int dispatched = (int) results.stream().filter(MessageProcessResult::dispatched).count();
        int markedAsRead = (int) results.stream().filter(MessageProcessResult::markedAsRead).count();
        int failed = (int) results.stream().filter(result -> StringUtils.hasText(result.error())).count();

        return new MailboxPollResponse(
                mailboxKey,
                mailbox.getMailbox(),
                messages.size(),
                dispatched,
                markedAsRead,
                failed,
                results,
                errors
        );
    }

    private MessageProcessResult processMessage(String mailboxKey, MailHandlerProperties.Mailbox mailbox, GraphMessageDto message) {
        try {
            InboundEmailDispatchRequest request = toInboundRequest(mailboxKey, mailbox, message);
            InboundDispatchResult dispatchResult = dispatchClient.dispatch(mailbox.getTargetUrl(), request);

            boolean markedAsRead = false;
            if (properties.getPoll().isMarkAsReadAfterSuccess()) {
                graphMailboxClient.markAsRead(mailbox, message.id());
                markedAsRead = true;
            }

            return new MessageProcessResult(
                    message.id(),
                    message.internetMessageId(),
                    message.subject(),
                    true,
                    dispatchResult == null ? null : dispatchResult.statusCode(),
                    dispatchResult == null ? null : dispatchResult.responseBody(),
                    markedAsRead,
                    null
            );
        } catch (Exception ex) {
            String error = "Failed to process mailbox " + mailboxKey + " message " + message.id() + ": " + ex.getMessage();
            log.error(error, ex);
            return new MessageProcessResult(
                    message.id(),
                    message.internetMessageId(),
                    message.subject(),
                    false,
                    null,
                    null,
                    false,
                    error
            );
        }
    }

    private InboundEmailDispatchRequest toInboundRequest(String mailboxKey, MailHandlerProperties.Mailbox mailbox, GraphMessageDto message) {
        Map<String, String> headers = headers(message.internetMessageHeaders());
        GraphBodyDto body = message.body();
        String contentType = body == null ? null : body.contentType();
        String content = body == null ? null : body.content();
        boolean html = contentType != null && "html".equalsIgnoreCase(contentType);

        List<InboundEmailAttachmentDto> attachments = Boolean.TRUE.equals(message.hasAttachments())
                ? graphMailboxClient.listAttachments(mailbox, message.id()).stream()
                .filter(attachment -> !Boolean.TRUE.equals(attachment.isInline()))
                .map(attachment -> toAttachment(mailbox, message.id(), attachment))
                .filter(attachment -> attachment != null && StringUtils.hasText(attachment.fileName()))
                .toList()
                : List.of();

        return new InboundEmailDispatchRequest(
                providerKey(mailboxKey, mailbox),
                message.id(),
                message.internetMessageId(),
                message.conversationId(),
                headers.get(HEADER_IN_REPLY_TO),
                headers.get(HEADER_REFERENCES),
                toAddress(message.from()),
                toAddresses(message.toRecipients()),
                toAddresses(message.ccRecipients()),
                toAddresses(message.bccRecipients()),
                message.subject(),
                html ? message.bodyPreview() : content,
                html ? content : null,
                attachments,
                message.receivedDateTime() == null ? LocalDateTime.now() : message.receivedDateTime().toLocalDateTime()
        );
    }

    private Map<String, MailHandlerProperties.Mailbox> configuredMailboxes() {
        if (StringUtils.hasText(properties.getMailboxesJson())) {
            return mailboxesFromJson(properties.getMailboxesJson());
        }

        Map<String, MailHandlerProperties.Mailbox> result = new LinkedHashMap<>();
        if (properties.getMailboxes() == null) {
            return result;
        }

        properties.getMailboxes().forEach((key, mailbox) -> {
            if (mailbox == null) {
                return;
            }
            if (!StringUtils.hasText(mailbox.getKey())) {
                mailbox.setKey(key);
            }
            result.put(key, mailbox);
        });
        return result;
    }

    private Map<String, MailHandlerProperties.Mailbox> mailboxesFromJson(String json) {
        try {
            List<MailHandlerProperties.Mailbox> mailboxList = objectMapper.readValue(
                    json,
                    new TypeReference<>() {
                    }
            );

            Map<String, MailHandlerProperties.Mailbox> result = new LinkedHashMap<>();
            for (MailHandlerProperties.Mailbox mailbox : mailboxList) {
                if (mailbox == null || !StringUtils.hasText(mailbox.getKey())) {
                    throw new IllegalArgumentException("Every MAIL_HANDLER_MAILBOXES_JSON item must contain non-empty key");
                }
                result.put(mailbox.getKey(), mailbox);
            }
            return result;
        } catch (Exception ex) {
            throw new IllegalStateException("Failed to parse MAIL_HANDLER_MAILBOXES_JSON", ex);
        }
    }

    private String providerKey(String mailboxKey, MailHandlerProperties.Mailbox mailbox) {
        if (StringUtils.hasText(mailbox.getProviderKey())) {
            return mailbox.getProviderKey();
        }
        return "microsoft-graph-" + mailboxKey;
    }

    private Map<String, String> headers(List<GraphInternetMessageHeaderDto> source) {
        Map<String, String> result = new LinkedHashMap<>();
        if (source == null) {
            return result;
        }

        for (GraphInternetMessageHeaderDto header : source) {
            if (header == null || !StringUtils.hasText(header.name())) {
                continue;
            }
            result.put(header.name().toLowerCase(Locale.ROOT), header.value());
        }

        return result;
    }

    private InboundEmailAttachmentDto toAttachment(
            MailHandlerProperties.Mailbox mailbox,
            String messageId,
            GraphAttachmentDto attachment
    ) {
        if (attachment == null || !StringUtils.hasText(attachment.id())) {
            return null;
        }

        String contentBase64 = null;
        if (properties.getPoll().isFetchAttachmentContent()) {
            try {
                byte[] content = graphMailboxClient.getAttachmentContent(mailbox, messageId, attachment.id());
                contentBase64 = content.length == 0 ? null : Base64.getEncoder().encodeToString(content);
            } catch (Exception ex) {
                String warning = "Failed to fetch attachment content. mailbox="
                        + mailbox.getMailbox()
                        + ", messageId=" + messageId
                        + ", attachmentId=" + attachment.id()
                        + ", fileName=" + attachment.name()
                        + ", error=" + ex.getMessage();

                if (properties.getPoll().isFailOnAttachmentContentError()) {
                    throw new IllegalStateException(warning, ex);
                }

                log.warn(warning, ex);
            }
        }

        return new InboundEmailAttachmentDto(
                attachment.name(),
                attachment.contentType(),
                attachment.size(),
                contentBase64
        );
    }

    private InboundEmailAddressDto toAddress(GraphRecipientDto recipient) {
        if (recipient == null || recipient.emailAddress() == null) {
            return null;
        }
        return new InboundEmailAddressDto(
                recipient.emailAddress().name(),
                recipient.emailAddress().address()
        );
    }

    private List<InboundEmailAddressDto> toAddresses(List<GraphRecipientDto> recipients) {
        if (recipients == null || recipients.isEmpty()) {
            return List.of();
        }
        return recipients.stream()
                .map(this::toAddress)
                .filter(address -> address != null && StringUtils.hasText(address.email()))
                .toList();
    }

    private MailboxPollResponse emptyResponse(String mailboxKey, MailHandlerProperties.Mailbox mailbox, String message) {
        return new MailboxPollResponse(
                mailboxKey,
                mailbox == null ? null : mailbox.getMailbox(),
                0,
                0,
                0,
                0,
                List.of(),
                List.of(message)
        );
    }
}
