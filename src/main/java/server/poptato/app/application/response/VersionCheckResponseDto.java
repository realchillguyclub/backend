package server.poptato.app.application.response;

import java.time.LocalDateTime;

import server.poptato.app.domain.entity.AppRelease;

public record VersionCheckResponseDto(
        boolean updateAvailable,
        String currentVersion,
        String latestVersion,
        LocalDateTime releaseDate,
        String releaseNote,
        Boolean isMandatory
) {
    public static VersionCheckResponseDto noUpdate(String currentVersion) {
        return new VersionCheckResponseDto(
                false,
                currentVersion,
                currentVersion,
                null,
                null,
                null
        );
    }

    public static VersionCheckResponseDto hasUpdate(String currentVersion, AppRelease release) {
        return new VersionCheckResponseDto(
                true,
                currentVersion,
                release.getVersion(),
                release.getCreateDate(),
                release.getReleaseNotes(),
                release.getIsMandatory()
        );
    }
}
