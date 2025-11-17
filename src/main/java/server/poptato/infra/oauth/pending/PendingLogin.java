package server.poptato.infra.oauth.pending;

public record PendingLogin(
        String socialType,
        String accessToken
) {
}
