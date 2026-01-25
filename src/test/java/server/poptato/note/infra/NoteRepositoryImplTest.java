package server.poptato.note.infra;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Import;

import server.poptato.configuration.DatabaseTestConfig;
import server.poptato.configuration.MySqlDataJpaTest;
import server.poptato.note.domain.entity.Note;
import server.poptato.note.domain.repository.NoteRepository;
import server.poptato.note.infra.impl.NoteRepositoryImpl;

@MySqlDataJpaTest
@Import(NoteRepositoryImpl.class)
public class NoteRepositoryImplTest extends DatabaseTestConfig {

    @Autowired
    private NoteRepository noteRepository;

    private Note createNote(Long userId) {
        Note note = Note.builder()
                .userId(userId)
                .build();
        tem.persist(note);
        tem.flush();
        tem.clear();
        return note;
    }

    private Boolean getIsDeleted(Long id) {
        Object result = tem.getEntityManager().createNativeQuery(
                "SELECT is_deleted FROM note WHERE id = :id"
        ).setParameter("id", id).getSingleResult();
        if (result instanceof Boolean) {
            return (Boolean) result;
        }
        return ((Number) result).intValue() == 1;
    }

    @Nested
    @DisplayName("[SCN-REP-IMPL-NOTE-001] NoteRepositoryImpl Soft Delete 테스트")
    class SoftDeleteTests {

        @Test
        @DisplayName("[TC-IMPL-001] softDeleteByUserId 호출 시 해당 유저의 모든 Note가 soft delete 된다")
        void softDeleteByUserId_deletesAllUserNotes() {
            // given
            Long userId = 1000L;
            Long otherUserId = 2000L;

            Note note1 = createNote(userId);
            Note note2 = createNote(userId);
            Note otherNote = createNote(otherUserId);

            // when
            noteRepository.softDeleteByUserId(userId);
            tem.flush();
            tem.clear();

            // then
            assertThat(getIsDeleted(note1.getId())).isTrue();
            assertThat(getIsDeleted(note2.getId())).isTrue();
            assertThat(getIsDeleted(otherNote.getId())).isFalse();
        }

        @Test
        @DisplayName("[TC-IMPL-002] softDeleteById 호출 시 해당 Note가 soft delete 된다")
        void softDeleteById_deletesSingleNote() {
            // given
            Long userId = 1000L;
            Note note1 = createNote(userId);
            Note note2 = createNote(userId);

            // when
            noteRepository.softDeleteById(note1.getId());
            tem.flush();
            tem.clear();

            // then
            assertThat(getIsDeleted(note1.getId())).isTrue();
            assertThat(getIsDeleted(note2.getId())).isFalse();
        }
    }
}
