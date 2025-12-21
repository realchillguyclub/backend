package server.poptato.infra.oauth.pending;

import server.poptato.user.domain.value.SocialType;

public record PendingLogin(
        SocialType socialType,
        String accessToken
) {
}
