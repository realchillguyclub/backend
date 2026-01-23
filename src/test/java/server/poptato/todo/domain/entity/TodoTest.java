package server.poptato.todo.domain.entity;

import static org.assertj.core.api.Assertions.*;

import java.time.LocalDate;
import java.time.LocalTime;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import server.poptato.configuration.ServiceTestConfig;
import server.poptato.todo.domain.value.TodayStatus;
import server.poptato.todo.domain.value.Type;

class TodoTest extends ServiceTestConfig {

    @Nested
    @DisplayName("[SCN-ENT-TODO-001] Todo 생성 팩토리 메서드")
    class CreateTodo {

        @Test
        @DisplayName("[TC-CRT-001] createBacklog: 기본 백로그 생성")
        void createBacklog_success() {
            // given
            Long userId = 1L;
            String content = "테스트 할 일";
            Integer backlogOrder = 5;

            // when
            Todo todo = Todo.createBacklog(userId, content, backlogOrder);

            // then
            assertThat(todo.getUserId()).isEqualTo(userId);
            assertThat(todo.getContent()).isEqualTo(content);
            assertThat(todo.getBacklogOrder()).isEqualTo(backlogOrder);
            assertThat(todo.getType()).isEqualTo(Type.BACKLOG);
            assertThat(todo.isBookmark()).isFalse();
        }

        @Test
        @DisplayName("[TC-CRT-002] createBookmarkBacklog: 북마크된 백로그 생성")
        void createBookmarkBacklog_success() {
            // given
            Long userId = 1L;
            String content = "중요한 할 일";
            Integer backlogOrder = 3;

            // when
            Todo todo = Todo.createBookmarkBacklog(userId, content, backlogOrder);

            // then
            assertThat(todo.getUserId()).isEqualTo(userId);
            assertThat(todo.getContent()).isEqualTo(content);
            assertThat(todo.getBacklogOrder()).isEqualTo(backlogOrder);
            assertThat(todo.getType()).isEqualTo(Type.BACKLOG);
            assertThat(todo.isBookmark()).isTrue();
        }

        @Test
        @DisplayName("[TC-CRT-003] createCategoryBacklog: 카테고리가 있는 백로그 생성")
        void createCategoryBacklog_success() {
            // given
            Long userId = 1L;
            Long categoryId = 10L;
            String content = "카테고리 할 일";
            Integer backlogOrder = 2;

            // when
            Todo todo = Todo.createCategoryBacklog(userId, categoryId, content, backlogOrder);

            // then
            assertThat(todo.getUserId()).isEqualTo(userId);
            assertThat(todo.getCategoryId()).isEqualTo(categoryId);
            assertThat(todo.getContent()).isEqualTo(content);
            assertThat(todo.getBacklogOrder()).isEqualTo(backlogOrder);
            assertThat(todo.getType()).isEqualTo(Type.BACKLOG);
        }

        @Test
        @DisplayName("[TC-CRT-004] createYesterdayBacklog: 어제 할 일 백로그 생성")
        void createYesterdayBacklog_success() {
            // given
            Long userId = 1L;
            String content = "어제 할 일";
            Integer backlogOrder = 1;

            // when
            Todo todo = Todo.createYesterdayBacklog(userId, content, backlogOrder);

            // then
            assertThat(todo.getUserId()).isEqualTo(userId);
            assertThat(todo.getContent()).isEqualTo(content);
            assertThat(todo.getBacklogOrder()).isEqualTo(backlogOrder);
            assertThat(todo.getType()).isEqualTo(Type.YESTERDAY);
            assertThat(todo.getTodayDate()).isEqualTo(LocalDate.now().minusDays(1));
            assertThat(todo.getTodayStatus()).isEqualTo(TodayStatus.INCOMPLETE);
        }

        @Test
        @DisplayName("[TC-CRT-005] createTodayTodo: 오늘 할 일 생성")
        void createTodayTodo_success() {
            // given
            Long userId = 1L;
            String content = "오늘 할 일";
            LocalTime time = LocalTime.of(14, 30);
            boolean isBookmark = true;
            Integer todayOrder = 10;

            // when
            Todo todo = Todo.createTodayTodo(userId, content, time, isBookmark, todayOrder);

            // then
            assertThat(todo.getUserId()).isEqualTo(userId);
            assertThat(todo.getContent()).isEqualTo(content);
            assertThat(todo.getTime()).isEqualTo(time);
            assertThat(todo.isBookmark()).isEqualTo(isBookmark);
            assertThat(todo.isEvent()).isTrue();
            assertThat(todo.getType()).isEqualTo(Type.TODAY);
            assertThat(todo.getTodayDate()).isEqualTo(LocalDate.now());
            assertThat(todo.getTodayStatus()).isEqualTo(TodayStatus.INCOMPLETE);
            assertThat(todo.getTodayOrder()).isEqualTo(todayOrder);
        }
    }

