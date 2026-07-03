package lt.sba.mailhandler.integration.graph.dto;

import com.fasterxml.jackson.annotation.JsonProperty;

public record GraphTokenResponse(
        @JsonProperty("access_token") String accessToken,
        @JsonProperty("token_type") String tokenType,
        @JsonProperty("expires_in") long expiresIn
) {
}
