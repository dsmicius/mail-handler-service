package lt.sba.mailhandler.integration.dispatch.dto;

import java.time.LocalDateTime;
import java.util.List;

public record InboundEmailDispatchRequest(
        String providerKey,

        String externalMessageId,
        String internetMessageId,
        String conversationId,
        String inReplyTo,
        String referencesHeader,

        InboundEmailAddressDto from,

        List<InboundEmailAddressDto> to,
        List<InboundEmailAddressDto> cc,
        List<InboundEmailAddressDto> bcc,

        String subject,
        String bodyText,
        String bodyHtml,

        List<InboundEmailAttachmentDto> attachments,

        LocalDateTime receivedAt
) {
}
