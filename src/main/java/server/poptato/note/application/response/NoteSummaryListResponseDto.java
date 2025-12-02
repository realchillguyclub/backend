package server.poptato.note.application.response;

import server.poptato.note.domain.summary.NoteSummary;

import java.util.List;

public record NoteSummaryListResponseDto(
        List<NoteResponseDto> notes
) {

    public static NoteSummaryListResponseDto from(List<NoteSummary> noteSummaryList) {
        return new NoteSummaryListResponseDto(
                noteSummaryList.stream()
                        .map(NoteResponseDto::fromPreview)
                        .toList()
        );
    }
}
