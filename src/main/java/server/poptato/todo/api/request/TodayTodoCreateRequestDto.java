package server.poptato.todo.api.request;

import jakarta.validation.constraints.NotBlank;

public record TodayTodoCreateRequestDto(
        @NotBlank(message = "오늘 할 일 생성 시 내용은 필수입니다.")
        String content
) {
}
