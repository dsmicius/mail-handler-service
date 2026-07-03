package lt.sba.mailhandler.integration.graph.dto;

import java.util.List;

public record GraphMessageListResponse(
        List<GraphMessageDto> value
) {
}
