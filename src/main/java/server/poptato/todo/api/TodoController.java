package server.poptato.todo.api;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;
import server.poptato.auth.application.service.JwtService;
import server.poptato.global.response.ApiResponse;
import server.poptato.global.response.status.SuccessStatus;
import server.poptato.todo.api.request.*;
import server.poptato.todo.application.TodoService;
import server.poptato.todo.application.response.HistoryCalendarListResponseDto;
import server.poptato.todo.application.response.LegacyHistoryCalendarResponseDto;
import server.poptato.todo.application.response.PaginatedHistoryResponseDto;
import server.poptato.todo.application.response.TodoDetailResponseDto;
import server.poptato.todo.domain.value.AppVersion;
import server.poptato.user.domain.value.MobileType;

import java.time.LocalDate;
import java.util.List;

@RestController
@RequiredArgsConstructor
@Validated
public class TodoController {

    private final TodoService todoService;
    private final JwtService jwtService;

    /**
     * 할 일 삭제 API.
     * 사용자가 특정 할 일을 삭제합니다.
     *
     * @param authorizationHeader 요청 헤더의 Authorization (Bearer 토큰)
     * @param todoId 삭제할 할 일 ID
     * @return 성공 여부를 나타내는 응답
     */
    @DeleteMapping("/todo/{todoId}")
    public ResponseEntity<ApiResponse<SuccessStatus>> deleteTodo(
            @RequestHeader("Authorization") String authorizationHeader,
            @PathVariable Long todoId
    ) {
        todoService.deleteTodoById(jwtService.extractUserIdFromToken(authorizationHeader), todoId);
        return ApiResponse.onSuccess(SuccessStatus._OK);
    }

    /**
     * 할 일 상태 스와이프 API.
     * 사용자가 할 일 상태를 스와이프 방식으로 업데이트합니다.
     *
     * @param authorizationHeader 요청 헤더의 Authorization (Bearer 토큰)
     * @param swipeRequestDto 스와이프 요청 데이터
     * @return 성공 여부를 나타내는 응답
     */
    @PatchMapping("/swipe")
    public ResponseEntity<ApiResponse<SuccessStatus>> swipe(
            @RequestHeader("Authorization") String authorizationHeader,
            @Valid @RequestBody SwipeRequestDto swipeRequestDto
    ) {
        todoService.swipe(jwtService.extractUserIdFromToken(authorizationHeader), swipeRequestDto);
        return ApiResponse.onSuccess(SuccessStatus._OK);
    }

    /**
     * 할 일 즐겨찾기 상태 토글 API.
     * 사용자가 특정 할 일의 즐겨찾기 상태를 토글합니다.
     *
     * @param authorizationHeader 요청 헤더의 Authorization (Bearer 토큰)
     * @param todoId 대상 할 일 ID
     * @return 성공 여부를 나타내는 응답
     */
    @PatchMapping("/todo/{todoId}/bookmark")
    public ResponseEntity<ApiResponse<SuccessStatus>> toggleIsBookmark(
            @RequestHeader("Authorization") String authorizationHeader,
            @PathVariable Long todoId
    ) {
        todoService.toggleIsBookmark(jwtService.extractUserIdFromToken(authorizationHeader), todoId);
        return ApiResponse.onSuccess(SuccessStatus._OK);
    }

    /**
     * 할 일 드래그 앤 드롭 API.
     * 사용자가 드래그 앤 드롭 방식으로 할 일 순서를 변경합니다.
     *
     * @param authorizationHeader 요청 헤더의 Authorization (Bearer 토큰)
     * @param todoDragAndDropRequestDto 순서 변경 요청 데이터
     * @return 성공 여부를 나타내는 응답
     */
    @PatchMapping("/todo/dragAndDrop")
    public ResponseEntity<ApiResponse<SuccessStatus>> dragAndDrop(
            @RequestHeader("Authorization") String authorizationHeader,
            @Valid @RequestBody TodoDragAndDropRequestDto todoDragAndDropRequestDto
    ) {
        todoService.dragAndDrop(jwtService.extractUserIdFromToken(authorizationHeader), todoDragAndDropRequestDto);
        return ApiResponse.onSuccess(SuccessStatus._OK);
    }

