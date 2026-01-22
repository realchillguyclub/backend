package server.poptato.todo.infra;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.MethodOrderer;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestMethodOrder;
import org.springframework.beans.factory.annotation.Autowired;

import server.poptato.configuration.DatabaseTestConfig;
import server.poptato.configuration.MySqlDataJpaTest;
import server.poptato.todo.domain.entity.Todo;
import server.poptato.todo.domain.value.TodayStatus;
import server.poptato.todo.domain.value.Type;
import server.poptato.todo.infra.repository.JpaTodoRepository;

@MySqlDataJpaTest
public class JpaTodoRepositoryTest extends DatabaseTestConfig {

    @Autowired
    private JpaTodoRepository jpaTodoRepository;

    private Todo createTodo(Long userId, Long categoryId, String content) {
        Todo todo = Todo.builder()
                .userId(userId)
                .categoryId(categoryId)
                .type(Type.BACKLOG)
                .content(content)
                .todayStatus(TodayStatus.INCOMPLETE)
                .build();
        tem.persist(todo);
        tem.flush();
        tem.clear();
        return todo;
    }

    private Boolean getIsDeleted(Long id) {
        return (Boolean) tem.getEntityManager().createNativeQuery(
                "SELECT is_deleted FROM todo WHERE id = :id"
        ).setParameter("id", id).getSingleResult();
    }

    @Nested
    @DisplayName("[SCN-REP-TODO-001] Soft Delete 테스트")
    @TestMethodOrder(MethodOrderer.DisplayName.class)
    class SoftDeleteTests {

        @Test
        @DisplayName("[TC-SOFT-DELETE-001] softDeleteByUserId 호출 시 해당 유저의 모든 Todo가 soft delete 된다")
        void softDeleteByUserId_deletesAllUserTodos() {
            // given
            Long userId = 100L;
            Long otherUserId = 200L;

            Todo todo1 = createTodo(userId, null, "todo1");
            Todo todo2 = createTodo(userId, null, "todo2");
            Todo otherTodo = createTodo(otherUserId, null, "other");

            // when
            jpaTodoRepository.softDeleteByUserId(userId);
            tem.flush();
            tem.clear();

            // then - Native Query로 is_deleted 값 직접 확인 (@SQLRestriction 우회)
            assertThat(getIsDeleted(todo1.getId())).isTrue();
            assertThat(getIsDeleted(todo2.getId())).isTrue();
            assertThat(getIsDeleted(otherTodo.getId())).isFalse();
        }

        @Test
        @DisplayName("[TC-SOFT-DELETE-002] softDeleteByCategoryId 호출 시 해당 카테고리의 모든 Todo가 soft delete 된다")
        void softDeleteByCategoryId_deletesAllCategoryTodos() {
            // given
            Long userId = 100L;
            Long categoryId = 10L;
            Long otherCategoryId = 20L;

            Todo todo1 = createTodo(userId, categoryId, "todo1");
            Todo todo2 = createTodo(userId, categoryId, "todo2");
            Todo otherTodo = createTodo(userId, otherCategoryId, "other");

            // when
            jpaTodoRepository.softDeleteByCategoryId(categoryId);
            tem.flush();
            tem.clear();

            // then - Native Query로 is_deleted 값 직접 확인 (@SQLRestriction 우회)
            assertThat(getIsDeleted(todo1.getId())).isTrue();
            assertThat(getIsDeleted(todo2.getId())).isTrue();
            assertThat(getIsDeleted(otherTodo.getId())).isFalse();
        }
    }
}
