package server.poptato.todo.application;

import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import server.poptato.category.domain.entity.Category;
import server.poptato.category.domain.repository.CategoryRepository;
import server.poptato.category.validator.CategoryValidator;
import server.poptato.todo.api.request.BacklogCreateRequestDto;
import server.poptato.todo.application.response.BacklogCreateResponseDto;
import server.poptato.todo.application.response.BacklogListResponseDto;
import server.poptato.todo.application.response.BacklogResponseDto;
import server.poptato.todo.application.response.PaginatedYesterdayResponseDto;
import server.poptato.todo.domain.entity.Routine;
import server.poptato.todo.domain.entity.Todo;
import server.poptato.todo.domain.repository.RoutineRepository;
import server.poptato.todo.domain.repository.TodoRepository;
import server.poptato.todo.domain.value.BacklogCategoryType;
import server.poptato.todo.domain.value.TodayStatus;
import server.poptato.todo.domain.value.Type;
import server.poptato.user.domain.value.MobileType;
import server.poptato.user.validator.UserValidator;

import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;

@Transactional
@RequiredArgsConstructor
@Service
public class TodoBacklogService {
    private final TodoRepository todoRepository;
    private final CategoryRepository categoryRepository;
    private final RoutineRepository routineRepository;
    private final UserValidator userValidator;
    private final CategoryValidator categoryValidator;
    private static final Long ALL_CATEGORY = -1L;
    private static final Long BOOKMARK_CATEGORY = 0L;

    /**
     * 백로그 목록 조회 메서드.
     * 사용자 ID와 카테고리 ID를 기반으로 페이지네이션된 백로그 목록을 반환합니다.
     *
     * @param userId 사용자 ID
     * @param categoryId 카테고리 ID
     * @param page 페이지 번호
     * @param size 페이지 크기
     * @return 백로그 목록과 페이징 정보
     */
    public BacklogListResponseDto getBacklogList(Long userId, Long categoryId, MobileType mobileType, int page, int size) {
        userValidator.checkIsExistUser(userId);
        // 1. Enum을 통해 카테고리 타입 결정
        BacklogCategoryType backlogCategoryType = BacklogCategoryType.from(categoryId);
        // 2. 카테고리 이름 조회 및 일반 카테고리 검증
        String categoryName = getCategoryNameAndValidateIfNormal(backlogCategoryType, userId, categoryId);
        // 3. 백로그 조회
        PageRequest pageRequest = PageRequest.of(page, size);
        Page<Todo> backlogs = backlogCategoryType.getBacklogs(todoRepository, userId, categoryId, pageRequest);
        // 4. Routine 조회 및 매핑
        Map<Long, List<String>> routineMap = getRoutineMap(backlogs.getContent());

        List<BacklogResponseDto> backlogDtos = backlogs.stream()
                .map(todo -> BacklogResponseDto.of(
                        todo,
                        mobileType,
                        routineMap.getOrDefault(todo.getId(), Collections.emptyList())
                ))
                .toList();

        return BacklogListResponseDto.of(
                categoryName,
                backlogDtos,
                backlogs.getTotalElements(),
                backlogs.getTotalPages()
        );
    }

    /**
     * 백로그 생성 메서드.
     * 사용자 ID와 요청 데이터를 기반으로 새로운 백로그를 생성합니다.
     *
     * @param userId 사용자 ID
     * @param backlogCreateRequestDto 백로그 생성 요청 데이터
     * @return 생성된 백로그의 정보
     */
    public BacklogCreateResponseDto createBacklog(Long userId, BacklogCreateRequestDto backlogCreateRequestDto) {
        userValidator.checkIsExistUser(userId);
        categoryValidator.validateCategory(userId, backlogCreateRequestDto.categoryId());
        Integer maxBacklogOrder = todoRepository.findMaxBacklogOrderByUserIdOrZero(userId);
        Todo newBacklog = createNewBacklog(userId, backlogCreateRequestDto, maxBacklogOrder);
        return BacklogCreateResponseDto.from(newBacklog);
    }

