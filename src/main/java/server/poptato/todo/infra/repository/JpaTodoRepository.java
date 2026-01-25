package server.poptato.todo.infra.repository;

import jakarta.persistence.Tuple;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import server.poptato.todo.domain.entity.Todo;
import server.poptato.todo.domain.value.TodayStatus;
import server.poptato.todo.domain.value.Type;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

public interface JpaTodoRepository extends JpaRepository<Todo, Long> {

    @Query("""
        SELECT t
        FROM Todo t
        JOIN CompletedDateTime c ON t.id = c.todoId
        WHERE t.userId = :userId
          AND t.type = 'TODAY'
          AND t.todayStatus = 'COMPLETED'
          AND FUNCTION('DATE', c.dateTime) = :todayDate
        ORDER BY c.dateTime ASC
    """)
    List<Todo> findCompletedTodays(
            @Param("userId") Long userId,
            @Param("todayDate") LocalDate todayDate
    );

    @EntityGraph(attributePaths = {"category", "category.emoji"})
    @Query("""
        SELECT t
        FROM Todo t
        WHERE t.userId = :userId
          AND t.type = :type
          AND t.todayDate = :todayDate
          AND t.todayStatus = :todayStatus
        ORDER BY t.todayOrder DESC
    """)
    List<Todo> findIncompleteTodays(
            @Param("userId") Long userId,
            @Param("type") Type type,
            @Param("todayDate") LocalDate todayDate,
            @Param("todayStatus") TodayStatus todayStatus
    );

    @Query("""
        SELECT t
        FROM Todo t
        WHERE t.userId = :userId
          AND t.type = :type
          AND t.todayDate = :todayDate
          AND t.todayStatus = :todayStatus
        ORDER BY t.todayOrder DESC
    """)
    List<Todo> findIncompleteTodaysWithCategory(
            @Param("userId") Long userId,
            @Param("type") Type type,
            @Param("todayDate") LocalDate todayDate,
            @Param("todayStatus") TodayStatus todayStatus
    );

    @EntityGraph(attributePaths = {"category", "category.emoji"})
    @Query("""
        SELECT t
        FROM Todo t
        JOIN CompletedDateTime c ON t.id = c.todoId
        WHERE t.userId = :userId
          AND t.type = 'TODAY'
          AND t.todayStatus = 'COMPLETED'
          AND FUNCTION('DATE', c.dateTime) = :todayDate
        ORDER BY c.dateTime ASC
    """)
    List<Todo> findCompletedTodaysWithCategory(
            @Param("userId") Long userId,
            @Param("todayDate") LocalDate todayDate
    );

    @Query("""
        SELECT t
        FROM Todo t
        WHERE t.userId = :userId
          AND t.type = :type
          AND t.todayStatus = :todayStatus
    """)
    Page<Todo> findByUserIdAndTypeAndTodayStatus(
            @Param("userId") Long userId,
            @Param("type") Type type,
            @Param("todayStatus") TodayStatus todayStatus,
            Pageable pageable
    );

    @Query("""
        SELECT t
        FROM Todo t
        WHERE t.id = :id
    """)
    Optional<Todo> findById(@Param("id") Long id);

    @Query("""
        SELECT COALESCE(MAX(t.todayOrder), 0)
        FROM Todo t
        WHERE t.userId = :userId
          AND t.todayOrder IS NOT NULL
    """)
    Integer findMaxTodayOrderByUserIdOrZero(@Param("userId") Long userId);

    @Query("""
        SELECT COALESCE(MIN(t.todayOrder), 0)
        FROM Todo t
        WHERE t.userId = :userId
          AND t.todayOrder IS NOT NULL
    """)
    Integer findMinTodayOrderByUserIdOrZero(@Param("userId") Long userId);

    @Query("""
        SELECT COALESCE(MAX(t.backlogOrder), 0)
        FROM Todo t
        WHERE t.userId = :userId
          AND t.backlogOrder IS NOT NULL
    """)
    Integer findMaxBacklogOrderByUserIdOrZero(@Param("userId") Long userId);

    @EntityGraph(attributePaths = {"category", "category.emoji"})
    @Query("""
        SELECT t
        FROM Todo t
        WHERE t.userId = :userId
          AND t.type = :type
          AND (t.todayStatus != :status OR t.todayStatus IS NULL)
        ORDER BY t.backlogOrder DESC
    """)
    Page<Todo> findAllBacklogs(
            @Param("userId") Long userId,
            @Param("type") Type type,
            @Param("status") TodayStatus status,
            Pageable pageable
    );

    @Query("""
        SELECT t
        FROM Todo t
        WHERE t.userId = :userId
          AND t.deadline = :localDate
          AND t.type IN ('BACKLOG', 'YESTERDAY')
    """)
    Page<Todo> findDeadlineBacklogs(
            @Param("userId") Long userId,
            @Param("localDate") LocalDate localDate,
            Pageable pageable
    );

    @EntityGraph(attributePaths = {"category", "category.emoji"})
    @Query("""
        SELECT t
        FROM Todo t
        WHERE t.userId = :userId
          AND t.isBookmark = true
          AND t.type = :type
          AND (t.todayStatus != :status OR t.todayStatus IS NULL)
        ORDER BY t.backlogOrder DESC
    """)
    Page<Todo> findBookmarkBacklogs(
            @Param("userId") Long userId,
            @Param("type") Type type,
            @Param("status") TodayStatus status,
            Pageable pageable
    );

