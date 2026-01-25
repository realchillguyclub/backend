package server.poptato.todo.application;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

import java.time.LocalDate;
import java.util.List;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.springframework.test.util.ReflectionTestUtils;

import server.poptato.configuration.ServiceTestConfig;
import server.poptato.todo.domain.entity.Todo;
import server.poptato.todo.domain.repository.TodoRepository;
import server.poptato.todo.domain.value.TodayStatus;
import server.poptato.todo.domain.value.Type;
import server.poptato.user.domain.repository.UserRepository;

class TodoBatchServiceTest extends ServiceTestConfig {

    @Mock
    private TodoRepository todoRepository;

    @Mock
    private UserRepository userRepository;

    @Mock
    private TodoService todoService;

    @InjectMocks
    private TodoBatchService todoBatchService;

    @Nested
    @DisplayName("[SCN-SVC-TODO-BATCH-001] 오늘 할 일 상태를 업데이트한다")
    class UpdateTodayTodosAndSave {

        @Test
        @DisplayName("[TC-BATCH-001] 완료된 반복 할 일은 백로그로 이동")
        void updateTodayTodos_completedRepeatToBacklog() {
            // given
            Long userId = 1L;
            Todo completedRepeatTodo = mock(Todo.class);
            when(completedRepeatTodo.getUserId()).thenReturn(userId);
            when(completedRepeatTodo.getTodayStatus()).thenReturn(TodayStatus.COMPLETED);
            when(completedRepeatTodo.isRepeat()).thenReturn(true);

            when(todoRepository.findByType(Type.TODAY)).thenReturn(List.of(completedRepeatTodo));
            when(todoRepository.findMaxBacklogOrderByUserIdOrZero(userId)).thenReturn(5);

            // when
            todoBatchService.updateTodayTodosAndSave();

            // then
            verify(completedRepeatTodo).updateType(Type.BACKLOG);
            verify(completedRepeatTodo).updateTodayStatus(null);
            verify(completedRepeatTodo).updateTodayOrder(null);
            verify(completedRepeatTodo).updateBacklogOrder(6);
            verify(todoRepository).save(completedRepeatTodo);
        }

        @Test
        @DisplayName("[TC-BATCH-002] 완료된 루틴 할 일은 백로그로 이동")
        void updateTodayTodos_completedRoutineToBacklog() {
            // given
            Long userId = 1L;
            Todo completedRoutineTodo = mock(Todo.class);
            when(completedRoutineTodo.getUserId()).thenReturn(userId);
            when(completedRoutineTodo.getTodayStatus()).thenReturn(TodayStatus.COMPLETED);
            when(completedRoutineTodo.isRepeat()).thenReturn(false);
            when(completedRoutineTodo.isRoutine()).thenReturn(true);

            when(todoRepository.findByType(Type.TODAY)).thenReturn(List.of(completedRoutineTodo));
            when(todoRepository.findMaxBacklogOrderByUserIdOrZero(userId)).thenReturn(3);

            // when
            todoBatchService.updateTodayTodosAndSave();

            // then
            verify(completedRoutineTodo).updateType(Type.BACKLOG);
            verify(completedRoutineTodo).updateBacklogOrder(4);
            verify(todoRepository).save(completedRoutineTodo);
        }

        @Test
        @DisplayName("[TC-BATCH-003] 미완료 할 일은 어제로 변경")
        void updateTodayTodos_incompleteToYesterday() {
            // given
            Long userId = 1L;
            Todo incompleteTodo = mock(Todo.class);
            when(incompleteTodo.getUserId()).thenReturn(userId);
            when(incompleteTodo.getTodayStatus()).thenReturn(TodayStatus.INCOMPLETE);

            when(todoRepository.findByType(Type.TODAY)).thenReturn(List.of(incompleteTodo));
            when(todoRepository.findMaxBacklogOrderByUserIdOrZero(userId)).thenReturn(2);

            // when
            todoBatchService.updateTodayTodosAndSave();

            // then
            verify(incompleteTodo).updateType(Type.YESTERDAY);
            verify(incompleteTodo).updateTodayOrder(null);
            verify(incompleteTodo).updateBacklogOrder(3);
            verify(todoRepository).save(incompleteTodo);
        }

        @Test
        @DisplayName("[TC-BATCH-004] 오늘 할 일이 없는 경우")
        void updateTodayTodos_emptyList() {
            // given
            when(todoRepository.findByType(Type.TODAY)).thenReturn(List.of());

            // when
            todoBatchService.updateTodayTodosAndSave();

            // then
            verify(todoRepository).findByType(Type.TODAY);
        }
    }

    @Nested
    @DisplayName("[SCN-SVC-TODO-BATCH-002] 마감기한/요일 반복 할 일을 오늘로 변경한다")
    class UpdateDeadlineTodos {

        @Test
        @DisplayName("[TC-DDL-001] 마감기한 할 일 업데이트")
        void updateDeadlineTodos_success() {
            // given
            ReflectionTestUtils.setField(todoBatchService, "batchSize", 100);
            List<Long> userIds = List.of(1L, 2L, 3L);
            when(userRepository.findAllUserIds()).thenReturn(userIds);
            doNothing().when(todoService).processUpdateDeadlineTodos(any(LocalDate.class), anyList());

            // when
            todoBatchService.updateDeadlineTodos();

            // then
            verify(userRepository).findAllUserIds();
            verify(todoService).processUpdateDeadlineTodos(any(LocalDate.class), anyList());
        }
    }
}
