package server.poptato.auth.application;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;
import java.util.Base64;
import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.springframework.test.util.ReflectionTestUtils;

import server.poptato.auth.application.service.JwtService;
import server.poptato.auth.domain.entity.RefreshToken;
import server.poptato.auth.domain.repository.RefreshTokenRepository;
import server.poptato.auth.status.AuthErrorStatus;
import server.poptato.configuration.ServiceTestConfig;
import server.poptato.global.dto.TokenPair;
import server.poptato.global.exception.CustomException;
import server.poptato.user.domain.value.MobileType;

class JwtServiceDbTest extends ServiceTestConfig {

    @Mock
    RefreshTokenRepository refreshTokenRepository;

    @InjectMocks
    JwtService jwtService;

    private static final String RAW_SECRET = "this-is-a-test-secret-at-least-32-bytes";
    private static final String BASE64_SECRET = Base64.getEncoder().encodeToString(RAW_SECRET.getBytes(StandardCharsets.UTF_8));

    @BeforeEach
    void setUp() {
        ReflectionTestUtils.setField(jwtService, "jwtSecret", BASE64_SECRET);
    }

    @Nested
    @DisplayName("[SCN-SVC-JWT-DB-001] generateTokenPair 테스트")
    class GenerateTokenPairTest {

        @Test
        @DisplayName("[TC-JWT-DB-001] generateTokenPair 호출 시 액세스 토큰과 리프레시 토큰이 생성된다")
        void generateTokenPair_returnsTokenPair() {
            // given
            Long userId = 1L;
            MobileType mobileType = MobileType.ANDROID;
            String clientId = "client-123";
            String userIp = "127.0.0.1";
            String userAgent = "TestAgent/1.0";

            when(refreshTokenRepository.save(any(RefreshToken.class)))
                    .thenAnswer(invocation -> invocation.getArgument(0));

            // when
            TokenPair tokenPair = jwtService.generateTokenPair(userId, mobileType, clientId, userIp, userAgent);

            // then
            assertThat(tokenPair.accessToken()).isNotBlank();
            assertThat(tokenPair.refreshToken()).isNotBlank();
            verify(refreshTokenRepository).save(any(RefreshToken.class));
        }

        @Test
        @DisplayName("[TC-JWT-DB-002] generateTokenPair 호출 시 RefreshToken이 DB에 저장된다")
        void generateTokenPair_savesRefreshToken() {
            // given
            Long userId = 2L;
            MobileType mobileType = MobileType.IOS;
            String clientId = "ios-client";
            String userIp = "192.168.1.1";
            String userAgent = "iOS/15.0";

            ArgumentCaptor<RefreshToken> captor = ArgumentCaptor.forClass(RefreshToken.class);
            when(refreshTokenRepository.save(captor.capture()))
                    .thenAnswer(invocation -> invocation.getArgument(0));

            // when
            jwtService.generateTokenPair(userId, mobileType, clientId, userIp, userAgent);

            // then
            RefreshToken savedToken = captor.getValue();
            assertThat(savedToken.getUserId()).isEqualTo(userId);
            assertThat(savedToken.getMobileType()).isEqualTo(mobileType);
            assertThat(savedToken.getClientId()).isEqualTo(clientId);
            assertThat(savedToken.getUserIp()).isEqualTo(userIp);
            assertThat(savedToken.getUserAgent()).isEqualTo(userAgent);
            assertThat(savedToken.isActive()).isTrue();
        }
    }

    @Nested
    @DisplayName("[SCN-SVC-JWT-DB-002] validateAndGetRefreshToken 테스트")
    class ValidateAndGetRefreshTokenTest {

        @Test
        @DisplayName("[TC-JWT-DB-003] 유효한 jti와 refreshToken으로 RefreshToken을 조회한다")
        void validateAndGetRefreshToken_returnsToken() {
            // given
            String jti = "test-jti";
            String refreshToken = "test-refresh-token";

            RefreshToken storedToken = RefreshToken.create(
                    1L, jti, MobileType.ANDROID, "client", refreshToken,
                    LocalDateTime.now(), LocalDateTime.now().plusDays(14), "127.0.0.1", "Agent"
            );

            when(refreshTokenRepository.findByJti(jti)).thenReturn(Optional.of(storedToken));

            // when
            RefreshToken result = jwtService.validateAndGetRefreshToken(jti, refreshToken);

            // then
            assertThat(result).isEqualTo(storedToken);
        }

        @Test
        @DisplayName("[TC-JWT-DB-004] jti로 조회된 토큰이 없으면 _EXPIRED_OR_NOT_FOUND_REFRESH_TOKEN 예외가 발생한다")
        void validateAndGetRefreshToken_notFound_throwsException() {
            // given
            String jti = "non-existent-jti";
            String refreshToken = "token";

            when(refreshTokenRepository.findByJti(jti)).thenReturn(Optional.empty());

            // when & then
            assertThatThrownBy(() -> jwtService.validateAndGetRefreshToken(jti, refreshToken))
                    .isInstanceOf(CustomException.class)
                    .satisfies(ex -> assertThat(((CustomException) ex).getErrorCode())
                            .isEqualTo(AuthErrorStatus._EXPIRED_OR_NOT_FOUND_REFRESH_TOKEN));
        }

