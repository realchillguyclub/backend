package server.poptato.auth.application;

import static org.assertj.core.api.Assertions.*;

import java.nio.charset.StandardCharsets;
import java.util.Base64;
import java.util.Date;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.MethodOrderer;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestMethodOrder;
import org.mockito.InjectMocks;
import org.springframework.test.util.ReflectionTestUtils;

import io.jsonwebtoken.Header;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import server.poptato.auth.application.service.JwtService;
import server.poptato.auth.status.AuthErrorStatus;
import server.poptato.configuration.ServiceTestConfig;
import server.poptato.global.exception.CustomException;

class JwtServiceTokenTest extends ServiceTestConfig {

    @InjectMocks
    private JwtService jwtService;

    private static final String RAW_SECRET = "this-is-a-test-secret-at-least-32-bytes";
    private static final String OTHER_RAW_SECRET = "another-different-test-secret-32-bytes";
    private static final String BASE64_SECRET = Base64.getEncoder().encodeToString(RAW_SECRET.getBytes(StandardCharsets.UTF_8));
    private static final String OTHER_BASE64_SECRET = Base64.getEncoder().encodeToString(OTHER_RAW_SECRET.getBytes(StandardCharsets.UTF_8));
    private static final String ISS = "ILLDAN_API_SERVER";

    private static String bearer(String token) {
        return "Bearer " + token;
    }

    private static String validAccess(String userId, String base64Secret) {
        Date now = new Date();
        Date exp = new Date(now.getTime()
                + JwtService.ACCESS_TOKEN_EXPIRATION_MINUTE.toMillis());

        return Jwts.builder()
                .setHeaderParam(Header.TYPE, Header.JWT_TYPE)
                .setIssuer(ISS)
                .setSubject("ACCESS_TOKEN")
                .setIssuedAt(now)
                .setExpiration(exp)
                .claim("USER_ID", userId)
                .signWith(Keys.hmacShaKeyFor(base64Secret.getBytes(StandardCharsets.UTF_8)))
                .compact();
    }

    private static String expiredToken(String subject, String userId, String base64Secret) {
        long now = System.currentTimeMillis();
        Date past = new Date(now - 1_000L);

        return Jwts.builder()
                .setHeaderParam(Header.TYPE, Header.JWT_TYPE)
                .setIssuer(ISS)
                .setSubject(subject)
                .setIssuedAt(new Date(past.getTime() - 1_000L))
                .setExpiration(past)
                .claim("USER_ID", userId)
                .signWith(Keys.hmacShaKeyFor(base64Secret.getBytes(StandardCharsets.UTF_8)))
                .compact();
    }

    private static String forgedToken(String subject, String userId, String otherBase64Secret) {
        Date now = new Date();
        Date exp = new Date(now.getTime()
                + JwtService.ACCESS_TOKEN_EXPIRATION_MINUTE.toMillis());

        return Jwts.builder()
                .setHeaderParam(Header.TYPE, Header.JWT_TYPE)
                .setIssuer(ISS)
                .setSubject(subject)
                .setIssuedAt(now)
                .setExpiration(exp)
                .claim("USER_ID", userId)
                .signWith(Keys.hmacShaKeyFor(otherBase64Secret.getBytes(StandardCharsets.UTF_8)))
                .compact();
    }

    @BeforeEach
    void setUp() {
        ReflectionTestUtils.setField(jwtService, "jwtSecret", BASE64_SECRET);
    }

    @Nested
    @TestMethodOrder(MethodOrderer.DisplayName.class)
    @DisplayName("[SCN-SVC-AUTH-JWT-001] 토큰을 생성하고 검증,파싱할 수 있다.")
    class CreateToken {

        @Test
        @DisplayName("[TC-CREATE-TOKEN-001] Access 토큰 생성/검증 통과 후 userId를 정상적으로 추출한다")
        void accessToken_create_verify_parse_userId() {
            // given
            String userId = "123";

            // when
            String accessToken = jwtService.createAccessToken(userId);

            // then
            assertThatCode(() -> jwtService.verifyAccessToken(accessToken)).doesNotThrowAnyException();
            assertThat(jwtService.getUserIdInToken(accessToken)).isEqualTo(userId);
        }

        @Test
        @DisplayName("[TC-CREATE-TOKEN-002] Refresh 토큰 생성/검증 통과 후 userId와 jti를 정상적으로 추출한다")
        void refreshToken_create_verify_parse_userId() {
            // given
            String userId = "123";
            String jti = "test-jti-uuid";

            // when
            String refreshToken = jwtService.createRefreshToken(userId, jti);

            // then
            assertThatCode(() -> jwtService.verifyRefreshToken(refreshToken)).doesNotThrowAnyException();
            assertThat(jwtService.getUserIdInToken(refreshToken)).isEqualTo(userId);
            assertThat(jwtService.getJtiFromToken(refreshToken)).isEqualTo(jti);
        }
    }

    @Nested
    @TestMethodOrder(MethodOrderer.DisplayName.class)
    @DisplayName("[SCN-SVC-AUTH-JWT-002] 만료/변조 토큰을 검증하면 적절한 예외를 받을 수 있다.")
    class ValidateExpiredOrForgedTokens {

        @Test
        @DisplayName("[TC-VERIFY-ERRORS-001] 만료된 Access 토큰을 검증하면 _EXPIRED_ACCESS_TOKEN 예외가 발생한다")
        void expired_access_should_throw_expired_access() {
            // given
            String token = expiredToken("ACCESS_TOKEN", "123", BASE64_SECRET);

            // when & then
            assertThatThrownBy(() -> jwtService.verifyAccessToken(token))
                    .isInstanceOf(CustomException.class)
                    .satisfies(ex -> assertThat(((CustomException) ex).getHttpStatus())
                            .isEqualTo(server.poptato.auth.status.AuthErrorStatus._EXPIRED_ACCESS_TOKEN.getHttpStatus()));
        }

