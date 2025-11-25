package server.poptato.note.domain.repository;

import server.poptato.note.domain.entity.Note;

public interface NoteRepository {

    Note save(Note note);
}
