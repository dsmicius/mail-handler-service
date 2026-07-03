package lt.sba.mailhandler.api.controller;

import lombok.RequiredArgsConstructor;
import lt.sba.mailhandler.api.dto.MailboxPollResponse;
import lt.sba.mailhandler.api.dto.PollAllResponse;
import lt.sba.mailhandler.application.service.InboundPollService;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/inbound")
@RequiredArgsConstructor
public class InboundPollController {

    private final InboundPollService service;

    @PostMapping(value = "/poll", produces = MediaType.APPLICATION_JSON_VALUE)
    public PollAllResponse pollAll() {
        return service.pollAllEnabledMailboxes();
    }

    @PostMapping(value = "/poll/{mailboxKey}", produces = MediaType.APPLICATION_JSON_VALUE)
    public MailboxPollResponse pollMailbox(@PathVariable String mailboxKey) {
        return service.pollMailbox(mailboxKey);
    }
}
