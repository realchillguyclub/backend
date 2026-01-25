package server.poptato.user.domain.repository;

import server.poptato.user.domain.entity.User;

import java.util.List;
import java.util.Optional;

public interface UserRepository {

    Optional<User> findBySocialId(String socialId);

    Optional<User> findById(Long userId);

    User save(User user);

    List<Long> findAllUserIds();

    List<User> findByIsPushAlarmTrue();

    long count();

    void softDeleteById(Long userId);
}
