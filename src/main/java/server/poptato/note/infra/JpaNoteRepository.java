package server.poptato.note.infra;

import org.springframework.data.jpa.repository.JpaRepository;
import server.poptato.note.domain.entity.Note;

import java.util.Optional;

public interface JpaNoteRepository extends JpaRepository<Note, Long> {

    Optional<Note> findByIdAndUserId(Long noteId, Long userId);
}
