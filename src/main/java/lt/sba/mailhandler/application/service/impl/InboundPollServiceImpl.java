package lt.sba.mailhandler.application.service.impl;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import lt.sba.mailhandler.api.dto.MailboxPollResponse;
import lt.sba.mailhandler.api.dto.MessageProcessResult;
import lt.sba.mailhandler.api.dto.PollAllResponse;
import lt.sba.mailhandler.application.service.InboundPollService;
import lt.sba.mailhandler.config.MailHandlerProperties;
import lt.sba.mailhandler.integration.complaints.client.ComplaintsInboundClient;
import lt.sba.mailhandler.integration.complaints.dto.InboundEmailAddressDto;
import lt.sba.mailhandler.integration.complaints.dto.InboundEmailAttachmentDto;
import lt.sba.mailhandler.integration.complaints.dto.ProcessInboundEmailRequest;
import lt.sba.mailhandler.integration.complaints.dto.ProcessInboundEmailResponse;
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
    private final ComplaintsInboundClient complaintsInboundClient;

    @Override
    public PollAllResponse pollAllEnabledMailboxes() {
        List<MailboxPollResponse> responses = new ArrayList<>();

        for (Map.Entry<String, MailHandlerProperties.Mailbox> entry : properties.getMailboxes().entrySet()) {
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
        MailHandlerProperties.Mailbox mailbox = properties.getMailboxes().get(mailboxKey);
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
        int matched = (int) results.stream().filter(MessageProcessResult::matched).count();
        int duplicates = (int) results.stream().filter(MessageProcessResult::duplicate).count();
        int unmatched = (int) results.stream().filter(result -> result.dispatched() && !result.matched()).count();
        int markedAsRead = (int) results.stream().filter(MessageProcessResult::markedAsRead).count();
        int failed = (int) results.stream().filter(result -> StringUtils.hasText(result.error())).count();

        return new MailboxPollResponse(
                mailboxKey,
                mailbox.getMailbox(),
                messages.size(),
                dispatched,
                matched,
                duplicates,
                unmatched,
                markedAsRead,
                failed,
                results,
                errors
        );
    }

    private MessageProcessResult processMessage(String mailboxKey, MailHandlerProperties.Mailbox mailbox, GraphMessageDto message) {
        try {
            ProcessInboundEmailRequest request = toInboundRequest(mailboxKey, mailbox, message);
            ProcessInboundEmailResponse response = complaintsInboundClient.process(mailbox.getTargetUrl(), request);

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
                    response != null && response.matched(),
                    response != null && response.duplicate(),
                    response != null && response.jiraCommentCreated(),
                    response == null ? null : response.communicationId(),
                    response == null ? null : response.issueKey(),
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
                    false,
                    false,
                    false,
                    null,
                    null,
                    false,
                    error
            );
        }
    }

    private ProcessInboundEmailRequest toInboundRequest(String mailboxKey, MailHandlerProperties.Mailbox mailbox, GraphMessageDto message) {
        Map<String, String> headers = headers(message.internetMessageHeaders());
        GraphBodyDto body = message.body();
        String contentType = body == null ? null : body.contentType();
        String content = body == null ? null : body.content();
        boolean html = contentType != null && "html".equalsIgnoreCase(contentType);

        List<InboundEmailAttachmentDto> attachments = Boolean.TRUE.equals(message.hasAttachments())
                ? graphMailboxClient.listAttachments(mailbox, message.id()).stream()
                .filter(attachment -> !Boolean.TRUE.equals(attachment.isInline()))
                .map(this::toAttachment)
                .toList()
                : List.of();

        return new ProcessInboundEmailRequest(
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

    private InboundEmailAttachmentDto toAttachment(GraphAttachmentDto attachment) {
        return new InboundEmailAttachmentDto(
                attachment.name(),
                attachment.contentType(),
                attachment.size()
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
                0,
                0,
                0,
                List.of(),
                List.of(message)
        );
    }
}
