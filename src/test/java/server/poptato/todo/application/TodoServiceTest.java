package server.poptato.todo.application;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.List;
import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.test.util.ReflectionTestUtils;

import jakarta.persistence.EntityManager;
import server.poptato.category.domain.entity.Category;
import server.poptato.category.domain.repository.CategoryRepository;
import server.poptato.category.validator.CategoryValidator;
import server.poptato.configuration.ServiceTestConfig;
import server.poptato.emoji.domain.entity.Emoji;
import server.poptato.emoji.domain.repository.EmojiRepository;
import server.poptato.global.exception.CustomException;
import server.poptato.todo.api.request.ContentUpdateRequestDto;
import server.poptato.todo.api.request.DeadlineUpdateRequestDto;
import server.poptato.todo.api.request.RoutineUpdateRequestDto;
import server.poptato.todo.api.request.SwipeRequestDto;
import server.poptato.todo.api.request.TimeUpdateRequestDto;
import server.poptato.todo.api.request.TodoCategoryUpdateRequestDto;
import server.poptato.todo.api.request.CheckYesterdayTodosRequestDto;
import server.poptato.todo.api.request.TodoDragAndDropRequestDto;
import server.poptato.todo.application.response.PaginatedHistoryResponseDto;
import server.poptato.todo.application.response.TodoDetailResponseDto;
import server.poptato.todo.domain.entity.CompletedDateTime;
import server.poptato.todo.domain.entity.TimeAlarm;
import server.poptato.todo.domain.entity.Todo;
import server.poptato.todo.domain.repository.CompletedDateTimeRepository;
import server.poptato.todo.domain.repository.RoutineRepository;
import server.poptato.todo.domain.repository.TimeAlarmRepository;
import server.poptato.todo.domain.repository.TodoRepository;
import server.poptato.todo.domain.value.TodayStatus;
import server.poptato.todo.domain.value.Type;
import server.poptato.todo.status.TodoErrorStatus;
import server.poptato.user.domain.value.MobileType;
import server.poptato.user.validator.UserValidator;

class TodoServiceTest extends ServiceTestConfig {

    @Mock private UserValidator userValidator;
    @Mock private CategoryValidator categoryValidator;
    @Mock private TodoRepository todoRepository;
    @Mock private TimeAlarmRepository timeAlarmRepository;
    @Mock private RoutineRepository routineRepository;
    @Mock private CompletedDateTimeRepository completedDateTimeRepository;
    @Mock private CategoryRepository categoryRepository;
    @Mock private EmojiRepository emojiRepository;
    @Mock private EntityManager entityManager;

    @InjectMocks
    private TodoService todoService;

    @BeforeEach
    void setUp() {
        ReflectionTestUtils.setField(todoService, "entityManager", entityManager);
    }

    @Captor
    private ArgumentCaptor<Todo> todoCaptor;

    @Captor
    private ArgumentCaptor<CompletedDateTime> completedDateTimeCaptor;

    @Captor
    private ArgumentCaptor<TimeAlarm> timeAlarmCaptor;

    @Nested
    @DisplayName("[SCN-SVC-TODO-001] 할 일을 삭제한다")
    class DeleteTodoById {

        @Test
        @DisplayName("[TC-DEL-001] 성공: 본인의 할 일을 삭제한다 (soft delete)")
        void deleteTodoById_success() {
            // given
            Long userId = 1L;
            Long todoId = 100L;
            Todo todo = mock(Todo.class);
            when(todo.getUserId()).thenReturn(userId);

            doNothing().when(userValidator).checkIsExistUser(userId);
            when(todoRepository.findById(todoId)).thenReturn(Optional.of(todo));

            // when
            todoService.deleteTodoById(userId, todoId);

            // then
            verify(todoRepository).softDeleteById(todoId);
        }

        @Test
        @DisplayName("[TC-DEL-002] 실패: 존재하지 않는 할 일")
        void deleteTodoById_todoNotExist() {
            // given
            Long userId = 1L;
            Long todoId = 100L;

            doNothing().when(userValidator).checkIsExistUser(userId);
            when(todoRepository.findById(todoId)).thenReturn(Optional.empty());

            // when & then
            assertThatThrownBy(() -> todoService.deleteTodoById(userId, todoId))
                    .isInstanceOf(CustomException.class)
                    .hasFieldOrPropertyWithValue("errorCode", TodoErrorStatus._TODO_NOT_EXIST);
        }

