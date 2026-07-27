package lt.sba.mailhandler.integration.dispatch.dto;

import com.fasterxml.jackson.databind.JsonNode;

public record InboundDispatchResult(
        int statusCode,
        JsonNode responseBody
) {
}
