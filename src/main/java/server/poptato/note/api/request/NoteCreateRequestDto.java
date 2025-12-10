package server.poptato.note.api.request;

import jakarta.validation.constraints.Size;

public record NoteCreateRequestDto(
        @Size(max = 255, message = "제목은 최대 255자까지 입력 가능합니다.")
        String title,

        @Size(max = 10000, message = "내용은 최대 10000자까지 입력 가능합니다.")
        String content
) {
}
