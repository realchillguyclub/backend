package server.poptato.app.application;

import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;

import lombok.RequiredArgsConstructor;
import server.poptato.app.api.request.SkipRequestDto;
import server.poptato.app.application.event.AppUpdateLogEvent;
import server.poptato.app.application.response.DownloadResponseDto;
import server.poptato.app.application.response.VersionCheckResponseDto;
import server.poptato.app.domain.entity.AppRelease;
import server.poptato.app.domain.repository.AppReleaseRepository;
import server.poptato.app.domain.value.Platform;
import server.poptato.app.domain.value.UpdateEventType;
import server.poptato.app.status.AppErrorStatus;
import server.poptato.global.exception.CustomException;
import server.poptato.infra.s3.service.S3Service;

@Service
@RequiredArgsConstructor
public class AppService {

    private final AppReleaseRepository appReleaseRepository;
    private final ApplicationEventPublisher eventPublisher;
    private final S3Service s3Service;

    /**
     * 앱의 버전을 체크하고 업데이트 필요 여부를 반환한다.
     * 업데이트 존재 여부와 관계없이 모든 VERSION_CHECK 호출을 로깅한다.
     */
    public VersionCheckResponseDto checkVersion(Long userId, String currentVersion, Platform platform) {
        AppRelease activeRelease = appReleaseRepository.findByPlatformAndIsActiveTrue(platform)
                .orElse(null);

        boolean updateAvailable = activeRelease != null && !activeRelease.getVersion().equals(currentVersion);

        publishUpdateLogEvent(userId, UpdateEventType.VERSION_CHECK, currentVersion,
                updateAvailable ? activeRelease.getVersion() : null, platform, updateAvailable);

        if (!updateAvailable) {
            return VersionCheckResponseDto.noUpdate(currentVersion);
        }

        return VersionCheckResponseDto.hasUpdate(currentVersion, activeRelease);
    }

    /**
     * 다운로드 URL(S3 Presigned URL)을 생성하여 반환한다.
     * S3 호출 성공 후에만 DOWNLOADED 로그를 저장한다.
     */
    public DownloadResponseDto getDownloadUrl(Long userId, Platform platform, String currentVersion) {
        AppRelease activeRelease = appReleaseRepository.findByPlatformAndIsActiveTrue(platform)
                .orElseThrow(() -> new CustomException(AppErrorStatus._RELEASE_NOT_FOUND));

        String presignedUrl;
        try {
            presignedUrl = s3Service.generatePresignedUrl(activeRelease.getFilePath());
        } catch (Exception e) {
            throw new CustomException(AppErrorStatus._PRESIGNED_URL_GENERATION_FAILED);
        }

        publishUpdateLogEvent(userId, UpdateEventType.DOWNLOADED, currentVersion,
                activeRelease.getVersion(), platform, true);

        return DownloadResponseDto.of(activeRelease, presignedUrl);
    }

    /**
     * 사용자가 업데이트를 스킵했을 때 로그를 기록한다.
     */
    public void skipUpdate(Long userId, SkipRequestDto request) {
        publishUpdateLogEvent(userId, UpdateEventType.UPDATE_SKIPPED, request.currentVersion(),
                request.targetVersion(), request.platform(), true);
    }

    /**
     * 업데이트 로그 이벤트를 비동기로 발행한다.
     */
    private void publishUpdateLogEvent(Long userId, UpdateEventType eventType, String currentVersion,
                                       String targetVersion, Platform platform, Boolean updateAvailable) {
        eventPublisher.publishEvent(AppUpdateLogEvent.of(userId, eventType, currentVersion,
                targetVersion, platform, updateAvailable));
    }
}