    @EntityGraph(attributePaths = {"category", "category.emoji"})
    @Query("""
        SELECT t
        FROM Todo t
        WHERE t.userId = :userId
          AND t.categoryId = :categoryId
          AND t.type = :type
          AND (t.todayStatus != :status OR t.todayStatus IS NULL)
        ORDER BY t.backlogOrder DESC
    """)
    Page<Todo> findBacklogsByCategoryId(
            @Param("userId") Long userId,
            @Param("categoryId") Long categoryId,
            @Param("type") Type type,
            @Param("status") TodayStatus status,
            Pageable pageable
    );

    @Query("""
        SELECT t
        FROM Todo t
        WHERE t.id IN (
            SELECT c.todoId
            FROM CompletedDateTime c
            WHERE DATE(c.dateTime) = :localDate
        )
          AND t.userId = :userId
        ORDER BY (
            SELECT c.dateTime
            FROM CompletedDateTime c
            WHERE c.todoId = t.id
              AND DATE(c.dateTime) = :localDate
        ) ASC
    """)
    Page<Todo> findHistories(
            @Param("userId") Long userId,
            @Param("localDate") LocalDate localDate,
            Pageable pageable
    );

    @Query("""
        SELECT t
        FROM Todo t
        WHERE t.type = :type
    """)
    List<Todo> findByType(@Param("type") Type type);

    void deleteAllByCategoryId(Long categoryId);

    @Modifying(clearAutomatically = true, flushAutomatically = true)
    @Query("""
        UPDATE Todo t
        SET t.isDeleted = true
        WHERE t.id = :todoId
    """)
    void softDeleteById(@Param("todoId") Long todoId);

    @Modifying(clearAutomatically = true, flushAutomatically = true)
    @Query("""
        UPDATE Todo t
        SET t.isDeleted = true
        WHERE t.id IN :todoIds
    """)
    void softDeleteByIds(@Param("todoIds") List<Long> todoIds);

    @Modifying(clearAutomatically = true, flushAutomatically = true)
    @Query("""
        UPDATE Todo t
        SET t.isDeleted = true
        WHERE t.userId = :userId
    """)
    void softDeleteByUserId(@Param("userId") Long userId);

    @Modifying(clearAutomatically = true, flushAutomatically = true)
    @Query("""
        UPDATE Todo t
        SET t.isDeleted = true, t.categoryId = null
        WHERE t.categoryId = :categoryId
    """)
    void softDeleteByCategoryId(@Param("categoryId") Long categoryId);

    @Query("""
        SELECT t FROM Todo t
        WHERE t.userId = :userId
          AND t.todayStatus = :todayStatus
          AND t.type = 'TODAY'
    """)
    List<Todo> findIncompleteTodayTodos(
            @Param("userId") Long userId,
            @Param("todayStatus") TodayStatus todayStatus
    );

    @Query("""
        SELECT t FROM Todo t
        WHERE t.type = 'BACKLOG'
          AND t.userId = :userId
          AND t.deadline = :deadline
    """)
    List<Todo> findTodosByDeadLine(
            @Param("userId") Long userId,
            @Param("deadline") LocalDate deadline
    );

    @Query(value = """
        SELECT t.* FROM todo t
        JOIN routine r ON t.id = r.todo_id
        WHERE t.type = 'BACKLOG'
          AND r.day = :todayDay
          AND t.user_id = :userId
          AND t.is_deleted = false
    """, nativeQuery = true)
    List<Todo> findRoutineTodosByDay(
            @Param("userId") Long userId,
            @Param("todayDay") String todayDay
    );

    @Query("""
        SELECT t
        FROM Todo t
        WHERE t.userId = :userId
          AND t.type = 'YESTERDAY'
          AND t.todayStatus = 'INCOMPLETE'
        ORDER BY t.todayOrder DESC
    """)
    List<Todo> findIncompleteYesterdays(@Param("userId") Long userId);

    @Query(value = """
        SELECT t.deadline AS date, COUNT(*) AS count
        FROM todo t
        WHERE t.user_id = :userId
          AND t.deadline IS NOT NULL
          AND t.deadline > CURDATE()
          AND YEAR(t.deadline) = :year
          AND MONTH(t.deadline) = :month
          AND t.type = 'BACKLOG'
          AND t.is_deleted = false
        GROUP BY t.deadline
        ORDER BY t.deadline
    """, nativeQuery = true)
    List<Tuple> findDatesWithBacklogCount(
            @Param("userId") Long userId,
            @Param("year") String year,
            @Param("month") int month
    );

    @Query("""
        SELECT CASE WHEN COUNT(t) > 0 THEN true ELSE false END
        FROM Todo t
        WHERE t.userId = :userId
          AND t.type = :type
          AND t.todayStatus = :todayStatus
    """)
    boolean existsByUserIdAndTypeAndTodayStatus(
            @Param("userId") Long userId,
            @Param("type") Type type,
            @Param("todayStatus") TodayStatus todayStatus
    );

    @Query(value = """
        SELECT t.user_id AS userId, COALESCE(MAX(t.today_order), 0) AS maxOrder
        FROM todo t
        WHERE t.user_id IN (:userIds)
          AND t.is_deleted = false
        GROUP BY t.user_id
    """, nativeQuery = true)
    List<Tuple> findMaxTodayOrdersByUserIds(@Param("userIds") List<Long> userIds);
}
