package server.poptato.todo.infra.repository;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import server.poptato.todo.domain.entity.Routine;
import server.poptato.todo.domain.projection.RoutineCountProjection;

public interface JpaRoutineRepository extends JpaRepository<Routine, Long> {

	@Modifying
	@Query("DELETE FROM Routine r WHERE r.todoId = :todoId")
	void deleteByTodoId(@Param("todoId") Long todoId);

	List<Routine> findAllByTodoId(Long todoId);

	@Query("""
       SELECT r.day AS day, COUNT(r) AS count
       FROM Routine r
       WHERE r.todoId IN (
           SELECT t.id
           FROM Todo t
           WHERE t.userId = :userId
             AND t.isRoutine = true
             AND t.type = 'BACKLOG'
       )
       AND r.day IS NOT NULL
       GROUP BY r.day
       """)
	List<RoutineCountProjection> countRoutinesByDay(@Param("userId") Long userId);

    List<Routine> findAllByTodoIdIn(List<Long> todoIds);
}
