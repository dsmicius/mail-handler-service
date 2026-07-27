package lt.sba.mailhandler.integration.graph.dto;

import com.fasterxml.jackson.annotation.JsonProperty;

public record GraphAttachmentDto(
        @JsonProperty("@odata.type")
        String odataType,
        String id,
        String name,
        String contentType,
        Long size,
        Boolean isInline
) {
}
