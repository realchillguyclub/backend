package server.poptato.todo.application;

import java.sql.Date;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.TextStyle;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.function.BiConsumer;
import java.util.function.Function;
import java.util.stream.Collectors;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import lombok.RequiredArgsConstructor;
import server.poptato.category.domain.entity.Category;
import server.poptato.category.domain.repository.CategoryRepository;
import server.poptato.category.validator.CategoryValidator;
import server.poptato.emoji.domain.entity.Emoji;
import server.poptato.emoji.domain.repository.EmojiRepository;
import server.poptato.global.exception.CustomException;
import server.poptato.global.util.FileUtil;
import server.poptato.todo.api.request.CheckYesterdayTodosRequestDto;
import server.poptato.todo.api.request.ContentUpdateRequestDto;
import server.poptato.todo.api.request.DeadlineUpdateRequestDto;
import server.poptato.todo.api.request.RoutineUpdateRequestDto;
import server.poptato.todo.api.request.SwipeRequestDto;
import server.poptato.todo.api.request.TimeUpdateRequestDto;
import server.poptato.todo.api.request.TodoCategoryUpdateRequestDto;
import server.poptato.todo.api.request.TodoDragAndDropRequestDto;
import server.poptato.todo.application.response.HistoryCalendarListResponseDto;
import server.poptato.todo.application.response.PaginatedHistoryResponseDto;
import server.poptato.todo.application.response.TodoDetailResponseDto;
import server.poptato.todo.domain.entity.CompletedDateTime;
import server.poptato.todo.domain.entity.Routine;
import server.poptato.todo.domain.entity.TimeAlarm;
import server.poptato.todo.domain.entity.Todo;
import server.poptato.todo.domain.projection.RoutineCountProjection;
import server.poptato.todo.domain.repository.CompletedDateTimeRepository;
import server.poptato.todo.domain.repository.RoutineRepository;
import server.poptato.todo.domain.repository.TimeAlarmRepository;
import server.poptato.todo.domain.repository.TodoRepository;
import server.poptato.todo.domain.value.TodayStatus;
import server.poptato.todo.domain.value.Type;
import server.poptato.todo.status.TodoErrorStatus;
import server.poptato.user.domain.value.MobileType;
import server.poptato.user.validator.UserValidator;

@RequiredArgsConstructor
@Service
public class TodoService {
    private final UserValidator userValidator;
    private final CategoryValidator categoryValidator;
    private final TodoRepository todoRepository;
    private final TimeAlarmRepository timeAlarmRepository;
    private final RoutineRepository routineRepository;
    private final CompletedDateTimeRepository completedDateTimeRepository;
    private final CategoryRepository categoryRepository;
    private final EmojiRepository emojiRepository;

    @PersistenceContext
    private EntityManager entityManager;

    /**
     * 특정 할 일을 삭제합니다.
     *
     * @param userId 사용자 ID
     * @param todoId 삭제할 할 일 ID
     */

    @Transactional
    public void deleteTodoById(Long userId, Long todoId) {
        userValidator.checkIsExistUser(userId);
        Todo findTodo = validateAndReturnTodo(userId, todoId);
        findTodo.softDelete();
    }

    /**
     * 특정 할 일을 조회하고 유효성 검사를 수행합니다.
     *
     * @param userId 사용자 ID
     * @param todoId 할 일 ID
     * @return 조회된 할 일 엔티티
     */
    private Todo validateAndReturnTodo(Long userId, Long todoId) {
        Todo findTodo = todoRepository.findById(todoId)
                .orElseThrow(() -> new CustomException(TodoErrorStatus._TODO_NOT_EXIST));
        if (!findTodo.getUserId().equals(userId)) {
            throw new CustomException(TodoErrorStatus._TODO_USER_NOT_MATCH);
        }
        return findTodo;
    }

    /**
     * 특정 할 일의 즐겨찾기 상태를 토글합니다.
     *
     * @param userId 사용자 ID
     * @param todoId 대상 할 일 ID
     */
    @Transactional
    public void toggleIsBookmark(Long userId, Long todoId) {
        Todo todo = validateAndReturnTodo(userId, todoId);
        todo.toggleBookmark();
    }

