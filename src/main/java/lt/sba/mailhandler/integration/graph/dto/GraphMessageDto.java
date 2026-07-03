package lt.sba.mailhandler.integration.graph.dto;

import java.time.OffsetDateTime;
import java.util.List;

public record GraphMessageDto(
        String id,
        String internetMessageId,
        String conversationId,
        String subject,
        GraphBodyDto body,
        String bodyPreview,
        GraphRecipientDto from,
        List<GraphRecipientDto> toRecipients,
        List<GraphRecipientDto> ccRecipients,
        List<GraphRecipientDto> bccRecipients,
        OffsetDateTime receivedDateTime,
        List<GraphInternetMessageHeaderDto> internetMessageHeaders,
        Boolean hasAttachments
) {
}
