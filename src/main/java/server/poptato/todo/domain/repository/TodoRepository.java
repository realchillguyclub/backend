package server.poptato.todo.domain.repository;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import jakarta.persistence.Tuple;
import server.poptato.todo.domain.entity.Todo;
import server.poptato.todo.domain.value.TodayStatus;
import server.poptato.todo.domain.value.Type;

public interface TodoRepository {

    List<Todo> findIncompleteTodays(Long userId, Type type, LocalDate todayDate, TodayStatus todayStatus);

    List<Todo> findCompletedTodays(Long userId, LocalDate todayDate);

    List<Todo> findIncompleteTodaysWithCategory(Long userId, Type type, LocalDate todayDate, TodayStatus todayStatus);

    List<Todo> findCompletedTodaysWithCategory(Long userId, LocalDate todayDate);

    Optional<Todo> findById(Long todoId);

    void delete(Todo todo);

    void deleteAll(List<Todo> todos);

    Todo save(Todo todo);

    void saveAll(List<Todo> todo);

    Page<Todo> findByUserIdAndTypeAndTodayStatus(Long userId, Type type, TodayStatus todayStatus, Pageable pageable);

    Integer findMaxTodayOrderByUserIdOrZero(Long userId);

    Integer findMinTodayOrderByUserIdOrZero(Long userId);

    Integer findMaxBacklogOrderByUserIdOrZero(Long userId);

    Page<Todo> findAllBacklogs(Long userId, Type type, TodayStatus status, Pageable pageable);

    Page<Todo> findDeadlineBacklogs(Long userId, LocalDate localDate, Pageable pageable);

    Page<Todo> findBookmarkBacklogs(Long userId, Type type, TodayStatus status, Pageable pageable);

    Page<Todo> findBacklogsByCategoryId(Long userId, Long categoryId, Type type, TodayStatus status, Pageable pageable);

    Page<Todo> findHistories(Long userId, LocalDate localDate, Pageable pageable);

    List<Todo> findByType(Type type);

    void deleteAllByCategoryId(Long categoryId);

    void softDeleteByUserId(Long userId);

    void softDeleteByCategoryId(Long categoryId);

    List<Todo> findIncompleteTodayTodos(Long userId, TodayStatus todayStatus);

    List<Todo> findTodosByDeadLine(Long userId, LocalDate deadline);

    List<Todo> findRoutineTodosByDay(Long userId, String todayDay);

    List<Todo> findIncompleteYesterdays(Long userId);

    List<Tuple> findDatesWithBacklogCount(Long userId, String year, int month);

    boolean existsByUserIdAndTypeAndTodayStatus(Long userId, Type type, TodayStatus todayStatus);

    Map<Long, Integer> findMaxTodayOrdersByUserIdsOrZero(List<Long> userIds);
}