        @Test
        @DisplayName("[TC-JWT-DB-005] 토큰이 ACTIVE 상태가 아니면 _ALREADY_USED_REFRESH_TOKEN 예외가 발생한다")
        void validateAndGetRefreshToken_notActive_throwsException() {
            // given
            String jti = "test-jti";
            String refreshToken = "test-refresh-token";

            RefreshToken storedToken = RefreshToken.create(
                    1L, jti, MobileType.ANDROID, "client", refreshToken,
                    LocalDateTime.now(), LocalDateTime.now().plusDays(14), "127.0.0.1", "Agent"
            );
            storedToken.markAsRotated();

            when(refreshTokenRepository.findByJti(jti)).thenReturn(Optional.of(storedToken));

            // when & then
            assertThatThrownBy(() -> jwtService.validateAndGetRefreshToken(jti, refreshToken))
                    .isInstanceOf(CustomException.class)
                    .satisfies(ex -> assertThat(((CustomException) ex).getErrorCode())
                            .isEqualTo(AuthErrorStatus._ALREADY_USED_REFRESH_TOKEN));
        }

        @Test
        @DisplayName("[TC-JWT-DB-006] 저장된 토큰과 입력 토큰이 다르면 _DIFFERENT_REFRESH_TOKEN 예외가 발생한다")
        void validateAndGetRefreshToken_differentToken_throwsException() {
            // given
            String jti = "test-jti";
            String storedRefreshToken = "stored-refresh-token";
            String inputRefreshToken = "different-refresh-token";

            RefreshToken storedToken = RefreshToken.create(
                    1L, jti, MobileType.ANDROID, "client", storedRefreshToken,
                    LocalDateTime.now(), LocalDateTime.now().plusDays(14), "127.0.0.1", "Agent"
            );

            when(refreshTokenRepository.findByJti(jti)).thenReturn(Optional.of(storedToken));

            // when & then
            assertThatThrownBy(() -> jwtService.validateAndGetRefreshToken(jti, inputRefreshToken))
                    .isInstanceOf(CustomException.class)
                    .satisfies(ex -> assertThat(((CustomException) ex).getErrorCode())
                            .isEqualTo(AuthErrorStatus._DIFFERENT_REFRESH_TOKEN));
        }
    }

    @Nested
    @DisplayName("[SCN-SVC-JWT-DB-003] rotateToken 테스트")
    class RotateTokenTest {

        @Test
        @DisplayName("[TC-JWT-DB-007] rotateToken 호출 시 기존 토큰이 ROTATED로 변경되고 새 토큰이 생성된다")
        void rotateToken_updatesOldAndCreatesNew() {
            // given
            RefreshToken oldToken = RefreshToken.create(
                    1L, "old-jti", MobileType.ANDROID, "client", "old-refresh-token",
                    LocalDateTime.now().minusDays(7), LocalDateTime.now().plusDays(7), "127.0.0.1", "Agent"
            );
            ReflectionTestUtils.setField(oldToken, "id", 100L);

            when(refreshTokenRepository.save(any(RefreshToken.class)))
                    .thenAnswer(invocation -> invocation.getArgument(0));

            // when
            TokenPair result = jwtService.rotateToken(oldToken, "192.168.1.1", "NewAgent");

            // then
            assertThat(result.accessToken()).isNotBlank();
            assertThat(result.refreshToken()).isNotBlank();
            assertThat(oldToken.isActive()).isFalse();
            verify(refreshTokenRepository).save(oldToken);
        }

        @Test
        @DisplayName("[TC-JWT-DB-008] rotateToken 호출 시 새 토큰이 DB에 저장된다")
        void rotateToken_savesNewToken() {
            // given
            RefreshToken oldToken = RefreshToken.create(
                    1L, "old-jti", MobileType.IOS, "client", "old-refresh-token",
                    LocalDateTime.now().minusDays(7), LocalDateTime.now().plusDays(7), "127.0.0.1", "Agent"
            );
            ReflectionTestUtils.setField(oldToken, "id", 100L);

            ArgumentCaptor<RefreshToken> captor = ArgumentCaptor.forClass(RefreshToken.class);
            when(refreshTokenRepository.save(captor.capture()))
                    .thenAnswer(invocation -> invocation.getArgument(0));

            // when
            jwtService.rotateToken(oldToken, "192.168.1.1", "NewAgent");

            // then
            // save가 2번 호출됨: 1) oldToken 업데이트, 2) newToken 저장
            assertThat(captor.getAllValues()).hasSize(2);
            RefreshToken newToken = captor.getAllValues().get(1);
            assertThat(newToken.getParentId()).isEqualTo(100L);
            assertThat(newToken.isActive()).isTrue();
        }
    }

    @Nested
    @DisplayName("[SCN-SVC-JWT-DB-004] revokeRefreshToken 테스트")
    class RevokeRefreshTokenTest {

        @Test
        @DisplayName("[TC-JWT-DB-009] revokeRefreshToken 호출 시 repository의 revokeByUserIdAndMobileType이 호출된다")
        void revokeRefreshToken_callsRepository() {
            // given
            Long userId = 1L;
            MobileType mobileType = MobileType.ANDROID;

            doNothing().when(refreshTokenRepository).revokeByUserIdAndMobileType(userId, mobileType);

            // when
            jwtService.revokeRefreshToken(userId, mobileType);

            // then
            verify(refreshTokenRepository).revokeByUserIdAndMobileType(userId, mobileType);
        }
    }

    @Nested
    @DisplayName("[SCN-SVC-JWT-DB-005] revokeAllRefreshTokens 테스트")
    class RevokeAllRefreshTokensTest {

        @Test
        @DisplayName("[TC-JWT-DB-010] revokeAllRefreshTokens 호출 시 repository의 revokeAllByUserId가 호출된다")
        void revokeAllRefreshTokens_callsRepository() {
            // given
            Long userId = 1L;

            doNothing().when(refreshTokenRepository).revokeAllByUserId(userId);

            // when
            jwtService.revokeAllRefreshTokens(userId);

            // then
            verify(refreshTokenRepository).revokeAllByUserId(userId);
        }
    }
}
