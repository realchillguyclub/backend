package server.poptato.todo.application.response;

import server.poptato.todo.domain.entity.Todo;

public record TodayTodoCreateResponseDto(
        Long todoId
){

    public static TodayTodoCreateResponseDto from(Todo todo) {
        return new TodayTodoCreateResponseDto(todo.getId());
    }
}