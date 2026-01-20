package server.poptato.auth.application;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

import java.time.LocalDateTime;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;

import server.poptato.auth.application.scheduler.RefreshTokenCleanupScheduler;
import server.poptato.auth.domain.repository.RefreshTokenRepository;
import server.poptato.configuration.ServiceTestConfig;

class RefreshTokenCleanupSchedulerTest extends ServiceTestConfig {

    @Mock
    RefreshTokenRepository refreshTokenRepository;

    @InjectMocks
    RefreshTokenCleanupScheduler scheduler;

    @Nested
    @DisplayName("[SCN-SVC-SCHEDULER-001] 만료 토큰 상태 업데이트")
    class UpdateExpiredTokensTest {

        @Test
        @DisplayName("[TC-SCHEDULER-001] updateExpiredTokens 호출 시 repository의 updateExpiredTokens가 호출된다")
        void updateExpiredTokens_callsRepository() {
            // given
            when(refreshTokenRepository.updateExpiredTokens(any(LocalDateTime.class))).thenReturn(5);

            // when
            scheduler.updateExpiredTokens();

            // then
            ArgumentCaptor<LocalDateTime> captor = ArgumentCaptor.forClass(LocalDateTime.class);
            verify(refreshTokenRepository).updateExpiredTokens(captor.capture());
            assertThat(captor.getValue()).isNotNull();
        }

        @Test
        @DisplayName("[TC-SCHEDULER-002] updateExpiredTokens가 업데이트 건수를 정상적으로 처리한다")
        void updateExpiredTokens_handlesCount() {
            // given
            when(refreshTokenRepository.updateExpiredTokens(any(LocalDateTime.class))).thenReturn(10);

            // when
            scheduler.updateExpiredTokens();

            // then
            verify(refreshTokenRepository).updateExpiredTokens(any(LocalDateTime.class));
        }
    }

    @Nested
    @DisplayName("[SCN-SVC-SCHEDULER-002] 오래된 비활성 토큰 물리 삭제")
    class HardDeleteOldInactiveTokensTest {

        @Test
        @DisplayName("[TC-SCHEDULER-003] hardDeleteOldInactiveTokens 호출 시 30일 전 기준으로 repository 메서드가 호출된다")
        void hardDeleteOldInactiveTokens_callsRepositoryWithThreshold() {
            // given
            when(refreshTokenRepository.hardDeleteOldInactiveTokens(any(LocalDateTime.class))).thenReturn(3);

            // when
            LocalDateTime before = LocalDateTime.now().minusDays(30);
            scheduler.hardDeleteOldInactiveTokens();
            LocalDateTime after = LocalDateTime.now().minusDays(30);

            // then
            ArgumentCaptor<LocalDateTime> captor = ArgumentCaptor.forClass(LocalDateTime.class);
            verify(refreshTokenRepository).hardDeleteOldInactiveTokens(captor.capture());

            LocalDateTime captured = captor.getValue();
            assertThat(captured).isAfterOrEqualTo(before.minusSeconds(1));
            assertThat(captured).isBeforeOrEqualTo(after.plusSeconds(1));
        }

        @Test
        @DisplayName("[TC-SCHEDULER-004] hardDeleteOldInactiveTokens가 삭제 건수를 정상적으로 처리한다")
        void hardDeleteOldInactiveTokens_handlesCount() {
            // given
            when(refreshTokenRepository.hardDeleteOldInactiveTokens(any(LocalDateTime.class))).thenReturn(0);

            // when
            scheduler.hardDeleteOldInactiveTokens();

            // then
            verify(refreshTokenRepository).hardDeleteOldInactiveTokens(any(LocalDateTime.class));
        }
    }
}
