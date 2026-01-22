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

    private Boolean getIsDeleted(Long id) {
        return (Boolean) tem.getEntityManager().createNativeQuery(
                "SELECT is_deleted FROM note WHERE id = :id"
        ).setParameter("id", id).getSingleResult();
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

            // then - Native Query로 is_deleted 값 직접 확인 (@SQLRestriction 우회)
            assertThat(getIsDeleted(note1.getId())).isTrue();
            assertThat(getIsDeleted(note2.getId())).isTrue();
            assertThat(getIsDeleted(otherNote.getId())).isFalse();
        }

        @Test
        @DisplayName("[TC-SOFT-DELETE-002] soft delete된 Note는 findByIdAndUserId로 조회되지 않는다")
        void findByIdAndUserId_excludesSoftDeletedNote() {
            // given
            Long userId = 300L;
            Note note = createNote(userId);
            Long noteId = note.getId();

            jpaNoteRepository.softDeleteByUserId(userId);
            tem.flush();
            tem.clear();

            // when
            var found = jpaNoteRepository.findByIdAndUserId(noteId, userId);

            // then
            assertThat(found).isEmpty();
        }

        @Test
        @DisplayName("[TC-SOFT-DELETE-003] soft delete된 Note는 findNotePreviewsByUserId에서 제외된다")
        void findNotePreviewsByUserId_excludesSoftDeletedNote() {
            // given
            Long userId = 400L;
            Note note1 = createNote(userId);
            Note note2 = createNote(userId);

            // note1만 soft delete
            tem.getEntityManager().createNativeQuery(
                    "UPDATE note SET is_deleted = true WHERE id = :id"
            ).setParameter("id", note1.getId()).executeUpdate();
            tem.flush();
            tem.clear();

            // when
            var previews = jpaNoteRepository.findNotePreviewsByUserId(userId, 50, 100);

            // then - note2만 조회됨
            assertThat(previews).hasSize(1);
            assertThat(previews.get(0).getId()).isEqualTo(note2.getId());
        }
    }
}
