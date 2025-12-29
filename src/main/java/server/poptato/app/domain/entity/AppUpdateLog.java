package server.poptato.app.domain.entity;

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
import server.poptato.app.domain.value.Platform;
import server.poptato.app.domain.value.UpdateEventType;
import server.poptato.global.dao.BaseEntity;

@Getter
@Entity
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Table(name = "app_update_log")
public class AppUpdateLog extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "user_id", nullable = false)
    private Long userId;

    @Enumerated(EnumType.STRING)
    @Column(name = "event_type", nullable = false, columnDefinition = "VARCHAR(30)")
    private UpdateEventType eventType;

    @Column(name = "current_version", nullable = false, length = 20)
    private String currentVersion;

    @Column(name = "target_version", length = 20)
    private String targetVersion;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, columnDefinition = "VARCHAR(20)")
    private Platform platform;

    @Column(name = "update_available", nullable = false)
    private Boolean updateAvailable;

    @Builder
    public AppUpdateLog(Long userId, UpdateEventType eventType, String currentVersion,
                        String targetVersion, Platform platform, Boolean updateAvailable) {
        this.userId = userId;
        this.eventType = eventType;
        this.currentVersion = currentVersion;
        this.targetVersion = targetVersion;
        this.platform = platform;
        this.updateAvailable = updateAvailable;
    }
}
