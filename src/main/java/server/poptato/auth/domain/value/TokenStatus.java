package server.poptato.auth.domain.value;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

/**
 * Refresh Token 상태
 */
@Getter
@RequiredArgsConstructor
public enum TokenStatus {
    ACTIVE,     // 활성 (사용 가능)
    REVOKED,    // 폐기 (로그아웃, 강제 만료)
    EXPIRED,    // 만료 (자연 만료)
    ROTATED,    // 로테이션 (Token Rotation으로 새 토큰 발급됨)
    DELETED     // 삭제 (Soft Delete, 보안 감사용 이력 보존)
}
