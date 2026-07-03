package lt.sba.mailhandler.api.dto;

import java.util.List;

public record PollAllResponse(
        int mailboxCount,
        int fetched,
        int dispatched,
        int failed,
        List<MailboxPollResponse> mailboxes
) {
}
