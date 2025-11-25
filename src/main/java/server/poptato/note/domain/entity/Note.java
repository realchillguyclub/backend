package server.poptato.note.domain.entity;

import jakarta.persistence.*;
import jakarta.validation.constraints.Size;
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
    @Column(name = "content")
    @Size(max = 10000, message = "내용은 최대 10000자까지 입력 가능합니다.")
    private String content;

    @Column(name = "user_id", nullable = false)
    private Long userId;

    @Builder
    public Note(String title, String content, Long userId) {
        this.title = title;
        this.content = content;
        this.userId = userId;
    }
}
