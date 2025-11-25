package server.poptato.note.infra.impl;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;
import server.poptato.note.domain.entity.Note;
import server.poptato.note.domain.repository.NoteRepository;
import server.poptato.note.infra.JpaNoteRepository;

@Repository
@RequiredArgsConstructor
public class NoteRepositoryImpl implements NoteRepository {

    private final JpaNoteRepository jpaNoteRepository;

    @Override
    public Note save(Note note) {
        return jpaNoteRepository.save(note);
    }

}
