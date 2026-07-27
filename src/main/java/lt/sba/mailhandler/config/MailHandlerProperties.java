package lt.sba.mailhandler.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

import java.util.LinkedHashMap;
import java.util.Map;

@Data
@Component
@ConfigurationProperties(prefix = "mail-handler")
public class MailHandlerProperties {
    private String apiKey;
    private String mailboxesJson;
    private Poll poll = new Poll();
    private Map<String, Mailbox> mailboxes = new LinkedHashMap<>();

    @Data
    public static class Poll {
        private boolean enabled = false;
        private long fixedDelayMs = 60_000L;
        private int maxMessages = 10;
        private boolean markAsReadAfterSuccess = true;
        private boolean fetchAttachmentContent = true;
        private boolean failOnAttachmentContentError = true;
    }

    @Data
    public static class Mailbox {
        private String key;
        private boolean enabled = true;
        private String providerKey;
        private String mailbox;
        private String folder = "Inbox";
        private String targetUrl;
    }
}
