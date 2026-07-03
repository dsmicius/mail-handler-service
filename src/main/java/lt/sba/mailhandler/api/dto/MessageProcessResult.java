package lt.sba.mailhandler.api.dto;

public record MessageProcessResult(
        String externalMessageId,
        String internetMessageId,
        String subject,
        boolean dispatched,
        boolean matched,
        boolean duplicate,
        boolean jiraCommentCreated,
        String communicationId,
        String issueKey,
        boolean markedAsRead,
        String error
) {
}
