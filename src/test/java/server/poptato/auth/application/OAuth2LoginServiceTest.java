package server.poptato.auth.application;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

import java.time.Duration;
import java.util.Optional;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.MethodOrderer;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestMethodOrder;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.util.MultiValueMap;
import org.springframework.web.util.UriComponentsBuilder;

import server.poptato.auth.api.request.LoginRequestDto;
import server.poptato.auth.application.response.AuthorizeUrlResponseDto;
import server.poptato.auth.application.response.LoginResponseDto;
import server.poptato.auth.application.response.OAuthCallbackResult;
import server.poptato.auth.application.service.AuthService;
import server.poptato.auth.application.service.OAuth2LoginService;
import server.poptato.configuration.ServiceTestConfig;
import server.poptato.infra.oauth.kakao.KakaoSocialService;
import server.poptato.infra.oauth.pending.DesktopPendingLoginRepository;
import server.poptato.infra.oauth.pending.PendingLogin;
import server.poptato.infra.oauth.state.OAuthState;
import server.poptato.infra.oauth.state.OAuthStateRepository;
import server.poptato.infra.oauth.state.PKCEUtils;
import server.poptato.user.domain.value.MobileType;
import server.poptato.user.domain.value.SocialType;

class OAuth2LoginServiceTest extends ServiceTestConfig {

    @Mock
    OAuthStateRepository oAuthStateRepository;

    @Mock
    DesktopPendingLoginRepository desktopPendingLoginRepository;

    @Mock
    KakaoSocialService kakaoSocialService;

    @Mock
    AuthService authService;

    @InjectMocks
    OAuth2LoginService oAuth2LoginService;

    @Test
    @DisplayName("[SCN-SVC-OAUTH2-001][TC-KAKAO-AUTHORIZATION-001] state/PKCE(code_verifier)가 저장되고 쿼리파라미터가 정확히 구성된 authorize URL을 반환한다")
    void buildAuthorizeRedirectForKakao_정상_URL_생성_state_PKCE_저장() {
        // given - @Value 주입 필드 설정
        String scope = "test-scope";
        String clientId = "test-kakao-client-id";
        String redirectUri = "https://my.app.com/oauth/kakao/callback";
        String authorizeUri = "https://kauth.kakao.com/oauth/authorize";

        ReflectionTestUtils.setField(oAuth2LoginService, "defaultScope", scope);
        ReflectionTestUtils.setField(oAuth2LoginService, "clientId", clientId);
        ReflectionTestUtils.setField(oAuth2LoginService, "redirectUri", redirectUri);
        ReflectionTestUtils.setField(oAuth2LoginService, "authorizeUri", authorizeUri);

        ArgumentCaptor<OAuthState> stateCaptor = ArgumentCaptor.forClass(OAuthState.class);
        ArgumentCaptor<Duration> ttlCaptor = ArgumentCaptor.forClass(Duration.class);

        doNothing().when(oAuthStateRepository).save(stateCaptor.capture(), ttlCaptor.capture());

        // when
        AuthorizeUrlResponseDto response = oAuth2LoginService.buildAuthorizeRedirectForKakao();

        // then - 저장 검증
        verify(oAuthStateRepository).save(any(OAuthState.class), any(Duration.class));
        OAuthState saved = stateCaptor.getValue();
        Duration ttl = ttlCaptor.getValue();

        assertThat(saved).isNotNull();
        assertThat(saved.getState()).isNotBlank();
        assertThat(saved.getCodeVerifier()).isNotBlank();
        assertThat(ttl).isEqualTo(Duration.ofMinutes(10));

        // then - URL 쿼리 파싱 및 검증
        MultiValueMap<String, String> params =
                UriComponentsBuilder.fromUriString(response.authorizeUrl()).build(true).getQueryParams();

        assertThat(response.authorizeUrl()).startsWith(authorizeUri);
        assertThat(params.getFirst("response_type")).isEqualTo("code");
        assertThat(params.getFirst("scope")).isEqualTo(scope);
        assertThat(params.getFirst("client_id")).isEqualTo(clientId);
        assertThat(params.getFirst("redirect_uri")).isEqualTo(redirectUri);
        assertThat(params.getFirst("state")).isEqualTo(saved.getState());
        assertThat(params.getFirst("code_challenge_method")).isEqualTo("S256");

        String expectedChallenge = PKCEUtils.generateCodeChallenge(saved.getCodeVerifier());
        assertThat(params.getFirst("code_challenge")).isEqualTo(expectedChallenge);
    }

