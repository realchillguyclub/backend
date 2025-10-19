package server.poptato.category.application;

import org.junit.jupiter.api.*;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.mockito.*;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import server.poptato.category.api.request.CategoryCreateUpdateRequestDto;
import server.poptato.category.api.request.CategoryDragAndDropRequestDto;
import server.poptato.category.application.response.CategoryCreateResponseDto;
import server.poptato.category.application.response.CategoryListResponseDto;
import server.poptato.category.domain.entity.Category;
import server.poptato.category.domain.repository.CategoryRepository;
import server.poptato.category.status.CategoryErrorStatus;
import server.poptato.category.validator.CategoryValidator;
import server.poptato.configuration.ServiceTestConfig;
import server.poptato.emoji.domain.repository.EmojiRepository;
import server.poptato.emoji.validator.EmojiValidator;
import server.poptato.global.exception.CustomException;
import server.poptato.global.util.FileUtil;
import server.poptato.todo.domain.repository.TodoRepository;
import server.poptato.user.domain.value.MobileType;
import server.poptato.user.status.UserErrorStatus;
import server.poptato.user.validator.UserValidator;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

public class CategoryServiceTest extends ServiceTestConfig {

    @Mock
    CategoryRepository categoryRepository;

    @Mock
    UserValidator userValidator;

    @Mock
    EmojiValidator emojiValidator;

    @Mock
    CategoryValidator categoryValidator;

    @Mock
    EmojiRepository emojiRepository;

    @Mock
    TodoRepository todoRepository;

    @Captor
    ArgumentCaptor<Category> categoryCaptor;

    @InjectMocks
    CategoryService categoryService;

    @Nested
    @TestMethodOrder(MethodOrderer.DisplayName.class)
    @DisplayName("[SCN-SVC-CATEGORY-001] 카테고리를 생성한다.")
    class CreateCategory {

        @Test
        @DisplayName("[SCN-SVC-CATEGORY-001][TC-CREATE-001] 카테고리를 정상적으로 생성한다.")
        void createCategory_success_incrementsOrderAndReturnsId() {
            // given
            Long userId = 10L;
            Long emojiId = 77L;
            String name = "Test";
            CategoryCreateUpdateRequestDto requestDto = new CategoryCreateUpdateRequestDto(name, emojiId);

            doNothing().when(userValidator).checkIsExistUser(userId);
            doNothing().when(emojiValidator).checkIsExistEmoji(emojiId);
            when(categoryRepository.findMaxCategoryOrderByUserId(userId)).thenReturn(Optional.of(3));

            Category persisted = mock(Category.class);
            when(persisted.getId()).thenReturn(100L);
            when(categoryRepository.save(any(Category.class))).thenReturn(persisted);

            // when
            CategoryCreateResponseDto responseDto = categoryService.createCategory(userId, requestDto);

            // then
            verify(categoryRepository).save(categoryCaptor.capture());
            Category toSave = categoryCaptor.getValue();
            assertThat(toSave.getUserId()).isEqualTo(userId);
            assertThat(toSave.getEmojiId()).isEqualTo(emojiId);
            assertThat(toSave.getName()).isEqualTo(name);
            assertThat(toSave.getCategoryOrder()).isEqualTo(4);
            assertThat(responseDto.categoryId()).isEqualTo(100L);
        }

        @Test
        @DisplayName("[SCN-SVC-CATEGORY-001][TC-CREATE-002] 기본 카테고리가 존재하지 않아 예외가 발생한다.")
        void createCategory_fail_defaultCategoryNotExist() {
            // given
            Long userId = 11L;
            Long emojiId = 55L;
            CategoryCreateUpdateRequestDto requestDto = new CategoryCreateUpdateRequestDto("Home", emojiId);

            doNothing().when(userValidator).checkIsExistUser(userId);
            doNothing().when(emojiValidator).checkIsExistEmoji(emojiId);
            when(categoryRepository.findMaxCategoryOrderByUserId(userId)).thenReturn(Optional.empty());

            // when & then
            assertThatThrownBy(() -> categoryService.createCategory(userId, requestDto))
                    .isInstanceOf(CustomException.class)
                    .hasMessageContaining(CategoryErrorStatus._DEFAULT_CATEGORY_NOT_EXIST.getMessage());
        }