        @Test
        @DisplayName("[TC-DEL-003] 실패: 다른 사용자의 할 일")
        void deleteTodoById_userNotMatch() {
            // given
            Long userId = 1L;
            Long otherUserId = 2L;
            Long todoId = 100L;
            Todo todo = createTodo(otherUserId, todoId, Type.BACKLOG);

            doNothing().when(userValidator).checkIsExistUser(userId);
            when(todoRepository.findById(todoId)).thenReturn(Optional.of(todo));

            // when & then
            assertThatThrownBy(() -> todoService.deleteTodoById(userId, todoId))
                    .isInstanceOf(CustomException.class)
                    .hasFieldOrPropertyWithValue("errorCode", TodoErrorStatus._TODO_USER_NOT_MATCH);
        }
    }

    @Nested
    @DisplayName("[SCN-SVC-TODO-002] 북마크를 토글한다")
    class ToggleIsBookmark {

        @Test
        @DisplayName("[TC-BMK-001] 성공: 북마크를 활성화한다")
        void toggleIsBookmark_enable() {
            // given
            Long userId = 1L;
            Long todoId = 100L;
            Todo todo = mock(Todo.class);
            when(todo.getUserId()).thenReturn(userId);

            when(todoRepository.findById(todoId)).thenReturn(Optional.of(todo));

            // when
            todoService.toggleIsBookmark(userId, todoId);

            // then
            verify(todo).toggleBookmark();
        }
    }

    @Nested
    @DisplayName("[SCN-SVC-TODO-003] 스와이프한다")
    class Swipe {

        @Test
        @DisplayName("[TC-SWP-001] 백로그 -> 오늘로 스와이프")
        void swipe_backlogToToday() {
            // given
            Long userId = 1L;
            Long todoId = 100L;
            Todo todo = mock(Todo.class);
            when(todo.getUserId()).thenReturn(userId);
            when(todo.getType()).thenReturn(Type.BACKLOG);

            doNothing().when(userValidator).checkIsExistUser(userId);
            when(todoRepository.findById(todoId)).thenReturn(Optional.of(todo));
            when(todoRepository.findMaxTodayOrderByUserIdOrZero(userId)).thenReturn(5);

            SwipeRequestDto requestDto = new SwipeRequestDto(todoId);

            // when
            todoService.swipe(userId, requestDto);

            // then
            verify(todo).changeToToday(5);
        }

        @Test
        @DisplayName("[TC-SWP-002] 오늘 -> 백로그로 스와이프 (미완료 상태)")
        void swipe_todayToBacklog_incomplete() {
            // given
            Long userId = 1L;
            Long todoId = 100L;
            Todo todo = mock(Todo.class);
            when(todo.getUserId()).thenReturn(userId);
            when(todo.getType()).thenReturn(Type.TODAY);
            when(todo.getTodayStatus()).thenReturn(TodayStatus.INCOMPLETE);

            doNothing().when(userValidator).checkIsExistUser(userId);
            when(todoRepository.findById(todoId)).thenReturn(Optional.of(todo));
            when(todoRepository.findMaxBacklogOrderByUserIdOrZero(userId)).thenReturn(10);

            SwipeRequestDto requestDto = new SwipeRequestDto(todoId);

            // when
            todoService.swipe(userId, requestDto);

            // then
            verify(todo).changeToBacklog(10);
        }

        @Test
        @DisplayName("[TC-SWP-003] 실패: 이미 완료된 할 일 스와이프 시도")
        void swipe_alreadyCompleted() {
            // given
            Long userId = 1L;
            Long todoId = 100L;
            Todo todo = mock(Todo.class);
            when(todo.getUserId()).thenReturn(userId);
            when(todo.getType()).thenReturn(Type.TODAY);
            when(todo.getTodayStatus()).thenReturn(TodayStatus.COMPLETED);

            doNothing().when(userValidator).checkIsExistUser(userId);
            when(todoRepository.findById(todoId)).thenReturn(Optional.of(todo));

            SwipeRequestDto requestDto = new SwipeRequestDto(todoId);

            // when & then
            assertThatThrownBy(() -> todoService.swipe(userId, requestDto))
                    .isInstanceOf(CustomException.class)
                    .hasFieldOrPropertyWithValue("errorCode", TodoErrorStatus._ALREADY_COMPLETED_TODO);
        }
    }

    @Nested
    @DisplayName("[SCN-SVC-TODO-004] 드래그 앤 드롭으로 순서를 변경한다")
    class DragAndDrop {

