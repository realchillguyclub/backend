package server.poptato.auth.domain;

import static org.assertj.core.api.Assertions.*;

import java.time.LocalDateTime;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

import server.poptato.auth.domain.entity.RefreshToken;
import server.poptato.auth.domain.value.TokenStatus;
import server.poptato.configuration.ServiceTestConfig;
import server.poptato.user.domain.value.MobileType;

class RefreshTokenTest extends ServiceTestConfig {

    @Nested
    @DisplayName("[SCN-DOMAIN-REFRESH-001] RefreshToken.create 테스트")
    class CreateTest {

        @Test
        @DisplayName("[TC-REFRESH-001] create 호출 시 ACTIVE 상태의 RefreshToken이 생성된다")
        void create_returnsActiveToken() {
            // given
            Long userId = 1L;
            String jti = "test-jti";
            MobileType mobileType = MobileType.ANDROID;
            String clientId = "client-123";
            String refreshToken = "refresh-token-value";
            LocalDateTime issuedAt = LocalDateTime.now();
            LocalDateTime expiryAt = issuedAt.plusDays(14);
            String userIp = "127.0.0.1";
            String userAgent = "TestAgent/1.0";

            // when
            RefreshToken token = RefreshToken.create(
                    userId, jti, mobileType, clientId, refreshToken,
                    issuedAt, expiryAt, userIp, userAgent
            );

            // then
            assertThat(token.getUserId()).isEqualTo(userId);
            assertThat(token.getJti()).isEqualTo(jti);
            assertThat(token.getMobileType()).isEqualTo(mobileType);
            assertThat(token.getClientId()).isEqualTo(clientId);
            assertThat(token.getTokenValue()).isEqualTo(refreshToken);
            assertThat(token.getIssuedAt()).isEqualTo(issuedAt);
            assertThat(token.getExpiryAt()).isEqualTo(expiryAt);
            assertThat(token.getUserIp()).isEqualTo(userIp);
            assertThat(token.getUserAgent()).isEqualTo(userAgent);
            assertThat(token.getReissueCount()).isZero();
            assertThat(token.isActive()).isTrue();
        }

        @Test
        @DisplayName("[TC-REFRESH-002] create 호출 시 familyId가 UUID 형식으로 생성된다")
        void create_familyIdIsGenerated() {
            // given
            LocalDateTime now = LocalDateTime.now();

            // when
            RefreshToken token = RefreshToken.create(
                    1L, "jti", MobileType.IOS, "client", "token",
                    now, now.plusDays(14), "127.0.0.1", "Agent"
            );

            // then
            assertThat(token.getFamilyId()).isNotNull();
            assertThat(token.getFamilyId()).hasSize(36); // UUID format: 8-4-4-4-12 = 36 chars
        }
    }

    @Nested
    @DisplayName("[SCN-DOMAIN-REFRESH-002] RefreshToken.rotate 테스트")
    class RotateTest {

        @Test
        @DisplayName("[TC-REFRESH-003] rotate 호출 시 동일한 familyId를 가진 새 토큰이 반환된다")
        void rotate_keepsFamilyId() {
            // given
            LocalDateTime now = LocalDateTime.now();
            RefreshToken oldToken = RefreshToken.create(
                    1L, "old-jti", MobileType.ANDROID, "client-123", "old-token",
                    now.minusDays(7), now.plusDays(7), "127.0.0.1", "OldAgent"
            );
            ReflectionTestUtils.setField(oldToken, "id", 100L);

            String newJti = "new-jti";
            String newRefreshToken = "new-token";
            LocalDateTime newIssuedAt = LocalDateTime.now();
            LocalDateTime newExpiryAt = newIssuedAt.plusDays(14);

            // when
            RefreshToken newToken = oldToken.rotate(newJti, newRefreshToken, newIssuedAt, newExpiryAt, "192.168.1.1", "NewAgent");

            // then
            assertThat(newToken.getFamilyId()).isEqualTo(oldToken.getFamilyId());
            assertThat(newToken.getUserId()).isEqualTo(oldToken.getUserId());
            assertThat(newToken.getMobileType()).isEqualTo(oldToken.getMobileType());
            assertThat(newToken.getClientId()).isEqualTo(oldToken.getClientId());
        }

