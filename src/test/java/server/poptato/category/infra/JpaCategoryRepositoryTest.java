package server.poptato.category.infra;

import static org.assertj.core.api.Assertions.*;

import java.util.Optional;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.MethodOrderer;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestMethodOrder;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;

import server.poptato.category.domain.entity.Category;
import server.poptato.configuration.DatabaseTestConfig;
import server.poptato.configuration.MySqlDataJpaTest;

@MySqlDataJpaTest
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
            assertThat(maxOrder).isPresent().contains(9);
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
            assertThat(maxOrder.get()).isZero();
        }
    }

    @Nested
    @DisplayName("[SCN-REP-CATEGORY-002] 기본(-1,0)과 사용자 카테고리를 categoryOrder 오름차순으로 페이지 조회한다")
    @TestMethodOrder(MethodOrderer.DisplayName.class)
    class FindDefaultAndByUserIdOrderedPage {

        @Test
        @DisplayName("[SCN-REP-CATEGORY-002][TC-PAGE-001] 기본(-1,0)과 사용자 카테고리를 합쳐 categoryOrder 오름차순으로 반환한다")
        void returnsOrderedAsc_includingDefaultAndUser_excludingOthers() {
            // given
            Long userId = 300L;
            seed(-1L, -1, "기본-전체");
            seed(-1L,  0, "기본-중요");
            seed(userId, 1, "me-1");
            seed(userId, 3, "me-3");
            seed(userId, 2, "me-2");
            seed(999L, 99, "other-99");

            // when
            Page<Category> page = jpaCategoryRepository
                    .findDefaultAndByUserIdOrderByCategoryOrder(userId, PageRequest.of(0, 10));

            // then
            assertThat(page.getContent()).extracting(Category::getUserId)
                    .allMatch(uid -> uid.equals(userId) || uid == -1L);
            assertThat(page.getContent()).extracting(Category::getCategoryOrder)
                    .containsExactly(-1, 0, 1, 2, 3);
            assertThat(page.getTotalElements()).isEqualTo(5);
        }

        @Test
        @DisplayName("[SCN-REP-CATEGORY-002][TC-PAGE-002] 조회 대상이 없으면 기본 카테고리(-1,0)만 반환한다")
        void returnsEmptyPage_whenNoDefaultAndNoUser() {
            // given
            Long userId = 400L;
            seed(-1L, -1, "기본-전체");
            seed(-1L,  0, "기본-중요");


            // when
            Page<Category> page = jpaCategoryRepository
                    .findDefaultAndByUserIdOrderByCategoryOrder(userId, PageRequest.of(0, 5));

            // then
            assertThat(page.getContent()).extracting(Category::getUserId)
                    .allMatch(uid -> uid == -1L);
            assertThat(page.getContent()).extracting(Category::getCategoryOrder)
                    .containsExactly(-1, 0);
            assertThat(page.getTotalElements()).isEqualTo(2);
            assertThat(page.getTotalPages()).isEqualTo(1);
        }
    }
}
