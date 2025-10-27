package server.poptato.todo.api;

import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;
import server.poptato.auth.application.service.JwtService;
import server.poptato.global.response.ApiResponse;
import server.poptato.global.response.status.SuccessStatus;
import server.poptato.todo.api.request.BacklogCreateRequestDto;
import server.poptato.todo.application.TodoBacklogService;
import server.poptato.todo.application.response.BacklogCreateResponseDto;
import server.poptato.todo.application.response.BacklogListResponseDto;
import server.poptato.todo.application.response.PaginatedYesterdayResponseDto;
import server.poptato.user.domain.value.MobileType;

@RestController
@RequiredArgsConstructor
public class TodoBacklogController {

    private final TodoBacklogService todoBacklogService;
    private final JwtService jwtService;

    /**
     * 백로그 목록 조회 API.
     *
     * 사용자가 선택한 카테고리에 해당하는 백로그 목록을 조회합니다.
     * 페이지 번호와 크기를 요청 파라미터로 전달받아 페이징된 데이터를 제공합니다.
     *
     * @param authorizationHeader 요청 헤더의 Authorization (Bearer 토큰)
     * @param mobileType 클라이언트의 모바일 타입
     * @param categoryId 조회할 카테고리 ID
     * @param page 요청 페이지 번호 (기본값: 0)
     * @param size 한 페이지당 항목 수 (기본값: 8)
     * @return 백로그 목록 및 페이징 정보
     */
    @GetMapping(value = "/backlogs")
    public ResponseEntity<ApiResponse<BacklogListResponseDto>> getBacklogList(
            @RequestHeader("Authorization") String authorizationHeader,
            @RequestHeader(value = "X-Mobile-Type", required = false, defaultValue = "ANDROID") MobileType mobileType,
            @RequestParam(value = "category") Long categoryId,
            @RequestParam(value = "page", defaultValue = "0") int page,
            @RequestParam(value = "size", defaultValue = "8") int size

    ) {
        BacklogListResponseDto response = todoBacklogService.getBacklogList(jwtService.extractUserIdFromToken(authorizationHeader), categoryId, mobileType, page, size);
        return ApiResponse.onSuccess(SuccessStatus._OK, response);
    }

    /**
     * 백로그 생성 API.
     *
     * 사용자가 새로운 백로그를 생성합니다. 요청 본문에 백로그 제목과 세부 내용을 전달받아 처리합니다.
     *
     * @param authorizationHeader 요청 헤더의 Authorization (Bearer 토큰)
     * @param backlogCreateRequestDto 백로그 생성 요청 데이터
     * @return 생성된 백로그의 ID 및 관련 정보
     */
    @PostMapping("/backlog")
    public ResponseEntity<ApiResponse<BacklogCreateResponseDto>> createBacklog(
            @RequestHeader("Authorization") String authorizationHeader,
            @Validated @RequestBody BacklogCreateRequestDto backlogCreateRequestDto
    ) {
        BacklogCreateResponseDto response = todoBacklogService.createBacklog(jwtService.extractUserIdFromToken(authorizationHeader), backlogCreateRequestDto);
        return ApiResponse.onSuccess(SuccessStatus._OK, response);
    }

    /**
     * 어제의 백로그 목록 조회 API.
     *
     * 사용자가 어제 작업했던 백로그 항목들을 조회합니다.
     * 페이지 번호와 크기를 요청 파라미터로 전달받아 페이징된 데이터를 제공합니다.
     *
     * @param authorizationHeader 요청 헤더의 Authorization (Bearer 토큰)
     * @param page 요청 페이지 번호 (기본값: 0)
     * @param size 한 페이지당 항목 수 (기본값: 15)
     * @return 어제의 백로그 목록 및 페이징 정보
     */
    @GetMapping("/yesterdays")
    public ResponseEntity<ApiResponse<PaginatedYesterdayResponseDto>> getYesterdays(
            @RequestHeader("Authorization") String authorizationHeader,
            @RequestParam(value = "page", defaultValue = "0") int page,
            @RequestParam(value = "size", defaultValue = "15") int size
    ) {
        PaginatedYesterdayResponseDto response = todoBacklogService.getYesterdays(jwtService.extractUserIdFromToken(authorizationHeader), page, size);
        return ApiResponse.onSuccess(SuccessStatus._OK, response);
    }

    /**
     * 어제 백로그 생성 API.
     *
     * 어제 백로그 항목을 추가합니다.
     *
     * @param authorizationHeader 요청 헤더의 Authorization (Bearer 토큰)
     * @param backlogCreateRequestDto 백로그 생성 요청 데이터
     * @return 생성된 어제 백로그 항목
     */
    @PostMapping("/yesterdays")
    public ResponseEntity<ApiResponse<BacklogCreateResponseDto>> createYesterdayBacklog(
            @RequestHeader("Authorization") String authorizationHeader,
            @RequestBody BacklogCreateRequestDto backlogCreateRequestDto
    ) {
        BacklogCreateResponseDto response = todoBacklogService.createYesterdayBacklog(jwtService.extractUserIdFromToken(authorizationHeader), backlogCreateRequestDto);
        return ApiResponse.onSuccess(SuccessStatus._CREATED, response);
    }
}