    @Nested
    @DisplayName("[SCN-ENT-TODO-002] 상태 변경 메서드")
    class StateChange {

        @Test
        @DisplayName("[TC-CHG-001] changeToToday: 백로그에서 오늘로 변경")
        void changeToToday_success() {
            // given
            Todo todo = Todo.createBacklog(1L, "테스트", 5);
            Integer maxTodayOrder = 10;

            // when
            todo.changeToToday(maxTodayOrder);

            // then
            assertThat(todo.getType()).isEqualTo(Type.TODAY);
            assertThat(todo.getBacklogOrder()).isNull();
            assertThat(todo.getTodayOrder()).isEqualTo(maxTodayOrder + 1);
            assertThat(todo.getTodayStatus()).isEqualTo(TodayStatus.INCOMPLETE);
            assertThat(todo.getTodayDate()).isEqualTo(LocalDate.now());
        }

        @Test
        @DisplayName("[TC-CHG-002] changeToBacklog: 오늘에서 백로그로 변경")
        void changeToBacklog_success() {
            // given
            Todo todo = Todo.createTodayTodo(1L, "테스트", null, false, 5);
            Integer maxBacklogOrder = 20;

            // when
            todo.changeToBacklog(maxBacklogOrder);

            // then
            assertThat(todo.getType()).isEqualTo(Type.BACKLOG);
            assertThat(todo.getBacklogOrder()).isEqualTo(maxBacklogOrder + 1);
            assertThat(todo.getTodayOrder()).isNull();
            assertThat(todo.getTodayStatus()).isNull();
            assertThat(todo.getTodayDate()).isNull();
        }

        @Test
        @DisplayName("[TC-CHG-003] toggleBookmark: 북마크 토글")
        void toggleBookmark_success() {
            // given
            Todo todo = Todo.createBacklog(1L, "테스트", 1);
            assertThat(todo.isBookmark()).isFalse();

            // when
            todo.toggleBookmark();

            // then
            assertThat(todo.isBookmark()).isTrue();

            // when - 다시 토글
            todo.toggleBookmark();

            // then
            assertThat(todo.isBookmark()).isFalse();
        }

        @Test
        @DisplayName("[TC-CHG-004] completeTodayTodo: 오늘 할 일 완료")
        void completeTodayTodo_success() {
            // given
            Todo todo = Todo.createTodayTodo(1L, "테스트", null, false, 5);
            assertThat(todo.getTodayStatus()).isEqualTo(TodayStatus.INCOMPLETE);

            // when
            todo.completeTodayTodo();

            // then
            assertThat(todo.getTodayStatus()).isEqualTo(TodayStatus.COMPLETED);
            assertThat(todo.getTodayOrder()).isNull();
        }

        @Test
        @DisplayName("[TC-CHG-005] incompleteTodayTodo: 오늘 할 일 미완료로 변경")
        void incompleteTodayTodo_success() {
            // given
            Todo todo = Todo.createTodayTodo(1L, "테스트", null, false, 5);
            todo.completeTodayTodo();
            Integer minTodayOrder = 1;

            // when
            todo.incompleteTodayTodo(minTodayOrder);

            // then
            assertThat(todo.getTodayStatus()).isEqualTo(TodayStatus.INCOMPLETE);
            assertThat(todo.getTodayOrder()).isEqualTo(minTodayOrder - 1);
        }

        @Test
        @DisplayName("[TC-CHG-006] updateYesterdayToCompleted: 어제 할 일 완료로 변경")
        void updateYesterdayToCompleted_success() {
            // given
            Todo todo = Todo.createYesterdayBacklog(1L, "어제 할 일", 3);

            // when
            todo.updateYesterdayToCompleted();

            // then
            assertThat(todo.getTodayStatus()).isEqualTo(TodayStatus.COMPLETED);
            assertThat(todo.getBacklogOrder()).isNull();
        }
    }

    @Nested
    @DisplayName("[SCN-ENT-TODO-003] 필드 업데이트 메서드")
    class FieldUpdate {

        @Test
        @DisplayName("[TC-UPD-001] updateTime: 시간 업데이트")
        void updateTime_success() {
            // given
            Todo todo = Todo.createBacklog(1L, "테스트", 1);
            LocalTime newTime = LocalTime.of(15, 30);

            // when
            todo.updateTime(newTime);

            // then
            assertThat(todo.getTime()).isEqualTo(newTime);
        }

