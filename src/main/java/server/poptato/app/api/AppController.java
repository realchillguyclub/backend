package server.poptato.app.api;

import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import lombok.RequiredArgsConstructor;
import server.poptato.app.api.request.SkipRequestDto;
import server.poptato.app.application.AppService;
import server.poptato.app.application.response.DownloadResponseDto;
import server.poptato.app.application.response.VersionCheckResponseDto;
import server.poptato.app.domain.value.Platform;
import server.poptato.auth.application.service.JwtService;
import server.poptato.global.response.ApiResponse;
import server.poptato.global.response.status.SuccessStatus;

@RestController
@RequestMapping("/app")
@RequiredArgsConstructor
public class AppController {

    private final AppService appService;
    private final JwtService jwtService;

    /**
     * 버전 체크 API.
     *
     * 현재 앱 버전과 플랫폼 정보를 받아 업데이트 필요 여부를 확인합니다.
     *
     * @param authorizationHeader 요청 헤더의 Authorization (Bearer 토큰)
     * @param currentVersion 현재 앱 버전 (e.g., "1.0.0")
     * @param platform 플랫폼 (MACOS | WINDOWS)
     * @return 업데이트 정보를 포함한 응답
     */
    @GetMapping("/version")
    public ResponseEntity<ApiResponse<VersionCheckResponseDto>> checkVersion(
            @RequestHeader("Authorization") String authorizationHeader,
            @RequestParam String currentVersion,
            @RequestParam Platform platform
    ) {
        Long userId = jwtService.extractUserIdFromToken(authorizationHeader);
        VersionCheckResponseDto response = appService.checkVersion(userId, currentVersion, platform);
        return ApiResponse.onSuccess(SuccessStatus._OK, response);
    }

    /**
     * 다운로드 URL 조회 API.
     *
     * 플랫폼에 해당하는 활성화된 릴리즈의 S3 Presigned URL을 반환합니다.
     *
     * @param authorizationHeader 요청 헤더의 Authorization (Bearer 토큰)
     * @param platform 플랫폼 (MACOS | WINDOWS)
     * @param currentVersion 현재 앱 버전 (로깅용)
     * @return 다운로드 정보를 포함한 응답
     */
    @GetMapping("/download")
    public ResponseEntity<ApiResponse<DownloadResponseDto>> getDownloadUrl(
            @RequestHeader("Authorization") String authorizationHeader,
            @RequestParam Platform platform,
            @RequestParam(required = false, defaultValue = "unknown") String currentVersion
    ) {
        Long userId = jwtService.extractUserIdFromToken(authorizationHeader);
        DownloadResponseDto response = appService.getDownloadUrl(userId, platform, currentVersion);
        return ApiResponse.onSuccess(SuccessStatus._OK, response);
    }

    /**
     * 업데이트 스킵 API.
     *
     * 사용자가 "다음에 설치" 선택 시 로그를 기록합니다.
     *
     * @param authorizationHeader 요청 헤더의 Authorization (Bearer 토큰)
     * @param request 스킵 요청 데이터 (currentVersion, targetVersion, platform)
     * @return 성공 응답
     */
    @PostMapping("/skip")
    public ResponseEntity<ApiResponse<SuccessStatus>> skipUpdate(
            @RequestHeader("Authorization") String authorizationHeader,
            @Validated @RequestBody SkipRequestDto request
    ) {
        Long userId = jwtService.extractUserIdFromToken(authorizationHeader);
        appService.skipUpdate(userId, request);
        return ApiResponse.onSuccess(SuccessStatus._OK);
    }
}
