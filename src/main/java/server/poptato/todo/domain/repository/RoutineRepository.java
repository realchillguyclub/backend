package server.poptato.todo.domain.repository;

import java.util.List;

import server.poptato.todo.domain.entity.Routine;
import server.poptato.todo.domain.projection.RoutineCountProjection;

public interface RoutineRepository {

    void deleteByTodoId(Long todoId);

    void saveAll(List<Routine> routineDays);

    List<Routine> findAllByTodoId(Long todoId);

    List<Routine> findAllByTodoIdIn(List<Long> todoIds);

	List<RoutineCountProjection> countRoutinesByDay(Long userId);
}
