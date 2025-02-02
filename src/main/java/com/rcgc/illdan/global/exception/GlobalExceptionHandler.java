package com.rcgc.illdan.global.exception;

import com.rcgc.illdan.global.dto.ApiResponse;
import com.rcgc.illdan.global.dto.ErrorReasonDto;
import com.rcgc.illdan.global.status.ErrorStatus;
import lombok.extern.slf4j.Slf4j;
import org.hibernate.exception.ConstraintViolationException;
import org.springframework.context.support.DefaultMessageSourceResolvable;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.validation.FieldError;
import org.springframework.web.HttpMediaTypeNotSupportedException;
import org.springframework.web.HttpRequestMethodNotSupportedException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.MissingRequestHeaderException;
import org.springframework.web.bind.MissingServletRequestParameterException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.context.request.WebRequest;
import org.springframework.web.servlet.NoHandlerFoundException;
import org.springframework.web.servlet.mvc.method.annotation.ResponseEntityExceptionHandler;

import java.util.List;
import java.util.stream.Collectors;

@Slf4j
@RestControllerAdvice
public class GlobalExceptionHandler extends ResponseEntityExceptionHandler {

    // 커스텀 예외 처리
    @ExceptionHandler(CustomException.class)
    public ResponseEntity<ApiResponse<ErrorReasonDto>> handleCustomException(CustomException e) {
        logError(e.getMessage(), e);
        return ApiResponse.onFailure(e.getErrorCode());
    }

    // Security 인증 관련 처리
    @ExceptionHandler(SecurityException.class)
    public ResponseEntity<ApiResponse<ErrorReasonDto>> handleSecurityException(SecurityException e) {
        logError(e.getMessage(), e);
        return ApiResponse.onFailure(ErrorStatus._UNAUTHORIZED);
    }

    // IllegalArgumentException 처리 (잘못된 인자가 전달된 경우)
    @ExceptionHandler(IllegalArgumentException.class)
    public ResponseEntity<ApiResponse<ErrorReasonDto>> handleIllegalArgumentException(IllegalArgumentException e) {
        String errorMessage = "잘못된 요청입니다: " + e.getMessage();
        logError("IllegalArgumentException", errorMessage);
        return ApiResponse.onFailureWithCustomMessage(ErrorStatus._BAD_REQUEST, errorMessage);
    }

    // NullPointerException 처리
    @ExceptionHandler(NullPointerException.class)
    public ResponseEntity<ApiResponse<ErrorReasonDto>> handleNullPointerException(NullPointerException e) {
        String errorMessage = "서버에서 예기치 않은 오류가 발생했습니다. 요청을 처리하는 중에 Null 값이 참조되었습니다.";
        logError("NullPointerException", e);
        return ApiResponse.onFailureWithCustomMessage(ErrorStatus._INTERNAL_SERVER_ERROR, errorMessage);
    }

    // NumberFormatException 처리
    @ExceptionHandler(NumberFormatException.class)
    public ResponseEntity<ApiResponse<ErrorReasonDto>> handleNumberFormatException(NumberFormatException e) {
        String errorMessage = "숫자 형식이 잘못되었습니다: " + e.getMessage();
        logError("NumberFormatException", e);
        return ApiResponse.onFailureWithCustomMessage(ErrorStatus._BAD_REQUEST, errorMessage);
    }

    // IndexOutOfBoundsException 처리
    @ExceptionHandler(IndexOutOfBoundsException.class)
    public ResponseEntity<ApiResponse<ErrorReasonDto>> handleIndexOutOfBoundsException(IndexOutOfBoundsException e) {
        String errorMessage = "인덱스가 범위를 벗어났습니다: " + e.getMessage();
        logError("IndexOutOfBoundsException", e);
        return ApiResponse.onFailureWithCustomMessage(ErrorStatus._BAD_REQUEST, errorMessage);
    }

    // ConstraintViolationException 처리 (쿼리 파라미터에 올바른 값이 들어오지 않은 경우)
    @ExceptionHandler(ConstraintViolationException.class)
    public ResponseEntity<ApiResponse<ErrorReasonDto>> handleValidationParameterError(ConstraintViolationException ex) {
        String errorMessage = ex.getMessage();
        logError("ConstraintViolationException", errorMessage);
        return ApiResponse.onFailureWithCustomMessage(ErrorStatus._BAD_REQUEST, errorMessage);
    }

    // MissingRequestHeaderException 처리 (필수 헤더가 누락된 경우)
    @ExceptionHandler(MissingRequestHeaderException.class)
    public ResponseEntity<ApiResponse<ErrorReasonDto>> handleMissingRequestHeaderException(MissingRequestHeaderException ex) {
        String errorMessage = "필수 헤더 '" + ex.getHeaderName() + "'가 없습니다.";
        logError("MissingRequestHeaderException", errorMessage);
        return ApiResponse.onFailureWithCustomMessage(ErrorStatus._BAD_REQUEST, errorMessage);
    }

