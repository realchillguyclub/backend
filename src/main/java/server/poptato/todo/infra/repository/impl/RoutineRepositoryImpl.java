package server.poptato.todo.infra.repository.impl;

import java.util.List;

import org.springframework.stereotype.Repository;

import lombok.RequiredArgsConstructor;
import server.poptato.todo.domain.entity.Routine;
import server.poptato.todo.domain.projection.RoutineCountProjection;
import server.poptato.todo.domain.repository.RoutineRepository;
import server.poptato.todo.infra.repository.JpaRoutineRepository;

@Repository
@RequiredArgsConstructor
public class RoutineRepositoryImpl implements RoutineRepository {

    private final JpaRoutineRepository jpaRoutineRepository;

    @Override
    public void deleteByTodoId(Long todoId) {
        jpaRoutineRepository.deleteByTodoId(todoId);
    }

    @Override
    public void saveAll(List<Routine> routineDays) {
        jpaRoutineRepository.saveAll(routineDays);
    }

    @Override
    public List<Routine> findAllByTodoId(Long todoId) {
        return jpaRoutineRepository.findAllByTodoId(todoId);
    }

    @Override
    public List<Routine> findAllByTodoIdIn(List<Long> todoIds) {
        return jpaRoutineRepository.findAllByTodoIdIn(todoIds);
    }

    @Override
	public List<RoutineCountProjection> countRoutinesByDay(Long userId) {
		return jpaRoutineRepository.countRoutinesByDay(userId);
	}
}
