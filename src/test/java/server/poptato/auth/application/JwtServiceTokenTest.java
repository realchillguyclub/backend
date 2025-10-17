package server.poptato.auth.application;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Header;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import org.junit.jupiter.api.*;
import org.mockito.InjectMocks;
import org.springframework.test.util.ReflectionTestUtils;
import server.poptato.auth.application.service.JwtService;
import server.poptato.auth.status.AuthErrorStatus;
import server.poptato.configuration.ServiceTestConfig;
import server.poptato.global.exception.CustomException;

import java.nio.charset.StandardCharsets;
import java.util.Base64;
import java.util.Date;

import static org.assertj.core.api.Assertions.*;

class JwtServiceTokenTest extends ServiceTestConfig {

    @InjectMocks
    private JwtService jwtService;

    private static final String RAW_SECRET = "this-is-a-test-secret-at-least-32-bytes";
    private static final String OTHER_RAW_SECRET = "another-different-test-secret-32-bytes";
    private static final String BASE64_SECRET = Base64.getEncoder().encodeToString(RAW_SECRET.getBytes(StandardCharsets.UTF_8));
    private static final String OTHER_BASE64_SECRET = Base64.getEncoder().encodeToString(OTHER_RAW_SECRET.getBytes(StandardCharsets.UTF_8));

    private static String bearer(String token) {
        return "Bearer " + token;
    }

    private static String validAccess(String userId, String base64Secret) {
        Date now = new Date();
        Date exp = new Date(now.getTime()
                + JwtService.ACCESS_TOKEN_EXPIRATION_MINUTE * JwtService.MINUTE_IN_MILLISECONDS);
        Claims c = Jwts.claims()
                .setSubject("ACCESS_TOKEN")
                .setIssuedAt(now)
                .setExpiration(exp);
        c.put("USER_ID", userId);

        return Jwts.builder()
                .setHeaderParam(Header.TYPE, Header.JWT_TYPE)
                .setClaims(c)
                .signWith(Keys.hmacShaKeyFor(base64Secret.getBytes(StandardCharsets.UTF_8)))
                .compact();
    }

    private static String expiredToken(String subject, String userId, String base64Secret) {
        long now = System.currentTimeMillis();
        Date past = new Date(now - 1_000L);
        Claims c = Jwts.claims()
                .setSubject(subject)
                .setIssuedAt(new Date(past.getTime() - 1_000L))
                .setExpiration(past);
        c.put("USER_ID", userId);

        return Jwts.builder()
                .setHeaderParam(Header.TYPE, Header.JWT_TYPE)
                .setClaims(c)
                .signWith(Keys.hmacShaKeyFor(base64Secret.getBytes(StandardCharsets.UTF_8)))
                .compact();
    }

    private static String forgedToken(String subject, String userId, String otherBase64Secret) {
        Date now = new Date();
        Date exp = new Date(now.getTime()
                + JwtService.ACCESS_TOKEN_EXPIRATION_MINUTE * JwtService.MINUTE_IN_MILLISECONDS);
        Claims c = Jwts.claims()
                .setSubject(subject)
                .setIssuedAt(now)
                .setExpiration(exp);
        c.put("USER_ID", userId);

        return Jwts.builder()
                .setHeaderParam(Header.TYPE, Header.JWT_TYPE)
                .setClaims(c)
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
        @DisplayName("[TC-CREATE-TOKEN-002] Refresh 토큰 생성/검증 통과 후 userId를 정상적으로 추출한다")
        void refreshToken_create_verify_parse_userId() {
            // given
            String userId = "123";

            // when
            String refreshToken = jwtService.createRefreshToken(userId);

            // then
            assertThatCode(() -> jwtService.verifyRefreshToken(refreshToken)).doesNotThrowAnyException();
            assertThat(jwtService.getUserIdInToken(refreshToken)).isEqualTo(userId);
        }
    }
}
