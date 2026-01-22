package server.poptato.user.domain.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import server.poptato.auth.api.request.LoginRequestDto;
import server.poptato.global.dao.BaseEntity;
import server.poptato.infra.oauth.SocialUserInfo;
import server.poptato.user.domain.value.SocialType;

@Getter
@Entity
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Table(name = "users")
public class User extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Enumerated(EnumType.STRING)
    @Column(name = "social_type", nullable = false)
    private SocialType socialType;

    @Column(name = "social_id", nullable = false)
    private String socialId;

    @Column(nullable = false)
    private String name;

    @Column(nullable = false)
    private String email;

    @Column(name = "image_url")
    private String imageUrl;

    @Column(name = "is_push_alarm", nullable = false)
    private Boolean isPushAlarm;

    @Column(name = "is_deleted", nullable = false)
    private boolean isDeleted = false;

    @Builder
    public User(SocialType socialType, String socialId, String name, String email, String imageUrl, Boolean isPushAlarm) {
        this.socialType = socialType;
        this.socialId = socialId;
        this.name = name;
        this.email = email;
        this.imageUrl = imageUrl;
        this.isPushAlarm = isPushAlarm;
    }

    public void updateImageUrl(String imageUrl) {
        this.imageUrl = imageUrl;
    }

    public static User createUser(LoginRequestDto request, SocialUserInfo userInfo, String imageUrl) {
        return User.builder()
                .socialType(request.socialType())
                .socialId(userInfo.socialId())
                .name(userInfo.name())
                .email(userInfo.email())
                .imageUrl(imageUrl)
                .isPushAlarm(true)
                .build();
    }
    
    /**
     * 탈퇴 시, socialId를 변경하여 동일 소셜 계정으로 재가입 가능하도록 함.
     */
    public void softDelete() {
		this.socialId = "DELETED_" + System.currentTimeMillis() + "_" + this.socialId;
		this.isDeleted = true;
    }
}
