package server.poptato.note.application.response;

import server.poptato.note.domain.entity.Note;

public record NoteResponseDto(
        Long noteId,
        String title,
        String content
) {

    public static NoteResponseDto from(Note note) {
        return new NoteResponseDto(note.getId(), note.getTitle(), note.getContent());
    }
}
