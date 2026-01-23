package server.poptato.todo.infra;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Import;

import server.poptato.configuration.DatabaseTestConfig;
import server.poptato.configuration.MySqlDataJpaTest;
import server.poptato.todo.domain.entity.Todo;
import server.poptato.todo.domain.repository.TodoRepository;
import server.poptato.todo.domain.value.TodayStatus;
import server.poptato.todo.domain.value.Type;
import server.poptato.todo.infra.repository.impl.TodoRepositoryImpl;

@MySqlDataJpaTest
@Import(TodoRepositoryImpl.class)
public class TodoRepositoryImplTest extends DatabaseTestConfig {

    @Autowired
    private TodoRepository todoRepository;

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
        Object result = tem.getEntityManager().createNativeQuery(
                "SELECT is_deleted FROM todo WHERE id = :id"
        ).setParameter("id", id).getSingleResult();
        if (result instanceof Boolean) {
            return (Boolean) result;
        }
        return ((Number) result).intValue() == 1;
    }

    @Nested
    @DisplayName("[SCN-REP-IMPL-001] TodoRepositoryImpl Soft Delete 테스트")
    class SoftDeleteTests {

        @Test
        @DisplayName("[TC-IMPL-001] softDeleteByUserId 호출 시 해당 유저의 모든 Todo가 soft delete 된다")
        void softDeleteByUserId_deletesAllUserTodos() {
            // given
            Long userId = 1000L;
            Long otherUserId = 2000L;

            Todo todo1 = createTodo(userId, null, "todo1");
            Todo todo2 = createTodo(userId, null, "todo2");
            Todo otherTodo = createTodo(otherUserId, null, "other");

            // when
            todoRepository.softDeleteByUserId(userId);
            tem.flush();
            tem.clear();

            // then
            assertThat(getIsDeleted(todo1.getId())).isTrue();
            assertThat(getIsDeleted(todo2.getId())).isTrue();
            assertThat(getIsDeleted(otherTodo.getId())).isFalse();
        }

        @Test
        @DisplayName("[TC-IMPL-002] softDeleteByCategoryId 호출 시 해당 카테고리의 모든 Todo가 soft delete 된다")
        void softDeleteByCategoryId_deletesAllCategoryTodos() {
            // given
            Long userId = 1000L;
            Long categoryId = 100L;
            Long otherCategoryId = 200L;

            Todo todo1 = createTodo(userId, categoryId, "todo1");
            Todo todo2 = createTodo(userId, categoryId, "todo2");
            Todo otherTodo = createTodo(userId, otherCategoryId, "other");

            // when
            todoRepository.softDeleteByCategoryId(categoryId);
            tem.flush();
            tem.clear();

            // then
            assertThat(getIsDeleted(todo1.getId())).isTrue();
            assertThat(getIsDeleted(todo2.getId())).isTrue();
            assertThat(getIsDeleted(otherTodo.getId())).isFalse();
        }
    }
}
