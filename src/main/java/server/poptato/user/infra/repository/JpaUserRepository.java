package server.poptato.user.infra.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
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
        """)
    Optional<User> findBySocialId(@Param("socialId") String socialId);

    @Query("""
        SELECT u.id FROM User u
        """)
    List<Long> findAllUserIds();

    @Query("""
        SELECT u FROM User u
        WHERE u.id = :id
        """)
    Optional<User> findById(@Param("id") Long id);

    @Query("""
        SELECT u FROM User u
        WHERE u.isPushAlarm = true
        """)
    List<User> findByIsPushAlarmTrue();

    @Query("""
        SELECT COUNT(u) FROM User u
        """)
    long count();

    @Modifying(clearAutomatically = true, flushAutomatically = true)
    @Query("""
        UPDATE User u
        SET u.isDeleted = true,
            u.socialId = CONCAT('DELETED_', u.id, '_', u.socialId)
        WHERE u.id = :userId
        """)
    void softDeleteById(@Param("userId") Long userId);
}
