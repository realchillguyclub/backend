package server.poptato.note.infra.impl;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;
import server.poptato.note.domain.entity.Note;
import server.poptato.note.domain.repository.NoteRepository;
import server.poptato.note.domain.summary.NoteSummary;
import server.poptato.note.infra.JpaNoteRepository;
import server.poptato.note.infra.projection.NoteListItemProjection;

import java.util.List;
import java.util.Optional;

@Repository
@RequiredArgsConstructor
public class NoteRepositoryImpl implements NoteRepository {

    private final JpaNoteRepository jpaNoteRepository;

    @Override
    public Note save(Note note) {
        return jpaNoteRepository.save(note);
    }

    @Override
    public List<NoteSummary> findSummariesByUserId(Long userId) {
        List<NoteListItemProjection> projections = jpaNoteRepository.findNoteListByUserId(userId, 20, 30);
        return projections.stream()
                .map((p) -> new NoteSummary(p.getId(), p.getPreviewTitle(), p.getPreviewContent(), p.getModifyDate()))
                .toList();
    }

    @Override
    public Optional<Note> findByIdAndUserId(Long noteId, Long userId) {
        return jpaNoteRepository.findByIdAndUserId(noteId, userId);
    }

    @Override
    public void delete(Note note) {
        jpaNoteRepository.delete(note);
    }
}
