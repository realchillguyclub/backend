package server.poptato.todo.application;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import server.poptato.infra.firebase.application.FcmNotificationBatchService;
import server.poptato.todo.api.request.EventCreateRequestDto;
import server.poptato.todo.api.request.TodayTodoCreateRequestDto;
import server.poptato.todo.application.response.TodayListResponseDto;
import server.poptato.todo.application.response.TodayResponseDto;
import server.poptato.todo.application.response.TodayTodoCreateResponseDto;
import server.poptato.todo.domain.entity.Routine;
import server.poptato.todo.domain.entity.Todo;
import server.poptato.todo.domain.repository.RoutineRepository;
import server.poptato.todo.domain.repository.TodoRepository;
import server.poptato.todo.domain.value.TodayStatus;
import server.poptato.todo.domain.value.Type;
import server.poptato.user.domain.repository.UserRepository;
import server.poptato.user.domain.value.MobileType;
import server.poptato.user.validator.UserValidator;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Transactional
@RequiredArgsConstructor
@Service
public class TodoTodayService {
    private final TodoRepository todoRepository;
    private final RoutineRepository routineRepository;
    private final UserRepository userRepository;
    private final UserValidator userValidator;
    private final FcmNotificationBatchService fcmNotificationBatchService;

    /**
     * 오늘의 할 일 목록을 조회합니다.
     *
     * @param userId 사용자 ID
     * @param page 요청 페이지 번호
     * @param size 한 페이지에 보여줄 항목 수
     * @param todayDate 오늘 날짜
     * @return TodayListResponseDto 오늘의 할 일 목록과 페이지 정보
     */
    public TodayListResponseDto getTodayList(
            long userId, MobileType mobileType, int page, int size, LocalDate todayDate
    ) {
        userValidator.checkIsExistUser(userId);

        List<Todo> todays = getAllTodays(userId, todayDate);
        List<Todo> todaySubList = getTodayPagination(todays, page, size);
        int totalPageCount = (int) Math.ceil((double) todays.size() / size);

        List<TodayResponseDto> todayDtos = todaySubList.stream()
                .map(todo -> {
                    List<String> routineDays = routineRepository.findAllByTodoId(todo.getId())
                            .stream()
                            .map(Routine::getDay)
                            .collect(Collectors.toList());
                    return TodayResponseDto.of(todo, routineDays, mobileType);
                })
                .collect(Collectors.toList());

        return TodayListResponseDto.of(todayDate, todayDtos, totalPageCount);
    }

    /**
     * 오늘의 할 일 목록을 페이징 처리합니다.
     *
     * @param todays 오늘의 모든 할 일 목록
     * @param page 요청 페이지 번호
     * @param size 한 페이지에 보여줄 항목 수
     * @return 페이징 처리된 할 일 목록
     */
    private List<Todo> getTodayPagination(List<Todo> todays, int page, int size) {
        int start = page * size;
        int end = Math.min(start + size, todays.size());

        if (start >= end) {
            return new ArrayList<>();
        }

        return todays.subList(start, end);
    }

    /**
     * 사용자의 오늘의 모든 할 일을 조회합니다.
     * 완료된 항목과 미완료 항목을 포함합니다.
     *
     * @param userId 사용자 ID
     * @param todayDate 오늘 날짜
     * @return 오늘의 모든 할 일 목록
     */
    private List<Todo> getAllTodays(long userId, LocalDate todayDate) {
        List<Todo> todays = new ArrayList<>();
        List<Todo> incompleteTodos = todoRepository.findIncompleteTodaysWithCategory(userId, Type.TODAY, todayDate, TodayStatus.INCOMPLETE);
        List<Todo> completedTodos = todoRepository.findCompletedTodaysWithCategory(userId, todayDate);

        todays.addAll(incompleteTodos);
        todays.addAll(completedTodos);

        return todays;
    }

    /**
     * 오늘 할 일 생성 메서드.
     * 사용자 ID와 요청 데이터를 기반으로 새로운 오늘 할 일을 생성합니다.
     *
     * @param userId 사용자 ID
     * @param todayTodoCreateRequestDto 오늘 할 일 생성 요청 데이터
     * @return 생성된 오늘 할 일의 정보
     */
    public TodayTodoCreateResponseDto createTodayTodo(Long userId, TodayTodoCreateRequestDto todayTodoCreateRequestDto) {
        userValidator.checkIsExistUser(userId);
        Integer maxTodayOrder = todoRepository.findMaxTodayOrderByUserIdOrZero(userId);
        Todo newTodayTodo = Todo.createTodayTodo(
                userId,
                todayTodoCreateRequestDto.content(),
                null,
                false,
                maxTodayOrder
        );

        todoRepository.save(newTodayTodo);
        return TodayTodoCreateResponseDto.from(newTodayTodo);
    }

    /**
     * 이벤트 생성 및 전체 사용자 대상 Today Todo 생성 처리.
     *
     * @param request 이벤트 생성 요청 데이터
     */
    @Transactional
    public void createEventAndTodosIfNeeded(EventCreateRequestDto request) {
        // 알림 수신 동의 유저에게 푸쉬 알림 전송
        fcmNotificationBatchService.sendEventNotifications(
                request.pushAlarmTitle(),
                request.pushAlarmContent()
        );

        // 필요 시 전체 유저에게 Today Todo 생성
        if (request.isCreateTodayTodo()) {
            createTodayTodosForAllUsers(request);
        }
    }

    /**
     * 전체 사용자에게 Today Todo를 생성한다.
     *
     * @param request 이벤트 요청 데이터
     */
    private void createTodayTodosForAllUsers(EventCreateRequestDto request) {
        List<Long> userIds = userRepository.findAllUserIds();
        Map<Long, Integer> maxTodayOrders = todoRepository.findMaxTodayOrdersByUserIdsOrZero(userIds);

        List<Todo> todosToSave = userIds.stream()
                .map(userId -> {
                    int todayOrder = maxTodayOrders.get(userId) + 1;
                    return Todo.createTodayTodo(
                            userId,
                            request.todoContent(),
                            request.todoTime(),
                            request.isBookmarked(),
                            todayOrder
                    );
                })
                .toList();

        todoRepository.saveAll(todosToSave);
    }
}