    /**
     * 할 일의 상태를 스와이프 방식으로 변경합니다.
     *
     * @param userId 사용자 ID
     * @param swipeRequestDto 스와이프 요청 데이터
     */
    @Transactional
    public void swipe(Long userId, SwipeRequestDto swipeRequestDto) {
        userValidator.checkIsExistUser(userId);
        Todo findTodo = validateAndReturnTodo(userId, swipeRequestDto.todoId());
        if (Type.TODAY == findTodo.getType()) {
            swipeTodayToBacklog(findTodo);
        } else if (Type.BACKLOG == findTodo.getType()) {
            swipeBacklogToToday(findTodo);
        }
    }

    /**
     * 백로그 할 일을 TODAY 할 일로 변경합니다.
     *
     * @param todo 변경할 할 일 객체
     */
    private void swipeBacklogToToday(Todo todo) {
        Integer maxTodayOrder = todoRepository.findMaxTodayOrderByUserIdOrZero(todo.getUserId());
        todo.changeToToday(maxTodayOrder);
    }

    /**
     * TODAY 할 일을 백로그로 변경합니다.
     *
     * @param todo 변경할 할 일 객체
     */
    private void swipeTodayToBacklog(Todo todo) {
        if (TodayStatus.COMPLETED == todo.getTodayStatus()) {
            throw new CustomException(TodoErrorStatus._ALREADY_COMPLETED_TODO);
        }
        Integer maxBacklogOrder = todoRepository.findMaxBacklogOrderByUserIdOrZero(todo.getUserId());
        todo.changeToBacklog(maxBacklogOrder);
    }

    /**
     * 할 일의 순서를 드래그 앤 드롭 방식으로 변경합니다.
     *
     * @param userId 사용자 ID
     * @param requestDto 순서 변경 요청 데이터
     */
    @Transactional
    public void dragAndDrop(Long userId, TodoDragAndDropRequestDto requestDto) {
        userValidator.checkIsExistUser(userId);

        List<Todo> todos = requestDto.todoIds().stream()
                .map(todoId -> {
                    Todo todo = todoRepository.findById(todoId)
                            .orElseThrow(() -> new CustomException(TodoErrorStatus._TODO_NOT_EXIST));
                    if (!todo.getUserId().equals(userId)) {
                        // 사용자의 할 일이 아닌 경우
                        throw new CustomException(TodoErrorStatus._TODO_USER_NOT_MATCH);
                    }
                    return todo;
                })
                .toList();

        if (Type.TODAY == requestDto.type()) {
            reassignOrder(todos, Todo::getTodayOrder, Todo::updateTodayOrder);
        } else if (Type.BACKLOG == requestDto.type()) {
            reassignOrder(todos, Todo::getBacklogOrder, Todo::updateBacklogOrder);
        }
    }

    /**
     * 할 일 목록의 정렬 순서를 재할당하는 공통 메서드.
     *
     * @param todos 재할당할 할 일 목록
     * @param getOrder 각 할 일의 기존 순서를 가져오는 함수
     * @param setOrder 각 할 일에 새로운 순서를 설정하는 함수
     */
    private void reassignOrder(List<Todo> todos,
                               Function<Todo, Integer> getOrder,
                               BiConsumer<Todo, Integer> setOrder) {
        // 기존 순서를 가져와 내림차순으로 정렬
        List<Integer> newOrders = todos.stream()
                .map(todo -> {
                    if (TodayStatus.COMPLETED == todo.getTodayStatus()) {
                        // 완료된 할 일은 순서를 -1로 설정
                        return -1;
                    }
                    return getOrder.apply(todo);
                })
                .sorted(Collections.reverseOrder())
                .toList();

        // 정렬된 순서를 각 할 일에 재할당하고 저장
        for (int todoIndex = 0; todoIndex < todos.size(); todoIndex++) {
            Todo todo = todos.get(todoIndex);
            if (TodayStatus.COMPLETED == todo.getTodayStatus()) {
                // 완료된 할 일은 순서 수정 x
                continue;
            }
            setOrder.accept(todo, newOrders.get(todoIndex));
            todoRepository.save(todo);
        }
    }

