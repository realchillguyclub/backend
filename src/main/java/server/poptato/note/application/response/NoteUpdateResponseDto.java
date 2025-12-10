package server.poptato.note.application.response;

import server.poptato.note.domain.entity.Note;

import java.time.LocalDateTime;

public record NoteUpdateResponseDto(
        Long noteId,
        LocalDateTime modifyDate
) {
    public static NoteUpdateResponseDto from(Note note) {
        return new NoteUpdateResponseDto(note.getId(), note.getModifyDate());
    }

}
