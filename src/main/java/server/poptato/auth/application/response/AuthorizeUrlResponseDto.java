package server.poptato.auth.application.response;

public record AuthorizeUrlResponseDto(
        String authorizeUrl
) {
    public static AuthorizeUrlResponseDto of(String authorizeUrl) {
        return new AuthorizeUrlResponseDto(authorizeUrl);
    }
}