        @Test
        @DisplayName("[SCN-SVC-CATEGORY-001][TC-CREATE-003] 요청한 이모지가 존재하지 않아 예외가 발생한다.")
        void createCategory_fail_emojiNotExist() {
            // given
            Long userId = 12L;
            Long emojiId = 999L;
            CategoryCreateUpdateRequestDto requestDto = new CategoryCreateUpdateRequestDto("Study", emojiId);

            doNothing().when(userValidator).checkIsExistUser(userId);
            doThrow(new CustomException(CategoryErrorStatus._CATEGORY_NOT_EXIST))
                    .when(emojiValidator).checkIsExistEmoji(emojiId);

            // when & then
            assertThatThrownBy(() -> categoryService.createCategory(userId, requestDto))
                    .isInstanceOf(CustomException.class);
        }

        @Test
        @DisplayName("[SCN-SVC-CATEGORY-001][TC-CREATE-004] 요청한 사용자가 존재하지 않아 예외가 발생한다.")
        void createCategory_fail_userNotExist() {
            // given
            Long userId = 404L;
            Long emojiId = 1L;
            CategoryCreateUpdateRequestDto requestDto = new CategoryCreateUpdateRequestDto("Etc", emojiId);

            doThrow(new CustomException(CategoryErrorStatus._CATEGORY_NOT_EXIST))
                    .when(userValidator).checkIsExistUser(userId);

            // when & then
            assertThatThrownBy(() -> categoryService.createCategory(userId, requestDto))
                    .isInstanceOf(CustomException.class);
        }
    }

    @Nested
    @TestMethodOrder(MethodOrderer.DisplayName.class)
    @DisplayName("[SCN-SVC-CATEGORY-002] 카테고리 목록을 조회한다.")
    class ListCategories {

        @Test
        @DisplayName("[SCN-SVC-CATEGORY-002][TC-LIST-001] 정상 조회 시 이모지 URL을 확장자로 변환하여 DTO 목록과 totalPages를 반환한다.")
        void list_success_returnsMappedDtosWithTotalPages() {
            // given
            Long userId = 10L;
            int page = 0;
            int size = 2;

            doNothing().when(userValidator).checkIsExistUser(userId);

            Category c1 = mock(Category.class);
            when(c1.getId()).thenReturn(1L);
            when(c1.getEmojiId()).thenReturn(101L);
            when(c1.getName()).thenReturn("Work");

            Category c2 = mock(Category.class);
            when(c2.getId()).thenReturn(2L);
            when(c2.getEmojiId()).thenReturn(102L);
            when(c2.getName()).thenReturn("Home");

            Page<Category> pageData =
                    new PageImpl<>(List.of(c1, c2), PageRequest.of(page, size), 5);
            when(categoryRepository.findCategories(eq(userId), any(PageRequest.class))).thenReturn(pageData);

            when(emojiRepository.findImageUrlById(101L)).thenReturn("emoji/e1.svg");
            when(emojiRepository.findImageUrlById(102L)).thenReturn("emoji/e2.svg");

            try (MockedStatic<FileUtil> mocked = mockStatic(FileUtil.class)) {
                mocked.when(() -> FileUtil.changeFileExtension("emoji/e1.svg", MobileType.ANDROID.getImageUrlExtension()))
                        .thenReturn("emoji/e1.pdf");
                mocked.when(() -> FileUtil.changeFileExtension("emoji/e2.svg", MobileType.ANDROID.getImageUrlExtension()))
                        .thenReturn("emoji/e2.pdf");

                // when
                CategoryListResponseDto responseDto = categoryService.getCategories(userId, MobileType.ANDROID, page, size);

                // then
                assertThat(responseDto.categories()).hasSize(2);
                assertThat(responseDto.totalPageCount()).isEqualTo(pageData.getTotalPages());
                verify(emojiRepository).findImageUrlById(101L);
                verify(emojiRepository).findImageUrlById(102L);
                mocked.verify(() -> FileUtil.changeFileExtension("emoji/e1.svg", MobileType.ANDROID.getImageUrlExtension()), times(1));
                mocked.verify(() -> FileUtil.changeFileExtension("emoji/e2.svg", MobileType.ANDROID.getImageUrlExtension()), times(1));
            }
        }