        @Test
        @DisplayName("[TC-DND-001] TODAY 타입 드래그 앤 드롭")
        void dragAndDrop_today() {
            // given
            Long userId = 1L;
            Todo todo1 = mock(Todo.class);
            Todo todo2 = mock(Todo.class);
            when(todo1.getUserId()).thenReturn(userId);
            when(todo2.getUserId()).thenReturn(userId);
            when(todo1.getTodayStatus()).thenReturn(TodayStatus.INCOMPLETE);
            when(todo2.getTodayStatus()).thenReturn(TodayStatus.INCOMPLETE);
            when(todo1.getTodayOrder()).thenReturn(1);
            when(todo2.getTodayOrder()).thenReturn(2);

            doNothing().when(userValidator).checkIsExistUser(userId);
            when(todoRepository.findById(1L)).thenReturn(Optional.of(todo1));
            when(todoRepository.findById(2L)).thenReturn(Optional.of(todo2));

            TodoDragAndDropRequestDto requestDto = new TodoDragAndDropRequestDto(Type.TODAY, List.of(2L, 1L));

            // when
            todoService.dragAndDrop(userId, requestDto);

            // then
            verify(todoRepository).save(todo2);
            verify(todoRepository).save(todo1);
        }

        @Test
        @DisplayName("[TC-DND-002] BACKLOG 타입 드래그 앤 드롭")
        void dragAndDrop_backlog() {
            // given
            Long userId = 1L;
            Todo todo1 = mock(Todo.class);
            Todo todo2 = mock(Todo.class);
            when(todo1.getUserId()).thenReturn(userId);
            when(todo2.getUserId()).thenReturn(userId);
            when(todo1.getTodayStatus()).thenReturn(null);
            when(todo2.getTodayStatus()).thenReturn(null);
            when(todo1.getBacklogOrder()).thenReturn(1);
            when(todo2.getBacklogOrder()).thenReturn(2);

            doNothing().when(userValidator).checkIsExistUser(userId);
            when(todoRepository.findById(1L)).thenReturn(Optional.of(todo1));
            when(todoRepository.findById(2L)).thenReturn(Optional.of(todo2));

            TodoDragAndDropRequestDto requestDto = new TodoDragAndDropRequestDto(Type.BACKLOG, List.of(2L, 1L));

            // when
            todoService.dragAndDrop(userId, requestDto);

            // then
            verify(todoRepository).save(todo2);
            verify(todoRepository).save(todo1);
        }
    }

    @Nested
    @DisplayName("[SCN-SVC-TODO-005] 할 일 상세 정보를 조회한다")
    class GetTodoInfo {

        @Test
        @DisplayName("[TC-INFO-001] 카테고리와 이모지가 있는 할 일 조회")
        void getTodoInfo_withCategoryAndEmoji() {
            // given
            Long userId = 1L;
            Long todoId = 100L;
            Long categoryId = 10L;
            Long emojiId = 5L;
            MobileType mobileType = MobileType.IOS;

            Todo todo = mock(Todo.class);
            when(todo.getUserId()).thenReturn(userId);
            when(todo.getCategoryId()).thenReturn(categoryId);
            when(todo.getContent()).thenReturn("테스트 할 일");
            when(todo.getTime()).thenReturn(LocalTime.of(10, 0));
            when(todo.getDeadline()).thenReturn(LocalDate.now().plusDays(7));
            when(todo.isBookmark()).thenReturn(false);
            when(todo.isRepeat()).thenReturn(false);
            when(todo.isRoutine()).thenReturn(false);

            Category category = mock(Category.class);
            when(category.getName()).thenReturn("운동");
            when(category.getEmojiId()).thenReturn(emojiId);

            Emoji emoji = mock(Emoji.class);
            when(emoji.getImageUrl()).thenReturn("http://example.com/emoji.png");

            doNothing().when(userValidator).checkIsExistUser(userId);
            when(todoRepository.findById(todoId)).thenReturn(Optional.of(todo));
            when(categoryRepository.findById(categoryId)).thenReturn(Optional.of(category));
            when(emojiRepository.findById(emojiId)).thenReturn(Optional.of(emoji));
            when(routineRepository.findAllByTodoId(todoId)).thenReturn(List.of());

            // when
            TodoDetailResponseDto response = todoService.getTodoInfo(userId, mobileType, todoId);

            // then
            assertThat(response).isNotNull();
            verify(categoryRepository).findById(categoryId);
            verify(emojiRepository).findById(emojiId);
        }

