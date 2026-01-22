package server.poptato.category.application;

import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import server.poptato.category.api.request.CategoryCreateUpdateRequestDto;
import server.poptato.category.api.request.CategoryDragAndDropRequestDto;
import server.poptato.category.application.response.CategoryCreateResponseDto;
import server.poptato.category.application.response.CategoryListResponseDto;
import server.poptato.category.application.response.CategoryResponseDto;
import server.poptato.category.domain.entity.Category;
import server.poptato.category.domain.repository.CategoryRepository;
import server.poptato.category.status.CategoryErrorStatus;
import server.poptato.category.validator.CategoryValidator;
import server.poptato.emoji.domain.repository.EmojiRepository;
import server.poptato.emoji.validator.EmojiValidator;
import server.poptato.global.exception.CustomException;
import server.poptato.global.util.FileUtil;
import server.poptato.todo.domain.repository.TodoRepository;
import server.poptato.user.domain.value.MobileType;
import server.poptato.user.validator.UserValidator;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Transactional
public class CategoryService {
    private final CategoryRepository categoryRepository;
    private final UserValidator userValidator;
    private final EmojiValidator emojiValidator;
    private final CategoryValidator categoryValidator;
    private final EmojiRepository emojiRepository;
    private final TodoRepository todoRepository;

    private static final Long ALL_CATEGORY = -1L;
    private static final Long BOOKMARK_CATEGORY = 0L;

    /**
     * 카테고리를 생성합니다.
     *
     * @param userId 사용자 ID
     * @param request 카테고리 생성 요청 데이터 (이름, 이모지 ID)
     * @return 생성된 카테고리 ID
     * @throws CustomException 기본 카테고리가 존재하지 않을 경우
     */
    public CategoryCreateResponseDto createCategory(Long userId, CategoryCreateUpdateRequestDto request) {
        userValidator.checkIsExistUser(userId);
        emojiValidator.checkIsExistEmoji(request.emojiId());
        int maxCategoryId = categoryRepository.findMaxCategoryOrderByUserId(userId).orElseThrow(
                () -> new CustomException(CategoryErrorStatus._DEFAULT_CATEGORY_NOT_EXIST));
        Category newCategory = categoryRepository.save(
                Category.builder()
                        .userId(userId)
                        .categoryOrder(maxCategoryId + 1)
                        .emojiId(request.emojiId())
                        .name(request.name())
                        .build()
        );
        return CategoryCreateResponseDto.of(newCategory.getId());
    }

    /**
     * 사용자의 카테고리 목록을 조회합니다.
     *
     * @param userId 사용자 ID
     * @param page 요청 페이지 번호
     * @param size 한 페이지에 포함할 카테고리 수
     * @return 카테고리 목록 및 페이징 정보
     */
    public CategoryListResponseDto getCategories(Long userId, MobileType mobileType, int page, int size) {
        userValidator.checkIsExistUser(userId);
        PageRequest pageRequest = PageRequest.of(page, size);
        Page<Category> categories = categoryRepository.findCategories(userId, pageRequest);
        return convertToCategoryListDto(categories, mobileType);
    }

    /**
     * 카테고리 목록을 DTO로 변환합니다.
     *
     * @param categories 카테고리 페이지 객체
     * @return 변환된 카테고리 목록 DTO
     */
    private CategoryListResponseDto convertToCategoryListDto(Page<Category> categories, MobileType mobileType) {
        String extension = mobileType.getImageUrlExtension();
        List<CategoryResponseDto> categoryResponseDtoList = categories.stream()
                .map(category -> {
                    String imageUrl = emojiRepository.findImageUrlById(category.getEmojiId());
                    String modifiedImageUrl = FileUtil.changeFileExtension(imageUrl, extension);
                    return CategoryResponseDto.of(category, modifiedImageUrl);
                })
                .collect(Collectors.toList());

        return new CategoryListResponseDto(categoryResponseDtoList, categories.getTotalPages());
    }