        @Test
        @DisplayName("[SCN-SVC-CATEGORY-002][TC-LIST-002] 조회 결과가 비어 있으면 빈 목록과 totalPages 0을 반환하며 이모지 조회와 파일 확장자 변환을 호출하지 않는다.")
        void list_empty_returnsEmptyAndDoesNotCallEmojiOrFileUtil() {
            // given
            Long userId = 11L;
            int page = 0;
            int size = 10;

            doNothing().when(userValidator).checkIsExistUser(userId);

            Page<Category> emptyPage = new PageImpl<>(List.of(), PageRequest.of(page, size), 0);
            when(categoryRepository.findCategories(eq(userId), any(PageRequest.class))).thenReturn(emptyPage);

            try (MockedStatic<FileUtil> mocked = mockStatic(FileUtil.class)) {
                // when
                CategoryListResponseDto responseDto = categoryService.getCategories(userId, MobileType.IOS, page, size);

                // then
                assertThat(responseDto.categories()).isEmpty();
                assertThat(responseDto.totalPageCount()).isZero();
                verifyNoInteractions(emojiRepository);
                mocked.verifyNoInteractions();
            }
        }

        @Test
        @DisplayName("[SCN-SVC-CATEGORY-002][TC-LIST-003] 사용자가 존재하지 않으면 예외를 던지고 카테고리 조회와 이모지 조회 그리고 파일 확장자 변환을 수행하지 않는다.")
        void list_userNotFound_throwsAndNoFurtherCalls() {
            // given
            Long userId = 404L;
            int page = 0;
            int size = 5;

            doThrow(new CustomException(UserErrorStatus._USER_NOT_EXIST))
                    .when(userValidator).checkIsExistUser(userId);

            try (MockedStatic<FileUtil> mocked = mockStatic(FileUtil.class)) {
                // when & then
                assertThatThrownBy(() -> categoryService.getCategories(userId, MobileType.ANDROID, page, size))
                        .isInstanceOf(CustomException.class);

                verifyNoInteractions(categoryRepository, emojiRepository);
                mocked.verifyNoInteractions();
            }
        }

        @Test
        @DisplayName("[SCN-SVC-CATEGORY-002][TC-LIST-004] N개 항목이 조회되면 파일 확장자 변환 함수가 각 항목마다 정확히 한 번씩 호출된다.")
        void list_callsFileUtilExactlyOncePerItem() {
            // given
            Long userId = 20L;
            int page = 0;
            int size = 3;

            doNothing().when(userValidator).checkIsExistUser(userId);

            Category c1 = mock(Category.class);
            when(c1.getId()).thenReturn(10L);
            when(c1.getEmojiId()).thenReturn(501L);
            when(c1.getName()).thenReturn("A");

            Category c2 = mock(Category.class);
            when(c2.getId()).thenReturn(11L);
            when(c2.getEmojiId()).thenReturn(502L);
            when(c2.getName()).thenReturn("B");

            Category c3 = mock(Category.class);
            when(c3.getId()).thenReturn(12L);
            when(c3.getEmojiId()).thenReturn(503L);
            when(c3.getName()).thenReturn("C");

            Page<Category> pageData =
                    new PageImpl<>(List.of(c1, c2, c3), PageRequest.of(page, size), 3);
            when(categoryRepository.findCategories(eq(userId), any(PageRequest.class))).thenReturn(pageData);

            when(emojiRepository.findImageUrlById(501L)).thenReturn("e1.svg");
            when(emojiRepository.findImageUrlById(502L)).thenReturn("e2.svg");
            when(emojiRepository.findImageUrlById(503L)).thenReturn("e3.svg");

            try (MockedStatic<FileUtil> mocked = mockStatic(FileUtil.class)) {
                mocked.when(() -> FileUtil.changeFileExtension(anyString(), anyString()))
                        .thenAnswer(inv -> inv.getArgument(0));

                // when
                CategoryListResponseDto responseDto = categoryService.getCategories(userId, MobileType.ANDROID, page, size);

                // then
                assertThat(responseDto.categories()).hasSize(3);

                mocked.verify(() -> FileUtil.changeFileExtension("e1.svg", MobileType.ANDROID.getImageUrlExtension()), times(1));
                mocked.verify(() -> FileUtil.changeFileExtension("e2.svg", MobileType.ANDROID.getImageUrlExtension()), times(1));
                mocked.verify(() -> FileUtil.changeFileExtension("e3.svg", MobileType.ANDROID.getImageUrlExtension()), times(1));

                verify(emojiRepository, times(1)).findImageUrlById(501L);
                verify(emojiRepository, times(1)).findImageUrlById(502L);
                verify(emojiRepository, times(1)).findImageUrlById(503L);
            }
        }
    }

