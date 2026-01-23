package server.poptato.user.infra;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.List;
import java.util.Optional;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.MethodOrderer;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestMethodOrder;
import org.springframework.beans.factory.annotation.Autowired;

import server.poptato.configuration.DatabaseTestConfig;
import server.poptato.configuration.MySqlDataJpaTest;
import server.poptato.user.domain.entity.User;
import server.poptato.user.domain.value.SocialType;
import server.poptato.user.infra.repository.JpaUserRepository;

@MySqlDataJpaTest
public class JpaUserRepositoryTest extends DatabaseTestConfig {

    @Autowired
    private JpaUserRepository jpaUserRepository;

    private User createUser(String socialId, String name, boolean isPushAlarm) {
        User user = User.builder()
                .socialType(SocialType.KAKAO)
                .socialId(socialId)
                .name(name)
                .email(name + "@test.com")
                .isPushAlarm(isPushAlarm)
                .build();
        tem.persist(user);
        tem.flush();
        tem.clear();
        return user;
    }

    private Boolean getIsDeleted(Long id) {
        Object result = tem.getEntityManager().createNativeQuery(
                "SELECT is_deleted FROM users WHERE id = :id"
        ).setParameter("id", id).getSingleResult();
        if (result instanceof Boolean) {
            return (Boolean) result;
        }
        return ((Number) result).intValue() == 1;
    }

    private void softDeleteUser(Long userId) {
        tem.getEntityManager().createNativeQuery(
                "UPDATE users SET is_deleted = true, social_id = CONCAT('DELETED_', social_id) WHERE id = :id"
        ).setParameter("id", userId).executeUpdate();
        tem.flush();
        tem.clear();
    }

    @Nested
    @DisplayName("[SCN-REP-USER-001] @SQLRestriction 동작 테스트")
    @TestMethodOrder(MethodOrderer.DisplayName.class)
    class SQLRestrictionTests {

        @Test
        @DisplayName("[TC-SQL-RESTRICTION-001] soft delete된 User는 findById로 조회되지 않는다")
        void findById_excludesSoftDeletedUser() {
            // given
            User user = createUser("social123", "testUser", true);
            Long userId = user.getId();

            softDeleteUser(userId);

            // when
            Optional<User> found = jpaUserRepository.findById(userId);

            // then
            assertThat(found).isEmpty();
        }

        @Test
        @DisplayName("[TC-SQL-RESTRICTION-002] soft delete된 User는 findBySocialId로 조회되지 않는다")
        void findBySocialId_excludesSoftDeletedUser() {
            // given
            String socialId = "social456";
            User user = createUser(socialId, "testUser", true);

            softDeleteUser(user.getId());
            String deletedSocialId = "DELETED_" + socialId;

            // when - deletedSocialId로 조회해서 SQLRestriction 동작 검증
            Optional<User> found = jpaUserRepository.findBySocialId(deletedSocialId);

            // then - socialId는 일치하지만 is_deleted=true이므로 조회되지 않아야 함
            assertThat(found).isEmpty();
        }

        @Test
        @DisplayName("[TC-SQL-RESTRICTION-003] soft delete된 User는 findAllUserIds에서 제외된다")
        void findAllUserIds_excludesSoftDeletedUser() {
            // given
            User activeUser = createUser("active123", "activeUser", true);
            User deletedUser = createUser("deleted123", "deletedUser", true);

            softDeleteUser(deletedUser.getId());

            // when
            List<Long> userIds = jpaUserRepository.findAllUserIds();

            // then
            assertThat(userIds).contains(activeUser.getId());
            assertThat(userIds).doesNotContain(deletedUser.getId());
        }

        @Test
        @DisplayName("[TC-SQL-RESTRICTION-004] soft delete된 User는 findByIsPushAlarmTrue에서 제외된다")
        void findByIsPushAlarmTrue_excludesSoftDeletedUser() {
            // given
            User activeUser = createUser("push123", "pushUser", true);
            User deletedUser = createUser("pushDeleted123", "pushDeletedUser", true);

            softDeleteUser(deletedUser.getId());

            // when
            List<User> users = jpaUserRepository.findByIsPushAlarmTrue();

            // then
            assertThat(users).extracting(User::getId).contains(activeUser.getId());
            assertThat(users).extracting(User::getId).doesNotContain(deletedUser.getId());
        }

        @Test
        @DisplayName("[TC-SQL-RESTRICTION-005] soft delete된 User는 count에서 제외된다")
        void count_excludesSoftDeletedUser() {
            // given
            long initialCount = jpaUserRepository.count();

            User activeUser = createUser("count123", "countUser", true);
            User deletedUser = createUser("countDeleted123", "countDeletedUser", true);

            // when - before soft delete
            long countBeforeDelete = jpaUserRepository.count();

            // then
            assertThat(countBeforeDelete).isEqualTo(initialCount + 2);

            // when - after soft delete
            softDeleteUser(deletedUser.getId());
            long countAfterDelete = jpaUserRepository.count();

            // then
            assertThat(countAfterDelete).isEqualTo(initialCount + 1);
        }
    }
}
