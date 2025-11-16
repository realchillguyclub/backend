package server.poptato.auth.application.service;

import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.util.UriComponentsBuilder;
import server.poptato.auth.api.request.LoginRequestDto;
import server.poptato.auth.application.response.AuthorizeUrlResponseDto;
import server.poptato.auth.application.response.LoginResponseDto;
import server.poptato.auth.status.AuthErrorStatus;
import server.poptato.global.exception.CustomException;
import server.poptato.infra.oauth.kakao.KakaoSocialService;
import server.poptato.infra.oauth.state.OAuthState;
import server.poptato.infra.oauth.state.OAuthStateRepository;
import server.poptato.infra.oauth.state.PKCEUtils;
import server.poptato.user.domain.value.MobileType;
import server.poptato.user.domain.value.SocialType;

import java.time.Duration;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class OAuth2LoginService {

    private final OAuthStateRepository oAuthStateRepository;
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
                Duration.ofMinutes(10)
        );

        // 4. 카카오 인가 URL 조립
        return AuthorizeUrlResponseDto.of(
                UriComponentsBuilder.fromUriString(authorizeUri)
                .queryParam("response_type", "code")
                .queryParam("scope", defaultScope)
                .queryParam("client_id", clientId)
                .queryParam("redirect_uri", redirectUri)
                .queryParam("state", state)
                .queryParam("code_challenge_method", "S256")
                .queryParam("code_challenge", codeChallenge)
                .build(true)
                .toUriString()
        );
    }

    /**
     * ***********************************************
     * 카카오 콜백 처리 및 로그인 후 토큰 발급
     * ***********************************************
     *
     * [주요 역할]
     * 1. 카카오 콜백으로 전달된 code/state 검증
     * 2. state 기반으로 Redis에서 code_verifier 복원
     * 3. 카카오 토큰 교환 API 호출 (grant_type=authorization_code)
     * 4. access_token 획득 후 기존 AuthService.login() 재사용
     * 5. Redis state 삭제 (재사용/리플레이 방지)
     *
     * [보안 포인트]
     * - state 불일치 시 _INVALID_STATE 예외 발생
     * - code_verifier 불일치 시 카카오가 token 교환 거부
     *
     * @param code 카카오가 전달한 인가 코드
     * @param state 최초 인가 요청 시 서버가 저장한 상태 토큰
     * @return 로그인 결과 (accessToken, refreshToken, userId, isNewUser)
     */
    @Transactional
    public LoginResponseDto handleKakaoCallback(String code, String state) {
        // 1. state 검증 및 복원
        OAuthState savedState = oAuthStateRepository.find(state)
                .orElseThrow(() -> new CustomException(AuthErrorStatus._INVALID_STATE));

        try {
            // 2. 카카오로 토큰 요청 → access_token 획득
            String accessToken = kakaoSocialService.getKakaoUserAccessToken(
                    clientId,
                    redirectUri,
                    code,
                    savedState.getCodeVerifier());

            // 3. 기존 AuthService 로직 재사용 (로그인 처리 + JWT 발급)
            LoginRequestDto requestDto =
                    new LoginRequestDto(SocialType.KAKAO, accessToken, MobileType.DESKTOP, null, null, null);

            return authService.login(requestDto);
        } finally {
            // 4. state 정보 제거 (원타임 보장)
            cleanupState(state);
        }
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
}