    /**
     * 할 일 상세 조회 API.
     * 특정 할 일의 세부 정보를 조회합니다.
     *
     * @param authorizationHeader 요청 헤더의 Authorization (Bearer 토큰)
     * @param mobileType 클라이언트의 모바일 타입
     * @param todoId 조회할 할 일 ID
     * @return 할 일 상세 정보
     */
    @GetMapping("/todo/{todoId}")
    public ResponseEntity<ApiResponse<TodoDetailResponseDto>> getTodoInfo(
            @RequestHeader("Authorization") String authorizationHeader,
            @RequestHeader(value = "X-Mobile-Type", required = false, defaultValue = "ANDROID") MobileType mobileType,
            @PathVariable Long todoId
    ) {
        TodoDetailResponseDto response = todoService.getTodoInfo(jwtService.extractUserIdFromToken(authorizationHeader), mobileType, todoId);
        return ApiResponse.onSuccess(SuccessStatus._OK, response);
    }

    /**
     * 할 일 시간 업데이트 API.
     * 사용자가 특정 할 일의 시간을 업데이트합니다.
     *
     * @param authorizationHeader 요청 헤더의 Authorization (Bearer 토큰)
     * @param todoId 업데이트할 할 일 ID
     * @param timeUpdateRequestDto 시간 업데이트 요청 데이터
     * @return 성공 여부를 나타내는 응답
     */
    @PatchMapping("/todo/{todoId}/time")
    public ResponseEntity<ApiResponse<SuccessStatus>> updateTime(
            @RequestHeader("Authorization") String authorizationHeader,
            @PathVariable Long todoId,
            @Valid @RequestBody TimeUpdateRequestDto timeUpdateRequestDto
    ) {
        todoService.updateTime(jwtService.extractUserIdFromToken(authorizationHeader), todoId, timeUpdateRequestDto);
        return ApiResponse.onSuccess(SuccessStatus._OK);
    }

    /**
     * 할 일 기한 업데이트 API.
     * 사용자가 특정 할 일의 마감 기한을 업데이트합니다.
     *
     * @param authorizationHeader 요청 헤더의 Authorization (Bearer 토큰)
     * @param todoId 업데이트할 할 일 ID
     * @param deadlineUpdateRequestDto 마감 기한 업데이트 요청 데이터
     * @return 성공 여부를 나타내는 응답
     */
    @PatchMapping("/todo/{todoId}/deadline")
    public ResponseEntity<ApiResponse<SuccessStatus>> updateDeadline(
            @RequestHeader("Authorization") String authorizationHeader,
            @PathVariable Long todoId,
            @Valid @RequestBody DeadlineUpdateRequestDto deadlineUpdateRequestDto
    ) {
        todoService.updateDeadline(jwtService.extractUserIdFromToken(authorizationHeader), todoId, deadlineUpdateRequestDto);
        return ApiResponse.onSuccess(SuccessStatus._OK);
    }

    /**
     * 할 일 루틴 요일 등록 API. (v1.3.0~)
     * 사용자가 특정 할 일에 반복할 요일을 등록합니다.
     * 요청된 요일 리스트로 기존 루틴을 대체합니다.
     *
     * @param authorizationHeader 요청 헤더의 Authorization (Bearer 토큰)
     * @param todoId 루틴을 등록할 할 일 ID
     * @param routineUpdateRequestDto 루틴 요일 등록 요청 데이터
     * @return 성공 여부를 나타내는 응답
     */
    @PutMapping("/todo/{todoId}/routine")
    public ResponseEntity<ApiResponse<SuccessStatus>> createRoutine(
            @RequestHeader("Authorization") String authorizationHeader,
            @PathVariable Long todoId,
            @Valid @RequestBody RoutineUpdateRequestDto routineUpdateRequestDto
    ) {
        todoService.createRoutine(jwtService.extractUserIdFromToken(authorizationHeader), todoId, routineUpdateRequestDto);
        return ApiResponse.onSuccess(SuccessStatus._CREATED);
    }

    /**
     * 할 일 루틴 삭제 API. (v1.3.0~)
     * 사용자가 특정 할 일의 루틴을 삭제합니다.
     *
     * @param authorizationHeader 요청 헤더의 Authorization (Bearer 토큰)
     * @param todoId 루틴을 삭제할 할 일 ID
     * @return 성공 여부를 나타내는 응답
     */
    @DeleteMapping("/todo/{todoId}/routine")
    public ResponseEntity<ApiResponse<SuccessStatus>> deleteRoutine(
            @RequestHeader("Authorization") String authorizationHeader,
            @PathVariable Long todoId
    ) {
        todoService.deleteRoutine(jwtService.extractUserIdFromToken(authorizationHeader), todoId);
        return ApiResponse.onSuccess(SuccessStatus._OK);
    }

