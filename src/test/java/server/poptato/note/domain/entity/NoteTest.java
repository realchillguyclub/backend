package server.poptato.note.domain.entity;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

class NoteTest {

    @Test
    @DisplayName("[TC-ENTITY-001] isDeleted 기본값은 false이다")
    void isDeleted_defaultIsFalse() {
        // given
        Note note = Note.builder()
                .userId(1L)
                .build();

        // then
        assertThat(note.isDeleted()).isFalse();
    }
}
