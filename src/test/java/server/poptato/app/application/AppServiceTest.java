package server.poptato.app.application;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

import java.util.Optional;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.MethodOrderer;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestMethodOrder;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.InjectMocks;
import org.mockito.Mock;

import server.poptato.app.api.request.SkipRequestDto;
import server.poptato.app.application.response.DownloadResponseDto;
import server.poptato.app.application.response.VersionCheckResponseDto;
import server.poptato.app.domain.entity.AppRelease;
import server.poptato.app.domain.entity.AppUpdateLog;
import server.poptato.app.domain.repository.AppReleaseRepository;
import server.poptato.app.domain.repository.AppUpdateLogRepository;
import server.poptato.app.domain.value.Platform;
import server.poptato.app.domain.value.UpdateEventType;
import server.poptato.app.status.AppErrorStatus;
import server.poptato.configuration.ServiceTestConfig;
import server.poptato.global.exception.CustomException;
import server.poptato.infra.s3.service.S3Service;

public class AppServiceTest extends ServiceTestConfig {

    @Mock
    AppReleaseRepository appReleaseRepository;

    @Mock
    AppUpdateLogRepository appUpdateLogRepository;

    @Mock
    S3Service s3Service;

    @Captor
    ArgumentCaptor<AppUpdateLog> logCaptor;

    @InjectMocks
    AppService appService;

    @Nested
    @TestMethodOrder(MethodOrderer.DisplayName.class)
    @DisplayName("[SCN-SVC-APP-001] 버전을 체크한다.")
    class CheckVersion {

        @Test
        @DisplayName("[SCN-SVC-APP-001][TC-VERSION-001] 업데이트가 있으면 updateAvailable=true와 릴리즈 정보를 반환한다.")
        void checkVersion_updateAvailable_returnsUpdateInfo() {
            // given
            Long userId = 1L;
            String currentVersion = "1.0.0";

            AppRelease release = mock(AppRelease.class);
            when(release.getVersion()).thenReturn("1.2.0");
            when(release.getIsMandatory()).thenReturn(false);

            when(appReleaseRepository.findByPlatformAndIsActiveTrue(Platform.MACOS))
                    .thenReturn(Optional.of(release));

            // when
            VersionCheckResponseDto response = appService.checkVersion(userId, currentVersion, Platform.MACOS);

            // then
            assertThat(response.updateAvailable()).isTrue();
            assertThat(response.currentVersion()).isEqualTo(currentVersion);
            assertThat(response.latestVersion()).isEqualTo("1.2.0");
            assertThat(response.isMandatory()).isFalse();

            verify(appUpdateLogRepository).save(logCaptor.capture());
            AppUpdateLog log = logCaptor.getValue();
            assertThat(log.getUserId()).isEqualTo(userId);
            assertThat(log.getEventType()).isEqualTo(UpdateEventType.VERSION_CHECK);
            assertThat(log.getUpdateAvailable()).isTrue();
        }

        @Test
        @DisplayName("[SCN-SVC-APP-001][TC-VERSION-002] 업데이트가 없으면 updateAvailable=false를 반환한다.")
        void checkVersion_noUpdate_returnsNoUpdate() {
            // given
            Long userId = 1L;
            String currentVersion = "1.2.0";

            AppRelease release = mock(AppRelease.class);
            when(release.getVersion()).thenReturn("1.2.0");

            when(appReleaseRepository.findByPlatformAndIsActiveTrue(Platform.MACOS))
                    .thenReturn(Optional.of(release));

            // when
            VersionCheckResponseDto response = appService.checkVersion(userId, currentVersion, Platform.MACOS);

            // then
            assertThat(response.updateAvailable()).isFalse();
            assertThat(response.currentVersion()).isEqualTo(currentVersion);
            assertThat(response.latestVersion()).isEqualTo(currentVersion);

            verify(appUpdateLogRepository).save(logCaptor.capture());
            assertThat(logCaptor.getValue().getUpdateAvailable()).isFalse();
        }

        @Test
        @DisplayName("[SCN-SVC-APP-001][TC-VERSION-003] 활성화된 릴리즈가 없으면 updateAvailable=false를 반환한다.")
        void checkVersion_noActiveRelease_returnsNoUpdate() {
            // given
            Long userId = 1L;
            String currentVersion = "1.0.0";

            when(appReleaseRepository.findByPlatformAndIsActiveTrue(Platform.WINDOWS))
                    .thenReturn(Optional.empty());

            // when
            VersionCheckResponseDto response = appService.checkVersion(userId, currentVersion, Platform.WINDOWS);

            // then
            assertThat(response.updateAvailable()).isFalse();
        }

        @Test
        @DisplayName("[SCN-SVC-APP-001][TC-VERSION-004] 강제 업데이트가 필요한 경우 isMandatory=true를 반환한다.")
        void checkVersion_mandatory_returnsMandatoryTrue() {
            // given
            Long userId = 1L;
            String currentVersion = "1.0.0";

            AppRelease release = mock(AppRelease.class);
            when(release.getVersion()).thenReturn("2.0.0");
            when(release.getIsMandatory()).thenReturn(true);

            when(appReleaseRepository.findByPlatformAndIsActiveTrue(Platform.WINDOWS))
                    .thenReturn(Optional.of(release));

            // when
            VersionCheckResponseDto response = appService.checkVersion(userId, currentVersion, Platform.WINDOWS);

            // then
            assertThat(response.updateAvailable()).isTrue();
            assertThat(response.isMandatory()).isTrue();
            assertThat(response.latestVersion()).isEqualTo("2.0.0");
        }

