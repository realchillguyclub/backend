package server.poptato.category.api;

import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;
import server.poptato.auth.application.service.JwtService;
import server.poptato.category.api.request.CategoryCreateUpdateRequestDto;
import server.poptato.category.api.request.CategoryDragAndDropRequestDto;
import server.poptato.category.application.CategoryService;
import server.poptato.category.application.response.CategoryCreateResponseDto;
import server.poptato.category.application.response.CategoryListResponseDto;
import server.poptato.global.response.ApiResponse;
import server.poptato.global.response.status.SuccessStatus;
import server.poptato.user.domain.value.MobileType;

@RestController
@RequestMapping("/category")
@RequiredArgsConstructor
public class CategoryController {

    private final CategoryService categoryService;
    private final JwtService jwtService;

    /**
     * 카테고리 생성 API.
     *
     * 사용자가 새로운 카테고리를 생성합니다. 요청 본문에 카테고리 이름과 이모지 ID를 포함하여 생성 요청을 보냅니다.
     *
     * @param authorizationHeader 요청 헤더의 Authorization (Bearer 토큰)
     * @param categoryCreateRequestDto 카테고리 생성 요청 데이터 (이름, 이모지 ID)
     * @return 생성된 카테고리 ID를 포함한 응답
     */
    @PostMapping
    public ResponseEntity<ApiResponse<CategoryCreateResponseDto>> createCategory(
            @RequestHeader("Authorization") String authorizationHeader,
            @Validated @RequestBody CategoryCreateUpdateRequestDto categoryCreateRequestDto
    ) {
        CategoryCreateResponseDto response = categoryService.createCategory(jwtService.extractUserIdFromToken(authorizationHeader), categoryCreateRequestDto);
        return ApiResponse.onSuccess(SuccessStatus._OK, response);
    }

    /**
     * 카테고리 목록 조회 API.
     *
     * 사용자가 소유한 카테고리를 페이지네이션 형태로 조회합니다.
     * 페이지 번호와 페이지 크기를 쿼리 파라미터로 전달하며, 기본값은 첫 페이지(0), 항목 수는 6개입니다.
     *
     * @param authorizationHeader 요청 헤더의 Authorization (Bearer 토큰)
     * @param mobileType 클라이언트의 모바일 타입
     * @param page 요청 페이지 번호 (기본값: 0)
     * @param size 한 페이지당 항목 수 (기본값: 6)
     * @return 카테고리 목록과 페이징 정보를 포함한 응답
     */
    @GetMapping("/list")
    public ResponseEntity<ApiResponse<CategoryListResponseDto>> getCategories(
            @RequestHeader("Authorization") String authorizationHeader,
            @RequestHeader(value = "X-Mobile-Type", required = false, defaultValue = "ANDROID") MobileType mobileType,
            @RequestParam(value = "page", defaultValue = "0") int page,
            @RequestParam(value = "size", defaultValue = "6") int size
    ) {
        CategoryListResponseDto response = categoryService.getCategories(jwtService.extractUserIdFromToken(authorizationHeader), mobileType, page, size);
        return ApiResponse.onSuccess(SuccessStatus._OK, response);
    }

    /**
     * 카테고리 수정 API.
     *
     * 사용자가 특정 카테고리의 이름과 이모지 ID를 수정합니다.
     * 요청 본문에 수정할 이름과 이모지 ID를 포함합니다.
     *
     * @param authorizationHeader 요청 헤더의 Authorization (Bearer 토큰)
     * @param categoryId 수정할 카테고리의 ID
     * @param categoryUpdateRequestDto 카테고리 수정 요청 데이터 (이름, 이모지 ID)
     * @return 성공 여부를 나타내는 응답
     */
    @PutMapping("/{categoryId}")
    public ResponseEntity<ApiResponse<SuccessStatus>> updateCategory(
            @RequestHeader("Authorization") String authorizationHeader,
            @PathVariable Long categoryId,
            @Validated @RequestBody CategoryCreateUpdateRequestDto categoryUpdateRequestDto
    ) {
        categoryService.updateCategory(jwtService.extractUserIdFromToken(authorizationHeader), categoryId, categoryUpdateRequestDto);
        return ApiResponse.onSuccess(SuccessStatus._OK);
    }

    /**
     * 카테고리 삭제 API.
     *
     * 사용자가 특정 카테고리를 삭제합니다. 해당 카테고리에 속한 모든 할 일도 함께 삭제됩니다.
     *
     * @param authorizationHeader 요청 헤더의 Authorization (Bearer 토큰)
     * @param categoryId 삭제할 카테고리의 ID
     * @return 성공 여부를 나타내는 응답
     */
    @DeleteMapping("/{categoryId}")
    public ResponseEntity<ApiResponse<SuccessStatus>> deleteCategory(
            @RequestHeader("Authorization") String authorizationHeader,
            @PathVariable Long categoryId
    ) {
        categoryService.deleteCategory(jwtService.extractUserIdFromToken(authorizationHeader), categoryId);
        return ApiResponse.onSuccess(SuccessStatus._OK);
    }

    /**
     * 카테고리 순서 변경 API.
     *
     * 사용자가 드래그 앤 드롭을 통해 카테고리의 순서를 변경합니다.
     * 요청 본문에 새로운 카테고리 순서를 반영한 ID 목록을 전달받습니다.
     *
     * @param authorizationHeader 요청 헤더의 Authorization (Bearer 토큰)
     * @param categoryDragAndDropRequestDto 카테고리 순서 변경 요청 데이터
     * @return 성공 여부를 나타내는 응답
     */
    @PatchMapping("/dragAndDrop")
    public ResponseEntity<ApiResponse<SuccessStatus>> dragAndDrop(
            @RequestHeader("Authorization") String authorizationHeader,
            @Validated @RequestBody CategoryDragAndDropRequestDto categoryDragAndDropRequestDto
    ) {
        categoryService.dragAndDrop(jwtService.extractUserIdFromToken(authorizationHeader), categoryDragAndDropRequestDto);
        return ApiResponse.onSuccess(SuccessStatus._OK);
    }
}
