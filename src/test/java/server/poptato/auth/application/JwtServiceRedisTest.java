package server.poptato.auth.application;

import io.lettuce.core.RedisCommandTimeoutException;
import org.junit.jupiter.api.*;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.springframework.data.redis.RedisConnectionFailureException;
import org.springframework.test.util.ReflectionTestUtils;
import server.poptato.auth.application.service.JwtService;
import server.poptato.auth.infra.JwtRepository;
import server.poptato.auth.status.AuthErrorStatus;
import server.poptato.configuration.ServiceTestConfig;
import server.poptato.global.dto.TokenPair;
import server.poptato.global.exception.CustomException;
import server.poptato.user.domain.value.MobileType;

import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.Base64;
import java.util.Optional;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

class JwtServiceRedisTest extends ServiceTestConfig {

    @Mock
    JwtRepository jwtRepository;

    @InjectMocks
    private JwtService jwtService;

    private static final String RAW_SECRET = "this-is-a-test-secret-at-least-32-bytes";
    private static final String BASE64_SECRET =
            Base64.getEncoder().encodeToString(RAW_SECRET.getBytes(StandardCharsets.UTF_8));

    @BeforeEach
    void setUp() {
        // 실제 환경에서는 @PostConstruct에서 Base64 인코딩을 수행하지만,
        // 테스트에서는 이미 인코딩된 값을 주입하여 서명 키를 동일하게 맞춘다.
        ReflectionTestUtils.setField(jwtService, "jwtSecret", BASE64_SECRET);
    }

    @Nested
    @TestMethodOrder(MethodOrderer.DisplayName.class)
    @DisplayName("[SCN-SVC-AUTH-JWT-004] 토큰 페어를 발급받고 Redis에 저장/비교/장애를 처리할 수 있다.")
    class TokenPairRedis {

        @Test
        @DisplayName("[TC-REDIS-001] 토큰 페어 생성 과정에서 Redis에 Refresh 토큰이 userId + MobileType 기준으로 보관된다")
        void generateTokenPair_savesRefreshToRedis() {
            // given
            String userId = "123";
            MobileType mobileType = MobileType.ANDROID;

            // when
            TokenPair pair = jwtService.generateTokenPair(userId, mobileType);

            // then
            verify(jwtRepository).saveRefreshToken(
                    eq(userId),
                    eq(mobileType),
                    eq(pair.refreshToken()),
                    eq(Duration.ofDays(14))
            );

            assertThat(pair.refreshToken()).isNotBlank();
            assertThat(pair.accessToken()).isNotBlank();
        }

        @Test
        @DisplayName("[TC-REDIS-002] Redis 저장값과 입력 Refresh 토큰이 같으면 비교 검증이 통과한다")
        void compareRefreshToken_match() {
            // given
            String userId = "123";
            MobileType mobileType = MobileType.ANDROID;
            when(jwtRepository.findRefreshToken(userId, mobileType)).thenReturn(Optional.of("refresh-token"));

            // when & then
            assertThatCode(() -> jwtService.compareRefreshToken(userId, mobileType, "refresh-token"))
                    .doesNotThrowAnyException();
        }

        @Test
        @DisplayName("[TC-REDIS-003] Refresh 토큰을 삭제하면 Redis에서 userId + MobileType 키가 삭제된다")
        void deleteRefreshToken_deletesKey() {
            // given
            String userId = "123";
            MobileType mobileType = MobileType.ANDROID;

            // when
            jwtService.deleteRefreshToken(userId, mobileType);

            // then
            verify(jwtRepository).deleteRefreshToken(userId, mobileType);
        }

        @Test
        @DisplayName("[TC-REDIS-EXCEPTION-001] Redis에 저장된 Refresh 토큰이 없으면 _EXPIRED_OR_NOT_FOUND_REFRESH_TOKEN_IN_REDIS 예외가 발생한다")
        void compareRefreshToken_notFound() {
            // given
            String userId = "123";
            MobileType mobileType = MobileType.ANDROID;
            when(jwtRepository.findRefreshToken(userId, mobileType)).thenReturn(Optional.empty());

            // when & then
            assertThatThrownBy(() -> jwtService.compareRefreshToken(userId, mobileType, "x"))
                    .isInstanceOf(CustomException.class)
                    .satisfies(ex -> assertThat(((CustomException) ex).getHttpStatus())
                            .isEqualTo(AuthErrorStatus._EXPIRED_OR_NOT_FOUND_REFRESH_TOKEN_IN_REDIS.getHttpStatus()));
        }

        @Test
        @DisplayName("[TC-REDIS-EXCEPTION-002] Redis 저장값과 입력 Refresh 토큰이 다르면 _DIFFERENT_REFRESH_TOKEN 예외가 발생한다")
        void compareRefreshToken_different() {
            // given
            String userId = "123";
            MobileType mobileType = MobileType.ANDROID;
            when(jwtRepository.findRefreshToken(userId, mobileType)).thenReturn(Optional.of("stored"));

            // when & then
            assertThatThrownBy(() -> jwtService.compareRefreshToken(userId, mobileType, "input"))
                    .isInstanceOf(CustomException.class)
                    .satisfies(ex -> assertThat(((CustomException) ex).getHttpStatus())
                            .isEqualTo(AuthErrorStatus._DIFFERENT_REFRESH_TOKEN.getHttpStatus()));
        }

        @Test
        @DisplayName("[TC-REDIS-EXCEPTION-003] RedisConnectionFailureException이 발생하면 _REDIS_UNAVAILABLE 예외가 발생한다")
        void compareRefreshToken_redisDown_connectionFailure() {
            // given
            String userId = "123";
            MobileType mobileType = MobileType.ANDROID;
            when(jwtRepository.findRefreshToken(userId, mobileType))
                    .thenThrow(new RedisConnectionFailureException("down"));

            // when & then
            assertThatThrownBy(() -> jwtService.compareRefreshToken(userId, mobileType, "r"))
                    .isInstanceOf(CustomException.class)
                    .satisfies(ex -> assertThat(((CustomException) ex).getHttpStatus())
                            .isEqualTo(AuthErrorStatus._REDIS_UNAVAILABLE.getHttpStatus()));
        }

        @Test
        @DisplayName("[TC-REDIS-EXCEPTION-004] RedisCommandTimeoutException이 발생하면 _REDIS_UNAVAILABLE 예외가 발생한다")
        void compareRefreshToken_redisDown_timeout() {
            // given
            String userId = "123";
            MobileType mobileType = MobileType.ANDROID;
            when(jwtRepository.findRefreshToken(userId, mobileType))
                    .thenThrow(new RedisCommandTimeoutException("timeout"));

            // when & then
            assertThatThrownBy(() -> jwtService.compareRefreshToken(userId, mobileType, "r"))
                    .isInstanceOf(CustomException.class)
                    .satisfies(ex -> assertThat(((CustomException) ex).getHttpStatus())
                            .isEqualTo(AuthErrorStatus._REDIS_UNAVAILABLE.getHttpStatus()));
        }
    }
}