    @Nested
    @TestMethodOrder(MethodOrderer.DisplayName.class)
    @DisplayName("[SCN-SVC-CATEGORY-003] 카테고리를 수정한다.")
    class UpdateCategory {

        @Test
        @DisplayName("[SCN-SVC-CATEGORY-003][TC-UPDATE-001] 정상 수정 시 카테고리 엔티티에 요청 값이 반영되고 저장을 한 번 수행한다")
        void update_success_updatesEntityAndSavesOnce() {
            // given
            Long userId = 10L;
            Long categoryId = 100L;
            Long emojiId = 77L;
            CategoryCreateUpdateRequestDto requestDto = new CategoryCreateUpdateRequestDto("NewName", emojiId);

            Category category = mock(Category.class);
            when(categoryValidator.validateAndReturnCategory(userId, categoryId)).thenReturn(category);

            // when
            categoryService.updateCategory(userId, categoryId, requestDto);

            // then
            verify(category).update(requestDto);
            verify(categoryRepository, times(1)).save(category);
            verifyNoMoreInteractions(categoryRepository);
        }

        @Test
        @DisplayName("[SCN-SVC-CATEGORY-003][TC-UPDATE-002] 사용자가 존재하지 않으면 예외를 던지고 카테고리 조회와 저장을 수행하지 않는다")
        void update_userNotFound_throwsAndDoesNotQueryOrSave() {
            // given
            Long userId = 404L;
            Long categoryId = 1L;
            Long emojiId = 10L;
            CategoryCreateUpdateRequestDto requestDto = new CategoryCreateUpdateRequestDto("Name", emojiId);

            doThrow(new CustomException(UserErrorStatus._USER_NOT_EXIST))
                    .when(userValidator).checkIsExistUser(userId);

            // when & then
            assertThatThrownBy(() -> categoryService.updateCategory(userId, categoryId, requestDto))
                    .isInstanceOf(CustomException.class);

            verifyNoInteractions(categoryValidator);
            verify(categoryRepository, never()).save(any());
        }

        @ParameterizedTest
        @ValueSource(strings = {"_CATEGORY_NOT_EXIST", "_CATEGORY_USER_NOT_MATCH"})
        @DisplayName("[SCN-SVC-CATEGORY-003][TC-UPDATE-003] 요청한 이모지가 존재하지 않으면 예외를 던지고 카테고리 조회와 저장을 수행하지 않는다")
        void update_emojiNotFound_throwsAndDoesNotQueryOrSave(String statusName) {
            // given
            Long userId = 10L;
            Long categoryId = 1L;
            Long emojiId = 999L;
            CategoryCreateUpdateRequestDto requestDto = new CategoryCreateUpdateRequestDto("Name", emojiId);

            doNothing().when(userValidator).checkIsExistUser(userId);
            doThrow(new CustomException(CategoryErrorStatus.valueOf(statusName)))
                    .when(categoryValidator).validateAndReturnCategory(userId, categoryId);

            // when & then
            assertThatThrownBy(() -> categoryService.updateCategory(userId, categoryId, requestDto))
                    .isInstanceOf(CustomException.class);

            verify(categoryRepository, never()).save(any());
        }

