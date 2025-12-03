package server.poptato.note.domain.repository;

import server.poptato.note.domain.entity.Note;
import server.poptato.note.domain.summary.NoteSummary;

import java.util.List;
import java.util.Optional;

public interface NoteRepository {

    Note save(Note note);

    List<NoteSummary> findSummariesByUserId(Long userId);

    Optional<Note> findByIdAndUserId(Long noteId, Long userId);

    void delete(Note note);
}
