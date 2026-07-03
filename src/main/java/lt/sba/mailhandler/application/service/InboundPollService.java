package lt.sba.mailhandler.application.service;

import lt.sba.mailhandler.api.dto.MailboxPollResponse;
import lt.sba.mailhandler.api.dto.PollAllResponse;

public interface InboundPollService {
    PollAllResponse pollAllEnabledMailboxes();

    MailboxPollResponse pollMailbox(String mailboxKey);
}
