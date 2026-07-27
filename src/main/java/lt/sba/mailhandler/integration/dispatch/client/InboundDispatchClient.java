package lt.sba.mailhandler.integration.dispatch.client;

import lt.sba.mailhandler.integration.dispatch.dto.InboundDispatchResult;
import lt.sba.mailhandler.integration.dispatch.dto.InboundEmailDispatchRequest;

public interface InboundDispatchClient {
    InboundDispatchResult dispatch(String targetUrl, InboundEmailDispatchRequest request);
}
