package server.poptato.infra.oauth.state;

import lombok.RequiredArgsConstructor;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Repository;

import java.time.Duration;
import java.util.Optional;

@Repository
@RequiredArgsConstructor
public class OAuthStateRepository {

    private final StringRedisTemplate stringRedisTemplate;

    /**
     * *******************************************
     * state 및 code_verifier 저장 (TTL 적용)
     * *******************************************
     *
     * @param oAuthState  저장할 state 객체 (state + code_verifier)
     * @param ttl         만료 시간(Duration)
     *
     * [동작 과정]
     * 1. "oauth:state:{state}" 형태의 Redis 키 생성
     * 2. code_verifier 필드 저장
     * 3. TTL 설정 → 일정 시간이 지나면 자동 삭제
     */
    public void save(OAuthState oAuthState, Duration ttl) {
        String key = buildKey(oAuthState.getState());
        stringRedisTemplate.opsForValue().set(key, oAuthState.getCodeVerifier(), ttl);
    }

    /**
     * *******************************************
     * state 조회 (존재 검증 + 복원)
     * *******************************************
     *
     * @param state  인가 요청 시 생성한 state 문자열
     * @return 저장된 OAuthState(Optional)
     *
     * [동작 과정]
     * 1. "oauth:state:{state}" 키로 조회
     * 2. 존재하지 않으면 Optional.empty() 반환
     * 3. 존재하면 code_verifier를 읽어서 OAuthState 복원
     */
    public Optional<OAuthState> find(String state) {
        String key = buildKey(state);

        String codeVerifier = stringRedisTemplate.opsForValue().get(key);
        if (codeVerifier == null) {
            return Optional.empty();
        }

        return Optional.of(OAuthState.builder()
                .state(state)
                .codeVerifier(codeVerifier)
                .build());
    }

    /**
     * *******************************************
     * state 삭제 (원타임 보장)
     * *******************************************
     *
     * @param state 삭제할 state 문자열
     *
     * [동작 목적]
     * - 콜백 처리 완료 후 state 재사용을 방지
     * - 로그인 취소나 에러 발생 시도 즉시 삭제하여 리플레이 공격 차단
     */
    public void delete(String state) {
        stringRedisTemplate.delete(buildKey(state));
    }

    /**
     * Redis 키 네임스페이스 생성
     * @param state state 문자열
     * @return oauth:state:{state} 형식의 키
     */
    private String buildKey(String state) {
        return "oauth:state:" + state;
    }
}
