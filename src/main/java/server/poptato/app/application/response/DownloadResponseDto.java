package server.poptato.app.application.response;

import server.poptato.app.domain.entity.AppRelease;

public record DownloadResponseDto(
        String version,
        String platform,
        String downloadUrl,
        String sha512,
        Long size
) {
    public static DownloadResponseDto of(AppRelease release, String presignedUrl) {
        return new DownloadResponseDto(
                release.getVersion(),
                release.getPlatform().name(),
                presignedUrl,
                release.getSha512(),
                release.getFileSize()
        );
    }
}