        @Test
        @DisplayName("[TC-INFO-002] 카테고리가 없는 할 일 조회")
        void getTodoInfo_withoutCategory() {
            // given
            Long userId = 1L;
            Long todoId = 100L;
            MobileType mobileType = MobileType.ANDROID;

            Todo todo = mock(Todo.class);
            when(todo.getUserId()).thenReturn(userId);
            when(todo.getCategoryId()).thenReturn(null);
            when(todo.getContent()).thenReturn("테스트 할 일");
            when(todo.getTime()).thenReturn(null);
            when(todo.getDeadline()).thenReturn(null);
            when(todo.isBookmark()).thenReturn(false);
            when(todo.isRepeat()).thenReturn(false);
            when(todo.isRoutine()).thenReturn(false);

            doNothing().when(userValidator).checkIsExistUser(userId);
            when(todoRepository.findById(todoId)).thenReturn(Optional.of(todo));
            when(routineRepository.findAllByTodoId(todoId)).thenReturn(List.of());

            // when
            TodoDetailResponseDto response = todoService.getTodoInfo(userId, mobileType, todoId);

            // then
            assertThat(response).isNotNull();
            verify(categoryRepository, never()).findById(anyLong());
        }
    }

    @Nested
    @DisplayName("[SCN-SVC-TODO-006] 시간을 업데이트한다")
    class UpdateTime {

        @Test
        @DisplayName("[TC-TIME-001] 시간 설정: 알람이 없으면 새로 생성")
        void updateTime_createAlarm() {
            // given
            Long userId = 1L;
            Long todoId = 100L;
            LocalTime newTime = LocalTime.of(14, 30);

            Todo todo = mock(Todo.class);
            when(todo.getUserId()).thenReturn(userId);

            doNothing().when(userValidator).checkIsExistUser(userId);
            when(todoRepository.findById(todoId)).thenReturn(Optional.of(todo));
            when(timeAlarmRepository.findByTodoId(todoId)).thenReturn(Optional.empty());

            TimeUpdateRequestDto requestDto = new TimeUpdateRequestDto(newTime);

            // when
            todoService.updateTime(userId, todoId, requestDto);

            // then
            verify(timeAlarmRepository).save(any(TimeAlarm.class));
            verify(todo).updateTime(newTime);
        }

        @Test
        @DisplayName("[TC-TIME-002] 시간 설정: 기존 알람 업데이트")
        void updateTime_updateExistingAlarm() {
            // given
            Long userId = 1L;
            Long todoId = 100L;
            LocalTime newTime = LocalTime.of(14, 30);

            Todo todo = mock(Todo.class);
            when(todo.getUserId()).thenReturn(userId);

            TimeAlarm existingAlarm = mock(TimeAlarm.class);

            doNothing().when(userValidator).checkIsExistUser(userId);
            when(todoRepository.findById(todoId)).thenReturn(Optional.of(todo));
            when(timeAlarmRepository.findByTodoId(todoId)).thenReturn(Optional.of(existingAlarm));

            TimeUpdateRequestDto requestDto = new TimeUpdateRequestDto(newTime);

            // when
            todoService.updateTime(userId, todoId, requestDto);

            // then
            verify(existingAlarm).updateNotified(false);
            verify(timeAlarmRepository).save(existingAlarm);
            verify(todo).updateTime(newTime);
        }

        @Test
        @DisplayName("[TC-TIME-003] 시간 삭제: 기존 알람 삭제")
        void updateTime_deleteAlarm() {
            // given
            Long userId = 1L;
            Long todoId = 100L;

            Todo todo = mock(Todo.class);
            when(todo.getUserId()).thenReturn(userId);

            TimeAlarm existingAlarm = mock(TimeAlarm.class);

            doNothing().when(userValidator).checkIsExistUser(userId);
            when(todoRepository.findById(todoId)).thenReturn(Optional.of(todo));
            when(timeAlarmRepository.findByTodoId(todoId)).thenReturn(Optional.of(existingAlarm));

            TimeUpdateRequestDto requestDto = new TimeUpdateRequestDto(null);

            // when
            todoService.updateTime(userId, todoId, requestDto);

            // then
            verify(timeAlarmRepository).delete(existingAlarm);
            verify(todo).updateTime(null);
        }
    }

    @Nested
    @DisplayName("[SCN-SVC-TODO-007] 마감기한을 업데이트한다")
    class UpdateDeadline {

