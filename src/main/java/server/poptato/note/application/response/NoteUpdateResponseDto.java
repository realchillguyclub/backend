package server.poptato.note.application.response;

import server.poptato.global.util.TimeUtil;
import server.poptato.note.domain.entity.Note;

public record NoteUpdateResponseDto(
        Long noteId,
        String modifyDate,
        String modifyTime
) {
    public static NoteUpdateResponseDto from(Note note) {
        return new NoteUpdateResponseDto(
                note.getId(),
                TimeUtil.getDate(note.getModifyDate()),
                TimeUtil.getTime(note.getModifyDate())
        );
    }
}
