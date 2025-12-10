package server.poptato.note.application.response;

import server.poptato.note.domain.preview.NotePreview;

import java.util.List;

public record NotePreviewsResponseDto(
        List<NoteResponseDto> notes
) {

    public static NotePreviewsResponseDto from(List<NotePreview> noteSummaryList) {
        return new NotePreviewsResponseDto(
                noteSummaryList.stream()
                        .map(NoteResponseDto::fromPreview)
                        .toList()
        );
    }
}
