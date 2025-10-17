package server.poptato.auth.application;

import org.junit.jupiter.api.*;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.springframework.data.redis.RedisConnectionFailureException;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.ValueOperations;
import org.springframework.test.util.ReflectionTestUtils;
import server.poptato.auth.application.service.JwtService;
import server.poptato.auth.status.AuthErrorStatus;
import server.poptato.configuration.ServiceTestConfig;
import server.poptato.global.dto.TokenPair;
import server.poptato.global.exception.CustomException;

import java.nio.charset.StandardCharsets;
import java.util.Base64;
import java.util.concurrent.TimeUnit;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

class JwtServiceRedisTest extends ServiceTestConfig {

    @Mock
    StringRedisTemplate stringRedisTemplate;

    @Mock
    ValueOperations<String, String> valueOps;

    @InjectMocks
    private JwtService jwtService;

    private static final String RAW_SECRET = "this-is-a-test-secret-at-least-32-bytes";
    private static final String BASE64_SECRET =
            Base64.getEncoder().encodeToString(RAW_SECRET.getBytes(StandardCharsets.UTF_8));

    @BeforeEach
    void setUp() {
        when(stringRedisTemplate.opsForValue()).thenReturn(valueOps);
        ReflectionTestUtils.setField(jwtService, "jwtSecret", BASE64_SECRET);
    }

    @Nested
    @TestMethodOrder(MethodOrderer.DisplayName.class)
    @DisplayName("[SCN-SVC-AUTH-JWT-004] 토큰 페어를 발급받고 Redis에 저장/비교/장애를 처리할 수 있다.")
    class TokenPairRedis {

        @Test
        @DisplayName("[TC-REDIS-001] 토큰 페어 생성 과정에서 Redis에 Refresh 토큰이 userId 기준으로 14일간 보관된다")
        void generateTokenPair_savesRefreshToRedis() {
            // given
            String userId = "123";

            // when
            TokenPair pair = jwtService.generateTokenPair(userId);

            // then
            ArgumentCaptor<String> refreshCaptor = ArgumentCaptor.forClass(String.class);
            verify(valueOps).set(
                    eq(userId),
                    refreshCaptor.capture(),
                    eq((long) JwtService.REFRESH_TOKEN_EXPIRATION_DAYS),
                    eq(TimeUnit.DAYS)
            );
            assertThat(refreshCaptor.getValue()).isNotBlank();
            assertThat(pair.refreshToken()).isNotBlank();
            assertThat(pair.accessToken()).isNotBlank();
        }

        @Test
        @DisplayName("[TC-REDIS-002] Redis 저장값과 입력 Refresh 토큰이 같으면 비교 검증이 통과한다")
        void compareRefreshToken_match() {
            // given
            when(valueOps.get("123")).thenReturn("refresh-token");

            // when & then
            assertThatCode(() -> jwtService.compareRefreshToken("123", "r-token"))
                    .doesNotThrowAnyException();
        }

        @Test
        @DisplayName("[TC-REDIS-003] Refresh 토큰을 삭제하면 Redis에서 해당 키가 삭제된다")
        void deleteRefreshToken_deletesKey() {
            // given
            reset(stringRedisTemplate);

            // when
            jwtService.deleteRefreshToken("123");

            // then
            verify(stringRedisTemplate).delete("123");
        }

        @Test
        @DisplayName("[TC-REDIS-EXCEPTION-001] Redis에 저장된 Refresh 토큰이 없으면 _EXPIRED_OR_NOT_FOUND_REFRESH_TOKEN_IN_REDIS 예외가 발생한다")
        void compareRefreshToken_notFound() {
            // given
            when(valueOps.get("123")).thenReturn(null);

            // when & then
            assertThatThrownBy(() -> jwtService.compareRefreshToken("123", "x"))
                    .isInstanceOf(CustomException.class)
                    .satisfies(ex -> assertThat(((CustomException) ex).getHttpStatus())
                            .isEqualTo(AuthErrorStatus._EXPIRED_OR_NOT_FOUND_REFRESH_TOKEN_IN_REDIS.getHttpStatus()));
        }

        @Test
        @DisplayName("[TC-REDIS-EXCEPTION-002] Redis 저장값과 입력 Refresh 토큰이 다르면 _DIFFERENT_REFRESH_TOKEN 예외가 발생한다")
        void compareRefreshToken_different() {
            // given
            when(valueOps.get("123")).thenReturn("stored");

            // when & then
            assertThatThrownBy(() -> jwtService.compareRefreshToken("123", "input"))
                    .isInstanceOf(CustomException.class)
                    .satisfies(ex -> assertThat(((CustomException) ex).getHttpStatus())
                            .isEqualTo(AuthErrorStatus._DIFFERENT_REFRESH_TOKEN.getHttpStatus()));
        }

        @Test
        @DisplayName("[TC-REDIS-EXCEPTION-003] RedisConnectionFailureException이 발생하면 _REDIS_UNAVAILABLE 예외가 발생한다")
        void compareRefreshToken_redisDown_connectionFailure() {
            // given
            when(valueOps.get("123")).thenThrow(new RedisConnectionFailureException("down"));

            // when & then
            assertThatThrownBy(() -> jwtService.compareRefreshToken("123", "r"))
                    .isInstanceOf(CustomException.class)
                    .satisfies(ex -> assertThat(((CustomException) ex).getHttpStatus())
                            .isEqualTo(AuthErrorStatus._REDIS_UNAVAILABLE.getHttpStatus()));
        }

        @Test
        @DisplayName("[TC-REDIS-EXCEPTION-004] RedisCommandTimeoutException이 발생하면 _REDIS_UNAVAILABLE 예외가 발생한다")
        void compareRefreshToken_redisDown_timeout() {
            // given
            when(valueOps.get("123")).thenThrow(new io.lettuce.core.RedisCommandTimeoutException("timeout"));

            // when & then
            assertThatThrownBy(() -> jwtService.compareRefreshToken("123", "r"))
                    .isInstanceOf(CustomException.class)
                    .satisfies(ex -> assertThat(((CustomException) ex).getHttpStatus())
                            .isEqualTo(AuthErrorStatus._REDIS_UNAVAILABLE.getHttpStatus()));
        }
    }
}
