package server.poptato.auth.application.scheduler;

import java.time.LocalDateTime;

import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import server.poptato.auth.domain.repository.RefreshTokenRepository;

/**
 * Refresh Token 정리 스케줄러
 * - 만료된 토큰 상태 업데이트
 * - 오래된 비활성 토큰 물리 삭제
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class RefreshTokenCleanupScheduler {

    private static final int RETENTION_DAYS = 30;

    private final RefreshTokenRepository refreshTokenRepository;

    /**
     * 만료된 토큰 상태 업데이트
     * ACTIVE 상태이면서 expiry_at이 지난 토큰을 EXPIRED로 변경한다.
     */
    @Scheduled(cron = "${scheduling.refreshTokenExpiredCron}")
    @Transactional
    public void updateExpiredTokens() {
        LocalDateTime now = LocalDateTime.now();
        int updatedCount = refreshTokenRepository.updateExpiredTokens(now);
        log.info("[RefreshToken Cleanup] 만료 토큰 상태 업데이트: {}건", updatedCount);
    }

    /**
     * 오래된 비활성 토큰 물리 삭제
     * REVOKED, EXPIRED, ROTATED 상태이면서 30일 이상 지난 토큰을 물리 삭제한다.
     */
    @Scheduled(cron = "${scheduling.refreshTokenHardDeleteCron}")
    @Transactional
    public void hardDeleteOldInactiveTokens() {
        LocalDateTime threshold = LocalDateTime.now().minusDays(RETENTION_DAYS);
        int deletedCount = refreshTokenRepository.hardDeleteOldInactiveTokens(threshold);
        log.info("[RefreshToken Cleanup] 오래된 토큰 물리 삭제: {}건", deletedCount);
    }
}