    /**
     * 특정 할 일의 상세 정보를 조회합니다.
     *
     * @param userId 사용자 ID
     * @param todoId 조회할 할 일 ID
     * @return 할 일 상세 정보
     */
    @Transactional(readOnly = true)
    public TodoDetailResponseDto getTodoInfo(Long userId, MobileType mobileType, Long todoId) {
        userValidator.checkIsExistUser(userId);
        String imageUrlExtension = mobileType.getImageUrlExtension();
        Todo findTodo = validateAndReturnTodo(userId, todoId);
        Category findCategory = findTodo.getCategoryId() != null ?
                categoryRepository.findById(findTodo.getCategoryId()).orElse(null) : null;
        Emoji findEmoji = findCategory != null && findCategory.getEmojiId() != null ?
                emojiRepository.findById(findCategory.getEmojiId()).orElse(null) : null;
        String modifiedImageUrl = findEmoji != null && findEmoji.getImageUrl() != null ?
                FileUtil.changeFileExtension(findEmoji.getImageUrl(), imageUrlExtension) : null;
        List<String> routineDays = routineRepository.findAllByTodoId(todoId).stream()
                .map(Routine::getDay)
                .toList();

        return TodoDetailResponseDto.of(findTodo, findCategory, modifiedImageUrl, routineDays);
    }

    /**
     * 특정 할 일의 시간을 업데이트합니다.
     *
     * @param userId 사용자 ID
     * @param todoId 업데이트할 할 일 ID
     * @param requestDto 시간 업데이트 요청 데이터
     */
    @Transactional
    public void updateTime(Long userId, Long todoId, TimeUpdateRequestDto requestDto) {
        userValidator.checkIsExistUser(userId);
        Todo findTodo = validateAndReturnTodo(userId, todoId);
        Optional<TimeAlarm> optionalAlarm = timeAlarmRepository.findByTodoId(todoId);

        if (requestDto.todoTime() != null) {
            TimeAlarm timeAlarm = optionalAlarm.orElseGet(() ->
                    TimeAlarm.builder()
                            .todoId(todoId)
                            .userId(userId)
                            .build());
            timeAlarm.updateNotified(false);
            timeAlarmRepository.save(timeAlarm);
        } else {
            optionalAlarm.ifPresent(timeAlarmRepository::delete);
        }

        findTodo.updateTime(requestDto.todoTime());
    }

    /**
     * 특정 할 일의 마감 기한을 업데이트합니다.
     *
     * @param userId 사용자 ID
     * @param todoId 업데이트할 할 일 ID
     * @param requestDto 마감 기한 업데이트 요청 데이터
     */
    @Transactional
    public void updateDeadline(Long userId, Long todoId, DeadlineUpdateRequestDto requestDto) {
        userValidator.checkIsExistUser(userId);
        Todo findTodo = validateAndReturnTodo(userId, todoId);
        findTodo.updateDeadline(requestDto.deadline());
    }

    /**
     * 특정 할 일의 루틴을 등록합니다. (v1.3.0~)
     *
     * @param userId 사용자 ID
     * @param todoId 루틴을 등록할 할 일 ID
     * @param requestDto 루틴 등록 요일
     */
    @Transactional
    public void createRoutine(Long userId, Long todoId, RoutineUpdateRequestDto requestDto) {
        userValidator.checkIsExistUser(userId);
        Todo findTodo = validateAndReturnTodo(userId, todoId);
        findTodo.setRoutine(true);
        findTodo.setRepeat(false);

        routineRepository.deleteByTodoId(todoId);
        List<String> newDays = requestDto.routineDays();
        if (!newDays.isEmpty()) {
            List<Routine> routineDays = newDays.stream()
                    .map(day -> Routine.builder()
                            .todoId(todoId)
                            .day(day)
                            .build())
                    .toList();
            routineRepository.saveAll(routineDays);
        }
    }

