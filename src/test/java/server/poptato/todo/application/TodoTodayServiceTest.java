package server.poptato.todo.application;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.BDDMockito.*;
import static org.mockito.BDDMockito.doNothing;
import static org.mockito.BDDMockito.mock;
import static org.mockito.BDDMockito.mockStatic;
import static org.mockito.BDDMockito.never;
import static org.mockito.BDDMockito.verify;
import static org.mockito.BDDMockito.when;
import static org.mockito.Mockito.eq;
import static org.mockito.Mockito.isNull;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.Collections;
import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockedStatic;

import server.poptato.configuration.ServiceTestConfig;
import server.poptato.infra.firebase.application.FcmNotificationBatchService;
import server.poptato.todo.api.request.EventCreateRequestDto;
import server.poptato.todo.api.request.TodayTodoCreateRequestDto;
import server.poptato.todo.application.response.TodayListResponseDto;
import server.poptato.todo.application.response.TodayTodoCreateResponseDto;
import server.poptato.todo.domain.entity.Todo;
import server.poptato.todo.domain.repository.RoutineRepository;
import server.poptato.todo.domain.repository.TodoRepository;
import server.poptato.todo.domain.value.TodayStatus;
import server.poptato.todo.domain.value.Type;
import server.poptato.user.domain.repository.UserRepository;
import server.poptato.user.domain.value.MobileType;
import server.poptato.user.validator.UserValidator;

class TodoTodayServiceTest extends ServiceTestConfig {

    @Mock
    private TodoRepository todoRepository;

    @Mock
    private RoutineRepository routineRepository;

    @Mock
    private UserRepository userRepository;

    @Mock
    private UserValidator userValidator;

    @Mock
    private FcmNotificationBatchService fcmNotificationBatchService;

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

    @Nested
    @DisplayName("[SCN-SVC-TODO-TODAY-002] 오늘의 할 일 목록을 조회한다")
    class GetTodayList {

        @Test
        @DisplayName("[TC-LIST-001] 오늘의 할 일 목록 조회 - 빈 목록")
        void getTodayList_emptyList() {
            // given
            Long userId = 1L;
            MobileType mobileType = MobileType.IOS;
            int page = 0;
            int size = 10;
            LocalDate todayDate = LocalDate.now();

            doNothing().when(userValidator).checkIsExistUser(userId);
            when(todoRepository.findIncompleteTodaysWithCategory(userId, Type.TODAY, todayDate, TodayStatus.INCOMPLETE))
                    .thenReturn(Collections.emptyList());
            when(todoRepository.findCompletedTodaysWithCategory(userId, todayDate))
                    .thenReturn(Collections.emptyList());

            // when
            TodayListResponseDto response = todoTodayService.getTodayList(userId, mobileType, page, size, todayDate);

            // then
            assertThat(response).isNotNull();
            assertThat(response.todays()).isEmpty();
            assertThat(response.totalPageCount()).isEqualTo(0);
        }

        @Test
        @DisplayName("[TC-LIST-002] 오늘의 할 일 목록 조회 - 할 일이 있는 경우")
        void getTodayList_withTodos() {
            // given
            Long userId = 1L;
            MobileType mobileType = MobileType.IOS;
            int page = 0;
            int size = 10;
            LocalDate todayDate = LocalDate.now();

            Todo incompleteTodo = mock(Todo.class);
            when(incompleteTodo.getId()).thenReturn(1L);
            when(incompleteTodo.getContent()).thenReturn("미완료 할 일");
            when(incompleteTodo.getTodayStatus()).thenReturn(TodayStatus.INCOMPLETE);
            when(incompleteTodo.isBookmark()).thenReturn(false);
            when(incompleteTodo.isRepeat()).thenReturn(false);
            when(incompleteTodo.isRoutine()).thenReturn(false);
            when(incompleteTodo.getDeadline()).thenReturn(null);
            when(incompleteTodo.getTime()).thenReturn(null);

            Todo completedTodo = mock(Todo.class);
            when(completedTodo.getId()).thenReturn(2L);
            when(completedTodo.getContent()).thenReturn("완료된 할 일");
            when(completedTodo.getTodayStatus()).thenReturn(TodayStatus.COMPLETED);
            when(completedTodo.isBookmark()).thenReturn(false);
            when(completedTodo.isRepeat()).thenReturn(false);
            when(completedTodo.isRoutine()).thenReturn(false);
            when(completedTodo.getDeadline()).thenReturn(null);
            when(completedTodo.getTime()).thenReturn(null);

            doNothing().when(userValidator).checkIsExistUser(userId);
            when(todoRepository.findIncompleteTodaysWithCategory(userId, Type.TODAY, todayDate, TodayStatus.INCOMPLETE))
                    .thenReturn(List.of(incompleteTodo));
            when(todoRepository.findCompletedTodaysWithCategory(userId, todayDate))
                    .thenReturn(List.of(completedTodo));
            when(routineRepository.findAllByTodoId(anyLong())).thenReturn(Collections.emptyList());

            // when
            TodayListResponseDto response = todoTodayService.getTodayList(userId, mobileType, page, size, todayDate);

            // then
            assertThat(response).isNotNull();
            assertThat(response.todays()).hasSize(2);
            assertThat(response.totalPageCount()).isEqualTo(1);
        }

