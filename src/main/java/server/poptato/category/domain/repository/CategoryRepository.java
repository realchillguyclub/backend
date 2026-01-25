package server.poptato.category.domain.repository;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import server.poptato.category.domain.entity.Category;

import java.util.Optional;

public interface CategoryRepository {

    Optional<Integer> findMaxCategoryOrderByUserId(Long userId);

    Category save(Category category);

    Page<Category> findDefaultAndByUserIdOrderByCategoryOrder(Long userId, Pageable pageable);

    Optional<Category> findById(Long categoryId);

    void softDeleteById(Long categoryId);

    void softDeleteByUserId(Long userId);

    default Page<Category> findCategories(Long userId, Pageable pageable) {
        return findDefaultAndByUserIdOrderByCategoryOrder(userId, pageable);
    }
}