        @Test
        @DisplayName("[TC-DDL-001] 성공: 마감기한 설정")
        void updateDeadline_success() {
            // given
            Long userId = 1L;
            Long todoId = 100L;
            LocalDate deadline = LocalDate.now().plusDays(7);

            Todo todo = mock(Todo.class);
            when(todo.getUserId()).thenReturn(userId);

            doNothing().when(userValidator).checkIsExistUser(userId);
            when(todoRepository.findById(todoId)).thenReturn(Optional.of(todo));

            DeadlineUpdateRequestDto requestDto = new DeadlineUpdateRequestDto(deadline);

            // when
            todoService.updateDeadline(userId, todoId, requestDto);

            // then
            verify(todo).updateDeadline(deadline);
        }
    }

    @Nested
    @DisplayName("[SCN-SVC-TODO-008] 루틴을 관리한다")
    class ManageRoutine {

        @Test
        @DisplayName("[TC-RTN-001] 루틴 생성")
        void createRoutine_success() {
            // given
            Long userId = 1L;
            Long todoId = 100L;
            List<String> routineDays = List.of("월", "수", "금");

            Todo todo = mock(Todo.class);
            when(todo.getUserId()).thenReturn(userId);

            doNothing().when(userValidator).checkIsExistUser(userId);
            when(todoRepository.findById(todoId)).thenReturn(Optional.of(todo));

            RoutineUpdateRequestDto requestDto = new RoutineUpdateRequestDto(routineDays);

            // when
            todoService.createRoutine(userId, todoId, requestDto);

            // then
            verify(todo).setRoutine(true);
            verify(todo).setRepeat(false);
            verify(routineRepository).deleteByTodoId(todoId);
            verify(routineRepository).saveAll(anyList());
        }

        @Test
        @DisplayName("[TC-RTN-002] 루틴 삭제")
        void deleteRoutine_success() {
            // given
            Long userId = 1L;
            Long todoId = 100L;

            Todo todo = mock(Todo.class);
            when(todo.getUserId()).thenReturn(userId);

            doNothing().when(userValidator).checkIsExistUser(userId);
            when(todoRepository.findById(todoId)).thenReturn(Optional.of(todo));

            // when
            todoService.deleteRoutine(userId, todoId);

            // then
            verify(todo).setRoutine(false);
            verify(routineRepository).deleteByTodoId(todoId);
        }
    }

    @Nested
    @DisplayName("[SCN-SVC-TODO-009] 내용을 업데이트한다")
    class UpdateContent {

        @Test
        @DisplayName("[TC-CNT-001] 성공: 내용 수정")
        void updateContent_success() {
            // given
            Long userId = 1L;
            Long todoId = 100L;
            String newContent = "수정된 할 일 내용";

            Todo todo = mock(Todo.class);
            when(todo.getUserId()).thenReturn(userId);

            doNothing().when(userValidator).checkIsExistUser(userId);
            when(todoRepository.findById(todoId)).thenReturn(Optional.of(todo));

            ContentUpdateRequestDto requestDto = new ContentUpdateRequestDto(newContent);

            // when
            todoService.updateContent(userId, todoId, requestDto);

            // then
            verify(todo).updateContent(newContent);
        }
    }

    @Nested
    @DisplayName("[SCN-SVC-TODO-010] 완료 상태를 업데이트한다")
    class UpdateIsCompleted {

        @Test
        @DisplayName("[TC-CMP-001] 미완료 -> 완료 변경")
        void updateIsCompleted_incompleteToComplete() {
            // given
            Long userId = 1L;
            Long todoId = 100L;

            Todo todo = mock(Todo.class);
            when(todo.getUserId()).thenReturn(userId);
            when(todo.getId()).thenReturn(todoId);
            when(todo.getTodayStatus()).thenReturn(TodayStatus.INCOMPLETE);

            doNothing().when(userValidator).checkIsExistUser(userId);
            when(todoRepository.findById(todoId)).thenReturn(Optional.of(todo));

            // when
            todoService.updateIsCompleted(userId, todoId);

            // then
            verify(todo).completeTodayTodo();
            verify(completedDateTimeRepository).save(any(CompletedDateTime.class));
        }

        @Test
        @DisplayName("[TC-CMP-002] 완료 -> 미완료 변경")
        void updateIsCompleted_completeToIncomplete() {
            // given
            Long userId = 1L;
            Long todoId = 100L;
            LocalDate todayDate = LocalDate.now();

            Todo todo = mock(Todo.class);
            when(todo.getUserId()).thenReturn(userId);
            when(todo.getId()).thenReturn(todoId);
            when(todo.getTodayStatus()).thenReturn(TodayStatus.COMPLETED);
            when(todo.getTodayDate()).thenReturn(todayDate);

            CompletedDateTime completedDateTime = mock(CompletedDateTime.class);

            doNothing().when(userValidator).checkIsExistUser(userId);
            when(todoRepository.findById(todoId)).thenReturn(Optional.of(todo));
            when(todoRepository.findMinTodayOrderByUserIdOrZero(userId)).thenReturn(1);
            when(completedDateTimeRepository.findByTodoIdAndDate(todoId, todayDate))
                    .thenReturn(Optional.of(completedDateTime));

            // when
            todoService.updateIsCompleted(userId, todoId);

            // then
            verify(todo).incompleteTodayTodo(1);
            verify(completedDateTimeRepository).delete(completedDateTime);
        }
    }