        @Test
        @DisplayName("[SCN-SVC-CATEGORY-003][TC-UPDATE-004] 대상 카테고리가 유효하지 않으면 예외를 던지고 저장을 수행하지 않는다")
        void update_invalidCategory_throwsAndDoesNotSave() {
            // given
            Long userId = 10L;
            Long categoryId = 123L;
            Long emojiId = 7L;
            CategoryCreateUpdateRequestDto requestDto = new CategoryCreateUpdateRequestDto("Name", emojiId);

            doNothing().when(userValidator).checkIsExistUser(userId);
            doNothing().when(emojiValidator).checkIsExistEmoji(emojiId);
            doThrow(new CustomException(CategoryErrorStatus._CATEGORY_NOT_EXIST))
                    .when(categoryValidator).validateAndReturnCategory(userId, categoryId);

            // when & then
            assertThatThrownBy(() -> categoryService.updateCategory(userId, categoryId, requestDto))
                    .isInstanceOf(CustomException.class);

            verify(categoryRepository, never()).save(any());
        }

        @Test
        @DisplayName("[SCN-SVC-CATEGORY-003][TC-UPDATE-005] 검증과 저장 호출 순서를 사용자 검증 다음에 이모지 검증 그 다음에 카테고리 검증 마지막에 저장으로 보장한다")
        void update_callsInStrictOrder_user_then_emoji_then_category_then_save() {
            // given
            Long userId = 20L;
            Long categoryId = 200L;
            Long emojiId = 70L;
            CategoryCreateUpdateRequestDto requestDto = new CategoryCreateUpdateRequestDto("Ordered", emojiId);

            Category category = mock(Category.class);
            when(categoryValidator.validateAndReturnCategory(userId, categoryId)).thenReturn(category);

            // when
            categoryService.updateCategory(userId, categoryId, requestDto);

            // then
            InOrder inOrder = inOrder(userValidator, emojiValidator, categoryValidator, category, categoryRepository);
            inOrder.verify(userValidator).checkIsExistUser(userId);
            inOrder.verify(emojiValidator).checkIsExistEmoji(emojiId);
            inOrder.verify(categoryValidator).validateAndReturnCategory(userId, categoryId);
            inOrder.verify(category).update(requestDto);
            inOrder.verify(categoryRepository).save(category);
            verifyNoMoreInteractions(categoryRepository);
        }
    }

    @Nested
    @TestMethodOrder(MethodOrderer.DisplayName.class)
    @DisplayName("[SCN-SVC-CATEGORY-004] 카테고리를 삭제한다.")
    class DeleteCategory {

        @Test
        @DisplayName("[SCN-SVC-CATEGORY-004][TC-DELETE-001] 정상 삭제 시 카테고리 삭제와 연관 Todo 삭제를 각각 한 번 수행한다.")
        void delete_success_deletesCategoryAndTodosOnce() {
            // given
            Long userId = 10L;
            Long categoryId = 100L;

            Category category = mock(Category.class);
            when(categoryValidator.validateAndReturnCategory(userId, categoryId)).thenReturn(category);

            // when
            categoryService.deleteCategory(userId, categoryId);

            // then
            verify(categoryRepository, times(1)).delete(same(category));
            verify(todoRepository, times(1)).deleteAllByCategoryId(categoryId);
            verifyNoMoreInteractions(categoryRepository, todoRepository);
        }

