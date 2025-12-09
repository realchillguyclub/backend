package server.poptato.note.infra.projection;

import java.time.LocalDateTime;

public interface NotePreviewProjection {
    Long getId();
    String getPreviewTitle();
    String getPreviewContent();
    LocalDateTime getModifyDate();
}
