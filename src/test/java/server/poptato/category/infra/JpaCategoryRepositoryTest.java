package server.poptato.category.infra;

import static org.assertj.core.api.Assertions.assertThat;

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

    private Boolean getIsDeleted(Long id) {
        Object result = tem.getEntityManager().createNativeQuery(
                "SELECT is_deleted FROM category WHERE id = :id"
        ).setParameter("id", id).getSingleResult();
        if (result instanceof Boolean) {
            return (Boolean) result;
        }
        return ((Number) result).intValue() == 1;
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

    @Nested
    @DisplayName("[SCN-REP-CATEGORY-003] Soft Delete 테스트")
    @TestMethodOrder(MethodOrderer.DisplayName.class)
    class SoftDeleteTests {

        private Category seedAndReturn(Long userId, int order, String name) {
            Category c = Category.builder()
                    .userId(userId)
                    .categoryOrder(order)
                    .emojiId(1L)
                    .name(name)
                    .build();
            tem.persist(c);
            tem.flush();
            tem.clear();
            return c;
        }

        @Test
        @DisplayName("[TC-SOFT-DELETE-001] softDeleteByUserId 호출 시 해당 유저의 모든 Category가 soft delete 된다")
        void softDeleteByUserId_deletesAllUserCategories() {
            // given
            Long userId = 500L;
            Long otherUserId = 600L;

            Category cat1 = seedAndReturn(userId, 1, "cat1");
            Category cat2 = seedAndReturn(userId, 2, "cat2");
            Category otherCat = seedAndReturn(otherUserId, 1, "other");

            // when
            jpaCategoryRepository.softDeleteByUserId(userId);
            tem.flush();
            tem.clear();

            // then - Native Query로 is_deleted 값 직접 확인 (@SQLRestriction 우회)
            assertThat(getIsDeleted(cat1.getId())).isTrue();
            assertThat(getIsDeleted(cat2.getId())).isTrue();
            assertThat(getIsDeleted(otherCat.getId())).isFalse();
        }

        @Test
        @DisplayName("[TC-SOFT-DELETE-002] soft delete된 Category는 findById로 조회되지 않는다")
        void findById_excludesSoftDeletedCategory() {
            // given
            Long userId = 700L;
            Category cat = seedAndReturn(userId, 1, "deleted-cat");
            Long catId = cat.getId();

            jpaCategoryRepository.softDeleteByUserId(userId);
            tem.flush();
            tem.clear();

            // when
            Optional<Category> found = jpaCategoryRepository.findById(catId);

            // then
            assertThat(found).isEmpty();
        }

        @Test
        @DisplayName("[TC-SOFT-DELETE-003] soft delete된 Category는 findDefaultAndByUserIdOrderByCategoryOrder에서 제외된다")
        void findDefaultAndByUserIdOrderByCategoryOrder_excludesSoftDeletedCategory() {
            // given
            Long userId = 800L;
            seedAndReturn(userId, 1, "active-cat");
            seedAndReturn(userId, 2, "deleted-cat");

            jpaCategoryRepository.softDeleteByUserId(userId);
            tem.flush();
            tem.clear();

            // 다시 active 카테고리 추가
            seedAndReturn(userId, 3, "new-active-cat");

            // when
            Page<Category> page = jpaCategoryRepository
                    .findDefaultAndByUserIdOrderByCategoryOrder(userId, PageRequest.of(0, 10));

            // then - soft delete된 카테고리는 제외되고 new-active-cat만 조회
            assertThat(page.getContent())
                    .extracting(Category::getName)
                    .doesNotContain("active-cat", "deleted-cat")
                    .contains("new-active-cat");
        }

        @Test
        @DisplayName("[TC-SOFT-DELETE-004] soft delete된 Category는 findMaxCategoryOrderByUserId에서 제외된다")
        void findMaxCategoryOrderByUserId_excludesSoftDeletedCategory() {
            // given
            Long userId = 900L;
            seed(-1L, -1, "기본-전체");
            seed(-1L, 0, "기본-중요");
            seedAndReturn(userId, 5, "active-cat");
            seedAndReturn(userId, 10, "to-be-deleted-cat");

            // soft delete 전 maxOrder = 10
            Optional<Integer> beforeDelete = jpaCategoryRepository.findMaxCategoryOrderByUserId(userId);
            assertThat(beforeDelete).contains(10);

            // when - order 10인 카테고리만 soft delete
            tem.getEntityManager().createNativeQuery(
                    "UPDATE category SET is_deleted = true WHERE user_id = :userId AND category_order = 10"
            ).setParameter("userId", userId).executeUpdate();
            tem.flush();
            tem.clear();

            // then - soft delete 후 maxOrder = 5
            Optional<Integer> afterDelete = jpaCategoryRepository.findMaxCategoryOrderByUserId(userId);
            assertThat(afterDelete).contains(5);
        }
    }
}
