package server.poptato.note.infra.projection;

import java.time.LocalDateTime;

public interface NoteListItemProjection {
    Long getId();
    String getTitle();
    String getPreviewContent();
    LocalDateTime getModifyDate();
}
