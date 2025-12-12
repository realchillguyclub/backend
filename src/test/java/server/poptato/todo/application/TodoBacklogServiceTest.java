package server.poptato.todo.application;

import org.junit.jupiter.api.*;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import server.poptato.category.domain.entity.Category;
import server.poptato.category.validator.CategoryValidator;
import server.poptato.configuration.ServiceTestConfig;
import server.poptato.todo.application.response.BacklogListResponseDto;
import server.poptato.todo.domain.entity.Routine;
import server.poptato.todo.domain.entity.Todo;
import server.poptato.todo.domain.repository.RoutineRepository;
import server.poptato.todo.domain.repository.TodoRepository;
import server.poptato.todo.domain.value.TodayStatus;
import server.poptato.todo.domain.value.Type;
import server.poptato.user.domain.value.MobileType;
import server.poptato.user.validator.UserValidator;

import java.util.Collections;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

class TodoBacklogServiceTest extends ServiceTestConfig {

    @Mock private TodoRepository todoRepository;
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
}