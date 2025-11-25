package server.poptato.note.domain.repository;

import server.poptato.note.domain.entity.Note;

import java.util.Optional;

public interface NoteRepository {

    Optional<Note> findByIdAndUserId(Long noteId, Long userId);

    Note save(Note note);
}
