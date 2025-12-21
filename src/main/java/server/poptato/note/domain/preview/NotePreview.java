package server.poptato.note.domain.preview;

import java.time.LocalDateTime;

public record NotePreview(
        Long id,
        String previewTitle,
        String previewContent,
        LocalDateTime modifyDate
) {}