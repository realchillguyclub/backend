package server.poptato.category.validator;

import org.junit.jupiter.api.*;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import server.poptato.category.domain.entity.Category;
import server.poptato.category.domain.repository.CategoryRepository;
import server.poptato.configuration.ServiceTestConfig;
import server.poptato.global.exception.CustomException;

import java.util.Optional;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.verify;

class CategoryValidatorTest extends ServiceTestConfig {

    @Mock
    CategoryRepository categoryRepository;

    @InjectMocks
    CategoryValidator categoryValidator;

    private Category category(Long ownerId) {
        return Category.builder()
                .userId(ownerId)
                .categoryOrder(0)
                .emojiId(1L)
                .name("dummy")
                .build();
    }

    @Nested
    @DisplayName("[SCN-VALID-CATEGORY-001] 특정 카테고리를 검증하고, 검증에 성공하면 해당 카테고리를 반환한다.")
    @TestMethodOrder(MethodOrderer.DisplayName.class)
    class ValidateAndReturnCategory {

        @Test
        @DisplayName("[SCN-VALID-CATEGORY-001][TC-RETURN-OK-001] 본인 소유 카테고리면 Category를 정상적으로 반환한다")
        void ownCategory_returnsCategory() {
            // given
            Long userId = 10L;
            Long categoryId = 100L;
            given(categoryRepository.findById(categoryId))
                    .willReturn(Optional.of(category(userId)));

            // when
            Category result = categoryValidator.validateAndReturnCategory(userId, categoryId);

            // then
            assertThat(result).isNotNull();
            verify(categoryRepository).findById(categoryId);
        }

        @Test
        @DisplayName("[SCN-VALID-CATEGORY-001][TC-RETURN-OK-002] 기본 카테고리(-1L)면 Category를 정상적으로 반환한다")
        void defaultCategory_returnsCategory() {
            // given
            Long userId = 10L;
            Long categoryId = 101L;
            given(categoryRepository.findById(categoryId))
                    .willReturn(Optional.of(category(-1L)));

            // when
            Category result = categoryValidator.validateAndReturnCategory(userId, categoryId);

            // then
            assertThat(result).isNotNull();
        }

        @Test
        @DisplayName("[SCN-VALID-CATEGORY-001][TC-NOT-EXIST-001] 카테고리가 없으면 _CATEGORY_NOT_EXIST 예외를 던진다")
        void notExist_throwsCategoryNotExist() {
            // given
            Long userId = 10L;
            Long categoryId = 999L;
            given(categoryRepository.findById(categoryId))
                    .willReturn(Optional.empty());

            // expect
            assertThatThrownBy(() ->
                    categoryValidator.validateAndReturnCategory(userId, categoryId)
            ).isInstanceOf(CustomException.class);
        }

        @Test
        @DisplayName("[SCN-VALID-CATEGORY-001][TC-OWNER-MISMATCH-001] 소유자 불일치면 _CATEGORY_USER_NOT_MATCH 예외를 던진다")
        void ownerMismatch_throwsUserNotMatch() {
            // given
            Long userId = 10L;
            Long categoryId = 102L;
            given(categoryRepository.findById(categoryId))
                    .willReturn(Optional.of(category(20L)));

            // expect
            assertThatThrownBy(() ->
                    categoryValidator.validateAndReturnCategory(userId, categoryId)
            ).isInstanceOf(CustomException.class);
        }
    }

    @Nested
    @DisplayName("[SCN-VALID-CATEGORY-002] 특정 카테고리를 검증한다.")
    @TestMethodOrder(MethodOrderer.DisplayName.class)
    class ValidateCategory {

        @Test
        @DisplayName("[SCN-VALID-CATEGORY-002][TC-VALID-OK-001] 본인 소유 카테고리면 예외 없이 통과한다")
        void ownCategory_noException() {
            // given
            Long userId = 10L;
            Long categoryId = 200L;
            given(categoryRepository.findById(categoryId))
                    .willReturn(Optional.of(category(userId)));

            // expect
            assertThatCode(() -> categoryValidator.validateCategory(userId, categoryId))
                    .doesNotThrowAnyException();

            // then
            verify(categoryRepository).findById(categoryId);
        }

        @Test
        @DisplayName("[SCN-VALID-CATEGORY-002][TC-VALID-OK-002] 기본 카테고리(-1L)면 예외 없이 통과한다")
        void defaultCategory_noException() {
            // given
            Long userId = 10L;
            Long categoryId = 201L;
            given(categoryRepository.findById(categoryId))
                    .willReturn(Optional.of(category(-1L)));

            // expect
            assertThatCode(() -> categoryValidator.validateCategory(userId, categoryId))
                    .doesNotThrowAnyException();
        }

        @Test
        @DisplayName("[SCN-VALID-CATEGORY-002][TC-NOT-EXIST-001] 카테고리가 없으면 _CATEGORY_NOT_EXIST 예외를 던진다")
        void notExist_throwsCategoryNotExist() {
            // given
            Long userId = 10L;
            Long categoryId = 999L;
            given(categoryRepository.findById(categoryId))
                    .willReturn(Optional.empty());

            // expect
            assertThatThrownBy(() -> categoryValidator.validateCategory(userId, categoryId))
                    .isInstanceOf(CustomException.class);
        }

        @Test
        @DisplayName("[SCN-VALID-CATEGORY-002][TC-OWNER-MISMATCH-001] 소유자 불일치면 _CATEGORY_USER_NOT_MATCH 예외를 던진다")
        void ownerMismatch_throwsUserNotMatch() {
            // given
            Long userId = 10L;
            Long categoryId = 202L;
            given(categoryRepository.findById(categoryId))
                    .willReturn(Optional.of(category(20L)));

            // expect
            assertThatThrownBy(() -> categoryValidator.validateCategory(userId, categoryId))
                    .isInstanceOf(CustomException.class);
        }
    }
}
