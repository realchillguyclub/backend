package server.poptato.todo.application;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;
import static org.mockito.Mockito.anyList;

import java.util.Collections;
import java.util.List;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.MethodOrderer;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestMethodOrder;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;

import server.poptato.category.domain.entity.Category;
import server.poptato.category.domain.repository.CategoryRepository;
import server.poptato.category.validator.CategoryValidator;
import server.poptato.configuration.ServiceTestConfig;
import server.poptato.todo.api.request.BacklogCreateRequestDto;
import server.poptato.todo.application.response.BacklogCreateResponseDto;
import server.poptato.todo.application.response.BacklogListResponseDto;
import server.poptato.todo.application.response.PaginatedYesterdayResponseDto;
import server.poptato.todo.domain.entity.Routine;
import server.poptato.todo.domain.entity.Todo;
import server.poptato.todo.domain.repository.RoutineRepository;
import server.poptato.todo.domain.repository.TodoRepository;
import server.poptato.todo.domain.value.TodayStatus;
import server.poptato.todo.domain.value.Type;
import server.poptato.user.domain.value.MobileType;
import server.poptato.user.validator.UserValidator;

class TodoBacklogServiceTest extends ServiceTestConfig {

    @Mock private TodoRepository todoRepository;
    @Mock private CategoryRepository categoryRepository;
    @Mock private RoutineRepository routineRepository;
    @Mock private UserValidator userValidator;
    @Mock private CategoryValidator categoryValidator;

    @InjectMocks
    private TodoBacklogService backlogService;

    @Nested
    @TestMethodOrder(MethodOrderer.DisplayName.class)
    @DisplayName("[SCN-SVC-BACKLOG-001] 백로그 목록을 조회한다")
    class GetBacklogList {

        @Test
        @DisplayName("[TC-GET-001] 일반 카테고리 ID 조회 시: Validator를 통해 검증 및 이름을 가져오고, Routine을 일괄 조회한다")
        void get_backlog_list_with_normal_category() {
            // given
            Long userId = 1L;
            Long categoryId = 10L;
            int page = 0;
            int size = 10;
            MobileType mobileType = MobileType.IOS;

            doNothing().when(userValidator).checkIsExistUser(userId);

            Category mockCategory = mock(Category.class);
            when(mockCategory.getName()).thenReturn("운동");
            when(categoryValidator.validateAndReturnCategory(userId, categoryId))
                    .thenReturn(mockCategory);

            Todo todo = mock(Todo.class);
            when(todo.getId()).thenReturn(100L);
            Page<Todo> todoPage = new PageImpl<>(List.of(todo));

            Routine routine = mock(Routine.class);
            when(routine.getTodoId()).thenReturn(100L);
            when(routine.getDay()).thenReturn("MON");

            when(todoRepository.findBacklogsByCategoryId(eq(userId), eq(categoryId), any(Type.class), any(TodayStatus.class), any(PageRequest.class)))
                    .thenReturn(todoPage);

            when(routineRepository.findAllByTodoIdIn(List.of(100L))).thenReturn(List.of(routine));

            // when
            BacklogListResponseDto response = backlogService.getBacklogList(userId, categoryId, mobileType, page, size);

            // then
            assertThat(response.categoryName()).isEqualTo("운동");
            assertThat(response.totalCount()).isEqualTo(1);
            assertThat(response.backlogs()).hasSize(1);
            assertThat(response.backlogs().get(0).routineDays()).contains("MON");

            verify(categoryValidator).validateAndReturnCategory(userId, categoryId);
            verify(routineRepository).findAllByTodoIdIn(anyList());
        }