    // DataIntegrityViolationException 처리 (데이터베이스 제약 조건 위반)
    @ExceptionHandler(DataIntegrityViolationException.class)
    public ResponseEntity<ApiResponse<ErrorReasonDto>> handleDataIntegrityViolationException(DataIntegrityViolationException e) {
        String errorMessage = "데이터 무결성 제약 조건을 위반했습니다: " + e.getMessage();
        logError("DataIntegrityViolationException", e);
        return ApiResponse.onFailureWithCustomMessage(ErrorStatus._BAD_REQUEST, errorMessage);
    }

    // MissingServletRequestParameterException 처리 (필수 쿼리 파라미터가 입력되지 않은 경우)
    @Override
    protected ResponseEntity<Object> handleMissingServletRequestParameter(MissingServletRequestParameterException ex,
                                                                          HttpHeaders headers,
                                                                          HttpStatusCode status,
                                                                          WebRequest request) {
        String errorMessage = "필수 파라미터 '" + ex.getParameterName() + "'가 없습니다.";
        logError("MissingServletRequestParameterException", errorMessage);
        return ApiResponse.onFailureForOverrideMethod(ErrorStatus._BAD_REQUEST, errorMessage);
    }

    // MethodArgumentNotValidException 처리 (RequestBody로 들어온 필드들의 유효성 검증에 실패한 경우)
    @Override
    protected ResponseEntity<Object> handleMethodArgumentNotValid(MethodArgumentNotValidException ex,
                                                                  HttpHeaders headers,
                                                                  HttpStatusCode status,
                                                                  WebRequest request) {
        String combinedErrors = extractFieldErrors(ex.getBindingResult().getFieldErrors());
        logError("Validation error", combinedErrors);
        return ApiResponse.onFailureForOverrideMethod(ErrorStatus._BAD_REQUEST, combinedErrors);
    }

    // NoHandlerFoundException 처리 (요청 경로에 매핑된 핸들러가 없는 경우)
    @Override
    protected ResponseEntity<Object> handleNoHandlerFoundException(NoHandlerFoundException ex,
                                                                   HttpHeaders headers,
                                                                   HttpStatusCode status,
                                                                   WebRequest request) {
        String errorMessage = "해당 경로에 대한 핸들러를 찾을 수 없습니다: " + ex.getRequestURL();
        logError("NoHandlerFoundException", errorMessage);
        return ApiResponse.onFailureForOverrideMethod(ErrorStatus._NOT_FOUND_HANDLER, errorMessage);
    }

    // HttpRequestMethodNotSupportedException 처리 (지원하지 않는 HTTP 메소드 요청이 들어온 경우)
    @Override
    protected ResponseEntity<Object> handleHttpRequestMethodNotSupported(HttpRequestMethodNotSupportedException ex,
                                                                         HttpHeaders headers,
                                                                         HttpStatusCode status,
                                                                         WebRequest request) {
        String errorMessage = "지원하지 않는 HTTP 메소드 요청입니다: " + ex.getMethod();
        logError("HttpRequestMethodNotSupportedException", errorMessage);
        return ApiResponse.onFailureForOverrideMethod(ErrorStatus._METHOD_NOT_ALLOWED, errorMessage);
    }

    // HttpMediaTypeNotSupportedException 처리 (지원하지 않는 미디어 타입 요청이 들어온 경우)
    @Override
    protected ResponseEntity<Object> handleHttpMediaTypeNotSupported(HttpMediaTypeNotSupportedException ex,
                                                                     HttpHeaders headers,
                                                                     HttpStatusCode status,
                                                                     WebRequest request) {
        String errorMessage = "지원하지 않는 미디어 타입입니다: " + ex.getContentType();
        logError("HttpMediaTypeNotSupportedException", errorMessage);
        return ApiResponse.onFailureForOverrideMethod(ErrorStatus._UNSUPPORTED_MEDIA_TYPE, errorMessage);
    }

    // HttpMessageNotReadableException 처리 (잘못된 JSON 형식)
    @Override
    public ResponseEntity<Object> handleHttpMessageNotReadable(HttpMessageNotReadableException ex,
                                                               HttpHeaders headers,
                                                               HttpStatusCode status,
                                                               WebRequest request) {
        String errorMessage = "요청 본문을 읽을 수 없습니다. 올바른 JSON 형식이어야 합니다.";
        logError("HttpMessageNotReadableException", ex);
        return ApiResponse.onFailureForOverrideMethod(ErrorStatus._BAD_REQUEST, errorMessage);
    }

    // 내부 서버 에러 처리 (500)
    @ExceptionHandler(Exception.class)
    public ResponseEntity<ApiResponse<ErrorReasonDto>> handleException(Exception e) {
        logError(e.getMessage(), e);
        return ApiResponse.onFailure(ErrorStatus._INTERNAL_SERVER_ERROR);
    }

    // 유효성 검증 오류 메시지 추출 메서드 (FieldErrors)
    private String extractFieldErrors(List<FieldError> fieldErrors) {
        return fieldErrors.stream()
                .map(DefaultMessageSourceResolvable::getDefaultMessage)
                .collect(Collectors.joining(", "));
    }

    // 로그 기록 메서드
    private void logError(String message, Object errorDetails) {
        log.error("{}: {}", message, errorDetails);
    }
}
