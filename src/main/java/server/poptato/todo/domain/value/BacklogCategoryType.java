package server.poptato.todo.domain.value;

import lombok.AllArgsConstructor;
import lombok.Getter;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import server.poptato.todo.domain.entity.Todo;
import server.poptato.todo.domain.repository.TodoRepository;

import java.util.Arrays;
import java.util.Objects;

@Getter
@AllArgsConstructor
public enum BacklogCategoryType {

    ALL(-1L, "전체") {
        @Override
        public Page<Todo> getBacklogs(TodoRepository repo, Long userId, Long categoryId, Pageable pageable) {
            return repo.findAllBacklogs(userId, Type.BACKLOG, TodayStatus.COMPLETED, pageable);
        }
    },
    BOOKMARK(0L, "중요") {
        @Override
        public Page<Todo> getBacklogs(TodoRepository repo, Long userId, Long categoryId, Pageable pageable) {
            return repo.findBookmarkBacklogs(userId, Type.BACKLOG, TodayStatus.COMPLETED, pageable);
        }
    },
    NORMAL(null, null) {
        @Override
        public Page<Todo> getBacklogs(TodoRepository repo, Long userId, Long categoryId, Pageable pageable) {
            return repo.findBacklogsByCategoryId(userId, categoryId, Type.BACKLOG, TodayStatus.COMPLETED, pageable);
        }
    };

    private final Long id;
    private final String defaultName;

    public abstract Page<Todo> getBacklogs(TodoRepository repo, Long userId, Long categoryId, Pageable pageable);

    public static BacklogCategoryType from(Long categoryId) {
        return Arrays.stream(values())
                .filter(type -> Objects.equals(type.id, categoryId))
                .findFirst()
                .orElse(NORMAL);
    }
}