    /**
     * 할 일 내용 수정 API.
     * 사용자가 특정 할 일의 내용을 수정합니다.
     *
     * @param authorizationHeader 요청 헤더의 Authorization (Bearer 토큰)
     * @param todoId 수정할 할 일 ID
     * @param contentUpdateRequestDto 내용 수정 요청 데이터
     * @return 성공 여부를 나타내는 응답
     */
    @PatchMapping("/todo/{todoId}/content")
    public ResponseEntity<ApiResponse<SuccessStatus>> updateContent(
            @RequestHeader("Authorization") String authorizationHeader,
            @PathVariable Long todoId,
            @Valid @RequestBody ContentUpdateRequestDto contentUpdateRequestDto
    ) {
        todoService.updateContent(jwtService.extractUserIdFromToken(authorizationHeader), todoId, contentUpdateRequestDto);
        return ApiResponse.onSuccess(SuccessStatus._OK);
    }

    /**
     * 할 일 완료 상태 업데이트 API.
     * 사용자가 특정 할 일의 완료 상태를 업데이트합니다.
     *
     * @param authorizationHeader 요청 헤더의 Authorization (Bearer 토큰)
     * @param todoId 업데이트할 할 일 ID
     * @return 성공 여부를 나타내는 응답
     */
    @PatchMapping("/todo/{todoId}/achieve")
    public ResponseEntity<ApiResponse<SuccessStatus>> updateIsCompleted(
            @RequestHeader("Authorization") String authorizationHeader,
            @PathVariable Long todoId
    ) {
        todoService.updateIsCompleted(jwtService.extractUserIdFromToken(authorizationHeader), todoId);
        return ApiResponse.onSuccess(SuccessStatus._OK);
    }

    /**
     * 어제 한 일 체크 API.
     * 사용자의 어제 한 일 중에서 완료된 항목을 체크하고, 미완료 항목을 백로그로 이동시킵니다.
     *
     * @param authorizationHeader 요청 헤더의 Authorization (Bearer 토큰)
     * @param request 미완료 -> 완료로 변경된 todoId 리스트
     * @return 성공 여부 응답
     */
    @PostMapping("/todo/check/yesterdays")
    public ResponseEntity<ApiResponse<SuccessStatus>> checkYesterdayTodos(
            @RequestHeader("Authorization") String authorizationHeader,
            @Valid @RequestBody CheckYesterdayTodosRequestDto request
    ) {
        todoService.checkYesterdayTodos(jwtService.extractUserIdFromToken(authorizationHeader), request);
        return ApiResponse.onSuccess(SuccessStatus._OK);
    }

    /**
     * 할 일 카테고리 변경 API.
     * 사용자가 특정 할 일의 카테고리를 변경합니다.
     *
     * @param authorizationHeader 요청 헤더의 Authorization (Bearer 토큰)
     * @param todoId 변경할 할 일 ID
     * @param todoCategoryUpdateRequestDto 카테고리 변경 요청 데이터
     * @return 성공 여부를 나타내는 응답
     */
    @PatchMapping("/todo/{todoId}/category")
    public ResponseEntity<ApiResponse<SuccessStatus>> updateCategory(
            @RequestHeader("Authorization") String authorizationHeader,
            @PathVariable Long todoId,
            @Valid @RequestBody TodoCategoryUpdateRequestDto todoCategoryUpdateRequestDto
    ) {
        todoService.updateCategory(jwtService.extractUserIdFromToken(authorizationHeader), todoId, todoCategoryUpdateRequestDto);
        return ApiResponse.onSuccess(SuccessStatus._OK);
    }

    /**
     * 반복 설정 업데이트 API. (~v1.2.x)
     * 사용자가 특정 할 일의 반복 설정을 업데이트합니다.
     *
     * @param authorizationHeader 요청 헤더의 Authorization (Bearer 토큰)
     * @param todoId 업데이트할 할 일 ID
     * @return 성공 여부를 나타내는 응답
     */
    @PatchMapping("/todo/{todoId}/repeat")
    public ResponseEntity<ApiResponse<SuccessStatus>> updateIsRepeat(
            @RequestHeader("Authorization") String authorizationHeader,
            @PathVariable Long todoId
    ) {
        todoService.updateIsRepeat(jwtService.extractUserIdFromToken(authorizationHeader), todoId);
        return ApiResponse.onSuccess(SuccessStatus._OK);
    }

