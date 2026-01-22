package server.poptato.note.infra;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.MethodOrderer;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestMethodOrder;
import org.springframework.beans.factory.annotation.Autowired;

import server.poptato.configuration.DatabaseTestConfig;
import server.poptato.configuration.MySqlDataJpaTest;
import server.poptato.note.domain.entity.Note;

@MySqlDataJpaTest
public class JpaNoteRepositoryTest extends DatabaseTestConfig {

    @Autowired
    private JpaNoteRepository jpaNoteRepository;

    private Note createNote(Long userId) {
        Note note = Note.builder()
                .userId(userId)
                .build();
        tem.persist(note);
        tem.flush();
        tem.clear();
        return note;
    }

    @Nested
    @DisplayName("[SCN-REP-NOTE-001] Soft Delete 테스트")
    @TestMethodOrder(MethodOrderer.DisplayName.class)
    class SoftDeleteTests {

        @Test
        @DisplayName("[TC-SOFT-DELETE-001] softDeleteByUserId 호출 시 해당 유저의 모든 Note가 soft delete 된다")
        void softDeleteByUserId_deletesAllUserNotes() {
            // given
            Long userId = 100L;
            Long otherUserId = 200L;

            Note note1 = createNote(userId);
            Note note2 = createNote(userId);
            Note otherNote = createNote(otherUserId);

            // when
            jpaNoteRepository.softDeleteByUserId(userId);
            tem.flush();
            tem.clear();

            // then
            Note found1 = tem.find(Note.class, note1.getId());
            Note found2 = tem.find(Note.class, note2.getId());
            Note foundOther = tem.find(Note.class, otherNote.getId());

            assertThat(found1.isDeleted()).isTrue();
            assertThat(found2.isDeleted()).isTrue();
            assertThat(foundOther.isDeleted()).isFalse();
        }
    }
}
