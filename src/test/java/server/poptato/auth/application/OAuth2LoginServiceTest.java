package server.poptato.auth.application;

import org.junit.jupiter.api.*;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.util.MultiValueMap;
import org.springframework.web.util.UriComponentsBuilder;
import server.poptato.auth.api.request.LoginRequestDto;
import server.poptato.auth.application.response.LoginResponseDto;
import server.poptato.auth.application.service.AuthService;
import server.poptato.auth.application.service.OAuth2LoginService;
import server.poptato.auth.status.AuthErrorStatus;
import server.poptato.configuration.ServiceTestConfig;
import server.poptato.global.exception.CustomException;
import server.poptato.infra.oauth.kakao.KakaoSocialService;
import server.poptato.infra.oauth.state.OAuthState;
import server.poptato.infra.oauth.state.OAuthStateRepository;
import server.poptato.infra.oauth.state.PKCEUtils;
import server.poptato.user.domain.value.MobileType;
import server.poptato.user.domain.value.SocialType;

import java.time.Duration;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

class OAuth2LoginServiceTest extends ServiceTestConfig {

    @Mock
    OAuthStateRepository oAuthStateRepository;

    @Mock
    KakaoSocialService kakaoSocialService;

    @Mock
    AuthService authService;

    @InjectMocks
    OAuth2LoginService oAuth2LoginService;

    @Test
    @DisplayName("[SCN-SVC-OAUTH2-001] [TC-KAKAO-AUTHORIZATION-001] state/PKCE(code_verifier)가 저장되고 쿼리파라미터가 정확히 구성된 authorize URL을 반환한다")
    void buildAuthorizeRedirectForKakao_정상_URL_생성_state_PKCE_저장() {
        // given - @Value 주입 필드 설정
        String clientId    = "test-kakao-client-id";
        String redirectUri = "https://my.app.com/oauth/kakao/callback";
        String authorizeUri= "https://kauth.kakao.com/oauth/authorize";

        ReflectionTestUtils.setField(oAuth2LoginService, "kakaoClientId", clientId);
        ReflectionTestUtils.setField(oAuth2LoginService, "redirectUri", redirectUri);
        ReflectionTestUtils.setField(oAuth2LoginService, "authorizeUri", authorizeUri);

        ArgumentCaptor<OAuthState> stateCaptor = ArgumentCaptor.forClass(OAuthState.class);
        ArgumentCaptor<Duration> ttlCaptor = ArgumentCaptor.forClass(Duration.class);

        doNothing().when(oAuthStateRepository).save(stateCaptor.capture(), ttlCaptor.capture());

        // when
        String authorizeUrl = oAuth2LoginService.buildAuthorizeRedirectForKakao();

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
                UriComponentsBuilder.fromUriString(authorizeUrl).build(true).getQueryParams();

        assertThat(authorizeUrl).startsWith(authorizeUri);
        assertThat(params.getFirst("response_type")).isEqualTo("code");
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
            ReflectionTestUtils.setField(oAuth2LoginService, "kakaoClientId", "test-kakao-client-id");
            ReflectionTestUtils.setField(oAuth2LoginService, "redirectUri", "https://your.app.com/oauth/kakao/callback");
            ReflectionTestUtils.setField(oAuth2LoginService, "authorizeUri", "https://kauth.kakao.com/oauth/authorize");
        }

        @Test
        @DisplayName("[TC-CALLBACK-001] 유효한 state와 code가 전달되면 토큰을 교환하고 로그인 처리 후 state를 삭제한다")
        void handleKakaoCallback_성공() {
            // given
            injectValues();
            String code = "authorization-code";
            String state = "state-UUID";
            String verifier = "code-verifier";

            OAuthState saved = OAuthState.builder().state(state).codeVerifier(verifier).build();
            when(oAuthStateRepository.find(state)).thenReturn(Optional.of(saved));

            ArgumentCaptor<MultiValueMap<String, String>> formCaptor = ArgumentCaptor.forClass(MultiValueMap.class);
            String kakaoAccessToken = "kakao-user-access-token";
            when(kakaoSocialService.getKakaoUserAccessToken(formCaptor.capture())).thenReturn(kakaoAccessToken);

            ArgumentCaptor<LoginRequestDto> requestDtoArgumentCaptor =
                    ArgumentCaptor.forClass(LoginRequestDto.class);
            LoginResponseDto expected = LoginResponseDto.of("access", "refresh", false, 123L);
            when(authService.login(requestDtoArgumentCaptor.capture())).thenReturn(expected);

            // when
            LoginResponseDto actual = oAuth2LoginService.handleKakaoCallback(code, state);

            // then - 폼 검증
            MultiValueMap<String, String> form = formCaptor.getValue();
            assertThat(form.getFirst("grant_type")).isEqualTo("authorization_code");
            assertThat(form.getFirst("client_id")).isEqualTo("test-kakao-client-id");
            assertThat(form.getFirst("redirect_uri")).isEqualTo("https://your.app.com/oauth/kakao/callback");
            assertThat(form.getFirst("code")).isEqualTo(code);
            assertThat(form.getFirst("code_verifier")).isEqualTo(verifier);

            // then - AuthService.login 요청 DTO
            LoginRequestDto requestDto = requestDtoArgumentCaptor.getValue();
            assertThat(requestDto.socialType()).isEqualTo(SocialType.KAKAO);
            assertThat(requestDto.accessToken()).isEqualTo(kakaoAccessToken);
            assertThat(requestDto.mobileType()).isEqualTo(MobileType.DESKTOP);
            assertThat(requestDto.clientId()).isNull();
            assertThat(requestDto.name()).isNull();
            assertThat(requestDto.email()).isNull();

            // then - 반환 전달 및 state 삭제
            assertThat(actual).isSameAs(expected);
            verify(oAuthStateRepository).delete(state);
        }

        @Test
        @DisplayName("[TC-CALLBACK-002] 유효하지 않은 state가 전달되면 예외를 던지고 토큰 교환이나 로그인은 수행되지 않으며 state는 삭제되지 않는다")
        void handleKakaoCallback_state_invalid() {
            // given
            injectValues();
            String code = "authorization-code";
            String badState = "bad-state";
            when(oAuthStateRepository.find(badState)).thenReturn(Optional.empty());

            // when & then
            assertThatThrownBy(() -> oAuth2LoginService.handleKakaoCallback(code, badState))
                    .isInstanceOf(CustomException.class)
                    .extracting("errorCode")
                    .isEqualTo(AuthErrorStatus._INVALID_STATE);

            verifyNoInteractions(kakaoSocialService, authService);
            verify(oAuthStateRepository, never()).delete(anyString());
        }
    }
}