    @Nested
    @DisplayName("[SCN-SVC-TODO-011] 카테고리를 업데이트한다")
    class UpdateCategory {

        @Test
        @DisplayName("[TC-CAT-001] 성공: 카테고리 변경")
        void updateCategory_success() {
            // given
            Long userId = 1L;
            Long todoId = 100L;
            Long newCategoryId = 20L;

            Todo todo = mock(Todo.class);
            when(todo.getUserId()).thenReturn(userId);

            doNothing().when(userValidator).checkIsExistUser(userId);
            when(todoRepository.findById(todoId)).thenReturn(Optional.of(todo));
            doNothing().when(categoryValidator).validateCategory(userId, newCategoryId);

            TodoCategoryUpdateRequestDto requestDto = new TodoCategoryUpdateRequestDto(newCategoryId);

            // when
            todoService.updateCategory(userId, todoId, requestDto);

            // then
            verify(categoryValidator).validateCategory(userId, newCategoryId);
            verify(todo).updateCategory(newCategoryId);
        }

        @Test
        @DisplayName("[TC-CAT-002] 성공: 카테고리 해제 (null)")
        void updateCategory_removeCategory() {
            // given
            Long userId = 1L;
            Long todoId = 100L;

            Todo todo = mock(Todo.class);
            when(todo.getUserId()).thenReturn(userId);

            doNothing().when(userValidator).checkIsExistUser(userId);
            when(todoRepository.findById(todoId)).thenReturn(Optional.of(todo));

            TodoCategoryUpdateRequestDto requestDto = new TodoCategoryUpdateRequestDto(null);

            // when
            todoService.updateCategory(userId, todoId, requestDto);

            // then
            verify(categoryValidator, never()).validateCategory(anyLong(), anyLong());
            verify(todo).updateCategory(null);
        }
    }

    @Nested
    @DisplayName("[SCN-SVC-TODO-012] 반복 설정을 관리한다")
    class ManageRepeat {

        @Test
        @DisplayName("[TC-RPT-001] 반복 토글")
        void updateIsRepeat_toggle() {
            // given
            Long userId = 1L;
            Long todoId = 100L;

            Todo todo = mock(Todo.class);
            when(todo.getUserId()).thenReturn(userId);

            doNothing().when(userValidator).checkIsExistUser(userId);
            when(todoRepository.findById(todoId)).thenReturn(Optional.of(todo));

            // when
            todoService.updateIsRepeat(userId, todoId);

            // then
            verify(todo).toggleRepeat();
        }

        @Test
        @DisplayName("[TC-RPT-002] 일반 반복 생성")
        void createIsRepeat_success() {
            // given
            Long userId = 1L;
            Long todoId = 100L;

            Todo todo = mock(Todo.class);
            when(todo.getUserId()).thenReturn(userId);

            doNothing().when(userValidator).checkIsExistUser(userId);
            when(todoRepository.findById(todoId)).thenReturn(Optional.of(todo));

            // when
            todoService.createIsRepeat(userId, todoId);

            // then
            verify(todo).setRepeat(true);
            verify(todo).setRoutine(false);
            verify(routineRepository).deleteByTodoId(todoId);
        }

        @Test
        @DisplayName("[TC-RPT-003] 일반 반복 삭제")
        void deleteIsRepeat_success() {
            // given
            Long userId = 1L;
            Long todoId = 100L;

            Todo todo = mock(Todo.class);
            when(todo.getUserId()).thenReturn(userId);

            doNothing().when(userValidator).checkIsExistUser(userId);
            when(todoRepository.findById(todoId)).thenReturn(Optional.of(todo));

            // when
            todoService.deleteIsRepeat(userId, todoId);

            // then
            verify(todo).setRepeat(false);
        }
    }

    @Nested
    @DisplayName("[SCN-SVC-TODO-013] 히스토리 캘린더를 조회한다")
    class GetHistoriesCalendar {

