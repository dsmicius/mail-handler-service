package lt.sba.mailhandler.application.service.impl;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import lt.sba.mailhandler.application.service.InboundPollService;
import lt.sba.mailhandler.config.MailHandlerProperties;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
public class InboundPollScheduler {

    private final MailHandlerProperties properties;
    private final InboundPollService service;

    @Scheduled(fixedDelayString = "${mail-handler.poll.fixed-delay-ms:60000}")
    public void poll() {
        if (!properties.getPoll().isEnabled()) {
            return;
        }

        try {
            var response = service.pollAllEnabledMailboxes();
            log.info(
                    "Inbound mail poll completed. mailboxes={}, fetched={}, dispatched={}, failed={}",
                    response.mailboxCount(),
                    response.fetched(),
                    response.dispatched(),
                    response.failed()
            );
        } catch (Exception ex) {
            log.error("Inbound mail poll failed", ex);
        }
    }
}
