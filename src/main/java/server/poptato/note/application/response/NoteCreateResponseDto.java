package server.poptato.note.application.response;

import server.poptato.note.domain.entity.Note;

public record NoteCreateResponseDto(
    Long noteId
) {

    public static NoteCreateResponseDto from(Note note) {
        return new NoteCreateResponseDto(note.getId());
    }
}