        @Test
        @DisplayName("[TC-GET-002] 전체 카테고리(-1) 조회 시: Validator와 DB 조회 없이 기본 이름을 반환하고 findAllBacklogs를 호출한다")
        void get_backlog_list_with_all_category() {
            // given
            Long userId = 1L;
            Long allCategoryId = -1L;

            doNothing().when(userValidator).checkIsExistUser(userId);

            Page<Todo> emptyPage = new PageImpl<>(Collections.emptyList());

            when(todoRepository.findAllBacklogs(eq(userId), any(Type.class), any(TodayStatus.class), any(PageRequest.class)))
                    .thenReturn(emptyPage);

            // when
            BacklogListResponseDto response = backlogService.getBacklogList(userId, allCategoryId, MobileType.ANDROID, 0, 10);

            // then
            assertThat(response.categoryName()).isEqualTo("전체");
            assertThat(response.backlogs()).isEmpty();

            verify(categoryValidator, never()).validateAndReturnCategory(any(), any());
            verify(todoRepository).findAllBacklogs(any(), any(), any(), any());
        }

        @Test
        @DisplayName("[TC-GET-003] 중요 카테고리(0) 조회 시: Validator 없이 기본 이름을 반환하고 findBookmarkBacklogs를 호출한다")
        void get_backlog_list_with_bookmark_category() {
            // given
            Long userId = 1L;
            Long bookmarkCategoryId = 0L;

            doNothing().when(userValidator).checkIsExistUser(userId);

            Page<Todo> emptyPage = new PageImpl<>(Collections.emptyList());

            when(todoRepository.findBookmarkBacklogs(eq(userId), any(Type.class), any(TodayStatus.class), any(PageRequest.class)))
                    .thenReturn(emptyPage);

            // when
            BacklogListResponseDto response = backlogService.getBacklogList(userId, bookmarkCategoryId, MobileType.IOS, 0, 10);

            // then
            assertThat(response.categoryName()).isEqualTo("중요");

            verify(categoryValidator, never()).validateAndReturnCategory(any(), any());
            verify(todoRepository).findBookmarkBacklogs(any(), any(), any(), any());
        }
    }

    @Nested
    @DisplayName("[SCN-SVC-BACKLOG-002] 백로그를 생성한다")
    class CreateBacklog {

        @Test
        @DisplayName("[TC-CREATE-001] 전체 카테고리(-1)로 백로그 생성")
        void createBacklog_allCategory() {
            // given
            Long userId = 1L;
            Long categoryId = -1L;
            String content = "새 백로그";
            BacklogCreateRequestDto requestDto = new BacklogCreateRequestDto(content, categoryId);

            doNothing().when(userValidator).checkIsExistUser(userId);
            doNothing().when(categoryValidator).validateCategory(userId, categoryId);
            when(todoRepository.findMaxBacklogOrderByUserIdOrZero(userId)).thenReturn(5);

            Todo savedTodo = mock(Todo.class);
            when(savedTodo.getId()).thenReturn(100L);
            when(todoRepository.save(any(Todo.class))).thenReturn(savedTodo);

            // when
            BacklogCreateResponseDto response = backlogService.createBacklog(userId, requestDto);

            // then
            assertThat(response).isNotNull();
            assertThat(response.todoId()).isEqualTo(100L);
            verify(todoRepository).save(any(Todo.class));
        }

        @Test
        @DisplayName("[TC-CREATE-002] 중요 카테고리(0)로 북마크 백로그 생성")
        void createBacklog_bookmarkCategory() {
            // given
            Long userId = 1L;
            Long categoryId = 0L;
            String content = "중요한 백로그";
            BacklogCreateRequestDto requestDto = new BacklogCreateRequestDto(content, categoryId);

            doNothing().when(userValidator).checkIsExistUser(userId);
            doNothing().when(categoryValidator).validateCategory(userId, categoryId);
            when(todoRepository.findMaxBacklogOrderByUserIdOrZero(userId)).thenReturn(3);

            Todo savedTodo = mock(Todo.class);
            when(savedTodo.getId()).thenReturn(101L);
            when(todoRepository.save(any(Todo.class))).thenReturn(savedTodo);

            // when
            BacklogCreateResponseDto response = backlogService.createBacklog(userId, requestDto);

            // then
            assertThat(response).isNotNull();
            assertThat(response.todoId()).isEqualTo(101L);
        }

