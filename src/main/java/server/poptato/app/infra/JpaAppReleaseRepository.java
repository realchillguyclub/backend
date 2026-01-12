package server.poptato.app.infra;

import org.springframework.data.jpa.repository.JpaRepository;

import server.poptato.app.domain.entity.AppRelease;
import server.poptato.app.domain.repository.AppReleaseRepository;

public interface JpaAppReleaseRepository extends AppReleaseRepository, JpaRepository<AppRelease, Long> {
}
