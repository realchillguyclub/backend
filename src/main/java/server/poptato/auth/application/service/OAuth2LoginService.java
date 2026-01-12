package server.poptato.auth.application.service;

import java.time.Duration;
import java.util.Optional;
import java.util.UUID;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.util.UriComponentsBuilder;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import server.poptato.auth.api.request.LoginRequestDto;
import server.poptato.auth.application.response.AuthorizeUrlResponseDto;
import server.poptato.auth.application.response.LoginResponseDto;
import server.poptato.auth.application.response.OAuthCallbackResult;
import server.poptato.auth.status.AuthErrorStatus;
import server.poptato.global.exception.CustomException;
import server.poptato.infra.oauth.kakao.KakaoSocialService;
import server.poptato.infra.oauth.pending.DesktopPendingLoginRepository;
import server.poptato.infra.oauth.pending.PendingLogin;
import server.poptato.infra.oauth.state.OAuthState;
import server.poptato.infra.oauth.state.OAuthStateRepository;
import server.poptato.infra.oauth.state.PKCEUtils;
import server.poptato.user.domain.value.MobileType;

@Slf4j
@Service
@RequiredArgsConstructor
public class OAuth2LoginService {

    private final OAuthStateRepository oAuthStateRepository;
    private final DesktopPendingLoginRepository desktopPendingLoginRepository;
    private final KakaoSocialService kakaoSocialService;
    private final AuthService authService;

    @Value("${oauth.kakao.client-id}")
    private String clientId;

    @Value("${oauth.kakao.redirect-uri}")
    private String redirectUri;

    @Value("${oauth.kakao.authorize-uri}")
    private String authorizeUri;

    @Value("${oauth.kakao.scope}")
    private String defaultScope;

    private static final Duration STATE_TTL = Duration.ofMinutes(10);
    private static final Duration PENDING_TTL = Duration.ofMinutes(3);

    /**
     * ************************************
     * 카카오 인가 요청 URL 생성
     * ************************************
     *
     * [주요 역할]
     * 1. CSRF 방지를 위한 state 토큰 생성
     * 2. PKCE(SHA256) 적용을 위한 code_verifier/code_challenge 생성
     * 3. state 및 code_verifier를 Redis에 10분 TTL로 저장
     * 4. 카카오 로그인창으로 리다이렉트할 authorize URL을 완성하여 반환
     *
     * [보안 포인트]
     * - state: 콜백 위조 방지
     * - code_challenge: Authorization Code 탈취 방지 (PKCE)
     *
     * @return 완성된 카카오 로그인 authorize URL (클라이언트가 이 URL을 열어 로그인 수행)
     */
    public AuthorizeUrlResponseDto buildAuthorizeRedirectForKakao() {
        // 1. state (CSRF 방지 토큰) 생성
        String state = UUID.randomUUID().toString();

        // 2. PKCE용 code_verifier 및 code_challenge 생성
        String codeVerifier = PKCEUtils.generateCodeVerifier();
        String codeChallenge = PKCEUtils.generateCodeChallenge(codeVerifier);

        // 3. state + verifier를 Redis에 저장 (10분 TTL)
        oAuthStateRepository.save(
                OAuthState.builder()
                        .state(state)
                        .codeVerifier(codeVerifier)
                        .build(),
                STATE_TTL
        );

        // 4. 카카오 인가 URL 조립
        String url = UriComponentsBuilder.fromUriString(authorizeUri)
                .queryParam("client_id", clientId)
                .queryParam("redirect_uri", redirectUri)
                .queryParam("response_type", "code")
                .queryParam("scope", defaultScope)
                .queryParam("state", state)
                .queryParam("code_challenge_method", "S256")
                .queryParam("code_challenge", codeChallenge)
                .build()
                .encode()
                .toUriString();

        return AuthorizeUrlResponseDto.of(url, state);
    }

    /**
     * ****************************************************
     * 2. 카카오 콜백 처리 (Desktop 전용, HTML 응답 전 단계)
     * ****************************************************
     *
     * [역할]
     * - error/code/state 파라미터를 해석하여 콜백 상태를 분류하고,
     *   실제 OAuth 플로우(state 검증, 토큰 교환, pending 저장)를 수행한다.
     *
     * [반환값]
     * - OAuthCallbackResult enum (SUCCESS / CANCELED / INVALID_REQUEST / ERROR)
     *
     * 컨트롤러는 이 결과만 가지고 어떤 HTML을 내려줄지 결정하며,
     * JWT 발급은 이후 폴링 단계에서 처리한다.
     */
    @Transactional
    public OAuthCallbackResult handleKakaoCallback(String code, String state, String error) {
        if (error != null) {
            cleanupState(state);
            return OAuthCallbackResult.CANCELED;
        }

        if (isBlank(code) || isBlank(state)) {
            cleanupState(state);
            return OAuthCallbackResult.INVALID_REQUEST;
        }

        try {
            OAuthState savedState = oAuthStateRepository.find(state)
                    .orElseThrow(() -> new CustomException(AuthErrorStatus._INVALID_STATE));

            String accessToken = kakaoSocialService.getKakaoUserAccessToken(
                    clientId,
                    redirectUri,
                    code,
                    savedState.getCodeVerifier()
            );

            desktopPendingLoginRepository.save(state, accessToken, PENDING_TTL);
            return OAuthCallbackResult.SUCCESS;

        } catch (Exception e) {
            return OAuthCallbackResult.ERROR;
        } finally {
            cleanupState(state);
        }
    }


    /**
     * ********************************************
     * 3. 데스크탑 전용 폴링 API 비즈니스 로직
     * ********************************************
     *
     * [역할]
     * 1) state로 pending 저장소에서 카카오 access_token 조회
     * 2) 없으면 아직 콜백 미도착 또는 TTL 만료 → Optional.empty()
     * 3) 있으면 pending 삭제 후 AuthService.login() 호출
     * 4) JWT(access/refresh) + userId + isNewUser 가 포함된 LoginResponseDto 반환
     *
     * - JWT 생성 및 refreshToken 저장은 기존 로직 AuthService.login()으로 처리한다.
     *
     * @param state     OAuth state 토큰
     * @param clientIp  클라이언트 IP
     * @param userAgent User-Agent
     */
    @Transactional
    public Optional<LoginResponseDto> pollDesktopLogin(String state, String clientIp, String userAgent) {
        Optional<PendingLogin> pendingLoginOptional = desktopPendingLoginRepository.find(state);

        if (pendingLoginOptional.isEmpty()) {
            return Optional.empty();
        }

        desktopPendingLoginRepository.delete(state);

        String kakaoAccessToken = pendingLoginOptional.get().accessToken();

        LoginRequestDto requestDto = new LoginRequestDto(
                pendingLoginOptional.get().socialType(),
                kakaoAccessToken,
                MobileType.DESKTOP,
                null,
                null,
                null
        );

        LoginResponseDto loginResponse = authService.login(requestDto, clientIp, userAgent);

        return Optional.of(loginResponse);
    }

    /**
     * Redis에 저장된 state 정보를 삭제한다.
     * 콜백 처리 후 반드시 호출하여 재사용을 방지한다.
     *
     * @param state 삭제할 state 토큰
     */
    public void cleanupState(String state) {
        oAuthStateRepository.delete(state);
    }

    private boolean isBlank(String value) {
        return value == null || value.isBlank();
    }
}