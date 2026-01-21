package server.poptato.app.infra;

import org.springframework.data.jpa.repository.JpaRepository;

import server.poptato.app.domain.entity.AppUpdateLog;
import server.poptato.app.domain.repository.AppUpdateLogRepository;

public interface JpaAppUpdateLogRepository extends AppUpdateLogRepository, JpaRepository<AppUpdateLog, Long> {
}
