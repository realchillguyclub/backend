package server.poptato.auth.api;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import server.poptato.auth.api.view.OAuthCallbackHtml;
import server.poptato.auth.application.response.AuthorizeUrlResponseDto;
import server.poptato.auth.application.response.LoginResponseDto;
import server.poptato.auth.application.response.OAuthCallbackResult;
import server.poptato.auth.application.service.OAuth2LoginService;
import server.poptato.auth.status.AuthErrorStatus;
import server.poptato.global.response.ApiResponse;
import server.poptato.global.response.status.SuccessStatus;
import java.util.Optional;

@Slf4j
@RestController
@RequestMapping("/auth/oauth2")
@RequiredArgsConstructor
public class OAuth2Controller {

    private final OAuth2LoginService oAuth2LoginService;

    /**
     * 인가 시작 엔드포인트: state/PKCE를 저장하고 카카오 authorize URL로 302 한다.
     * @return 카카오 authorize URL 반환
     */
    @GetMapping("/kakao/authorize")
    public ResponseEntity<ApiResponse<AuthorizeUrlResponseDto>> authorize(
    ) {
        AuthorizeUrlResponseDto response = oAuth2LoginService.buildAuthorizeRedirectForKakao();
        return ApiResponse.onSuccess(SuccessStatus._OK, response);
    }

    /**
     * 콜백 엔드포인트:
     * - code/state 검증
     * - 카카오 토큰 교환
     * - 데스크탑 폴링을 위한 로그인 대기 상태 저장
     * - HTML 페이지 응답 (JWT는 폴링 API에서 발급)
     */
    @GetMapping("/kakao/callback")
    public ResponseEntity<String> callback(
            @RequestParam(required = false) String code,
            @RequestParam(required = false) String state,
            @RequestParam(name = "error", required = false) String error,
            @RequestParam(name = "error_description", required = false) String errorDescription
    ) {
        OAuthCallbackResult result = oAuth2LoginService.handleKakaoCallback(code, state, error);

        return switch (result) {
            case SUCCESS -> ApiResponse.htmlOnSuccess(OAuthCallbackHtml.SUCCESS.html());
            case CANCELED -> ApiResponse.htmlOnFailure(AuthErrorStatus._OAUTH_ACCESS_DENIED, OAuthCallbackHtml.CANCELED.html());
            case INVALID_REQUEST -> ApiResponse.htmlOnFailure(AuthErrorStatus._OAUTH_ACCESS_DENIED, OAuthCallbackHtml.INVALID_REQUEST.html());
            case ERROR -> ApiResponse.htmlOnFailure(AuthErrorStatus._OAUTH_ACCESS_DENIED, OAuthCallbackHtml.ERROR.html());
        };
    }

    /**
     * 데스크탑 앱이 주기적으로 호출하는 폴링 엔드포인트.
     *
     * [동작]
     * - pending 상태가 없으면 : 204 No Content
     * - pending 상태가 있으면 :
     *      1) AuthService.login() 호출
     *      2) 로그인 결과로 액세스 토큰, 리프레시 토큰, 유저 ID, 신규 유저 여부를 포함한 응답
     */
    @GetMapping("/kakao/desktop/poll")
    public ResponseEntity<ApiResponse<LoginResponseDto>> poll(@RequestParam String state) {
        Optional<LoginResponseDto> result = oAuth2LoginService.pollDesktopLogin(state);

        return result.map(loginResponseDto ->
                ApiResponse.onSuccess(SuccessStatus._OK, loginResponseDto))
                .orElseGet(() -> ApiResponse.onSuccess(SuccessStatus._NO_CONTENT));
    }
}