    /**
     * 특정 할 일의 루틴을 삭제합니다. (v1.3.0~)
     *
     * @param userId 사용자 ID
     * @param todoId 루틴을 삭제할 할 일 ID
     */
    @Transactional
    public void deleteRoutine(Long userId, Long todoId) {
        userValidator.checkIsExistUser(userId);
        Todo findTodo = validateAndReturnTodo(userId, todoId);
        findTodo.setRoutine(false);
        routineRepository.deleteByTodoId(todoId);
    }

    /**
     * 특정 할 일의 내용을 업데이트합니다.
     *
     * @param userId 사용자 ID
     * @param todoId 업데이트할 할 일 ID
     * @param requestDto 내용 업데이트 요청 데이터
     */
    @Transactional
    public void updateContent(Long userId, Long todoId, ContentUpdateRequestDto requestDto) {
        userValidator.checkIsExistUser(userId);
        Todo findTodo = validateAndReturnTodo(userId, todoId);
        findTodo.updateContent(requestDto.content());
    }

    /**
     * 특정 할 일의 완료 상태를 업데이트합니다.
     *
     * @param userId 사용자 ID
     * @param todoId 업데이트할 할 일 ID
     */
    @Transactional
    public void updateIsCompleted(Long userId, Long todoId) {
        userValidator.checkIsExistUser(userId);
        Todo findTodo = validateAndReturnTodo(userId, todoId);
        updateTodayIsCompleted(findTodo);
    }

    /**
     * 오늘 할 일의 완료 상태를 토글합니다.
     * - 미완료(INCOMPLETE) → 완료(COMPLETED)로 변경하고 완료 시간을 기록합니다.
     * - 완료(COMPLETED) → 미완료(INCOMPLETE)로 변경하고 기존 완료 기록을 삭제합니다.
     *
     * @param findTodo 상태를 변경할 할 일 객체
     */
    private void updateTodayIsCompleted(Todo findTodo) {
        TodayStatus status = findTodo.getTodayStatus();

        if (TodayStatus.INCOMPLETE.equals(status)) {
            // 오늘 완료 상태로 변경하고 현재 시간을 완료 시간으로 저장
            findTodo.completeTodayTodo();
            completedDateTimeRepository.save(
                    CompletedDateTime.builder()
                            .todoId(findTodo.getId())
                            .dateTime(LocalDateTime.now())
                            .build()
            );

        } else if (TodayStatus.COMPLETED.equals(status)) {
            // 미완료로 변경하며, 오늘의 최소 순서를 가져와 반영
            Integer minTodayOrder = todoRepository.findMinTodayOrderByUserIdOrZero(findTodo.getUserId());
            findTodo.incompleteTodayTodo(minTodayOrder);

            // 기존 완료 기록이 존재하면 삭제, 없으면 예외 발생
            CompletedDateTime completedDateTime = completedDateTimeRepository
                    .findByTodoIdAndDate(findTodo.getId(), findTodo.getTodayDate())
                    .orElseThrow(() -> new CustomException(TodoErrorStatus._COMPLETED_DATETIME_NOT_EXIST));
            completedDateTimeRepository.delete(completedDateTime);
        }
    }