        @Test
        @DisplayName("[SCN-SVC-APP-001][TC-VERSION-005] 로그에 현재 버전과 대상 버전이 정확히 기록된다.")
        void checkVersion_logsCorrectVersions() {
            // given
            Long userId = 10L;
            String currentVersion = "1.0.0";

            AppRelease release = mock(AppRelease.class);
            when(release.getVersion()).thenReturn("1.5.0");

            when(appReleaseRepository.findByPlatformAndIsActiveTrue(Platform.MACOS))
                    .thenReturn(Optional.of(release));

            // when
            appService.checkVersion(userId, currentVersion, Platform.MACOS);

            // then
            verify(appUpdateLogRepository).save(logCaptor.capture());
            AppUpdateLog log = logCaptor.getValue();
            assertThat(log.getCurrentVersion()).isEqualTo("1.0.0");
            assertThat(log.getTargetVersion()).isEqualTo("1.5.0");
            assertThat(log.getPlatform()).isEqualTo(Platform.MACOS);
        }
    }

    @Nested
    @TestMethodOrder(MethodOrderer.DisplayName.class)
    @DisplayName("[SCN-SVC-APP-002] 다운로드 URL을 조회한다.")
    class GetDownloadUrl {

        @Test
        @DisplayName("[SCN-SVC-APP-002][TC-DOWNLOAD-001] 정상적으로 Presigned URL을 반환한다.")
        void getDownloadUrl_success_returnsPresignedUrl() {
            // given
            Long userId = 1L;
            String currentVersion = "1.0.0";

            AppRelease release = mock(AppRelease.class);
            when(release.getVersion()).thenReturn("1.2.0");
            when(release.getPlatform()).thenReturn(Platform.MACOS);
            when(release.getFilePath()).thenReturn("macos/Poptato-1.2.0.dmg");
            when(release.getSha512()).thenReturn("abc123");
            when(release.getFileSize()).thenReturn(85000000L);

            when(appReleaseRepository.findByPlatformAndIsActiveTrue(Platform.MACOS))
                    .thenReturn(Optional.of(release));
            when(s3Service.generatePresignedUrl("macos/Poptato-1.2.0.dmg"))
                    .thenReturn("https://s3.amazonaws.com/presigned-url");

            // when
            DownloadResponseDto response = appService.getDownloadUrl(userId, Platform.MACOS, currentVersion);

            // then
            assertThat(response.version()).isEqualTo("1.2.0");
            assertThat(response.platform()).isEqualTo("MACOS");
            assertThat(response.downloadUrl()).isEqualTo("https://s3.amazonaws.com/presigned-url");
            assertThat(response.sha512()).isEqualTo("abc123");
            assertThat(response.size()).isEqualTo(85000000L);

            verify(appUpdateLogRepository).save(logCaptor.capture());
            AppUpdateLog log = logCaptor.getValue();
            assertThat(log.getEventType()).isEqualTo(UpdateEventType.DOWNLOADED);
        }

        @Test
        @DisplayName("[SCN-SVC-APP-002][TC-DOWNLOAD-002] 활성화된 릴리즈가 없으면 예외를 던진다.")
        void getDownloadUrl_noRelease_throwsException() {
            // given
            Long userId = 1L;
            String currentVersion = "1.0.0";

            when(appReleaseRepository.findByPlatformAndIsActiveTrue(Platform.WINDOWS))
                    .thenReturn(Optional.empty());

            // when & then
            assertThatThrownBy(() -> appService.getDownloadUrl(userId, Platform.WINDOWS, currentVersion))
                    .isInstanceOf(CustomException.class)
                    .hasMessageContaining(AppErrorStatus._RELEASE_NOT_FOUND.getMessage());
        }
    }

    @Nested
    @TestMethodOrder(MethodOrderer.DisplayName.class)
    @DisplayName("[SCN-SVC-APP-003] 업데이트를 스킵한다.")
    class SkipUpdate {

        @Test
        @DisplayName("[SCN-SVC-APP-003][TC-SKIP-001] 정상적으로 스킵 로그를 저장한다.")
        void skipUpdate_success_savesLog() {
            // given
            Long userId = 1L;
            SkipRequestDto request = new SkipRequestDto("1.0.0", "1.2.0", Platform.MACOS);

            // when
            appService.skipUpdate(userId, request);

            // then
            verify(appUpdateLogRepository).save(logCaptor.capture());
            AppUpdateLog log = logCaptor.getValue();
            assertThat(log.getUserId()).isEqualTo(userId);
            assertThat(log.getEventType()).isEqualTo(UpdateEventType.UPDATE_SKIPPED);
            assertThat(log.getCurrentVersion()).isEqualTo("1.0.0");
            assertThat(log.getTargetVersion()).isEqualTo("1.2.0");
            assertThat(log.getPlatform()).isEqualTo(Platform.MACOS);
        }
    }
}
