package server.poptato.todo.api;

import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;
import server.poptato.auth.application.service.JwtService;
import server.poptato.global.response.ApiResponse;
import server.poptato.global.response.status.SuccessStatus;
import server.poptato.todo.api.request.EventCreateRequestDto;
import server.poptato.todo.application.TodoTodayService;
import server.poptato.todo.application.response.TodayListResponseDto;
import server.poptato.user.domain.value.MobileType;

import java.time.LocalDate;

@RestController
@RequiredArgsConstructor
public class TodoTodayController {

    private final TodoTodayService todoTodayService;
    private final JwtService jwtService;

    /**
     * 오늘의 할 일 조회 API.
     *
     * 사용자가 오늘 해야 할 일을 조회합니다. 요청 파라미터로 페이지 번호와 크기를 전달받아
     * 페이징된 데이터로 할 일 목록을 반환합니다.
     *
     * @param authorizationHeader 요청 헤더의 Authorization (Bearer 토큰)
     * @param mobileType 클라이언트의 모바일 타입
     * @param page 요청 페이지 번호 (기본값: 0)
     * @param size 한 페이지당 항목 수 (기본값: 8)
     * @return 오늘의 할 일 목록 및 페이징 정보
     */
    @GetMapping("/todays")
    public ResponseEntity<ApiResponse<TodayListResponseDto>> getTodayList(
            @RequestHeader("Authorization") String authorizationHeader,
            @RequestHeader(value = "X-Mobile-Type", required = false, defaultValue = "ANDROID") MobileType mobileType,
            @RequestParam(value = "page", defaultValue = "0") int page,
            @RequestParam(value = "size", defaultValue = "8") int size
    ) {
        LocalDate todayDate = LocalDate.now();
        // 오늘의 할 일 목록 조회
        TodayListResponseDto response = todoTodayService.getTodayList(
                jwtService.extractUserIdFromToken(authorizationHeader),
                mobileType,
                page,
                size,
                todayDate
        );
        return ApiResponse.onSuccess(SuccessStatus._OK, response);
    }

    /**
     * 이벤트 생성 API.
     *
     * 전체 사용자에게 푸쉬 알림을 전송하고, 필요 시 Today Todo를 생성합니다.
     *
     * @param eventCreateRequestDto 이벤트 생성 요청 데이터 (푸쉬 알림 제목/내용, 할 일 생성 여부 포함)
     * @return 성공 상태 응답
     */
    @PostMapping("/todays/event")
    public ResponseEntity<ApiResponse<SuccessStatus>> createEvent(
            @Validated @RequestBody EventCreateRequestDto eventCreateRequestDto
    ) {
        todoTodayService.createEventAndTodosIfNeeded(eventCreateRequestDto);
        return ApiResponse.onSuccess(SuccessStatus._CREATED);
    }
}
