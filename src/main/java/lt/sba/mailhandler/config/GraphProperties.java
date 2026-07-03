package lt.sba.mailhandler.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

@Data
@Component
@ConfigurationProperties(prefix = "microsoft.graph")
public class GraphProperties {
    private String tenantId;
    private String clientId;
    private String clientSecret;
    private String tokenUrl = "https://login.microsoftonline.com";
    private String apiUrl = "https://graph.microsoft.com/v1.0";
    private String scope = "https://graph.microsoft.com/.default";
}