    @Nested
    @TestMethodOrder(MethodOrderer.DisplayName.class)
    @DisplayName("[SCN-SVC-OAUTH2-002] 카카오 callback을 처리한다.")
    class KakaoCallback {

        private void injectValues() {
            ReflectionTestUtils.setField(oAuth2LoginService, "clientId", "test-kakao-client-id");
            ReflectionTestUtils.setField(oAuth2LoginService, "redirectUri", "https://your.app.com/oauth/kakao/callback");
            ReflectionTestUtils.setField(oAuth2LoginService, "authorizeUri", "https://kauth.kakao.com/oauth/authorize");
        }

        @Test
        @DisplayName("[TC-CALLBACK-001] 유효한 state와 code가 전달되면 토큰을 교환하고 pending 저장 후 SUCCESS를 반환하며 state를 삭제한다")
        void handleKakaoCallback_성공() {
            // given
            injectValues();
            String code = "authorization-code";
            String state = "state-UUID";
            String verifier = "code-verifier";

            OAuthState saved = OAuthState.builder().state(state).codeVerifier(verifier).build();
            when(oAuthStateRepository.find(state)).thenReturn(Optional.of(saved));

            ArgumentCaptor<String> clientIdCaptor = ArgumentCaptor.forClass(String.class);
            ArgumentCaptor<String> redirectUriCaptor = ArgumentCaptor.forClass(String.class);
            ArgumentCaptor<String> codeCaptor = ArgumentCaptor.forClass(String.class);
            ArgumentCaptor<String> verifierCaptor = ArgumentCaptor.forClass(String.class);

            String kakaoAccessToken = "kakao-user-access-token";
            when(kakaoSocialService.getKakaoUserAccessToken(
                    clientIdCaptor.capture(),
                    redirectUriCaptor.capture(),
                    codeCaptor.capture(),
                    verifierCaptor.capture()
            )).thenReturn(kakaoAccessToken);

            ArgumentCaptor<String> stateCaptor = ArgumentCaptor.forClass(String.class);
            ArgumentCaptor<String> accessTokenCaptor = ArgumentCaptor.forClass(String.class);
            ArgumentCaptor<Duration> ttlCaptor = ArgumentCaptor.forClass(Duration.class);

            doNothing().when(desktopPendingLoginRepository)
                    .save(stateCaptor.capture(), accessTokenCaptor.capture(), ttlCaptor.capture());

            // when
            OAuthCallbackResult result = oAuth2LoginService.handleKakaoCallback(code, state, null);

            // then - 결과 enum
            assertThat(result).isEqualTo(OAuthCallbackResult.SUCCESS);

            // then - 카카오 토큰 교환 요청 파라미터 검증
            assertThat(clientIdCaptor.getValue()).isEqualTo("test-kakao-client-id");
            assertThat(redirectUriCaptor.getValue()).isEqualTo("https://your.app.com/oauth/kakao/callback");
            assertThat(codeCaptor.getValue()).isEqualTo(code);
            assertThat(verifierCaptor.getValue()).isEqualTo(verifier);

            // then - pending 저장 검증
            assertThat(stateCaptor.getValue()).isEqualTo(state);
            assertThat(accessTokenCaptor.getValue()).isEqualTo(kakaoAccessToken);
            assertThat(ttlCaptor.getValue()).isEqualTo(Duration.ofMinutes(3)); // PENDING_TTL

            // then - state 삭제 호출
            verify(oAuthStateRepository).delete(state);

            // then - authService.login()은 콜백 단계에서 호출되지 않음
            verifyNoInteractions(authService);
        }

