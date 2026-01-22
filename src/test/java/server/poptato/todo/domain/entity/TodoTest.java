package server.poptato.todo.domain.entity;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import server.poptato.todo.domain.value.TodayStatus;
import server.poptato.todo.domain.value.Type;

class TodoTest {

    @Test
    @DisplayName("[TC-ENTITY-001] softDelete 호출 시 isDeleted가 true로 변경된다")
    void softDelete_setsIsDeletedTrue() {
        // given
        Todo todo = Todo.builder()
                .userId(1L)
                .type(Type.BACKLOG)
                .content("test")
                .todayStatus(TodayStatus.INCOMPLETE)
                .build();

        // when
        todo.softDelete();

        // then
        assertThat(todo.isDeleted()).isTrue();
    }

    @Test
    @DisplayName("[TC-ENTITY-002] isDeleted 기본값은 false이다")
    void isDeleted_defaultIsFalse() {
        // given
        Todo todo = Todo.builder()
                .userId(1L)
                .type(Type.BACKLOG)
                .content("test")
                .todayStatus(TodayStatus.INCOMPLETE)
                .build();

        // then
        assertThat(todo.isDeleted()).isFalse();
    }
}