    /**
     * 어제 할 일 목록 조회 메서드.
     * 사용자 ID를 기반으로 어제 완료되지 않은 할 일 목록을 페이지네이션된 형식으로 반환합니다.
     *
     * @param userId 사용자 ID
     * @param page 페이지 번호
     * @param size 페이지 크기
     * @return 어제 할 일 목록과 페이징 정보
     */
    public PaginatedYesterdayResponseDto getYesterdays(Long userId, int page, int size) {
        userValidator.checkIsExistUser(userId);

        Pageable pageable = PageRequest.of(page, size);
        Page<Todo> yesterdaysPage = todoRepository.findByUserIdAndTypeAndTodayStatus(userId, Type.YESTERDAY, TodayStatus.INCOMPLETE, pageable);

        return PaginatedYesterdayResponseDto.of(yesterdaysPage);
    }

    /**
     * 어제 백로그 생성 메서드.
     * 사용자 ID와 요청 데이터를 기반으로 어제 백로그 항목을 생성합니다.
     *
     * @param userId 사용자 ID
     * @param backlogCreateRequestDto 어제 백로그 생성 요청 데이터
     * @return 생성된 어제 백로그 항목
     */
    public BacklogCreateResponseDto createYesterdayBacklog(Long userId, BacklogCreateRequestDto backlogCreateRequestDto) {
        userValidator.checkIsExistUser(userId);
        categoryValidator.validateCategory(userId, backlogCreateRequestDto.categoryId());
        Integer maxBacklogOrder = todoRepository.findMaxBacklogOrderByUserIdOrZero(userId);
        Todo newYesterdayBacklog = Todo.createYesterdayBacklog(userId, backlogCreateRequestDto.content(), maxBacklogOrder);
        todoRepository.save(newYesterdayBacklog);
        return BacklogCreateResponseDto.from(newYesterdayBacklog);
    }

    /**
     * 카테고리 이름을 반환하되, 일반 카테고리라면 소유권 검증도 함께 수행한다
     *
     * @param type 백로그 카테고리 타입
     * @param categoryId 카테고리 ID
     * @return 카테고리명
     */
    private String getCategoryNameAndValidateIfNormal(BacklogCategoryType type, Long userId, Long categoryId) {
        if (type.getDefaultName() != null) {
            return type.getDefaultName();
        }

        Category category = categoryValidator.validateAndReturnCategory(userId, categoryId);

        return category.getName();
    }

    /**
     * Todo 리스트에서 ID를 추출하여 Routine을 일괄 조회하고 Map으로 그룹화
     *
     * @param todos 할 일 목록
     * @return 할 일 별로 Routine을 일괄 조회하여 그룹화 한 Map
     */
    private Map<Long, List<String>> getRoutineMap(List<Todo> todos) {
        if (todos.isEmpty()) {
            return Collections.emptyMap();
        }

        List<Long> todoIds = todos.stream()
                .map(Todo::getId)
                .toList();

        return routineRepository.findAllByTodoIdIn(todoIds).stream()
                .collect(Collectors.groupingBy(
                        Routine::getTodoId,
                        Collectors.mapping(Routine::getDay, Collectors.toList())
                ));
    }

    /**
     * 새로운 백로그 생성 메서드.
     * 요청 데이터를 기반으로 백로그를 생성하고 저장합니다.
     *
     * @param userId 사용자 ID
     * @param backlogCreateRequestDto 백로그 생성 요청 데이터
     * @param maxBacklogOrder 현재 백로그 최대 순서 값
     * @return 생성된 백로그 엔티티
     */
    private Todo createNewBacklog(Long userId, BacklogCreateRequestDto backlogCreateRequestDto, Integer maxBacklogOrder) {
        Todo backlog = null;
        Long categoryId = backlogCreateRequestDto.categoryId();
        if (Objects.equals(categoryId, ALL_CATEGORY)) {
            backlog = Todo.createBacklog(userId, backlogCreateRequestDto.content(), maxBacklogOrder + 1);
        } else if (Objects.equals(categoryId, BOOKMARK_CATEGORY)) {
            backlog = Todo.createBookmarkBacklog(userId, backlogCreateRequestDto.content(), maxBacklogOrder + 1);
        } else if (categoryId > BOOKMARK_CATEGORY) {
            backlog = Todo.createCategoryBacklog(userId, categoryId, backlogCreateRequestDto.content(), maxBacklogOrder + 1);
        }
        return todoRepository.save(backlog);
    }
}
