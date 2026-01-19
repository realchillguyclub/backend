package server.poptato.auth.domain.repository;

import java.time.LocalDateTime;
import java.util.Optional;

import server.poptato.auth.domain.entity.RefreshToken;
import server.poptato.user.domain.value.MobileType;

public interface RefreshTokenRepository {

    RefreshToken save(RefreshToken refreshToken);

    Optional<RefreshToken> findByJti(String jti);

    void revokeByUserIdAndMobileType(Long userId, MobileType mobileType);

    void revokeAllByUserId(Long userId);

    int updateExpiredTokens(LocalDateTime now);

    int hardDeleteOldInactiveTokens(LocalDateTime threshold);

    void revokeAllByFamilyId(String familyId);

    /**
     * 토큰을 원자적으로 ROTATED 상태로 변경합니다.
     * 동시 요청 시 race condition을 방지하기 위해 ACTIVE 상태인 경우에만 업데이트합니다.
     *
     * @param tokenId    토큰 ID
     * @param lastUsedAt 마지막 사용 시각
     * @param lastUsedIp 마지막 사용 IP
     * @return 업데이트된 행 수 (0이면 이미 다른 요청에서 처리됨)
     */
    int markAsRotatedIfActive(Long tokenId, LocalDateTime lastUsedAt, String lastUsedIp);
}
