package lt.sba.mailhandler.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

@Data
@Component
@ConfigurationProperties(prefix = "complaints-service")
public class ComplaintsServiceProperties {
    private String baseUrl;
    private String xApiKey;
    private String inboundProcessPath = "/api/email-inbound/process";

    public String inboundProcessUrl() {
        if (!StringUtils.hasText(baseUrl)) {
            return null;
        }

        String normalizedBaseUrl = trimTrailingSlash(baseUrl.trim());
        String normalizedPath = normalizePath(inboundProcessPath);
        return normalizedBaseUrl + normalizedPath;
    }

    private String trimTrailingSlash(String value) {
        while (value.endsWith("/")) {
            value = value.substring(0, value.length() - 1);
        }
        return value;
    }

    private String normalizePath(String value) {
        if (!StringUtils.hasText(value)) {
            return "/api/email-inbound/process";
        }
        String trimmed = value.trim();
        return trimmed.startsWith("/") ? trimmed : "/" + trimmed;
    }
}
