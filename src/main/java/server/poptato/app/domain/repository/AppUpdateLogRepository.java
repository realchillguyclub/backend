package server.poptato.app.domain.repository;

import server.poptato.app.domain.entity.AppUpdateLog;

public interface AppUpdateLogRepository {

    AppUpdateLog save(AppUpdateLog appUpdateLog);
}
