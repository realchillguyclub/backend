package server.poptato.user.domain.repository;

import server.poptato.user.domain.entity.Mobile;
import server.poptato.user.domain.value.MobileType;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

public interface MobileRepository {

    Mobile save(Mobile mobile);

    void deleteByClientId(String clientId);

    void deleteByUserId(Long userId);

    void deleteByUserIdAndType(Long userId, MobileType type);

    List<Mobile> findAllPushCapableByUserId(Long userId);

    Optional<Mobile> findByClientId(String clientId);

    void deleteOldTokens(LocalDateTime localDateTime);

    Optional<Mobile> findTopByUserIdOrderByModifyDateDesc(Long userId);

    Optional<Mobile> findByUserIdAndType(Long userId, MobileType type);
}