        @Test
        @DisplayName("[SCN-SVC-CATEGORY-004][TC-DELETE-002] 사용자가 존재하지 않으면 예외를 던지고 카테고리 검증과 삭제를 수행하지 않는다")
        void delete_userNotFound_throwsAndDoesNotProceed() {
            // given
            Long userId = 404L;
            Long categoryId = 1L;

            doThrow(new CustomException(UserErrorStatus._USER_NOT_EXIST))
                    .when(userValidator).checkIsExistUser(userId);

            // when & then
            assertThatThrownBy(() -> categoryService.deleteCategory(userId, categoryId))
                    .isInstanceOf(CustomException.class);

            verifyNoInteractions(categoryValidator);
            verifyNoInteractions(categoryRepository, todoRepository);
        }

        @Test
        @DisplayName("[SCN-SVC-CATEGORY-004][TC-DELETE-003] 대상 카테고리가 유효하지 않으면 예외를 던지고 카테고리 삭제와 연관 Todo 삭제를 수행하지 않는다")
        void delete_invalidCategory_throwsAndDoesNotDelete() {
            // given
            Long userId = 10L;
            Long categoryId = 999L;

            doThrow(new CustomException(CategoryErrorStatus._CATEGORY_NOT_EXIST))
                    .when(categoryValidator).validateAndReturnCategory(userId, categoryId);

            // when & then
            assertThatThrownBy(() -> categoryService.deleteCategory(userId, categoryId))
                    .isInstanceOf(CustomException.class)
                    .hasMessageContaining(CategoryErrorStatus._CATEGORY_NOT_EXIST.getMessage());

            verify(categoryRepository, never()).delete(any());
            verify(todoRepository, never()).deleteAllByCategoryId(anyLong());
        }
    }

    @Nested
    @TestMethodOrder(MethodOrderer.DisplayName.class)
    @DisplayName("[SCN-SVC-CATEGORY-005] 카테고리 순서를 재배치한다.")
    class ReorderCategories {

        @Test
        @DisplayName("[SCN-SVC-CATEGORY-005][TC-REORDER-001] 정상적으로 순서를 변경하면 기존 categoryOrder의 오름차순 값을 요청 ID 순서대로 재할당하고 각 카테고리를 한 번씩 저장한다.")
        void reorder_success_assignsSortedOrdersAndSavesOncePerCategory() {
            // given
            Long userId = 10L;
            Long idA = 10L;
            Long idB = 20L;
            Long idC = 30L;
            CategoryDragAndDropRequestDto requestDto = new CategoryDragAndDropRequestDto(List.of(idA, idB, idC));

            Category catA = mock(Category.class);
            when(catA.getId()).thenReturn(idA);
            when(catA.getCategoryOrder()).thenReturn(9);
            Category catB = mock(Category.class);
            when(catB.getId()).thenReturn(idB);
            when(catB.getCategoryOrder()).thenReturn(2);
            Category catC = mock(Category.class);
            when(catC.getId()).thenReturn(idC);
            when(catC.getCategoryOrder()).thenReturn(5);

            when(categoryRepository.findById(idA)).thenReturn(Optional.of(catA));
            when(categoryRepository.findById(idB)).thenReturn(Optional.of(catB));
            when(categoryRepository.findById(idC)).thenReturn(Optional.of(catC));

            // when
            categoryService.dragAndDrop(userId, requestDto);

            // then
            verify(catA).updateCategoryOrder(2);
            verify(catB).updateCategoryOrder(5);
            verify(catC).updateCategoryOrder(9);

            verify(categoryRepository, times(1)).save(catA);
            verify(categoryRepository, times(1)).save(catB);
            verify(categoryRepository, times(1)).save(catC);
        }

        @Test
        @DisplayName("[SCN-SVC-CATEGORY-005][TC-REORDER-002] 사용자가 존재하지 않으면 예외를 던지고 카테고리 조회와 검증 그리고 저장을 수행하지 않는다.")
        void reorder_userNotFound_throwsAndNoRepositoryCalls() {
            // given
            Long userId = 404L;
            CategoryDragAndDropRequestDto requestDto = new CategoryDragAndDropRequestDto(List.of(1L, 2L));

            doThrow(new CustomException(UserErrorStatus._USER_NOT_EXIST))
                    .when(userValidator).checkIsExistUser(userId);

            // when & then
            assertThatThrownBy(() -> categoryService.dragAndDrop(userId, requestDto))
                    .isInstanceOf(CustomException.class);

            verifyNoInteractions(categoryRepository, categoryValidator);
        }

