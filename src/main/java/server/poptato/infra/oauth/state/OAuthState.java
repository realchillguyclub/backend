package server.poptato.infra.oauth.state;

import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class OAuthState {
    private String state;
    private String codeVerifier;
}
