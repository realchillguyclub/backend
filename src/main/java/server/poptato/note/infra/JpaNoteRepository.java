package server.poptato.note.infra;

import org.springframework.data.jpa.repository.JpaRepository;
import server.poptato.note.domain.entity.Note;

public interface JpaNoteRepository extends JpaRepository<Note, Long> {
}
