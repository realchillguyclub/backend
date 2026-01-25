package server.poptato.user.domain.entity;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import server.poptato.user.domain.value.SocialType;

class UserTest {

    @Test
    @DisplayName("[TC-ENTITY-001] isDeleted 기본값은 false이다")
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
