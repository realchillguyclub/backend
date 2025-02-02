package com.rcgc.illdan.test.presentation;

import com.rcgc.illdan.global.dto.ApiResponse;
import com.rcgc.illdan.global.exception.CustomException;
import com.rcgc.illdan.global.status.ErrorStatus;
import com.rcgc.illdan.global.status.SuccessStatus;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/v1/test")
public class TestController {

    /**
     * 테스트 성공 응답 API
     *
     * 이 API는 테스트 목적으로 사용되며, 성공 응답을 반환합니다.
     * 주어진 요청이 정상적으로 처리될 경우, 200 OK 상태 코드와 함께
     * 기본 성공 메시지를 포함한 응답을 반환합니다.
     *
     * @return 성공 상태 응답 객체
     */
    @GetMapping("/response")
    public ResponseEntity<ApiResponse<SuccessStatus>> getApiResponse() {
        return ApiResponse.onSuccess(SuccessStatus._OK);
    }

    /**
     * 테스트 예외 발생 API
     *
     * 이 API는 테스트 목적으로 사용되며, 강제로 내부 서버 오류를 발생시킵니다.
     * 정상적인 요청 흐름에서 예외가 발생했을 때의 처리를 확인하는 용도로 사용됩니다.
     *
     * @throws CustomException 내부 서버 오류 예외 발생
     */
    @GetMapping("/error")
    public void getError() {
        throw new CustomException(ErrorStatus._INTERNAL_SERVER_ERROR);
    }
}
