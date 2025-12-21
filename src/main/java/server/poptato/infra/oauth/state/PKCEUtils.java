package server.poptato.infra.oauth.state;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.SecureRandom;
import java.util.Base64;

public final class PKCEUtils {

    private PKCEUtils() {}

    /**
     * 새로운 코드 검증 문자열(code_verifier)을 생성합니다.
     * @return 안전한 무작위 문자열(Base64 URL 인코딩, padding 제거)
     */
    public static String generateCodeVerifier() {
        byte[] randomBytes = new byte[32];
        new SecureRandom().nextBytes(randomBytes);

        return Base64.getUrlEncoder().withoutPadding().encodeToString(randomBytes);
    }

    /**
     * 코드 검증 문자열(code_verifier)을 기반으로 코드 챌린지(code_challenge)를 생성합니다.
     * PKCE의 SHA-256(S256) 방식 사용.
     *
     * @param codeVerifier 원본 코드 검증 문자열
     * @return Base64 URL 인코딩된 코드 챌린지
     */
    public static String generateCodeChallenge(String codeVerifier) {
        try {
            MessageDigest sha256Digest = MessageDigest.getInstance("SHA-256");
            byte[] hashedBytes = sha256Digest.digest(codeVerifier.getBytes(StandardCharsets.US_ASCII));

            return Base64.getUrlEncoder().withoutPadding().encodeToString(hashedBytes);
        } catch (Exception exception) {
            throw new IllegalStateException("Failed to generate PKCE code challenge", exception);
        }
    }
}
