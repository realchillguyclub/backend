package server.poptato.category.infra;

import org.junit.jupiter.api.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import server.poptato.category.domain.entity.Category;
import server.poptato.configuration.DatabaseTestConfig;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;

@DataJpaTest
public class JpaCategoryRepositoryTest extends DatabaseTestConfig {

    @Autowired
    private JpaCategoryRepository jpaCategoryRepository;

    private void seed(Long userId, int order, String name) {
        Category c = Category.builder()
                .userId(userId)
                .categoryOrder(order)
                .emojiId(1L)
                .name(name)
                .build();
        tem.persist(c);
        tem.flush();
        tem.clear();
    }

    @Nested
    @DisplayName("[SCN-REP-CATEGORY-001] 기본(-1,0)과 사용자 카테고리에 대해 최대 categoryOrder를 조회한다")
    @TestMethodOrder(MethodOrderer.DisplayName.class)
    class FindMaxCategoryOrder {

        @Test
        @DisplayName("[SCN-REP-CATEGORY-001][TC-MAX-ORDER-001] 사용자 카테고리와 기본 카테고리를을 합쳐 가장 큰 categoryOrder 값을 반환한다")
        void returnsMaxOrderAcrossUserAndSystem() {
            // given
            Long userId = 100L;

            seed(-1L, -1, "기본-전체");
            seed(-1L, 0, "기본-중요");
            seed(userId, 2, "me-2");
            seed(userId, 9, "me-9");
            seed(999L, 99, "other-99");

            // when
            Optional<Integer> maxOrder = jpaCategoryRepository.findMaxCategoryOrderByUserId(userId);

            // then
            assertThat(maxOrder).isPresent();
            assertThat(maxOrder.get()).isEqualTo(9);
        }

        @Test
        @DisplayName("[SCN-REPO-CATEGORY-001][TC-MAX-ORDER-002] 사용자 카테고리가 없으면 기본 카테고리 중 가장 큰 categoryOrder를 반환한다")
        void returnsEmptyWhenNoUserAndNoDefaultCategories() {
            // given
            Long userId = 200L;
            seed(-1L, -1, "기본-전체");
            seed(-1L, 0, "기본-중요");

            // when
            Optional<Integer> maxOrder = jpaCategoryRepository.findMaxCategoryOrderByUserId(userId);

            // then
            assertThat(maxOrder).isPresent();
            assertThat(maxOrder.get()).isEqualTo(0);
        }
    }
}
