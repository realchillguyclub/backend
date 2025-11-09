package server.poptato.user.domain.entity;

import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import server.poptato.auth.api.request.LoginRequestDto;
import server.poptato.global.dao.BaseEntity;
import server.poptato.user.domain.value.MobileType;

@Getter
@Entity
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Table(name = "mobile")
public class Mobile extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "user_id", nullable = false)
    private Long userId;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private MobileType type;

    @Lob
    @Column(name = "client_id", columnDefinition = "VARCHAR(256)")
    private String clientId;

    @Transient
    private boolean dirtyFlag = false;

    @Builder
    public Mobile(Long userId, MobileType type, String clientId) {
        this.userId = userId;
        this.type = type;
        this.clientId = clientId;
    }

    public void updateModifiedDate() {
        this.dirtyFlag = !this.dirtyFlag;
    }

    public static Mobile createMobile(LoginRequestDto requestDto, Long userId) {
        return Mobile.builder()
                .userId(userId)
                .type(requestDto.mobileType())
                .clientId(requestDto.clientId())
                .build();
    }
}
