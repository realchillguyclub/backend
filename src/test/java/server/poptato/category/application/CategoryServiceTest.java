package server.poptato.category.application;

import org.junit.jupiter.api.*;
import org.mockito.*;
import server.poptato.category.api.request.CategoryCreateUpdateRequestDto;
import server.poptato.category.application.response.CategoryCreateResponseDto;
import server.poptato.category.domain.entity.Category;
import server.poptato.category.domain.repository.CategoryRepository;
import server.poptato.category.status.CategoryErrorStatus;
import server.poptato.category.validator.CategoryValidator;
import server.poptato.configuration.ServiceTestConfig;
import server.poptato.emoji.domain.repository.EmojiRepository;
import server.poptato.emoji.validator.EmojiValidator;
import server.poptato.global.exception.CustomException;
import server.poptato.todo.domain.repository.TodoRepository;
import server.poptato.user.validator.UserValidator;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@TestMethodOrder(MethodOrderer.DisplayName.class)
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
    @DisplayName("[SCN-SVC-CATEGORY-001] 카테고리 생성(Create)")
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

}
