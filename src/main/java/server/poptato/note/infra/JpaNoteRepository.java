package server.poptato.note.infra;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import server.poptato.note.domain.entity.Note;
import server.poptato.note.infra.projection.NotePreviewProjection;

import java.util.List;
import java.util.Optional;

public interface JpaNoteRepository extends JpaRepository<Note, Long> {

    @Query(value = """
        SELECT
            n.id AS id,
            SUBSTRING(LTRIM(n.title), 1, :previewTitleLength) AS previewTitle,
            SUBSTRING(LTRIM(n.content), 1, :previewContentLength) AS previewContent,
            n.modify_date AS modifyDate
        FROM note n
        WHERE n.user_id = :userId
          AND n.is_deleted = false
        ORDER BY n.modify_date DESC
        """,
            nativeQuery = true)
    List<NotePreviewProjection> findNotePreviewsByUserId(
            @Param("userId") Long userId,
            @Param("previewTitleLength") int previewTitleLength,
            @Param("previewContentLength") int previewContentLength
    );

    @Query("""
        SELECT n
        FROM Note n
        WHERE n.id = :noteId
          AND n.userId = :userId
        """)
    Optional<Note> findByIdAndUserId(@Param("noteId") Long noteId, @Param("userId") Long userId);

    @Modifying(clearAutomatically = true, flushAutomatically = true)
    @Query("""
        UPDATE Note n
        SET n.isDeleted = true
        WHERE n.userId = :userId
        """)
    void softDeleteByUserId(@Param("userId") Long userId);
}
