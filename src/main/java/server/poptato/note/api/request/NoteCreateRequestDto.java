package server.poptato.note.api.request;

public record NoteCreateRequestDto(
    String title,
    String content
) {
}
