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
}
