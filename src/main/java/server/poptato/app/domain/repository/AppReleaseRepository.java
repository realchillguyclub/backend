package server.poptato.app.domain.repository;

import java.util.Optional;

import server.poptato.app.domain.entity.AppRelease;
import server.poptato.app.domain.value.Platform;

public interface AppReleaseRepository {

    Optional<AppRelease> findByPlatformAndIsActiveTrue(Platform platform);

    AppRelease save(AppRelease appRelease);
}
