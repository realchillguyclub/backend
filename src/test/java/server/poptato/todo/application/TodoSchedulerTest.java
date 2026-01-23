package server.poptato.todo.application;

import static org.mockito.Mockito.*;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;

import server.poptato.configuration.ServiceTestConfig;

class TodoSchedulerTest extends ServiceTestConfig {

    @Mock
    private TodoBatchService todoBatchService;

    @InjectMocks
    private TodoScheduler todoScheduler;

    @Test
    @DisplayName("[TC-SCH-001] 스케줄러 실행 시 배치 서비스 메서드가 호출된다")
    void updateTodoType_callsBatchServiceMethods() {
        // when
        todoScheduler.updateTodoType();

        // then
        verify(todoBatchService).updateTodayTodosAndSave();
        verify(todoBatchService).updateDeadlineTodos();
    }
}
