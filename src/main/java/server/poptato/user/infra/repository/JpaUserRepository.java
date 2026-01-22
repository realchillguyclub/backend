package server.poptato.user.infra.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import server.poptato.user.domain.entity.User;
import server.poptato.user.domain.repository.UserRepository;

import java.util.List;
import java.util.Optional;

public interface JpaUserRepository extends UserRepository, JpaRepository<User, Long> {

    @Query("""
        SELECT u FROM User u
        WHERE u.socialId = :socialId
          AND u.isDeleted = false
        """)
    Optional<User> findBySocialId(@Param("socialId") String socialId);

    @Query("""
        SELECT u.id FROM User u
        WHERE u.isDeleted = false
        """)
    List<Long> findAllUserIds();

    @Query("""
        SELECT u FROM User u
        WHERE u.id = :id
          AND u.isDeleted = false
        """)
    Optional<User> findById(@Param("id") Long id);

    @Query("""
        SELECT u FROM User u
        WHERE u.isPushAlarm = true
          AND u.isDeleted = false
        """)
    List<User> findByIsPushAlarmTrue();

    @Query("""
        SELECT COUNT(u) FROM User u
        WHERE u.isDeleted = false
        """)
    long count();
}
