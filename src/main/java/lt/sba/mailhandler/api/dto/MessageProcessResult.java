package lt.sba.mailhandler.api.dto;

import com.fasterxml.jackson.databind.JsonNode;

public record MessageProcessResult(
        String externalMessageId,
        String internetMessageId,
        String subject,
        boolean dispatched,
        Integer targetStatus,
        JsonNode targetResponse,
        boolean markedAsRead,
        String error
) {
}
