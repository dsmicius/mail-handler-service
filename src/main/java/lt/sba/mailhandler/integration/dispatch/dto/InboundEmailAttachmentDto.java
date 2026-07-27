package lt.sba.mailhandler.integration.dispatch.dto;

public record InboundEmailAttachmentDto(
        String fileName,
        String contentType,
        Long sizeBytes,
        String contentBase64
) {
}
