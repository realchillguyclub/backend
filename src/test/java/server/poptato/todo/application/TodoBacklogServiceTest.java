package server.poptato.todo.application;

import org.junit.jupiter.api.*;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import server.poptato.category.domain.entity.Category;
import server.poptato.category.domain.repository.CategoryRepository;
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
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

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
    @DisplayName("[SCN-SVC-TODO-BACKLOG-001] 백로그 목록을 조회한다")
    class GetBacklogList {

        @Test
        @DisplayName("[TC-GET-001] 일반 카테고리 ID로 백로그 목록을 조회하면 해당 카테고리의 Todo 목록과 루틴 정보를 반환한다")
        void get_backlog_list_with_normal_category() {
            // given
            Long userId = 1L;
            Long categoryId = 10L;
            int page = 0;
            int size = 10;
            MobileType mobileType = MobileType.IOS;

            doNothing().when(userValidator).checkIsExistUser(userId);
            doNothing().when(categoryValidator).validateCategory(userId, categoryId);

            Todo todo = mock(Todo.class);
            when(todo.getId()).thenReturn(100L);
            Page<Todo> todoPage = new PageImpl<>(List.of(todo));

            Category category = mock(Category.class);
            when(category.getName()).thenReturn("운동");

            Routine routine = mock(Routine.class);
            when(routine.getTodoId()).thenReturn(100L);
            when(routine.getDay()).thenReturn("월");

            // Repository Mocks
            when(todoRepository.findBacklogsByCategoryId(eq(userId), eq(categoryId), any(Type.class), any(TodayStatus.class), any(PageRequest.class)))
                    .thenReturn(todoPage);
            when(categoryRepository.findById(categoryId)).thenReturn(Optional.of(category));
            when(routineRepository.findAllByTodoIdIn(List.of(100L))).thenReturn(List.of(routine));

            // when
            BacklogListResponseDto response = backlogService.getBacklogList(userId, categoryId, mobileType, page, size);

            // then
            assertThat(response.categoryName()).isEqualTo("운동");
            assertThat(response.totalCount()).isEqualTo(1);
            assertThat(response.backlogs()).hasSize(1);
            assertThat(response.backlogs().get(0).routineDays()).contains("월");

            verify(todoRepository).findBacklogsByCategoryId(eq(userId), eq(categoryId), any(), any(), any());
        }

        @Test
        @DisplayName("[TC-GET-002] 전체 카테고리(ALL_CATEGORY) 조회 시 findAllBacklogs 메서드가 호출된다")
        void get_backlog_list_with_all_category() {
            // given
            Long userId = 1L;
            Long allCategoryId = -1L;
            int page = 0;
            int size = 10;

            doNothing().when(userValidator).checkIsExistUser(userId);

            Page<Todo> emptyPage = new PageImpl<>(Collections.emptyList());

            Category mockCategory = mock(Category.class);
            when(mockCategory.getName()).thenReturn("전체");
            when(categoryRepository.findById(allCategoryId)).thenReturn(Optional.of(mockCategory));

            when(todoRepository.findAllBacklogs(eq(userId), any(Type.class), any(TodayStatus.class), any(PageRequest.class)))
                    .thenReturn(emptyPage);

            // when
            BacklogListResponseDto response = backlogService.getBacklogList(userId, allCategoryId, MobileType.ANDROID, page, size);

            // then
            assertThat(response.backlogs()).isEmpty();

            verify(todoRepository).findAllBacklogs(eq(userId), any(), any(), any());
            verify(todoRepository, never()).findBookmarkBacklogs(any(), any(), any(), any());
        }

        @Test
        @DisplayName("[TC-GET-003] 중요 카테고리(BOOKMARK_CATEGORY) 조회 시 findBookmarkBacklogs 메서드가 호출된다")
        void get_backlog_list_with_bookmark_category() {
            // given
            Long userId = 1L;
            Long bookmarkCategoryId = 0L;

            doNothing().when(userValidator).checkIsExistUser(userId);

            Page<Todo> emptyPage = new PageImpl<>(Collections.emptyList());

            Category mockCategory = mock(Category.class);
            when(mockCategory.getName()).thenReturn("중요");
            when(categoryRepository.findById(bookmarkCategoryId)).thenReturn(Optional.of(mockCategory));

            when(todoRepository.findBookmarkBacklogs(eq(userId), any(Type.class), any(TodayStatus.class), any(PageRequest.class)))
                    .thenReturn(emptyPage);

            // when
            backlogService.getBacklogList(userId, bookmarkCategoryId, MobileType.IOS, 0, 10);

            // then
            // Verify: findBookmarkBacklogs가 호출되었는지 확인 (핵심)
            verify(todoRepository).findBookmarkBacklogs(eq(userId), any(), any(), any());
            verify(todoRepository, never()).findAllBacklogs(any(), any(), any(), any());
        }
    }
}