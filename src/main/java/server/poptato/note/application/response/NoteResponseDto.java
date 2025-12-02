package server.poptato.note.application.response;

import server.poptato.note.domain.entity.Note;
import server.poptato.note.domain.summary.NoteSummary;

import java.time.LocalDateTime;

public record NoteResponseDto(
        Long noteId,
        String title,
        String content,
        LocalDateTime modifyDate
) {

    public static NoteResponseDto from(Note note) {
        return new NoteResponseDto(note.getId(), note.getTitle(), note.getContent(), note.getModifyDate());
    }

    public static NoteResponseDto fromPreview(NoteSummary note) {
        return new NoteResponseDto(note.id(), note.previewTitle(), note.previewContent(), note.modifyDate());
    }
}
