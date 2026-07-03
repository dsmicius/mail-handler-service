package lt.sba.mailhandler.integration.complaints.client;

import lt.sba.mailhandler.integration.complaints.dto.ProcessInboundEmailRequest;
import lt.sba.mailhandler.integration.complaints.dto.ProcessInboundEmailResponse;

public interface ComplaintsInboundClient {
    ProcessInboundEmailResponse process(String targetUrl, ProcessInboundEmailRequest request);
}
