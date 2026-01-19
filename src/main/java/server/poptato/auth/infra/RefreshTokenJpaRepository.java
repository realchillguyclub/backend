package server.poptato.auth.infra;

import java.time.LocalDateTime;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import server.poptato.auth.domain.entity.RefreshToken;
import server.poptato.auth.domain.repository.RefreshTokenRepository;
import server.poptato.user.domain.value.MobileType;

public interface RefreshTokenJpaRepository extends JpaRepository<RefreshToken, Long>, RefreshTokenRepository {

    @Override
    Optional<RefreshToken> findByJti(String jti);

    @Override
    @Modifying
    @Query("""
        UPDATE RefreshToken r
        SET r.status = 'REVOKED'
        WHERE r.userId = :userId
          AND r.mobileType = :mobileType
          AND r.status = 'ACTIVE'
    """)
    void revokeByUserIdAndMobileType(@Param("userId") Long userId,
                                     @Param("mobileType") MobileType mobileType);

    @Override
    @Modifying
    @Query("""
        UPDATE RefreshToken r
        SET r.status = 'REVOKED'
        WHERE r.userId = :userId
          AND r.status = 'ACTIVE'
    """)
    void revokeAllByUserId(@Param("userId") Long userId);

    @Override
    @Modifying
    @Query("""
        UPDATE RefreshToken r
        SET r.status = 'EXPIRED'
        WHERE r.status = 'ACTIVE'
          AND r.expiryAt < :now
    """)
    int updateExpiredTokens(@Param("now") LocalDateTime now);

    @Override
    @Modifying
    @Query("""
        DELETE FROM RefreshToken r
        WHERE r.status IN ('REVOKED', 'EXPIRED', 'ROTATED')
          AND r.modifyDate < :threshold
    """)
    int hardDeleteOldInactiveTokens(@Param("threshold") LocalDateTime threshold);

    @Override
    @Modifying
    @Query("""
        UPDATE RefreshToken r
        SET r.status = 'REVOKED'
        WHERE r.familyId = :familyId
          AND r.status IN ('ACTIVE', 'ROTATED')
    """)
    void revokeAllByFamilyId(@Param("familyId") String familyId);

    @Override
    @Modifying
    @Query("""
        UPDATE RefreshToken r
        SET r.status = 'ROTATED',
            r.lastUsedAt = :lastUsedAt,
            r.lastUsedIp = :lastUsedIp
        WHERE r.id = :tokenId
          AND r.status = 'ACTIVE'
    """)
    int markAsRotatedIfActive(@Param("tokenId") Long tokenId,
                              @Param("lastUsedAt") LocalDateTime lastUsedAt,
                              @Param("lastUsedIp") String lastUsedIp);
}