    /**
     * 어제 한 일을 체크하고, 상태를 변경합니다.
     * - 체크된 할 일들은 `COMPLETED` 상태로 변경됩니다.
     * - 체크되지 않은 할 일들은 아래에 따라 동작합니다
     * 1) isEvent == true 라면 제거됩니다.
     * 2) isEvent == false 라면 `BACKLOG`로 이동합니다.
     *
     * @param userId 사용자 ID
     * @param request 체크된 할 일 목록 DTO
     */
    @Transactional
    public void checkYesterdayTodos(Long userId, CheckYesterdayTodosRequestDto request) {
        userValidator.checkIsExistUser(userId);
        List<Todo> allYesterdays = todoRepository.findIncompleteYesterdays(userId);
        Set<Long> checkedTodoIds = new HashSet<>(request.todoIds());

        // 1. 체크된 할 일들 (미완료 -> 완료)
        List<Todo> completedTodos = allYesterdays.stream()
                .filter(todo -> checkedTodoIds.contains(todo.getId()))
                .peek(this::updateYesterdayIsCompleted)
                .toList();

        // 2. 체크되지 않은 할 일들 분기 처리
        List<Todo> backloggedTodos = new ArrayList<>();
        List<Todo> toDelete = new ArrayList<>();

        for (Todo todo : allYesterdays) {
            if (!checkedTodoIds.contains(todo.getId())) {
                if (todo.isEvent()) {
                    toDelete.add(todo);
                } else {
                    todo.updateType(Type.BACKLOG);
                    backloggedTodos.add(todo);
                }
            }
        }

        todoRepository.saveAll(completedTodos);
        todoRepository.saveAll(backloggedTodos);
        // Soft Delete 처리
        for (Todo todo : toDelete) {
            todo.softDelete();
        }
        todoRepository.saveAll(toDelete);
        entityManager.flush();
        entityManager.clear();

        // 3. BACKLOG로 이동한 Todo 중에서, 아래의 경우를 처리
        //  1) 오늘 날짜 == 마감 기한
        //  2) 오늘 요일 == 요일 반복 설정
        processUpdateDeadlineTodos(LocalDate.now(), List.of(userId));
    }

    /**
     * 특정 할 일의 어제 완료 상태를 업데이트합니다.
     * - 미완료(INCOMPLETE) 상태 → 완료(COMPLETED) 상태로 변경
     * - 완료 시간을 "어제 날짜의 23:59"로 설정
     * - 반복 할 일이면 새로운 백로그 할 일을 생성
     *
     * @param findTodo 업데이트할 할 일 객체
     */
    private void updateYesterdayIsCompleted(Todo findTodo) {
        int existBacklogOrder = findTodo.getBacklogOrder();
        findTodo.updateYesterdayToCompleted();
        // 완료 시간을 "어제 날짜의 23:59"로 설정
        LocalDateTime yesterday = LocalDate.now().minusDays(1).atTime(23, 59);
        CompletedDateTime completedDateTime = CompletedDateTime.builder()
                .todoId(findTodo.getId())
                .dateTime(yesterday)
                .build();
        completedDateTimeRepository.save(completedDateTime);

        // 반복 할 일이라면, 오늘 날짜로 지정하여 백로그에 추가
        if (findTodo.isRepeat() || findTodo.isRoutine()) {
            findTodo.changeToBacklog(existBacklogOrder);
        }
    }

    /**
     * 히스토리 데이터를 조회합니다.
     *
     * @param userId 사용자 ID
     * @param localDate 조회할 날짜
     * @param page 페이지 번호
     * @param size 페이지 크기
     * @return 히스토리 데이터
     */
    @Transactional(readOnly = true)
    public PaginatedHistoryResponseDto getHistories(Long userId, LocalDate localDate, int page, int size) {
        userValidator.checkIsExistUser(userId);
        if (localDate.isBefore(LocalDate.now())) {
            Page<Todo> historiesPage = todoRepository.findHistories(userId, localDate, PageRequest.of(page, size));
            return PaginatedHistoryResponseDto.of(historiesPage, true);
        } else if (localDate.isEqual(LocalDate.now())) {
            Page<Todo> historiesPage = getTodayTodos(userId, page, size);
            return PaginatedHistoryResponseDto.from(historiesPage);
        }
		// 미래 날짜: 마감 백로그 + 요일 반복(루틴) 백로그를 합쳐서 페이징 처리
		Page<Todo> historiesPage = getFuturePlanTodosIncludingRoutines(userId, localDate, page, size);
		return PaginatedHistoryResponseDto.of(historiesPage, false);
	}

