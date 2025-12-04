package server.poptato.auth.application.response;

/**
 * 카카오 OAuth 콜백 처리 결과를 나타내는 enum.
 *
 * - SUCCESS         : 콜백 처리에 성공하여 pending 저장까지 완료된 상태
 * - CANCELED        : 사용자가 카카오 로그인 창에서 취소하거나 error 파라미터가 온 경우
 * - INVALID_REQUEST : 필수 파라미터(code/state)가 없거나 잘못된 경우
 * - ERROR           : 내부 예외(카카오 서버 오류, Redis 문제 등)로 콜백 처리에 실패한 경우
 *
 * 컨트롤러 ↔ 서비스 사이의 상태 전달용이며,
 * HTML 응답이나 HTTP 상태코드는 컨트롤러에서 이 값을 기준으로 결정한다.
 */
public enum OAuthCallbackResult {
    SUCCESS,
    CANCELED,
    INVALID_REQUEST,
    ERROR
}