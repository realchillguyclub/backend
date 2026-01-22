package server.poptato.category.domain.entity;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

class CategoryTest {

    @Test
    @DisplayName("[TC-ENTITY-001] softDelete 호출 시 isDeleted가 true로 변경된다")
    void softDelete_setsIsDeletedTrue() {
        // given
        Category category = Category.builder()
                .userId(1L)
                .emojiId(1L)
                .categoryOrder(1)
                .name("test")
                .build();

        // when
        category.softDelete();

        // then
        assertThat(category.isDeleted()).isTrue();
    }

    @Test
    @DisplayName("[TC-ENTITY-002] isDeleted 기본값은 false이다")
    void isDeleted_defaultIsFalse() {
        // given
        Category category = Category.builder()
                .userId(1L)
                .emojiId(1L)
                .categoryOrder(1)
                .name("test")
                .build();

        // then
        assertThat(category.isDeleted()).isFalse();
    }
}
