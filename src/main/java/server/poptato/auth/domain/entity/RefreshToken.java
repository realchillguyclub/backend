package server.poptato.auth.domain.entity;

import java.time.LocalDateTime;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import server.poptato.auth.domain.value.TokenStatus;
import server.poptato.global.dao.BaseEntity;
import server.poptato.user.domain.value.MobileType;

/**
 * Refresh Token 엔티티
 * Token Rotation 추적 및 사용 이력 로깅을 위한 테이블
 */
@Entity
@Table(name = "refresh_token")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class RefreshToken extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "parent_id")
    private Long parentId;

    @Column(name = "user_id", nullable = false)
    private Long userId;

    @Column(nullable = false, unique = true, length = 128)
    private String jti;
	
    @Enumerated(EnumType.STRING)
    @Column(name = "mobile_type", nullable = false, columnDefinition = "VARCHAR(20)")
    private MobileType mobileType;

    @Column(name = "client_id", length = 256)
    private String clientId;

    @Column(name = "user_agent", length = 512)
    private String userAgent;

    @Column(name = "user_ip", length = 45)
    private String userIp;

    @Column(name = "refresh_token", nullable = false, columnDefinition = "TEXT")
    private String refreshToken;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, columnDefinition = "VARCHAR(20)")
    private TokenStatus status;

    @Column(name = "issued_at", nullable = false)
    private LocalDateTime issuedAt;

    @Column(name = "expiry_at", nullable = false)
    private LocalDateTime expiryAt;

    @Column(name = "last_used_at")
    private LocalDateTime lastUsedAt;

    @Column(name = "last_used_ip", length = 45)
    private String lastUsedIp;

    @Column(name = "reissue_count", nullable = false)
    private int reissueCount;

    @Builder
    public RefreshToken(Long parentId, Long userId, String jti, MobileType mobileType,
                        String clientId, String userAgent, String userIp, String refreshToken,
                        LocalDateTime issuedAt, LocalDateTime expiryAt, int reissueCount) {
        this.parentId = parentId;
        this.userId = userId;
        this.jti = jti;
        this.mobileType = mobileType;
        this.clientId = clientId;
        this.userAgent = userAgent;
        this.userIp = userIp;
        this.refreshToken = refreshToken;
        this.status = TokenStatus.ACTIVE;
        this.issuedAt = issuedAt;
        this.expiryAt = expiryAt;
        this.reissueCount = reissueCount;
    }

    /**
     * 신규 Refresh Token 생성 (로그인 시)
     */
    public static RefreshToken create(Long userId, String jti, MobileType mobileType,
                                      String clientId, String refreshToken,
                                      LocalDateTime issuedAt, LocalDateTime expiryAt,
                                      String userIp, String userAgent) {
        return RefreshToken.builder()
                .userId(userId)
                .jti(jti)
                .mobileType(mobileType)
                .clientId(clientId)
                .refreshToken(refreshToken)
                .issuedAt(issuedAt)
                .expiryAt(expiryAt)
                .userIp(userIp)
                .userAgent(userAgent)
                .reissueCount(0)
                .build();
    }

    /**
     * Token Rotation으로 새 토큰 생성 (재발급 시)
     */
    public RefreshToken rotate(String newJti, String newRefreshToken,
                               LocalDateTime newIssuedAt, LocalDateTime newExpiryAt,
                               String newUserIp, String newUserAgent) {
        return RefreshToken.builder()
                .parentId(this.id)
                .userId(this.userId)
                .jti(newJti)
                .mobileType(this.mobileType)
                .clientId(this.clientId)
                .refreshToken(newRefreshToken)
                .issuedAt(newIssuedAt)
                .expiryAt(newExpiryAt)
                .userIp(newUserIp)
                .userAgent(newUserAgent)
                .reissueCount(this.reissueCount + 1)
                .build();
    }

    /**
     * 토큰 상태를 ROTATED로 변경 (Token Rotation 시)
     */
    public void markAsRotated() {
        this.status = TokenStatus.ROTATED;
    }

    /**
     * 토큰이 활성 상태인지 확인
     */
    public boolean isActive() {
        return this.status == TokenStatus.ACTIVE;
    }
}