    /**
     * 특정 카테고리를 수정합니다.
     *
     * @param userId 사용자 ID
     * @param categoryId 수정할 카테고리 ID
     * @param updateRequestDto 수정 요청 데이터 (이름, 이모지 ID)
     */
    public void updateCategory(Long userId, Long categoryId, CategoryCreateUpdateRequestDto updateRequestDto) {
        userValidator.checkIsExistUser(userId);
        emojiValidator.checkIsExistEmoji(updateRequestDto.emojiId());
        Category category = categoryValidator.validateAndReturnCategory(userId, categoryId);
        category.update(updateRequestDto);
        categoryRepository.save(category);
    }

    /**
     * 특정 카테고리를 삭제합니다 (Soft Delete).
     *
     * @param userId 사용자 ID
     * @param categoryId 삭제할 카테고리 ID
     */
    public void deleteCategory(Long userId, Long categoryId) {
        userValidator.checkIsExistUser(userId);
        Category category = categoryValidator.validateAndReturnCategory(userId, categoryId);
        category.softDelete();
        todoRepository.softDeleteByCategoryId(categoryId);
    }

    /**
     * 카테고리 순서를 드래그 앤 드롭 방식으로 변경합니다.
     *
     * @param userId 사용자 ID
     * @param request 카테고리 순서 변경 요청 데이터
     */
    public void dragAndDrop(Long userId, CategoryDragAndDropRequestDto request) {
        userValidator.checkIsExistUser(userId);
        List<Category> categories = getCategoriesByIds(request.categoryIds());
        checkIsValidToDragAndDrop(userId, categories, request);
        reassignCategoryOrder(categories, request.categoryIds());
    }

    /**
     * ID 목록을 기반으로 카테고리를 조회합니다.
     *
     * @param categoryIds 조회할 카테고리 ID 목록
     * @return 조회된 카테고리 목록
     * @throws CustomException 카테고리가 존재하지 않을 경우
     */
    private List<Category> getCategoriesByIds(List<Long> categoryIds) {
        List<Category> categories = new ArrayList<>();
        for (Long categoryId : categoryIds) {
            Category category = categoryRepository.findById(categoryId)
                    .orElseThrow(() -> new CustomException(CategoryErrorStatus._CATEGORY_NOT_EXIST));
            categories.add(category);
        }
        return categories;
    }

    /**
     * 드래그 앤 드롭 요청이 유효한지 검증합니다.
     *
     * @param userId 사용자 ID
     * @param categories 대상 카테고리 목록
     * @param request 요청 데이터
     * @throws CustomException 유효하지 않은 카테고리 순서 변경일 경우
     */
    private void checkIsValidToDragAndDrop(Long userId, List<Category> categories, CategoryDragAndDropRequestDto request) {
        for (Category category : categories) {
            categoryValidator.validateCategory(userId, category.getId());
            if (category.getId() == ALL_CATEGORY || category.getId() == BOOKMARK_CATEGORY) {
                throw new CustomException(CategoryErrorStatus._INVALID_DRAG_AND_DROP_CATEGORY);
            }
        }
    }

    /**
     * 카테고리 순서를 재할당합니다.
     *
     * @param categories 대상 카테고리 목록
     * @param categoryIds 순서를 변경할 카테고리 ID 목록
     */
    private void reassignCategoryOrder(List<Category> categories, List<Long> categoryIds) {
        List<Integer> categoryOrders = getCategoryOrders(categoryIds);
        Collections.sort(categoryOrders);
        for (int i = 0; i < categories.size(); i++) {
            categories.get(i).updateCategoryOrder(categoryOrders.get(i));
            categoryRepository.save(categories.get(i));
        }
    }

    /**
     * 카테고리 순서 목록을 조회합니다.
     *
     * @param categoryIds 조회할 카테고리 ID 목록
     * @return 카테고리 순서 목록
     */
    private List<Integer> getCategoryOrders(List<Long> categoryIds) {
        List<Integer> categoryOrders = new ArrayList<>();
        for (Long categoryId : categoryIds) {
            Category category = categoryRepository.findById(categoryId).get();
            categoryOrders.add(category.getCategoryOrder());
        }
        return categoryOrders;
    }
}