	/**
	 * 미래 날짜의 히스토리 조회를 위해,
	 * - 해당 날짜가 마감인 BACKLOG
	 * - 해당 날짜의 요일이 매칭되는 루틴 BACKLOG
	 * 을 합친 뒤 페이징하여 반환합니다.
	 */
	private Page<Todo> getFuturePlanTodosIncludingRoutines(Long userId, LocalDate targetDate, int page, int size) {
		// 1) 해당 날짜 마감 백로그 수집
		List<Todo> deadlineMatchedTodos = todoRepository.findTodosByDeadLine(userId, targetDate);

		// 2) 해당 날짜 요일에 매칭되는 루틴 백로그 수집
		String dayName = targetDate.getDayOfWeek().getDisplayName(TextStyle.SHORT, Locale.KOREAN);
		List<Todo> routineMatchedTodos = todoRepository.findRoutineTodosByDay(userId, dayName);

		// 3) 합치고 중복 제거 (같은 todo가 양쪽 쿼리에 걸칠 여지를 방지)
		Map<Long, Todo> merged = new LinkedHashMap<>();
		for (Todo t : deadlineMatchedTodos) {
			merged.put(t.getId(), t);
		}
		for (Todo t : routineMatchedTodos) {
			merged.putIfAbsent(t.getId(), t);
		}
		List<Todo> combined = new ArrayList<>(merged.values());

		// 4) 필요 시 정렬 기준을 부여
		// 현재는 id 순 정렬 -> TODO. 추후 time이 노출된다면 time을 기준으로 오름차순 정렬
		combined.sort(Comparator.comparing(Todo::getId));

		// 5) 수동 페이징
		PageRequest pageRequest = PageRequest.of(page, size);
		int start = (int) pageRequest.getOffset();
		int end = Math.min(start + pageRequest.getPageSize(), combined.size());
		List<Todo> pageContent = (start >= combined.size()) ? Collections.emptyList() : combined.subList(start, end);

		return new PageImpl<>(pageContent, pageRequest, combined.size());
	}

	/**
     * 오늘(TODAY)의 할 일 목록을 조회합니다.
     * 오늘 날짜 기준으로 다음의 할 일들을 조회하여 반환합니다:
     * - 미완료 상태(INCOMPLETE)의 할 일
     * - 완료 상태(COMPLETED)의 할 일
     * 두 목록을 하나로 합친 후, 요청된 페이지 기준으로 페이징 처리합니다.
     *
     * @param userId 사용자 ID
     * @param page 요청 페이지 번호 (0부터 시작)
     * @param size 페이지당 항목 수
     * @return 오늘 할 일 목록의 Page 객체
     */
    private Page<Todo> getTodayTodos(Long userId, int page, int size) {
        List<Todo> todayTodos = new ArrayList<>();
        List<Todo> incompleteTodos = todoRepository.findIncompleteTodays(userId, Type.TODAY, LocalDate.now(), TodayStatus.INCOMPLETE);
        List<Todo> completedTodos = todoRepository.findCompletedTodays(userId, LocalDate.now());
        todayTodos.addAll(incompleteTodos);
        todayTodos.addAll(completedTodos);

        PageRequest pageRequest = PageRequest.of(page, size);
        int start = (int) pageRequest.getOffset();
        int end = Math.min((start + pageRequest.getPageSize()), todayTodos.size());
        return new PageImpl<>(todayTodos.subList(start, end), pageRequest, todayTodos.size());
    }

    /**
     * 히스토리 캘린더 데이터를 조회합니다 (v1 - Legacy).
     * 사용자에게 히스토리가 존재하는 날짜 리스트만 반환합니다.
     * - 앱 버전이 1.2.0 미만일 때 호출됩니다.
     *
     * @param userId 사용자 ID
     * @param year 조회할 연도
     * @param month 조회할 월
     * @return 할 일이 존재하는 날짜 리스트
     */
    @Transactional(readOnly = true)
    public List<LocalDate> getLegacyHistoriesCalendar(Long userId, String year, int month) {
        return completedDateTimeRepository.findHistoryExistingDates(userId, year, month).stream()
                .map(LocalDateTime::toLocalDate)
                .distinct()
                .toList();
    }