        @Test
        @DisplayName("[TC-UPD-002] updateDeadline: 마감기한 업데이트")
        void updateDeadline_success() {
            // given
            Todo todo = Todo.createBacklog(1L, "테스트", 1);
            LocalDate newDeadline = LocalDate.now().plusDays(7);

            // when
            todo.updateDeadline(newDeadline);

            // then
            assertThat(todo.getDeadline()).isEqualTo(newDeadline);
        }

        @Test
        @DisplayName("[TC-UPD-003] updateContent: 내용 업데이트")
        void updateContent_success() {
            // given
            Todo todo = Todo.createBacklog(1L, "기존 내용", 1);
            String newContent = "수정된 내용";

            // when
            todo.updateContent(newContent);

            // then
            assertThat(todo.getContent()).isEqualTo(newContent);
        }

        @Test
        @DisplayName("[TC-UPD-004] updateCategory: 카테고리 업데이트")
        void updateCategory_success() {
            // given
            Todo todo = Todo.createBacklog(1L, "테스트", 1);
            Long newCategoryId = 20L;

            // when
            todo.updateCategory(newCategoryId);

            // then
            assertThat(todo.getCategoryId()).isEqualTo(newCategoryId);
        }

        @Test
        @DisplayName("[TC-UPD-005] updateTodayStatus: 오늘 상태 업데이트")
        void updateTodayStatus_success() {
            // given
            Todo todo = Todo.createTodayTodo(1L, "테스트", null, false, 5);

            // when
            todo.updateTodayStatus(TodayStatus.COMPLETED);

            // then
            assertThat(todo.getTodayStatus()).isEqualTo(TodayStatus.COMPLETED);
        }

        @Test
        @DisplayName("[TC-UPD-006] updateType: 타입 업데이트")
        void updateType_success() {
            // given
            Todo todo = Todo.createBacklog(1L, "테스트", 1);

            // when
            todo.updateType(Type.TODAY);

            // then
            assertThat(todo.getType()).isEqualTo(Type.TODAY);
        }

        @Test
        @DisplayName("[TC-UPD-007] updateTodayOrder: 오늘 순서 업데이트")
        void updateTodayOrder_success() {
            // given
            Todo todo = Todo.createTodayTodo(1L, "테스트", null, false, 5);
            Integer newOrder = 10;

            // when
            todo.updateTodayOrder(newOrder);

            // then
            assertThat(todo.getTodayOrder()).isEqualTo(newOrder);
        }

        @Test
        @DisplayName("[TC-UPD-008] updateBacklogOrder: 백로그 순서 업데이트")
        void updateBacklogOrder_success() {
            // given
            Todo todo = Todo.createBacklog(1L, "테스트", 5);
            Integer newOrder = 15;

            // when
            todo.updateBacklogOrder(newOrder);

            // then
            assertThat(todo.getBacklogOrder()).isEqualTo(newOrder);
        }
    }

    @Nested
    @DisplayName("[SCN-ENT-TODO-004] 반복 설정 메서드")
    class RepeatSetting {

        @Test
        @DisplayName("[TC-RPT-001] toggleRepeat: 반복 토글")
        void toggleRepeat_success() {
            // given
            Todo todo = Todo.createBacklog(1L, "테스트", 1);
            assertThat(todo.isRepeat()).isFalse();

            // when
            todo.toggleRepeat();

            // then
            assertThat(todo.isRepeat()).isTrue();

            // when - 다시 토글
            todo.toggleRepeat();

            // then
            assertThat(todo.isRepeat()).isFalse();
        }

        @Test
        @DisplayName("[TC-RPT-002] setRepeat: 반복 설정")
        void setRepeat_success() {
            // given
            Todo todo = Todo.createBacklog(1L, "테스트", 1);

            // when
            todo.setRepeat(true);

            // then
            assertThat(todo.isRepeat()).isTrue();

            // when
            todo.setRepeat(false);

            // then
            assertThat(todo.isRepeat()).isFalse();
        }

        @Test
        @DisplayName("[TC-RPT-003] setRoutine: 루틴 설정")
        void setRoutine_success() {
            // given
            Todo todo = Todo.createBacklog(1L, "테스트", 1);

            // when
            todo.setRoutine(true);

            // then
            assertThat(todo.isRoutine()).isTrue();

            // when
            todo.setRoutine(false);

            // then
            assertThat(todo.isRoutine()).isFalse();
        }
    }

    @Nested
    @DisplayName("[SCN-TODO-012] Soft Delete")
    class SoftDeleteTest {

        @Test
        @DisplayName("[TC-SD-001] softDelete 호출 시 isDeleted가 true로 변경된다")
        void softDelete_setsIsDeletedToTrue() {
            // given
            Todo todo = Todo.createBacklog(1L, "테스트 할 일", 1);
            assertThat(todo.isDeleted()).isFalse();

            // when
            todo.softDelete();

            // then
            assertThat(todo.isDeleted()).isTrue();
        }
    }
}
