package server.poptato.todo.infra.repository.impl;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Repository;

import jakarta.persistence.Tuple;
import lombok.RequiredArgsConstructor;
import server.poptato.todo.domain.entity.Todo;
import server.poptato.todo.domain.repository.TodoRepository;
import server.poptato.todo.domain.value.TodayStatus;
import server.poptato.todo.domain.value.Type;
import server.poptato.todo.infra.repository.JpaTodoRepository;

@Repository
@RequiredArgsConstructor
public class TodoRepositoryImpl implements TodoRepository {

    private final JpaTodoRepository jpaTodoRepository;

    @Override
    public List<Todo> findIncompleteTodays(Long userId, Type type, LocalDate todayDate, TodayStatus todayStatus) {
        return jpaTodoRepository.findIncompleteTodays(userId, type, todayDate, todayStatus);
    }

    @Override
    public List<Todo> findCompletedTodays(Long userId, LocalDate todayDate) {
        return jpaTodoRepository.findCompletedTodays(userId, todayDate);
    }

    @Override
    public List<Todo> findIncompleteTodaysWithCategory(Long userId, Type type, LocalDate todayDate, TodayStatus todayStatus) {
        return jpaTodoRepository.findIncompleteTodaysWithCategory(userId, type, todayDate, todayStatus);
    }

    @Override
    public List<Todo> findCompletedTodaysWithCategory(Long userId, LocalDate todayDate) {
        return jpaTodoRepository.findCompletedTodaysWithCategory(userId, todayDate);
    }

    @Override
    public Optional<Todo> findById(Long todoId) {
        return jpaTodoRepository.findById(todoId);
    }

    @Override
    public void delete(Todo todo) {
        jpaTodoRepository.delete(todo);
    }

    @Override
    public void deleteAll(List<Todo> todos) {
        jpaTodoRepository.deleteAll(todos);
    }

    @Override
    public Todo save(Todo todo) {
        return jpaTodoRepository.save(todo);
    }

    @Override
    public void saveAll(List<Todo> todos) {
        jpaTodoRepository.saveAll(todos);
    }

    @Override
    public Page<Todo> findByUserIdAndTypeAndTodayStatus(Long userId, Type type, TodayStatus todayStatus, Pageable pageable) {
        return jpaTodoRepository.findByUserIdAndTypeAndTodayStatus(userId, type, todayStatus, pageable);
    }

    @Override
    public Integer findMaxTodayOrderByUserIdOrZero(Long userId) {
        return jpaTodoRepository.findMaxTodayOrderByUserIdOrZero(userId);
    }

    @Override
    public Integer findMinTodayOrderByUserIdOrZero(Long userId) {
        return jpaTodoRepository.findMinTodayOrderByUserIdOrZero(userId);
    }

    @Override
    public Integer findMaxBacklogOrderByUserIdOrZero(Long userId) {
        return jpaTodoRepository.findMaxBacklogOrderByUserIdOrZero(userId);
    }

    @Override
    public Page<Todo> findAllBacklogs(Long userId, Type type, TodayStatus status, Pageable pageable) {
        return jpaTodoRepository.findAllBacklogs(userId, type, status, pageable);
    }

    @Override
    public Page<Todo> findDeadlineBacklogs(Long userId, LocalDate localDate, Pageable pageable) {
        return jpaTodoRepository.findDeadlineBacklogs(userId, localDate, pageable);
    }

    @Override
    public Page<Todo> findBookmarkBacklogs(Long userId, Type type, TodayStatus status, Pageable pageable) {
        return jpaTodoRepository.findBookmarkBacklogs(userId, type, status, pageable);
    }

    @Override
    public Page<Todo> findBacklogsByCategoryId(Long userId, Long categoryId, Type type, TodayStatus status, Pageable pageable) {
        return jpaTodoRepository.findBacklogsByCategoryId(userId, categoryId, type, status, pageable);
    }

    @Override
    public Page<Todo> findHistories(Long userId, LocalDate localDate, Pageable pageable) {
        return jpaTodoRepository.findHistories(userId, localDate, pageable);
    }

    @Override
    public List<Todo> findByType(Type type) {
        return jpaTodoRepository.findByType(type);
    }

    @Override
    public void deleteAllByCategoryId(Long categoryId) {
        jpaTodoRepository.deleteAllByCategoryId(categoryId);
    }

    @Override
    public void softDeleteByUserId(Long userId) {
        jpaTodoRepository.softDeleteByUserId(userId);
    }

    @Override
    public void softDeleteByCategoryId(Long categoryId) {
        jpaTodoRepository.softDeleteByCategoryId(categoryId);
    }

    @Override
    public List<Todo> findIncompleteTodayTodos(Long userId, TodayStatus todayStatus) {
        return jpaTodoRepository.findIncompleteTodayTodos(userId, todayStatus);
    }

    @Override
    public List<Todo> findTodosByDeadLine(Long userId, LocalDate deadline) {
        return jpaTodoRepository.findTodosByDeadLine(userId, deadline);
    }

    @Override
    public List<Todo> findRoutineTodosByDay(Long userId, String todayDay) {
        return jpaTodoRepository.findRoutineTodosByDay(userId, todayDay);
    }

    @Override
    public List<Todo> findIncompleteYesterdays(Long userId) {
        return jpaTodoRepository.findIncompleteYesterdays(userId);
    }

    @Override
    public List<Tuple> findDatesWithBacklogCount(Long userId, String year, int month) {
        return jpaTodoRepository.findDatesWithBacklogCount(userId, year, month);
    }

    @Override
    public boolean existsByUserIdAndTypeAndTodayStatus(Long userId, Type type, TodayStatus todayStatus) {
        return jpaTodoRepository.existsByUserIdAndTypeAndTodayStatus(userId, type, todayStatus);
    }

    @Override
    public Map<Long, Integer> findMaxTodayOrdersByUserIdsOrZero(List<Long> userIds) {
        return jpaTodoRepository.findMaxTodayOrdersByUserIds(userIds).stream()
                .collect(Collectors.toMap(
                        tuple -> ((Number) tuple.get("userId")).longValue(),
                        tuple -> ((Number) tuple.get("maxOrder")).intValue()
                ));
    }
}
