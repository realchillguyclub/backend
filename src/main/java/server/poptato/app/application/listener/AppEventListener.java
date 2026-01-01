package server.poptato.app.application.listener;

import org.springframework.context.event.EventListener;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import server.poptato.app.application.event.AppUpdateLogEvent;
import server.poptato.app.domain.entity.AppUpdateLog;
import server.poptato.app.domain.repository.AppUpdateLogRepository;

@Slf4j
@Component
@RequiredArgsConstructor
public class AppEventListener {

    private final AppUpdateLogRepository appUpdateLogRepository;

    /**
     * 앱 업데이트 로그를 비동기로 저장한다.
     */
    @Async
    @EventListener
    public void handleAppUpdateLog(AppUpdateLogEvent event) {
        try {
            AppUpdateLog updateLog = AppUpdateLog.builder()
                    .userId(event.userId())
                    .eventType(event.eventType())
                    .currentVersion(event.currentVersion())
                    .targetVersion(event.targetVersion())
                    .platform(event.platform())
                    .updateAvailable(event.updateAvailable())
                    .build();

            appUpdateLogRepository.save(updateLog);
        } catch (Exception e) {
            log.error("앱 업데이트 로그 저장 실패: userId={}, eventType={}", event.userId(), event.eventType(), e);
        }
    }
}
