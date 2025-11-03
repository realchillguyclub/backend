package server.poptato.global.response.status;

import lombok.AllArgsConstructor;
import lombok.Getter;
import org.springframework.http.HttpStatus;
import server.poptato.global.response.code.BaseCode;
import server.poptato.global.response.dto.ReasonDto;

@Getter
@AllArgsConstructor
public enum SuccessStatus implements BaseCode {
    // Global
    _OK(HttpStatus.OK, "GLOBAL-200", "요청 응답에 성공했습니다."),
    _CREATED(HttpStatus.CREATED, "GLOBAL-201", "생성에 성공했습니다."),
    _FOUND(HttpStatus.FOUND, "GLOBAL-302", "FOUND");

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
