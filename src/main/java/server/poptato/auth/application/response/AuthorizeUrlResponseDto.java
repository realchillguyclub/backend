package server.poptato.auth.application.response;

public record AuthorizeUrlResponseDto(
        String authorizeUrl,
        String state
) {
    public static AuthorizeUrlResponseDto of(String authorizeUrl, String state) {
        return new AuthorizeUrlResponseDto(authorizeUrl, state);
    }
}
