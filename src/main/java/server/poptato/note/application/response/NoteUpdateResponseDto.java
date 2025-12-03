package server.poptato.note.application.response;

import java.time.LocalDateTime;

public record NoteUpdateResponseDto(
        Long noteId,
        LocalDateTime modifyDate
) {}
