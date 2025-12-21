package server.poptato.note.application.response;

import server.poptato.global.util.TimeUtil;
import server.poptato.note.domain.entity.Note;
import server.poptato.note.domain.preview.NotePreview;

public record NoteResponseDto(
        Long noteId,
        String title,
        String content,
        String modifyDate,
        String modifyTime
) {

    public static NoteResponseDto from(Note note) {
        return new NoteResponseDto(
                note.getId(),
                note.getTitle(),
                note.getContent(),
                TimeUtil.getDate(note.getModifyDate()),
                TimeUtil.getTime(note.getModifyDate())
        );
    }

    public static NoteResponseDto fromPreview(NotePreview note) {
        return new NoteResponseDto(
                note.id(),
                note.previewTitle(),
                note.previewContent(),
                TimeUtil.getDate(note.modifyDate()),
                TimeUtil.getTime(note.modifyDate())
		);
    }
}
