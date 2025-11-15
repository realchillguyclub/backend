package server.poptato.auth.api;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import server.poptato.auth.application.response.LoginResponseDto;
import server.poptato.auth.application.service.OAuth2LoginService;
import server.poptato.auth.status.AuthErrorStatus;
import server.poptato.global.response.ApiResponse;
import server.poptato.global.response.status.SuccessStatus;

@Slf4j
@RestController
@RequestMapping("/auth/oauth2")
@RequiredArgsConstructor
public class OAuth2Controller {

    private final OAuth2LoginService oAuth2LoginService;

    /**
     * 인가 시작 엔드포인트: state/PKCE를 저장하고 카카오 authorize URL로 302 한다.
     * @return 카카오 authorize URL로 redirect
     */
    @GetMapping("/kakao/authorize")
    public ResponseEntity<ApiResponse<Void>> authorize(
    ) {
        String authorizeUrl = oAuth2LoginService.buildAuthorizeRedirectForKakao();
        return ApiResponse.redirect(authorizeUrl);
    }

    /**
     * 콜백 엔드포인트: code/state 검증 후 토큰 교환, JWT 생성
     * @return 로그인 결과로 액세스 토큰, 리프레시 토큰, 유저 ID, 신규 유저 여부를 포함한 응답
     */
    @GetMapping("/kakao/callback")
    public ResponseEntity<ApiResponse<Object>> callback(
            @RequestParam String code,
            @RequestParam String state,
            @RequestParam(name = "error", required = false) String error,
            @RequestParam(name = "error_description", required = false) String errorDescription
    ) {
        if (state == null || state.isBlank() || code == null || code.isBlank()) {
            oAuth2LoginService.cleanupState(state);
            return ApiResponse.onFailure(AuthErrorStatus._OAUTH_ACCESS_DENIED);
        }

        LoginResponseDto response = oAuth2LoginService.handleKakaoCallback(code, state);
        return ApiResponse.onSuccess(SuccessStatus._OK, response);
    }
}