        @Test
        @DisplayName("[TC-CALLBACK-002] 유효하지 않은 state가 전달되면 ERROR를 반환하고 토큰 교환이나 pending 저장, 로그인은 수행되지 않지만 state 삭제는 시도한다")
        void handleKakaoCallback_state_invalid() {
            // given
            injectValues();
            String code = "authorization-code";
            String badState = "bad-state";

            when(oAuthStateRepository.find(badState)).thenReturn(Optional.empty());

            // when
            OAuthCallbackResult result = oAuth2LoginService.handleKakaoCallback(code, badState, null);

            // then - 결과 enum
            assertThat(result).isEqualTo(OAuthCallbackResult.ERROR);

            // then - 카카오 토큰 교환 / pending / 로그인은 호출되지 않음
            verifyNoInteractions(kakaoSocialService, desktopPendingLoginRepository, authService);

            // then - state 삭제는 finally 에서 호출됨
            verify(oAuthStateRepository).delete(badState);
        }

        @Test
        @DisplayName("[TC-CALLBACK-003] error 파라미터가 존재하면 CANCELED를 반환하고 state를 삭제하며 토큰 교환과 pending 저장, 로그인은 수행되지 않는다")
        void handleKakaoCallback_error_param() {
            // given
            injectValues();
            String code = "authorization-code"; // 의미 없음
            String state = "state-to-clean";
            String error = "access_denied";

            // when
            OAuthCallbackResult result = oAuth2LoginService.handleKakaoCallback(code, state, error);

            // then
            assertThat(result).isEqualTo(OAuthCallbackResult.CANCELED);

            // error 분기에서는 state 정리만 수행
            verify(oAuthStateRepository).delete(state);
            verifyNoInteractions(kakaoSocialService, desktopPendingLoginRepository, authService);
        }
    }

    @Nested
    @TestMethodOrder(MethodOrderer.DisplayName.class)
    @DisplayName("[SCN-SVC-OAUTH2-003] 데스크탑 전용 폴링 로직을 처리한다.")
    class DesktopPoll {

        private static final String CLIENT_IP = "127.0.0.1";
        private static final String USER_AGENT = "TestAgent/1.0";

        @Test
        @DisplayName("[TC-POLL-001] pending 상태가 존재하면 삭제 후 AuthService.login을 호출하고 LoginResponseDto를 반환한다")
        void pollDesktopLogin_success() {
            // given
            String state = "state-UUID";
            String kakaoAccessToken = "kakao-access-token";

            PendingLogin pendingLogin = new PendingLogin(SocialType.KAKAO, kakaoAccessToken);
            when(desktopPendingLoginRepository.find(state)).thenReturn(Optional.of(pendingLogin));

            LoginResponseDto expected = LoginResponseDto.of("access-token", "refresh-token", false, 123L);
            ArgumentCaptor<LoginRequestDto> requestCaptor = ArgumentCaptor.forClass(LoginRequestDto.class);
            when(authService.login(requestCaptor.capture(), eq(CLIENT_IP), eq(USER_AGENT))).thenReturn(expected);

            // when
            Optional<LoginResponseDto> result = oAuth2LoginService.pollDesktopLogin(state, CLIENT_IP, USER_AGENT);

            // then
            assertThat(result).isPresent();
            assertThat(result.get()).isSameAs(expected);

            // pending 삭제
            verify(desktopPendingLoginRepository).delete(state);

            // AuthService.login 호출 DTO 검증
            LoginRequestDto dto = requestCaptor.getValue();
            assertThat(dto.socialType()).isEqualTo(SocialType.KAKAO);
            assertThat(dto.accessToken()).isEqualTo(kakaoAccessToken);
            assertThat(dto.mobileType()).isEqualTo(MobileType.DESKTOP);
            assertThat(dto.clientId()).isNull();
            assertThat(dto.name()).isNull();
            assertThat(dto.email()).isNull();
        }

        @Test
        @DisplayName("[TC-POLL-002] pending 상태가 없으면 Optional.empty()를 반환하고 삭제나 로그인은 수행하지 않는다")
        void pollDesktopLogin_pending_not_found() {
            // given
            String state = "unknown-state";
            when(desktopPendingLoginRepository.find(state)).thenReturn(Optional.empty());

            // when
            Optional<LoginResponseDto> result = oAuth2LoginService.pollDesktopLogin(state, CLIENT_IP, USER_AGENT);

            // then
            assertThat(result).isEmpty();

            verify(desktopPendingLoginRepository, never()).delete(anyString());
            verifyNoInteractions(authService);
        }
    }
}
