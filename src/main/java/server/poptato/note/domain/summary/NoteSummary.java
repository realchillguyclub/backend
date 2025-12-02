package server.poptato.note.domain.summary;

import java.time.LocalDateTime;

public record NoteSummary(
        Long id,
        String previewTitle,
        String previewContent,
        LocalDateTime modifyDate
) {}