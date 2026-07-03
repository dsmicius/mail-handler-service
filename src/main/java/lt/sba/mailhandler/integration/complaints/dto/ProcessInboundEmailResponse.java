package lt.sba.mailhandler.integration.complaints.dto;

public record ProcessInboundEmailResponse(
        boolean matched,
        boolean duplicate,
        String matchType,
        String communicationId,
        String duplicateOfCommunicationId,
        String replyToCommunicationId,
        String issueId,
        String issueKey,
        boolean jiraCommentCreated,
        String jiraCommentId,
        String jiraCommentError,
        String message
) {
}