        @Test
        @DisplayName("[TC-REFRESH-004] rotate 호출 시 reissueCount가 1 증가한다")
        void rotate_incrementsReissueCount() {
            // given
            LocalDateTime now = LocalDateTime.now();
            RefreshToken oldToken = RefreshToken.create(
                    1L, "old-jti", MobileType.IOS, "client", "old-token",
                    now.minusDays(7), now.plusDays(7), "127.0.0.1", "Agent"
            );
            ReflectionTestUtils.setField(oldToken, "id", 1L);

            // when
            RefreshToken newToken = oldToken.rotate("new-jti", "new-token", now, now.plusDays(14), "127.0.0.1", "Agent");

            // then
            assertThat(newToken.getReissueCount()).isEqualTo(oldToken.getReissueCount() + 1);
        }

        @Test
        @DisplayName("[TC-REFRESH-005] rotate로 생성된 토큰은 ACTIVE 상태이다")
        void rotate_newTokenIsActive() {
            // given
            LocalDateTime now = LocalDateTime.now();
            RefreshToken oldToken = RefreshToken.create(
                    1L, "old-jti", MobileType.DESKTOP, null, "old-token",
                    now.minusDays(7), now.plusDays(7), "127.0.0.1", "Agent"
            );
            ReflectionTestUtils.setField(oldToken, "id", 1L);

            // when
            RefreshToken newToken = oldToken.rotate("new-jti", "new-token", now, now.plusDays(14), "127.0.0.1", "Agent");

            // then
            assertThat(newToken.isActive()).isTrue();
        }
    }

    @Nested
    @DisplayName("[SCN-DOMAIN-REFRESH-003] RefreshToken.markAsRotated 테스트")
    class MarkAsRotatedTest {

        @Test
        @DisplayName("[TC-REFRESH-006] markAsRotated 호출 시 상태가 ROTATED로 변경되고 사용 이력이 기록된다")
        void markAsRotated_changesStatusToRotatedAndRecordsUsage() {
            // given
            LocalDateTime now = LocalDateTime.now();
            RefreshToken token = RefreshToken.create(
                    1L, "jti", MobileType.ANDROID, "client", "token",
                    now, now.plusDays(14), "127.0.0.1", "Agent"
            );
            assertThat(token.isActive()).isTrue();

            // when
            token.markAsRotated("192.168.1.1");

            // then
            assertThat(token.isActive()).isFalse();
            assertThat(token.getLastUsedAt()).isNotNull();
            assertThat(token.getLastUsedIp()).isEqualTo("192.168.1.1");
        }
    }

    @Nested
    @DisplayName("[SCN-DOMAIN-REFRESH-004] RefreshToken.isActive 테스트")
    class IsActiveTest {

        @Test
        @DisplayName("[TC-REFRESH-007] ACTIVE 상태의 토큰은 isActive가 true를 반환한다")
        void isActive_activeToken_returnsTrue() {
            // given
            LocalDateTime now = LocalDateTime.now();
            RefreshToken token = RefreshToken.create(
                    1L, "jti", MobileType.ANDROID, "client", "token",
                    now, now.plusDays(14), "127.0.0.1", "Agent"
            );

            // when & then
            assertThat(token.isActive()).isTrue();
        }

        @Test
        @DisplayName("[TC-REFRESH-008] ROTATED 상태의 토큰은 isActive가 false를 반환한다")
        void isActive_rotatedToken_returnsFalse() {
            // given
            LocalDateTime now = LocalDateTime.now();
            RefreshToken token = RefreshToken.create(
                    1L, "jti", MobileType.ANDROID, "client", "token",
                    now, now.plusDays(14), "127.0.0.1", "Agent"
            );
            token.markAsRotated("127.0.0.1");

            // when & then
            assertThat(token.isActive()).isFalse();
        }

        @Test
        @DisplayName("[TC-REFRESH-009] 상태가 REVOKED인 토큰은 isActive가 false를 반환한다")
        void isActive_revokedToken_returnsFalse() {
            // given
            LocalDateTime now = LocalDateTime.now();
            RefreshToken token = RefreshToken.create(
                    1L, "jti", MobileType.ANDROID, "client", "token",
                    now, now.plusDays(14), "127.0.0.1", "Agent"
            );
            ReflectionTestUtils.setField(token, "status", TokenStatus.REVOKED);

            // when & then
            assertThat(token.isActive()).isFalse();
        }
    }
}
