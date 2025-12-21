package server.poptato.note.domain.entity;

import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import server.poptato.global.dao.BaseEntity;

@Getter
@Entity
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Table(name = "note")
public class Note extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "title")
    private String title;

    @Lob
    @Column(name = "content", columnDefinition = "TEXT", length = 10_000)
    private String content;

    @Column(name = "user_id", nullable = false)
    private Long userId;

    @Builder
    public Note(Long userId) {
        this.userId = userId;
    }

    public void update(String title, String content) {
        this.title = title;
        this.content = content;
    }
}
