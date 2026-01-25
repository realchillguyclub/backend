package server.poptato.todo.domain.entity;

import java.time.LocalDate;
import java.time.LocalTime;

import org.hibernate.annotations.SQLRestriction;

import jakarta.persistence.Column;
import jakarta.persistence.ConstraintMode;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.ForeignKey;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.Lob;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import server.poptato.category.domain.entity.Category;
import server.poptato.global.dao.BaseEntity;
import server.poptato.todo.domain.value.TodayStatus;
import server.poptato.todo.domain.value.Type;

@Getter
@Entity
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Table(name = "todo")
@SQLRestriction("is_deleted = false")
public class Todo extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "user_id", nullable = false)
    private Long userId;

    @Column(name = "category_id")
    private Long categoryId;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private Type type;

    @Lob
    @Column(name = "content", columnDefinition = "LONGTEXT", nullable = false)
    private String content;

    @Column(name = "time")
    private LocalTime time;

    @Column(name = "deadline")
    private LocalDate deadline;

    @Column(name = "is_bookmark", nullable = false)
    private boolean isBookmark;

    @Column(name = "is_repeat", nullable = false)
    private boolean isRepeat;

    @Column(name = "is_routine", nullable = false)
    private boolean isRoutine;

    @Column(name = "is_event", nullable = false)
    private boolean isEvent;

    @Column(name = "today_date")
    private LocalDate todayDate;

    @Enumerated(EnumType.STRING)
    @Column(name = "today_status")
    private TodayStatus todayStatus;

    @Column(name = "today_order")
    private Integer todayOrder;

    @Column(name = "backlog_order")
    private Integer backlogOrder;

    @Column(name = "is_deleted", nullable = false)
    private boolean isDeleted = false;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "category_id", insertable = false, updatable = false, foreignKey = @ForeignKey(ConstraintMode.NO_CONSTRAINT))
    private Category category;

    @Builder
    public Todo(Long userId, Long categoryId, Type type, String content, LocalTime time, LocalDate deadline,
                boolean isBookmark, boolean isRepeat, boolean isRoutine, boolean isEvent, LocalDate todayDate, TodayStatus todayStatus,
                Integer todayOrder, Integer backlogOrder) {
        this.userId = userId;
        this.categoryId = categoryId;
        this.type = type;
        this.content = content;
        this.time = time;
        this.deadline = deadline;
        this.isBookmark = isBookmark;
        this.isRepeat = isRepeat;
        this.isRoutine = isRoutine;
        this.isEvent = isEvent;
        this.todayDate = todayDate;
        this.todayStatus = todayStatus;
        this.todayOrder = todayOrder;
        this.backlogOrder = backlogOrder;
    }

    private static Todo.TodoBuilder baseBacklogBuilder(Long userId, String content, Integer backlogOrder) {
        return Todo.builder()
                .userId(userId)
                .content(content)
                .backlogOrder(backlogOrder)
                .type(Type.BACKLOG);
    }

    public static Todo createBacklog(Long userId, String content, Integer backlogOrder) {
        return baseBacklogBuilder(userId, content, backlogOrder).build();
    }

    public static Todo createBookmarkBacklog(Long userId, String content, Integer backlogOrder) {
        return baseBacklogBuilder(userId, content, backlogOrder)
                .isBookmark(true)
                .build();
    }

    public static Todo createCategoryBacklog(Long userId, Long categoryId, String content, Integer backlogOrder) {
        return baseBacklogBuilder(userId, content, backlogOrder)
                .categoryId(categoryId)
                .build();
    }

    public static Todo createYesterdayBacklog(Long userId, String content, Integer backlogOrder) {
        return Todo.builder()
                .userId(userId)
                .content(content)
                .backlogOrder(backlogOrder)
                .type(Type.YESTERDAY)
                .todayDate(LocalDate.now().minusDays(1))
                .todayStatus(TodayStatus.INCOMPLETE)
                .build();
    }

    public static Todo createTodayTodo(Long userId, String content, LocalTime time, boolean isBookmark, Integer todayOrder) {
        return Todo.builder()
                .userId(userId)
                .content(content)
                .time(time)
                .isBookmark(isBookmark)
                .isEvent(true)
                .type(Type.TODAY)
                .todayDate(LocalDate.now())
                .todayStatus(TodayStatus.INCOMPLETE)
                .todayOrder(todayOrder)
                .build();
    }

    public void changeToToday(Integer maxTodayOrder) {
        this.type = Type.TODAY;
        this.backlogOrder = null;
        this.todayOrder = maxTodayOrder + 1;
        this.todayStatus = TodayStatus.INCOMPLETE;
        this.todayDate = LocalDate.now();
    }

    public void changeToBacklog(Integer maxBacklogOrder) {
        this.type = Type.BACKLOG;
        this.backlogOrder = maxBacklogOrder + 1;
        this.todayOrder = null;
        this.todayStatus = null;
        this.todayDate = null;
    }

    public void toggleBookmark() {
        this.isBookmark = !this.isBookmark;
    }

    public void updateTime(LocalTime time) {
        this.time = time;
    }

    public void updateDeadline(LocalDate deadline) {
        this.deadline = deadline;
    }

    public void updateContent(String content) {
        this.content = content;
    }

    public void incompleteTodayTodo(Integer minTodayOrder) {
        this.todayStatus = TodayStatus.INCOMPLETE;
        this.todayOrder = minTodayOrder - 1;
    }

    public void completeTodayTodo() {
        this.todayStatus = TodayStatus.COMPLETED;
        this.todayOrder = null;
    }

    public void updateYesterdayToCompleted() {
        this.todayStatus = TodayStatus.COMPLETED;
        this.backlogOrder = null;
    }

    public void updateCategory(Long categoryId) {
        this.categoryId = categoryId;
    }

    public void toggleRepeat() {
        this.isRepeat = !this.isRepeat;
    }

    public void setRepeat(boolean isRepeat) {
        this.isRepeat = isRepeat;
    }

    public void setRoutine(boolean isRoutine) {
        this.isRoutine = isRoutine;
    }

    public void updateTodayStatus(TodayStatus todayStatus) {
        this.todayStatus = todayStatus;
    }

    public void updateType(Type type) {
        this.type = type;
    }

    public void updateTodayOrder(Integer order) {
        this.todayOrder = order;
    }

    public void updateBacklogOrder(Integer order) {
        this.backlogOrder = order;
    }
}
