package server.poptato.auth.api;

import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;
import server.poptato.auth.api.request.FCMTokenRequestDto;
import server.poptato.auth.api.request.LoginRequestDto;
import server.poptato.auth.api.request.ReissueTokenRequestDto;
import server.poptato.auth.application.response.LoginResponseDto;
import server.poptato.auth.application.service.AuthService;
import server.poptato.auth.application.service.JwtService;
import server.poptato.global.dto.TokenPair;
import server.poptato.global.response.ApiResponse;
import server.poptato.global.response.status.SuccessStatus;

@RestController
@RequestMapping("/auth")
@RequiredArgsConstructor
public class AuthController {

    private final AuthService authService;
    private final JwtService jwtService;

    /**
     * 로그인 API.
     *
     * 사용자가 제공한 소셜 로그인 정보를 기반으로 인증을 처리합니다.
     * 새로운 유저일 경우 유저 데이터를 저장하고, FCM 토큰을 업데이트합니다.
     * 응답으로 액세스 토큰, 리프레시 토큰, 유저 ID, 신규 유저 여부를 제공합니다.
     *
     * @param loginRequestDto 사용자의 로그인 요청 정보 (소셜 타입, 액세스 토큰, 클라이언트 ID 등)
     * @return 로그인 결과로 액세스 토큰, 리프레시 토큰, 유저 ID, 신규 유저 여부를 포함한 응답
     */
    @PostMapping("/login")
    public ResponseEntity<ApiResponse<LoginResponseDto>> login(
            @Validated @RequestBody LoginRequestDto loginRequestDto
    ) {
        LoginResponseDto response = authService.login(loginRequestDto);
        return ApiResponse.onSuccess(SuccessStatus._OK, response);
    }

    /**
     * 로그아웃 API.
     *
     * Authorization 헤더에서 추출된 사용자 ID를 기반으로 로그아웃 처리합니다.
     * 로그아웃 시 해당 사용자의 리프레시 토큰이 삭제됩니다.
     *
     * @param authorizationHeader 요청 헤더의 Authorization (Bearer 토큰)
     * @return 성공 여부를 나타내는 응답 객체
     */
    @PostMapping("/logout")
    public ResponseEntity<ApiResponse<SuccessStatus>> logout(
            @RequestHeader("Authorization") String authorizationHeader,
            @Validated @RequestBody FCMTokenRequestDto fcmTokenRequestDto
            ) {
        authService.logout(jwtService.extractUserIdFromToken(authorizationHeader), fcmTokenRequestDto);
        return ApiResponse.onSuccess(SuccessStatus._OK);
    }

    /**
     * 토큰 갱신 API.
     *
     * 기존 리프레시 토큰을 사용하여 새로운 액세스 토큰과 리프레시 토큰을 발급받습니다.
     * 리프레시 토큰이 유효하지 않은 경우 예외가 발생합니다.
     * 앱 실행시 fcm 토큰의 타임스탬프를 갱신합니다
     *
     * @param reissueTokenRequestDto 토큰 갱신 요청 정보 (기존 리프레시 토큰), fcm 토큰
     * @return 새로 발급된 액세스 토큰과 리프레시 토큰
     */
    @PostMapping("/refresh")
    public ResponseEntity<ApiResponse<TokenPair>> refresh(
            @Validated @RequestBody ReissueTokenRequestDto reissueTokenRequestDto
    ) {
        TokenPair response = authService.refresh(reissueTokenRequestDto);
        return ApiResponse.onSuccess(SuccessStatus._OK, response);
    }

}
