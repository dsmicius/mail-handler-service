package lt.sba.mailhandler.integration.graph.client;

import lt.sba.mailhandler.config.MailHandlerProperties;
import lt.sba.mailhandler.integration.graph.dto.GraphAttachmentDto;
import lt.sba.mailhandler.integration.graph.dto.GraphMessageDto;

import java.util.List;

public interface GraphMailboxClient {
    List<GraphMessageDto> listUnreadMessages(MailHandlerProperties.Mailbox mailbox, int maxMessages);

    List<GraphAttachmentDto> listAttachments(MailHandlerProperties.Mailbox mailbox, String messageId);

    byte[] getAttachmentContent(MailHandlerProperties.Mailbox mailbox, String messageId, String attachmentId);

    void markAsRead(MailHandlerProperties.Mailbox mailbox, String messageId);
}
