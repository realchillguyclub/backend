package com.rcgc.illdan.global.status;

import com.rcgc.illdan.global.code.BaseErrorCode;
import com.rcgc.illdan.global.dto.ErrorReasonDto;
import lombok.AllArgsConstructor;
import lombok.Getter;
import org.springframework.http.HttpStatus;

@Getter
@AllArgsConstructor
public enum ErrorStatus implements BaseErrorCode {
    // Global Errors
    _INTERNAL_SERVER_ERROR(HttpStatus.INTERNAL_SERVER_ERROR, "GLOBAL-500", "서버 내부 오류가 발생했습니다. 관리자에게 문의하세요."),
    _BAD_REQUEST(HttpStatus.BAD_REQUEST, "GLOBAL-400", "잘못된 요청입니다."),
    _UNAUTHORIZED(HttpStatus.UNAUTHORIZED, "GLOBAL-401", "인증이 필요합니다."),
    _FORBIDDEN(HttpStatus.FORBIDDEN, "GLOBAL-403", "접근이 금지된 요청입니다."),
    _METHOD_NOT_ALLOWED(HttpStatus.METHOD_NOT_ALLOWED, "GLOBAL-405", "허용되지 않는 HTTP 메서드입니다."),
    _UNSUPPORTED_MEDIA_TYPE(HttpStatus.UNSUPPORTED_MEDIA_TYPE, "GLOBAL-415", "지원되지 않는 미디어 타입입니다."),
    _NOT_FOUND_HANDLER(HttpStatus.NOT_FOUND, "GLOBAL-404", "요청 경로에 대한 핸들러를 찾을 수 없습니다."),
    _FAILED_TRANSLATE_SWAGGER(HttpStatus.INTERNAL_SERVER_ERROR, "500", "Rest Docs로 생성된 json파일을 통한 스웨거 변환에 실패하였습니다."),
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