        @Test
        @DisplayName("[TC-VERIFY-ERRORS-002] 만료된 Refresh 토큰을 검증하면 _EXPIRED_REFRESH_TOKEN 예외가 발생한다")
        void expired_refresh_should_throw_expired_refresh() {
            // given
            String token = expiredToken("REFRESH_TOKEN", "123", BASE64_SECRET);

            // when & then
            assertThatThrownBy(() -> jwtService.verifyRefreshToken(token))
                    .isInstanceOf(CustomException.class)
                    .satisfies(ex -> assertThat(((CustomException) ex).getHttpStatus())
                            .isEqualTo(AuthErrorStatus._EXPIRED_REFRESH_TOKEN.getHttpStatus()));
        }

        @Test
        @DisplayName("[TC-VERIFY-ERRORS-003] 서명이 불일치한 Access 토큰을 검증하면 _INVALID_ACCESS_TOKEN 예외가 발생한다")
        void forged_access_should_throw_invalid_access() {
            // given
            String token = forgedToken("ACCESS_TOKEN", "123", OTHER_BASE64_SECRET);

            // when & then
            assertThatThrownBy(() -> jwtService.verifyAccessToken(token))
                    .isInstanceOf(CustomException.class)
                    .satisfies(ex -> assertThat(((CustomException) ex).getHttpStatus())
                            .isEqualTo(AuthErrorStatus._INVALID_ACCESS_TOKEN.getHttpStatus()));
        }

        @Test
        @DisplayName("[TC-VERIFY-ERRORS-004] 서명이 불일치한 Refresh 토큰을 검증하면 _INVALID_REFRESH_TOKEN 예외가 발생한다")
        void forged_refresh_should_throw_invalid_refresh() {
            // given
            String token = forgedToken("REFRESH_TOKEN", "123", OTHER_BASE64_SECRET);

            // when & then
            assertThatThrownBy(() -> jwtService.verifyRefreshToken(token))
                    .isInstanceOf(CustomException.class)
                    .satisfies(ex -> assertThat(((CustomException) ex).getHttpStatus())
                            .isEqualTo(AuthErrorStatus._INVALID_REFRESH_TOKEN.getHttpStatus()));
        }
    }

    @Nested
    @TestMethodOrder(MethodOrderer.DisplayName.class)
    @DisplayName("[SCN-SVC-AUTH-JWT-003] Authorization 헤더에서 userId를 추출할 수 있다.")
    class ExtractUserIdFromHeader {

        @Test
        @DisplayName("[TC-AUTH-HEADER-001] 정상 헤더에서 사용자 ID를 정상적으로 반환한다")
        void extract_valid_header_returns_id() {
            // given
            String token = validAccess("123", BASE64_SECRET);

            // when
            Long id = jwtService.extractUserIdFromToken(bearer(token));

            // then
            assertThat(id).isEqualTo(123L);
        }

        @Test
        @DisplayName("[TC-AUTH-HEADER-EXCEPTION-001] 헤더가 없거나 접두어가 'Bearer '가 아니면 _NOT_EXIST_ACCESS_TOKEN 예외가 발생한다")
        void extract_missing_or_bad_prefix_throws_not_exist() {
            // given
            String noHeader = null;
            String badPrefix = "Token abc";

            // when & then
            assertThatThrownBy(() -> jwtService.extractUserIdFromToken(noHeader))
                    .isInstanceOf(CustomException.class)
                    .satisfies(ex -> assertThat(((CustomException) ex).getHttpStatus())
                            .isEqualTo(AuthErrorStatus._NOT_EXIST_ACCESS_TOKEN.getHttpStatus()));

            assertThatThrownBy(() -> jwtService.extractUserIdFromToken(badPrefix))
                    .isInstanceOf(CustomException.class)
                    .satisfies(ex -> assertThat(((CustomException) ex).getHttpStatus())
                            .isEqualTo(AuthErrorStatus._NOT_EXIST_ACCESS_TOKEN.getHttpStatus()));
        }

        @Test
        @DisplayName("[TC-AUTH-HEADER-EXCEPTION-002] 만료된 토큰이 담긴 헤더를 전달하면 _EXPIRED_ACCESS_TOKEN 예외가 발생한다")
        void extract_with_expired_token_throws_expired() {
            // given
            String expired = expiredToken("ACCESS_TOKEN", "123", BASE64_SECRET);

            // when & then
            assertThatThrownBy(() -> jwtService.extractUserIdFromToken(bearer(expired)))
                    .isInstanceOf(CustomException.class)
                    .satisfies(ex -> assertThat(((CustomException) ex).getHttpStatus())
                            .isEqualTo(AuthErrorStatus._EXPIRED_ACCESS_TOKEN.getHttpStatus()));
        }

        @Test
        @DisplayName("[TC-AUTH-HEADER-EXCEPTION-003] 서명이 불일치한 토큰이 담긴 헤더를 전달하면 _INVALID_ACCESS_TOKEN 예외가 발생한다")
        void extract_with_forged_token_throws_invalid() {
            // given
            String forged = forgedToken("ACCESS_TOKEN", "123", OTHER_BASE64_SECRET);

            // when & then
            assertThatThrownBy(() -> jwtService.extractUserIdFromToken(bearer(forged)))
                    .isInstanceOf(CustomException.class)
                    .satisfies(ex -> assertThat(((CustomException) ex).getHttpStatus())
                            .isEqualTo(AuthErrorStatus._INVALID_ACCESS_TOKEN.getHttpStatus()));
        }
    }
}