	/**
	 * 히스토리 캘린더 데이터를 조회합니다 (v2 - New).
	 *
	 * 날짜별 값 산정 규칙:
	 * - 완료 히스토리(CompletedDateTime)가 존재하는 경우: 무조건 -1을 사용합니다. (백로그/루틴 합계가 있어도 -1 우선)
	 * - 완료 히스토리가 없고, 백로그 마감(backlog) 개수와 요일 반복(루틴) 개수의 합이 1 이상인 경우: 합계를 사용합니다.
	 * - 완료 히스토리도 없고, 합계가 0인 경우: 해당 날짜는 결과에서 제외됩니다.
	 *
	 * @param userId 사용자 ID
	 * @param year 조회할 연도 (예: "2025")
	 * @param month 조회할 월 (1~12)
	 * @return 날짜별 히스토리/백로그/루틴 정보 DTO
	 */
	@Transactional(readOnly = true)
	public HistoryCalendarListResponseDto getHistoriesCalendar(Long userId, String year, int month) {
		// 1) 백로그 일자별 카운트
		Map<LocalDate, Integer> backlogCounts = loadBacklogCountMap(userId, year, month);

		// 2) 요일별 루틴 카운트
		Map<String, Integer> routineCountByDay = loadRoutineCountByDay(userId);

		// 3) 해당 월의 각 날짜에 대해 (백로그 + 루틴) 합계를 계산
		Map<LocalDate, Integer> resultByDate = fillCountsFromBacklogAndRoutine(backlogCounts, routineCountByDay, year, month);

		// 4) 히스토리 날짜는 합계가 있어도 -1로 덮어씁니다.
		Set<LocalDate> historyDates = loadHistoryDateSet(userId, year, month);
		markHistoryDatesWithMinusOne(resultByDate, historyDates);

		return HistoryCalendarListResponseDto.from(resultByDate);
	}

	private Map<LocalDate, Integer> loadBacklogCountMap(Long userId, String year, int month) {
		return todoRepository.findDatesWithBacklogCount(userId, year, month).stream()
			.collect(Collectors.toMap(
				t -> ((Date) t.get("date")).toLocalDate(),
				t -> ((Number) t.get("count")).intValue()
			));
	}

	private Map<String, Integer> loadRoutineCountByDay(Long userId) {
		List<RoutineCountProjection> projections = routineRepository.countRoutinesByDay(userId);

		return projections.stream()
			.collect(Collectors.toMap(
				RoutineCountProjection::getDay,
				p -> p.getCount().intValue()
			));
	}

	private Map<LocalDate, Integer> fillCountsFromBacklogAndRoutine(
		Map<LocalDate, Integer> backlogCounts,
		Map<String, Integer> routineCountByDay,
		String year, int month
	) {

		Map<LocalDate, Integer> result = new HashMap<>();
		LocalDate firstDay = LocalDate.of(Integer.parseInt(year), month, 1);
		LocalDate lastDay = firstDay.withDayOfMonth(firstDay.lengthOfMonth());

		for (LocalDate d = LocalDate.now().plusDays(1); !d.isAfter(lastDay); d = d.plusDays(1)) {
			int backlog = backlogCounts.getOrDefault(d, 0);
			int routine = routineCountByDay.getOrDefault(
				d.getDayOfWeek().getDisplayName(TextStyle.SHORT, Locale.KOREAN), 0
			);
			int sum = backlog + routine;
			if (sum > 0) {
				result.put(d, sum);
			}
		}
		return result;
	}

	private Set<LocalDate> loadHistoryDateSet(Long userId, String year, int month) {
		return completedDateTimeRepository.findHistoryExistingDates(userId, year, month).stream()
			.map(LocalDateTime::toLocalDate)
			.collect(Collectors.toCollection(LinkedHashSet::new));
	}

