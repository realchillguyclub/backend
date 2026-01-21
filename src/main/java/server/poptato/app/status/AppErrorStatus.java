package server.poptato.app.status;

import org.springframework.http.HttpStatus;

import lombok.Getter;
import lombok.RequiredArgsConstructor;
import server.poptato.global.response.code.BaseErrorCode;
import server.poptato.global.response.dto.ErrorReasonDto;

@Getter
@RequiredArgsConstructor
public enum AppErrorStatus implements BaseErrorCode {
    _INVALID_PLATFORM(HttpStatus.BAD_REQUEST, "APP-001", "유효하지 않은 플랫폼입니다."),
    _RELEASE_NOT_FOUND(HttpStatus.NOT_FOUND, "APP-002", "해당 플랫폼의 활성화된 릴리즈가 없습니다."),
    _PRESIGNED_URL_GENERATION_FAILED(HttpStatus.INTERNAL_SERVER_ERROR, "APP-003", "다운로드 URL 생성에 실패했습니다.");

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
