package server.poptato.note.api.request;

public record NoteUpdateRequestDto(
        String title,
        String content
) {
}
