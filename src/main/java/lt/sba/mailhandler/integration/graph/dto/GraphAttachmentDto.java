package lt.sba.mailhandler.integration.graph.dto;

public record GraphAttachmentDto(
        String id,
        String name,
        String contentType,
        Long size,
        Boolean isInline
) {
}
