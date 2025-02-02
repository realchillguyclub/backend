package com.rcgc.illdan.global.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.rcgc.illdan.global.code.BaseCode;
import com.rcgc.illdan.global.code.BaseErrorCode;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;

@Getter
@RequiredArgsConstructor
public class ApiResponse<T> {
    private final Boolean isSuccess;
    private final String code;
    private final String message;
    @JsonInclude(JsonInclude.Include.NON_NULL)
    private final T result;

    // 성공 응답 - result 포함
    public static <T> ResponseEntity<ApiResponse<T>> onSuccess(BaseCode code, T result) {
        ApiResponse<T> response = new ApiResponse<>(true, code.getReasonHttpStatus().getCode(), code.getReasonHttpStatus().getMessage(), result);
        return ResponseEntity.status(code.getReasonHttpStatus().getHttpStatus()).body(response);
    }

    // 성공 응답 - result 없음
    public static <T> ResponseEntity<ApiResponse<T>> onSuccess(BaseCode code) {
        ApiResponse<T> response = new ApiResponse<>(true, code.getReasonHttpStatus().getCode(), code.getReasonHttpStatus().getMessage(), null);
        return ResponseEntity.status(code.getReasonHttpStatus().getHttpStatus()).body(response);
    }

    // 실패 응답 - 기본 메시지
    public static <T> ResponseEntity<ApiResponse<T>> onFailure(BaseErrorCode code) {
        ApiResponse<T> response = new ApiResponse<>(false, code.getReasonHttpStatus().getCode(), code.getReasonHttpStatus().getMessage(), null);
        return ResponseEntity.status(code.getReasonHttpStatus().getHttpStatus()).body(response);
    }

    // 실패 응답 - 커스텀 메시지 (result 없음)
    public static <T> ResponseEntity<ApiResponse<T>> onFailureWithCustomMessage(BaseErrorCode code, String customMessage) {
        ApiResponse<T> response = new ApiResponse<>(false, code.getReasonHttpStatus().getCode(), customMessage, null);
        return ResponseEntity.status(code.getReasonHttpStatus().getHttpStatus()).body(response);
    }

    // 실패 응답 - Override 메서드용
    public static <T> ResponseEntity<Object> onFailureForOverrideMethod(BaseErrorCode code, String message) {
        ApiResponse<T> response = new ApiResponse<>(false, code.getReasonHttpStatus().getCode(), message, null);
        return ResponseEntity.status(code.getReasonHttpStatus().getHttpStatus()).body(response);
    }
}
