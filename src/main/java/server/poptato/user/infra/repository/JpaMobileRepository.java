package server.poptato.user.infra.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import server.poptato.user.domain.entity.Mobile;
import server.poptato.user.domain.repository.MobileRepository;

import java.time.LocalDateTime;
import java.util.List;

public interface JpaMobileRepository extends MobileRepository, JpaRepository<Mobile, Long> {

    @Modifying
    @Query("""
        DELETE FROM Mobile m
        WHERE m.modifyDate < :localDateTime
    """)
    void deleteOldTokens(@Param("localDateTime") LocalDateTime localDateTime);

    @Query("""
        SELECT m FROM Mobile m
        WHERE m.userId = :userId
            AND m.type <> 'DESKTOP'
            AND m.clientId IS NOT NULL
    """)
    List<Mobile> findAllPushCapableByUserId(@Param("userId") Long userId);

}
