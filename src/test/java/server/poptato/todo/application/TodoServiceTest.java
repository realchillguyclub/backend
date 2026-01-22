package server.poptato.todo.application;

import jakarta.persistence.EntityManager;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.MethodOrderer;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestMethodOrder;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import server.poptato.category.domain.repository.CategoryRepository;
import server.poptato.category.validator.CategoryValidator;
import server.poptato.configuration.ServiceTestConfig;
import server.poptato.emoji.domain.repository.EmojiRepository;
import server.poptato.todo.domain.entity.Todo;
import server.poptato.todo.domain.repository.CompletedDateTimeRepository;
import server.poptato.todo.domain.repository.RoutineRepository;
import server.poptato.todo.domain.repository.TimeAlarmRepository;
import server.poptato.todo.domain.repository.TodoRepository;
import server.poptato.user.validator.UserValidator;

import java.util.Optional;

import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

class TodoServiceTest extends ServiceTestConfig {

    @Mock
    private UserValidator userValidator;
    @Mock
    private CategoryValidator categoryValidator;
    @Mock
    private TodoRepository todoRepository;
    @Mock
    private TimeAlarmRepository timeAlarmRepository;
    @Mock
    private RoutineRepository routineRepository;
    @Mock
    private CompletedDateTimeRepository completedDateTimeRepository;
    @Mock
    private CategoryRepository categoryRepository;
    @Mock
    private EmojiRepository emojiRepository;
    @Mock
    private EntityManager entityManager;

    @InjectMocks
    private TodoService todoService;

    @Nested
    @TestMethodOrder(MethodOrderer.DisplayName.class)
    @DisplayName("[SCN-SVC-TODO-001] 할 일을 삭제한다 (Soft Delete)")
    class DeleteTodo {

        @Test
        @DisplayName("[TC-DELETE-001] 할 일을 정상적으로 삭제한다 (Soft Delete)")
        void deleteTodoById_success() {
            // given
            Long userId = 1L;
            Long todoId = 10L;

            Todo todo = mock(Todo.class);
            given(todo.getUserId()).willReturn(userId);
            given(todoRepository.findById(todoId)).willReturn(Optional.of(todo));

            // when
            todoService.deleteTodoById(userId, todoId);

            // then
            verify(userValidator).checkIsExistUser(userId);
            verify(todoRepository).findById(todoId);
            verify(todo).softDelete();
        }
    }

}