    /**
     * 일반 반복 설정 등록 API. (v1.3.0~)
     * 특정 할 일의 일반 반복 설정을 등록합니다.
     *
     * @param authorizationHeader 요청 헤더의 Authorization (Bearer 토큰)
     * @param todoId 일반 반복 설정할 할 일 ID
     * @return 성공 여부를 나타내는 응답
     */
    @PostMapping("/todo/{todoId}/repeat")
    public ResponseEntity<ApiResponse<SuccessStatus>> createIsRepeat(
            @RequestHeader("Authorization") String authorizationHeader,
            @PathVariable Long todoId
    ) {
        todoService.createIsRepeat(jwtService.extractUserIdFromToken(authorizationHeader), todoId);
        return ApiResponse.onSuccess(SuccessStatus._OK);
    }

    /**
     * 일반 반복 설정 삭제 API. (v1.3.0~)
     * 특정 할 일의 일반 반복 설정을 삭제합니다.
     *
     * @param authorizationHeader 요청 헤더의 Authorization (Bearer 토큰)
     * @param todoId 일반 반복 설정을 삭제할 할 일 ID
     * @return 성공 여부를 나타내는 응답
     */
    @DeleteMapping("/todo/{todoId}/repeat")
    public ResponseEntity<ApiResponse<SuccessStatus>> deleteIsRepeat(
            @RequestHeader("Authorization") String authorizationHeader,
            @PathVariable Long todoId
    ) {
        todoService.deleteIsRepeat(jwtService.extractUserIdFromToken(authorizationHeader), todoId);
        return ApiResponse.onSuccess(SuccessStatus._OK);
    }

    /**
     * 히스토리 조회 API.
     * 사용자가 특정 날짜의 할 일 히스토리를 조회합니다.
     *
     * @param authorizationHeader 요청 헤더의 Authorization (Bearer 토큰)
     * @param page 요청 페이지 번호 (기본값: 0)
     * @param size 한 페이지당 항목 수 (기본값: 15)
     * @param date 조회할 날짜
     * @return 히스토리 목록
     */
    @GetMapping("/histories")
    public ResponseEntity<ApiResponse<PaginatedHistoryResponseDto>> getHistories(
            @RequestHeader("Authorization") String authorizationHeader,
            @RequestParam(value = "page", defaultValue = "0") int page,
            @RequestParam(value = "size", defaultValue = "15") int size,
            @RequestParam LocalDate date
    ) {
        PaginatedHistoryResponseDto response = todoService.getHistories(jwtService.extractUserIdFromToken(authorizationHeader), date, page, size);
        return ApiResponse.onSuccess(SuccessStatus._OK, response);
    }

    /**
     * 히스토리 캘린더 조회 API.
     * 사용자가 특정 연도 및 월의 할 일 히스토리를 조회합니다.
     * - 앱 버전이 V2 미만일 경우, 날짜 리스트를 감싼 응답 형식(`LegacyHistoryCalendarResponseDto`)으로 반환됩니다.
     * - 앱 버전이 V2 이상일 경우, 날짜별 히스토리 및 백로그 개수를 포함한 응답 형식(`HistoryCalendarListResponseDto`)으로 반환됩니다.
     *
     * @param authorizationHeader 요청 헤더의 Authorization (Bearer 토큰)
     * @param appVersion 요청 헤더의 앱 버전 (예: V1, V2)
     * @param year 조회할 연도
     * @param month 조회할 월
     * @return 히스토리 캘린더 응답 (버전에 따라 서로 다른 DTO 반환)
     */
    @GetMapping("/calendar")
    public ResponseEntity<ApiResponse<Object>> getHistoryCalendarDateList(
            @RequestHeader("Authorization") String authorizationHeader,
            @RequestHeader(value = "X-App-Version", required = false, defaultValue = "V1") AppVersion appVersion,
            @RequestParam String year,
            @RequestParam int month
    ) {
        Long userId = jwtService.extractUserIdFromToken(authorizationHeader);

        if (appVersion.isLegacy()) {
            List<LocalDate> dates = todoService.getLegacyHistoriesCalendar(userId, year, month);
            return ApiResponse.onSuccess(SuccessStatus._OK, LegacyHistoryCalendarResponseDto.of(dates));
        }

        HistoryCalendarListResponseDto response = todoService.getHistoriesCalendar(userId, year, month);
        return ApiResponse.onSuccess(SuccessStatus._OK, response);
    }
}
