package server.poptato.app.application.event;

import server.poptato.app.domain.value.Platform;
import server.poptato.app.domain.value.UpdateEventType;

public record AppUpdateLogEvent(
        Long userId,
        UpdateEventType eventType,
        String currentVersion,
        String targetVersion,
        Platform platform,
        Boolean updateAvailable
) {
    public static AppUpdateLogEvent of(Long userId, UpdateEventType eventType, String currentVersion, String targetVersion, Platform platform, Boolean updateAvailable) {
        return new AppUpdateLogEvent(userId, eventType, currentVersion, targetVersion, platform, updateAvailable);
    }
}
