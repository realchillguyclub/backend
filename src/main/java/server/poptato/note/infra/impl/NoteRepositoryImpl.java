package server.poptato.note.infra.impl;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;
import server.poptato.note.domain.entity.Note;
import server.poptato.note.domain.repository.NoteRepository;
import server.poptato.note.infra.JpaNoteRepository;

import java.util.Optional;

@Repository
@RequiredArgsConstructor
public class NoteRepositoryImpl implements NoteRepository {

    private final JpaNoteRepository jpaNoteRepository;

    @Override
    public Optional<Note> findByIdAndUserId(Long noteId, Long userId) {
        return jpaNoteRepository.findByIdAndUserId(noteId, userId);
    }

    @Override
    public Note save(Note note) {
        return jpaNoteRepository.save(note);
    }

}
