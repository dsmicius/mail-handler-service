package lt.sba.mailhandler.integration.complaints.dto;

public record InboundEmailAttachmentDto(
        String fileName,
        String contentType,
        Long sizeBytes
) {
}
