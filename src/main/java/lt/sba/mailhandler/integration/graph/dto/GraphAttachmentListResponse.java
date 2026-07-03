package lt.sba.mailhandler.integration.graph.dto;

import java.util.List;

public record GraphAttachmentListResponse(
        List<GraphAttachmentDto> value
) {
}
