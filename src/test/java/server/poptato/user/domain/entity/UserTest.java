package server.poptato.user.domain.entity;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import server.poptato.user.domain.value.SocialType;

class UserTest {

    @Test
    @DisplayName("[TC-ENTITY-001] softDelete 호출 시 isDeleted가 true로 변경되고 socialId가 DELETED_ 접두사로 변경된다")
    void softDelete_setsIsDeletedTrueAndChangesSocialId() {
        // given
        String originalSocialId = "kakao_12345";
        User user = User.builder()
                .socialType(SocialType.KAKAO)
                .socialId(originalSocialId)
                .name("test")
                .email("test@test.com")
                .isPushAlarm(true)
                .build();

        // when
        user.softDelete();

        // then
        assertThat(user.isDeleted()).isTrue();
        assertThat(user.getSocialId()).startsWith("DELETED_");
        assertThat(user.getSocialId()).endsWith("_" + originalSocialId);
    }

    @Test
    @DisplayName("[TC-ENTITY-002] isDeleted 기본값은 false이다")
    void isDeleted_defaultIsFalse() {
        // given
        User user = User.builder()
                .socialType(SocialType.KAKAO)
                .socialId("kakao_12345")
                .name("test")
                .email("test@test.com")
                .isPushAlarm(true)
                .build();

        // then
        assertThat(user.isDeleted()).isFalse();
    }
}
