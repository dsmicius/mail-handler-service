package lt.sba.mailhandler.config;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;

import java.util.LinkedHashMap;
import java.util.Map;

@Getter
@Setter
@ConfigurationProperties(prefix = "mail-handler")
public class MailHandlerProperties {
    private Poll poll = new Poll();
    private Map<String, Mailbox> mailboxes = new LinkedHashMap<>();

    @Getter
    @Setter
    public static class Poll {
        private boolean enabled = false;
        private long fixedDelayMs = 60_000L;
        private int maxMessages = 10;
        private boolean markAsReadAfterSuccess = true;
    }

    @Getter
    @Setter
    public static class Mailbox {
        private boolean enabled = true;
        private String providerKey;
        private String mailbox;
        private String folder = "Inbox";
        private String targetUrl;
    }
}