        @Test
        @DisplayName("[TC-CREATE-003] 일반 카테고리로 백로그 생성")
        void createBacklog_normalCategory() {
            // given
            Long userId = 1L;
            Long categoryId = 10L;
            String content = "카테고리 백로그";
            BacklogCreateRequestDto requestDto = new BacklogCreateRequestDto(content, categoryId);

            doNothing().when(userValidator).checkIsExistUser(userId);
            doNothing().when(categoryValidator).validateCategory(userId, categoryId);
            when(todoRepository.findMaxBacklogOrderByUserIdOrZero(userId)).thenReturn(7);

            Todo savedTodo = mock(Todo.class);
            when(savedTodo.getId()).thenReturn(102L);
            when(todoRepository.save(any(Todo.class))).thenReturn(savedTodo);

            // when
            BacklogCreateResponseDto response = backlogService.createBacklog(userId, requestDto);

            // then
            assertThat(response).isNotNull();
            assertThat(response.todoId()).isEqualTo(102L);
        }
    }

    @Nested
    @DisplayName("[SCN-SVC-BACKLOG-003] 어제 할 일 목록을 조회한다")
    class GetYesterdays {

        @Test
        @DisplayName("[TC-YEST-001] 어제 할 일 목록 조회")
        void getYesterdays_success() {
            // given
            Long userId = 1L;
            int page = 0;
            int size = 10;

            doNothing().when(userValidator).checkIsExistUser(userId);

            Todo yesterday1 = mock(Todo.class);
            when(yesterday1.getId()).thenReturn(1L);
            when(yesterday1.getContent()).thenReturn("어제 할 일 1");

            Todo yesterday2 = mock(Todo.class);
            when(yesterday2.getId()).thenReturn(2L);
            when(yesterday2.getContent()).thenReturn("어제 할 일 2");

            Page<Todo> yesterdaysPage = new PageImpl<>(List.of(yesterday1, yesterday2));
            when(todoRepository.findByUserIdAndTypeAndTodayStatus(eq(userId), eq(Type.YESTERDAY), eq(TodayStatus.INCOMPLETE), any()))
                    .thenReturn(yesterdaysPage);

            // when
            PaginatedYesterdayResponseDto response = backlogService.getYesterdays(userId, page, size);

            // then
            assertThat(response).isNotNull();
            assertThat(response.yesterdays()).hasSize(2);
        }

        @Test
        @DisplayName("[TC-YEST-002] 어제 할 일이 없는 경우")
        void getYesterdays_empty() {
            // given
            Long userId = 1L;
            int page = 0;
            int size = 10;

            doNothing().when(userValidator).checkIsExistUser(userId);

            Page<Todo> emptyPage = new PageImpl<>(Collections.emptyList());
            when(todoRepository.findByUserIdAndTypeAndTodayStatus(eq(userId), eq(Type.YESTERDAY), eq(TodayStatus.INCOMPLETE), any()))
                    .thenReturn(emptyPage);

            // when
            PaginatedYesterdayResponseDto response = backlogService.getYesterdays(userId, page, size);

            // then
            assertThat(response).isNotNull();
            assertThat(response.yesterdays()).isEmpty();
        }
    }

    @Nested
    @DisplayName("[SCN-SVC-BACKLOG-004] 어제 백로그를 생성한다")
    class CreateYesterdayBacklog {

        @Test
        @DisplayName("[TC-YEST-CREATE-001] 어제 백로그 생성 성공")
        void createYesterdayBacklog_success() {
            // given
            Long userId = 1L;
            String content = "어제 백로그";
            BacklogCreateRequestDto requestDto = new BacklogCreateRequestDto(content, null);

            doNothing().when(userValidator).checkIsExistUser(userId);
            doNothing().when(categoryValidator).validateCategory(userId, null);
            when(todoRepository.findMaxBacklogOrderByUserIdOrZero(userId)).thenReturn(2);
            when(todoRepository.save(any(Todo.class))).thenAnswer(invocation -> invocation.getArgument(0));

            // when
            BacklogCreateResponseDto response = backlogService.createYesterdayBacklog(userId, requestDto);

            // then
            assertThat(response).isNotNull();
            verify(todoRepository).save(any(Todo.class));
        }
    }
}