package server.poptato.auth.status;

import org.springframework.http.HttpStatus;

import lombok.Getter;
import lombok.RequiredArgsConstructor;
import server.poptato.global.response.code.BaseErrorCode;
import server.poptato.global.response.dto.ErrorReasonDto;

@Getter
@RequiredArgsConstructor
public enum AuthErrorStatus implements BaseErrorCode {
    _NOT_EXIST_ACCESS_TOKEN(HttpStatus.UNAUTHORIZED, "AUTH-001", "액세스 토큰이 존재하지 않습니다."),
    _EXPIRED_ACCESS_TOKEN(HttpStatus.UNAUTHORIZED, "AUTH-002", "액세스 토큰이 만료되었습니다."),
    _INVALID_ACCESS_TOKEN(HttpStatus.UNAUTHORIZED, "AUTH-003", "액세스 토큰이 유효하지 않습니다."),
    _DIFFERENT_REFRESH_TOKEN(HttpStatus.UNAUTHORIZED, "AUTH-004", "저장된 리프레쉬 토큰과 일치하지 않습니다."),
    _NOT_FOUND_VALID_PUBLIC_KEY(HttpStatus.UNAUTHORIZED, "AUTH-005", "유효한 공개 키를 찾을 수 없습니다."),
    _PUBLIC_KEY_GENERATION_FAILED(HttpStatus.INTERNAL_SERVER_ERROR, "AUTH-006", "공개 키 생성에 실패했습니다."),
    _INVALID_USER_ID_IN_TOKEN(HttpStatus.UNAUTHORIZED, "AUTH-007", "토큰 내 USER_ID가 유효하지 않습니다."),
    _EXPIRED_REFRESH_TOKEN(HttpStatus.UNAUTHORIZED, "AUTH-008", "리프레쉬 토큰이 만료되었습니다."),
    _INVALID_REFRESH_TOKEN(HttpStatus.UNAUTHORIZED, "AUTH-009", "리프레쉬 토큰이 유효하지 않습니다."),
    _PUBLIC_KEY_REQUEST_FAILED(HttpStatus.UNAUTHORIZED, "AUTH-010", "Apple 서버에서 공개 키를 가져올 수 없습니다."),
    _HAS_NOT_NEW_APPLE_USER_NAME(HttpStatus.UNAUTHORIZED, "AUTH-011", "[애플] 신규 유저의 name이 존재하지 않습니다."),
    _EXPIRED_APPLE_ID_TOKEN(HttpStatus.UNAUTHORIZED, "AUTH-012", "[애플] ID 토큰이 만료되었습니다."),
    _INVALID_APPLE_ID_TOKEN(HttpStatus.UNAUTHORIZED, "AUTH-013", "[애플] ID 토큰이 유효하지 않습니다."),
    _EXPIRED_OR_NOT_FOUND_REFRESH_TOKEN(HttpStatus.UNAUTHORIZED, "AUTH-014", "리프레쉬 토큰이 없거나 만료되었습니다."),
    _REDIS_UNAVAILABLE(HttpStatus.INTERNAL_SERVER_ERROR, "AUTH-015", "레디스 서버에 접근할 수 없습니다."),
    _SIGNUP_IN_PROGRESS(HttpStatus.CONFLICT, "AUTH-016", "이미 로그인이 진행 중입니다."),
    _OAUTH_ACCESS_DENIED(HttpStatus.BAD_REQUEST, "AUTH-017", "사용자가 소셜 로그인 동의를 거부했습니다."),
	_INVALID_STATE(HttpStatus.BAD_REQUEST, "AUTH-018", "유효하지 않은 OAuth state 값입니다."),
	_ALREADY_USED_REFRESH_TOKEN(HttpStatus.UNAUTHORIZED, "AUTH-019", "이미 사용된 리프레쉬 토큰입니다.")
	;

    private final HttpStatus httpStatus;
    private final String code;
    private final String message;

    @Override
    public ErrorReasonDto getReason() {
        return ErrorReasonDto.builder()
                .isSuccess(false)
                .code(code)
                .message(message)
                .build();
    }

    @Override
    public ErrorReasonDto getReasonHttpStatus() {
        return ErrorReasonDto.builder()
                .isSuccess(false)
                .httpStatus(httpStatus)
                .code(code)
                .message(message)
                .build();
    }
}
