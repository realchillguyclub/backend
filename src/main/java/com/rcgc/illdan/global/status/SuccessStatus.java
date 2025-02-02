package com.rcgc.illdan.global.status;

import com.rcgc.illdan.global.code.BaseCode;
import com.rcgc.illdan.global.dto.ReasonDto;
import lombok.AllArgsConstructor;
import lombok.Getter;
import org.springframework.http.HttpStatus;

@Getter
@AllArgsConstructor
public enum SuccessStatus implements BaseCode {
    // Global
    _OK(HttpStatus.OK, "GLOBAL-200", "요청 응답에 성공했습니다."),
    _CREATED(HttpStatus.CREATED, "GLOBAL-201", "요청에 의한 생성에 성공했습니다."),
    ;

    private final HttpStatus httpStatus;
    private final String code;
    private final String message;

    @Override
    public ReasonDto getReason() {
        return ReasonDto.builder()
                .isSuccess(true)
                .code(code)
                .message(message)
                .build();
    }

    @Override
    public ReasonDto getReasonHttpStatus() {
        return ReasonDto.builder()
                .isSuccess(true)
                .httpStatus(httpStatus)
                .code(code)
                .message(message)
                .build();
    }
}
