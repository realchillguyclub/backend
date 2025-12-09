package server.poptato.note.application.response;

import server.poptato.note.domain.preview.NotePreview;

import java.util.List;

public record NotePreviewListResponseDto(
        List<NoteResponseDto> notes
) {

    public static NotePreviewListResponseDto from(List<NotePreview> noteSummaryList) {
        return new NotePreviewListResponseDto(
                noteSummaryList.stream()
                        .map(NoteResponseDto::fromPreview)
                        .toList()
        );
    }
}