        @Test
        @DisplayName("[SCN-SVC-CATEGORY-005][TC-REORDER-003] 요청 목록에 시스템 카테고리(−1 또는 0)가 포함되어 있으면 예외를 던지고 저장을 수행하지 않는다.")
        void reorder_containsSystemCategory_throwsAndDoesNotSave() {
            // given
            Long userId = 10L;
            Long defaultAll = -1L;
            Long normal = 2L;
            CategoryDragAndDropRequestDto requestDto = new CategoryDragAndDropRequestDto(List.of(defaultAll, normal));

            Category defaultCat = mock(Category.class);
            when(defaultCat.getId()).thenReturn(defaultAll);
            Category normalCat = mock(Category.class);

            when(categoryRepository.findById(defaultAll)).thenReturn(Optional.of(defaultCat));
            when(categoryRepository.findById(normal)).thenReturn(Optional.of(normalCat));

            // when & then
            assertThatThrownBy(() -> categoryService.dragAndDrop(userId, requestDto))
                    .isInstanceOf(CustomException.class)
                    .hasMessageContaining(CategoryErrorStatus._INVALID_DRAG_AND_DROP_CATEGORY.getMessage());

            verify(categoryRepository, never()).save(any());
        }

        @Test
        @DisplayName("[SCN-SVC-CATEGORY-005][TC-REORDER-004] 요청 목록에 존재하지 않는 카테고리 ID가 포함되어 있으면 예외를 던지고 저장을 수행하지 않는다.")
        void reorder_containsNonExistingCategory_throwsAndDoesNotSave() {
            // given
            Long userId = 10L;
            Long exists = 1L;
            Long notExists = 999L;
            CategoryDragAndDropRequestDto requestDto = new CategoryDragAndDropRequestDto(List.of(exists, notExists));

            Category existing = mock(Category.class);

            when(categoryRepository.findById(exists)).thenReturn(Optional.of(existing));
            when(categoryRepository.findById(notExists)).thenReturn(Optional.empty()); // getCategoriesByIds에서 예외

            // when & then
            assertThatThrownBy(() -> categoryService.dragAndDrop(userId, requestDto))
                    .isInstanceOf(CustomException.class)
                    .hasMessageContaining(CategoryErrorStatus._CATEGORY_NOT_EXIST.getMessage());

            verify(categoryRepository, never()).save(any());
            verifyNoInteractions(categoryValidator);
        }

        @Test
        @DisplayName("[SCN-SVC-CATEGORY-005][TC-REORDER-005] 카테고리 검증에서 실패하면 예외를 던지고 저장을 수행하지 않는다.")
        void reorder_validatorFails_throwsAndDoesNotSave() {
            // given
            Long userId = 10L;
            Long idA = 10L;
            Long idB = 20L;
            CategoryDragAndDropRequestDto requestDto = new CategoryDragAndDropRequestDto(List.of(idA, idB));

            Category catA = mock(Category.class);
            when(catA.getId()).thenReturn(idA);
            Category catB = mock(Category.class);

            when(categoryRepository.findById(idA)).thenReturn(Optional.of(catA));
            when(categoryRepository.findById(idB)).thenReturn(Optional.of(catB));

            doThrow(new CustomException(CategoryErrorStatus._CATEGORY_USER_NOT_MATCH))
                    .when(categoryValidator).validateCategory(userId, idA);

            // when & then
            assertThatThrownBy(() -> categoryService.dragAndDrop(userId, requestDto))
                    .isInstanceOf(CustomException.class)
                    .hasMessageContaining(CategoryErrorStatus._CATEGORY_USER_NOT_MATCH.getMessage());

            verify(categoryRepository, never()).save(any());
        }
    }
}
