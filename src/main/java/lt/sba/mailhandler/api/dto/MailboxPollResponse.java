package lt.sba.mailhandler.api.dto;

import java.util.List;

public record MailboxPollResponse(
        String mailboxKey,
        String mailbox,
        int fetched,
        int dispatched,
        int markedAsRead,
        int failed,
        List<MessageProcessResult> messages,
        List<String> errors
) {
}
