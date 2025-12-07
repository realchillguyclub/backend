package server.poptato.todo.application;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockedStatic;
import server.poptato.configuration.ServiceTestConfig;
import server.poptato.todo.api.request.TodayTodoCreateRequestDto;
import server.poptato.todo.application.response.TodayTodoCreateResponseDto;
import server.poptato.todo.domain.entity.Todo;
import server.poptato.todo.domain.repository.TodoRepository;
import server.poptato.user.validator.UserValidator;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.*;

class TodoTodayServiceTest extends ServiceTestConfig {

    @Mock
    private TodoRepository todoRepository;

    @Mock
    private UserValidator userValidator;

    @InjectMocks
    private TodoTodayService todoTodayService;

    @Test
    @DisplayName("[SCN-SVC-TODO-TODAY-001][TC-CREATE-001] 오늘 할 일을 성공적으로 생성한다.")
    void create_today_todo_success() {
        // given
        Long userId = 1L;
        String content = "today-todo";
        TodayTodoCreateRequestDto requestDto = new TodayTodoCreateRequestDto(content);

        Integer maxTodayOrder = 3;

        given(todoRepository.findMaxTodayOrderByUserIdOrZero(userId)).willReturn(maxTodayOrder);

        Todo mockTodo = mock(Todo.class);
        given(mockTodo.getId()).willReturn(100L);

        // when
        TodayTodoCreateResponseDto response;

        try (MockedStatic<Todo> mockedStatic = mockStatic(Todo.class)) {
            mockedStatic.when(() ->
                            Todo.createTodayTodo(
                                    eq(userId),
                                    eq(content),
                                    isNull(),
                                    eq(false),
                                    eq(maxTodayOrder)
                            )
                    )
                    .thenReturn(mockTodo);

            response = todoTodayService.createTodayTodo(userId, requestDto);

            // then
            mockedStatic.verify(() ->
                    Todo.createTodayTodo(
                            eq(userId),
                            eq(content),
                            isNull(),
                            eq(false),
                            eq(maxTodayOrder)
                    )
            );
        }

        // then
        verify(userValidator).checkIsExistUser(userId);
        verify(todoRepository).findMaxTodayOrderByUserIdOrZero(userId);
        verify(todoRepository).save(mockTodo);

        assertThat(response).isNotNull();
        assertThat(response.todoId()).isEqualTo(100L);
    }
}
