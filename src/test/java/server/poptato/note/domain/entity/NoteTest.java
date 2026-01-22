package server.poptato.note.domain.entity;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

class NoteTest {

    @Test
    @DisplayName("[TC-ENTITY-001] softDelete 호출 시 isDeleted가 true로 변경된다")
    void softDelete_setsIsDeletedTrue() {
        // given
        Note note = Note.builder()
                .userId(1L)
                .build();

        // when
        note.softDelete();

        // then
        assertThat(note.isDeleted()).isTrue();
    }

    @Test
    @DisplayName("[TC-ENTITY-002] isDeleted 기본값은 false이다")
    void isDeleted_defaultIsFalse() {
        // given
        Note note = Note.builder()
                .userId(1L)
                .build();

        // then
        assertThat(note.isDeleted()).isFalse();
    }
}