	private void markHistoryDatesWithMinusOne(Map<LocalDate, Integer> resultByDate, Set<LocalDate> historyDates) {
		for (LocalDate date : historyDates) {
			// 완료 히스토리가 존재하는 날짜는 합계가 있어도 -1을 우선합니다.
			resultByDate.put(date, -1);
		}
	}

	/**
     * 특정 할 일의 카테고리를 업데이트합니다.
     *
     * @param userId 사용자 ID
     * @param todoId 업데이트할 할 일 ID
     * @param requestDto 카테고리 업데이트 요청 데이터
     */
    @Transactional
    public void updateCategory(Long userId, Long todoId, TodoCategoryUpdateRequestDto requestDto) {
        userValidator.checkIsExistUser(userId);
        Todo findTodo = validateAndReturnTodo(userId, todoId);
        if (requestDto.categoryId() != null) {
            categoryValidator.validateCategory(userId, requestDto.categoryId());
        }
        findTodo.updateCategory(requestDto.categoryId());
    }

    /**
     * 특정 할 일의 반복 설정을 업데이트합니다. (~v1.2.x)
     * True <-> False
     *
     * @param userId 사용자 ID
     * @param todoId 업데이트할 할 일 ID
     */
    @Transactional
    public void updateIsRepeat(Long userId, Long todoId) {
        userValidator.checkIsExistUser(userId);
        Todo findTodo = validateAndReturnTodo(userId, todoId);
        findTodo.toggleRepeat();
    }

    /**
     * 특정 할 일의 일반 반복 설정을 등록합니다. (v1.3.0~)
     *
     * @param userId 사용자 ID
     * @param todoId 일반 반복 설정할 할 일 ID
     */
    @Transactional
    public void createIsRepeat(Long userId, Long todoId) {
        userValidator.checkIsExistUser(userId);
        Todo findTodo = validateAndReturnTodo(userId, todoId);
        findTodo.setRepeat(true);
        findTodo.setRoutine(false);
        routineRepository.deleteByTodoId(todoId);
    }

    /**
     * 특정 할 일의 일반 반복 설정을 삭제합니다. (v1.3.0~)
     *
     * @param userId 사용자 ID
     * @param todoId 일반 반복 설정을 삭제할 할 일 ID
     */
    @Transactional
    public void deleteIsRepeat(Long userId, Long todoId) {
        userValidator.checkIsExistUser(userId);
        Todo findTodo = validateAndReturnTodo(userId, todoId);
        findTodo.setRepeat(false);
    }

    /**
     * 마감기한 또는 요일 반복이 설정된 할 일 -> 오늘(TODAY)로 변경합니다.
     * 1. 오늘 날짜 == 마감 기한과 일치하는 할 일들을 찾아 TODAY 상태로 업데이트합니다.
     * 2. 오늘 요일 == 요일 반복 설정과 일치하는 할 일들을 찾아 TODAY 상태로 업데이트합니다.
     * - 기본적인 todayOrder 값을 설정하여 정렬 순서를 유지합니다.
     *
     * @param today 오늘 날짜
     * @param userIds 업데이트할 사용자 ID 목록
     */
    @Transactional
    public void processUpdateDeadlineTodos(LocalDate today, List<Long> userIds) {
        String todayDay = today.getDayOfWeek().getDisplayName(TextStyle.SHORT, Locale.KOREAN);

        for (Long userId : userIds) {
            int todayOrder = todoRepository.findMaxTodayOrderByUserIdOrZero(userId);

            // 1. 마감 기한이 오늘인 BACKLOG -> TODAY
            List<Todo> deadlineMatchedTodos = todoRepository.findTodosByDeadLine(userId, today);

            for (Todo todo : deadlineMatchedTodos) {
                todo.changeToToday(todayOrder++);
            }

            // 2. 오늘 요일이 포함된 요일 반복 설정된 BACKLOG -> TODAY
            List<Todo> routineMatchedTodos = todoRepository.findRoutineTodosByDay(userId, todayDay);

            for (Todo todo : routineMatchedTodos) {
                todo.changeToToday(todayOrder++);
            }
        }
    }
}