        @Test
        @DisplayName("[TC-CAL-001] 레거시 캘린더 조회")
        void getLegacyHistoriesCalendar_success() {
            // given
            Long userId = 1L;
            String year = "2025";
            int month = 1;

            List<LocalDateTime> historyDates = List.of(
                    LocalDateTime.of(2025, 1, 5, 10, 0),
                    LocalDateTime.of(2025, 1, 10, 14, 30)
            );

            when(completedDateTimeRepository.findHistoryExistingDates(userId, year, month))
                    .thenReturn(historyDates);

            // when
            List<LocalDate> result = todoService.getLegacyHistoriesCalendar(userId, year, month);

            // then
            assertThat(result).hasSize(2);
            assertThat(result).contains(LocalDate.of(2025, 1, 5), LocalDate.of(2025, 1, 10));
        }
    }

    @Nested
    @DisplayName("[SCN-SVC-TODO-015] 히스토리를 조회한다")
    class GetHistories {

        @Test
        @DisplayName("[TC-HST-001] 과거 날짜 히스토리 조회")
        void getHistories_pastDate() {
            // given
            Long userId = 1L;
            LocalDate pastDate = LocalDate.now().minusDays(7);
            int page = 0;
            int size = 10;

            Todo todo = mock(Todo.class);
            Page<Todo> todoPage = new PageImpl<>(List.of(todo));

            doNothing().when(userValidator).checkIsExistUser(userId);
            when(todoRepository.findHistories(eq(userId), eq(pastDate), any(PageRequest.class)))
                    .thenReturn(todoPage);

            // when
            PaginatedHistoryResponseDto result = todoService.getHistories(userId, pastDate, page, size);

            // then
            assertThat(result).isNotNull();
            verify(todoRepository).findHistories(eq(userId), eq(pastDate), any(PageRequest.class));
        }

        @Test
        @DisplayName("[TC-HST-002] 오늘 날짜 히스토리 조회")
        void getHistories_today() {
            // given
            Long userId = 1L;
            LocalDate today = LocalDate.now();
            int page = 0;
            int size = 10;

            doNothing().when(userValidator).checkIsExistUser(userId);
            when(todoRepository.findIncompleteTodays(userId, Type.TODAY, today, TodayStatus.INCOMPLETE))
                    .thenReturn(List.of());
            when(todoRepository.findCompletedTodays(userId, today))
                    .thenReturn(List.of());

            // when
            PaginatedHistoryResponseDto result = todoService.getHistories(userId, today, page, size);

            // then
            assertThat(result).isNotNull();
            verify(todoRepository).findIncompleteTodays(userId, Type.TODAY, today, TodayStatus.INCOMPLETE);
            verify(todoRepository).findCompletedTodays(userId, today);
        }

        @Test
        @DisplayName("[TC-HST-003] 미래 날짜 히스토리 조회 (마감기한 + 루틴)")
        void getHistories_futureDate() {
            // given
            Long userId = 1L;
            LocalDate futureDate = LocalDate.now().plusDays(7);
            int page = 0;
            int size = 10;

            doNothing().when(userValidator).checkIsExistUser(userId);
            when(todoRepository.findTodosByDeadLine(userId, futureDate))
                    .thenReturn(List.of());
            when(todoRepository.findRoutineTodosByDay(eq(userId), anyString()))
                    .thenReturn(List.of());

            // when
            PaginatedHistoryResponseDto result = todoService.getHistories(userId, futureDate, page, size);

            // then
            assertThat(result).isNotNull();
            verify(todoRepository).findTodosByDeadLine(userId, futureDate);
            verify(todoRepository).findRoutineTodosByDay(eq(userId), anyString());
        }
    }

    @Nested
    @DisplayName("[SCN-SVC-TODO-016] 마감기한/루틴 기반 오늘로 이동한다")
    class ProcessUpdateDeadlineTodos {

        @Test
        @DisplayName("[TC-DL-001] 마감기한이 오늘인 할 일이 오늘로 이동한다")
        void processUpdateDeadlineTodos_deadlineMatched() {
            // given
            Long userId = 1L;
            LocalDate today = LocalDate.now();

            Todo deadlineTodo = mock(Todo.class);

            when(todoRepository.findMaxTodayOrderByUserIdOrZero(userId)).thenReturn(5);
            when(todoRepository.findTodosByDeadLine(userId, today))
                    .thenReturn(List.of(deadlineTodo));
            when(todoRepository.findRoutineTodosByDay(eq(userId), anyString()))
                    .thenReturn(List.of());

            // when
            todoService.processUpdateDeadlineTodos(today, List.of(userId));

            // then
            verify(deadlineTodo).changeToToday(5);
        }

