package server.poptato.infra.oauth.state;

import org.junit.jupiter.api.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Import;
import server.poptato.configuration.RedisTestConfig;

import java.time.Duration;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;

@Import(OAuthStateRepository.class)
class OAuthStateRepositoryTest extends RedisTestConfig {

    @Autowired
    private OAuthStateRepository oAuthStateRepository;

    @Nested
    @TestMethodOrder(MethodOrderer.DisplayName.class)
    @DisplayName("[SCN-REPO-OAUTH-STATE-001] state로 키를 생성하여 code_verifier 값을 저장한다.")
    class SaveFindAndTtl {

        @Test
        @DisplayName("[TC-SAVE-AND-FIND-001] 저장 후 조회 시 동일한 state와 code_verifier가 복원된다")
        void save_then_find_returns_same_values() {
            // given
            OAuthState input = OAuthState.builder()
                    .state("test-state")
                    .codeVerifier("test-code-verifier")
                    .build();

            // when
            oAuthStateRepository.save(input, Duration.ofSeconds(30));

            // then
            Optional<OAuthState> found = oAuthStateRepository.find("test-state");
            assertThat(found).isPresent();
            assertThat(found.get().getState()).isEqualTo("test-state");
            assertThat(found.get().getCodeVerifier()).isEqualTo("test-code-verifier");
        }

        @Test
        @DisplayName("[TC-TTL-EXPIRE-001] TTL이 만료된 후에는 조회 시 Optional.empty()를 반환한다")
        void ttl_expire_then_find_empty() throws InterruptedException {
            // given
            OAuthState input = OAuthState.builder()
                    .state("state-ttl-expire")
                    .codeVerifier("test-code-verifier")
                    .build();

            // when
            oAuthStateRepository.save(input, Duration.ofSeconds(1));

            // sanity: 저장 직후에는 존재
            assertThat(oAuthStateRepository.find("state-ttl-expire")).isPresent();

            // then: TTL 만료 여유를 두고 대기
            Thread.sleep(1500);

            Optional<OAuthState> afterTtl = oAuthStateRepository.find("test-code-verifier");
            assertThat(afterTtl).isEmpty();
        }
    }
}