        @Test
        @DisplayName("[TC-LIST-003] 오늘의 할 일 목록 조회 - 페이지 범위 초과")
        void getTodayList_pageOutOfRange() {
            // given
            Long userId = 1L;
            MobileType mobileType = MobileType.ANDROID;
            int page = 10;
            int size = 10;
            LocalDate todayDate = LocalDate.now();

            Todo todo = Todo.createTodayTodo(userId, "테스트", null, false, 1);

            doNothing().when(userValidator).checkIsExistUser(userId);
            when(todoRepository.findIncompleteTodaysWithCategory(userId, Type.TODAY, todayDate, TodayStatus.INCOMPLETE))
                    .thenReturn(List.of(todo));
            when(todoRepository.findCompletedTodaysWithCategory(userId, todayDate))
                    .thenReturn(Collections.emptyList());

            // when
            TodayListResponseDto response = todoTodayService.getTodayList(userId, mobileType, page, size, todayDate);

            // then
            assertThat(response).isNotNull();
            assertThat(response.todays()).isEmpty();
        }
    }

    @Nested
    @DisplayName("[SCN-SVC-TODO-TODAY-003] 이벤트를 생성하고 할 일을 배포한다")
    class CreateEventAndTodosIfNeeded {

        @Test
        @DisplayName("[TC-EVENT-001] 이벤트 생성 - Today Todo 생성하지 않음")
        void createEvent_withoutTodayTodo() {
            // given
            EventCreateRequestDto request = new EventCreateRequestDto(
                    "이벤트 알림",
                    "이벤트 내용입니다",
                    false,
                    false,
                    null,
                    null
            );

            doNothing().when(fcmNotificationBatchService).sendEventNotifications(any(), any());

            // when
            todoTodayService.createEventAndTodosIfNeeded(request);

            // then
            verify(fcmNotificationBatchService).sendEventNotifications("이벤트 알림", "이벤트 내용입니다");
            verify(userRepository, never()).findAllUserIds();
            verify(todoRepository, never()).saveAll(anyList());
        }

        @Test
        @DisplayName("[TC-EVENT-002] 이벤트 생성 - Today Todo도 함께 생성")
        void createEvent_withTodayTodo() {
            // given
            EventCreateRequestDto request = new EventCreateRequestDto(
                    "이벤트 알림",
                    "이벤트 내용입니다",
                    true,
                    true,
                    "오늘의 할 일 내용",
                    LocalTime.of(14, 0)
            );

            List<Long> userIds = List.of(1L, 2L, 3L);
            Map<Long, Integer> maxOrders = Map.of(1L, 5, 2L, 3, 3L, 10);

            doNothing().when(fcmNotificationBatchService).sendEventNotifications(any(), any());
            when(userRepository.findAllUserIds()).thenReturn(userIds);
            when(todoRepository.findMaxTodayOrdersByUserIdsOrZero(userIds)).thenReturn(maxOrders);

            // when
            todoTodayService.createEventAndTodosIfNeeded(request);

            // then
            verify(fcmNotificationBatchService).sendEventNotifications("이벤트 알림", "이벤트 내용입니다");
            verify(userRepository).findAllUserIds();
            verify(todoRepository).findMaxTodayOrdersByUserIdsOrZero(userIds);
            verify(todoRepository).saveAll(anyList());
        }
    }
}