        @Test
        @DisplayName("[TC-DL-002] 오늘 요일 루틴 할 일이 오늘로 이동한다")
        void processUpdateDeadlineTodos_routineMatched() {
            // given
            Long userId = 1L;
            LocalDate today = LocalDate.now();

            Todo routineTodo = mock(Todo.class);

            when(todoRepository.findMaxTodayOrderByUserIdOrZero(userId)).thenReturn(5);
            when(todoRepository.findTodosByDeadLine(userId, today))
                    .thenReturn(List.of());
            when(todoRepository.findRoutineTodosByDay(eq(userId), anyString()))
                    .thenReturn(List.of(routineTodo));

            // when
            todoService.processUpdateDeadlineTodos(today, List.of(userId));

            // then
            verify(routineTodo).changeToToday(5);
        }

        @Test
        @DisplayName("[TC-DL-003] 마감기한과 루틴 모두 매칭되면 순서대로 이동한다")
        void processUpdateDeadlineTodos_bothMatched() {
            // given
            Long userId = 1L;
            LocalDate today = LocalDate.now();

            Todo deadlineTodo = mock(Todo.class);
            Todo routineTodo = mock(Todo.class);

            when(todoRepository.findMaxTodayOrderByUserIdOrZero(userId)).thenReturn(5);
            when(todoRepository.findTodosByDeadLine(userId, today))
                    .thenReturn(List.of(deadlineTodo));
            when(todoRepository.findRoutineTodosByDay(eq(userId), anyString()))
                    .thenReturn(List.of(routineTodo));

            // when
            todoService.processUpdateDeadlineTodos(today, List.of(userId));

            // then
            verify(deadlineTodo).changeToToday(5);
            verify(routineTodo).changeToToday(6);
        }
    }

    private Todo createTodo(Long userId, Long todoId, Type type) {
        return Todo.builder()
                .userId(userId)
                .content("테스트 할 일")
                .type(type)
                .build();
    }

    @Nested
    @DisplayName("[SCN-SVC-TODO-012] 어제 할 일을 체크한다")
    class CheckYesterdayTodos {

        @Test
        @DisplayName("[TC-YESTERDAY-001] 체크되지 않은 이벤트 Todo는 soft delete 된다")
        void checkYesterdayTodos_uncheckedEventTodo_softDeleted() {
            // given
            Long userId = 1L;
            Long eventTodoId = 100L;

            Todo eventTodo = mock(Todo.class);
            when(eventTodo.getId()).thenReturn(eventTodoId);
            when(eventTodo.isEvent()).thenReturn(true);

            when(todoRepository.findIncompleteYesterdays(userId)).thenReturn(List.of(eventTodo));

            CheckYesterdayTodosRequestDto request = new CheckYesterdayTodosRequestDto(List.of());

            // when
            todoService.checkYesterdayTodos(userId, request);

            // then
            verify(todoRepository).softDeleteByIds(List.of(eventTodoId));
        }

        @Test
        @DisplayName("[TC-YESTERDAY-002] 체크되지 않은 일반 Todo는 백로그로 이동된다")
        void checkYesterdayTodos_uncheckedNormalTodo_movedToBacklog() {
            // given
            Long userId = 1L;

            Todo normalTodo = mock(Todo.class);
            when(normalTodo.getId()).thenReturn(200L);
            when(normalTodo.isEvent()).thenReturn(false);

            when(todoRepository.findIncompleteYesterdays(userId)).thenReturn(List.of(normalTodo));

            CheckYesterdayTodosRequestDto request = new CheckYesterdayTodosRequestDto(List.of());

            // when
            todoService.checkYesterdayTodos(userId, request);

            // then
            verify(normalTodo).updateType(Type.BACKLOG);
            verify(todoRepository, never()).softDeleteByIds(anyList());
        }

        @Test
        @DisplayName("[TC-YESTERDAY-003] 삭제할 이벤트가 없으면 softDeleteByIds를 호출하지 않는다")
        void checkYesterdayTodos_noEventToDelete_noSoftDelete() {
            // given
            Long userId = 1L;

            when(todoRepository.findIncompleteYesterdays(userId)).thenReturn(List.of());

            CheckYesterdayTodosRequestDto request = new CheckYesterdayTodosRequestDto(List.of());

            // when
            todoService.checkYesterdayTodos(userId, request);

            // then
            verify(todoRepository, never()).softDeleteByIds(anyList());
        }
    }
}
