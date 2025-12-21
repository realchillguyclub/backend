package server.poptato.auth.infra;

import lombok.RequiredArgsConstructor;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Repository;
import server.poptato.user.domain.value.MobileType;

import java.time.Duration;
import java.util.Optional;
import java.util.Set;

@Repository
@RequiredArgsConstructor
public class JwtRepository {

    private static final String REFRESH_PREFIX = "jwt:refresh:";

    private final StringRedisTemplate stringRedisTemplate;

    /**
     * 리프레시 토큰 저장
     *
     * @param userId      유저 ID
     * @param mobileType  모바일 타입
     * @param refreshToken 저장할 리프레시 토큰
     * @param ttlDays     만료 기간(일)
     */
    public void saveRefreshToken(String userId, MobileType mobileType, String refreshToken, Duration ttlDays) {
        String key = buildKey(userId, mobileType);
        stringRedisTemplate.opsForValue().set(key, refreshToken, ttlDays);
    }

    /**
     * 리프레시 토큰 조회
     *
     * @param userId 유저 ID
     * @param mobileType  모바일 타입
     * @return 저장된 리프레시 토큰(Optional)
     */
    public Optional<String> findRefreshToken(String userId, MobileType mobileType) {
        String key = buildKey(userId, mobileType);
        String value = stringRedisTemplate.opsForValue().get(key);
        return Optional.ofNullable(value);
    }

    /**
     * 리프레시 토큰 삭제
     *
     * @param userId 유저 ID
     * @param mobileType  모바일 타입
     */
    public void deleteRefreshToken(String userId, MobileType mobileType) {
        String key = buildKey(userId, mobileType);
        stringRedisTemplate.delete(key);
    }

    /**
     * 특정 유저의 모든 기기(ANDROID/IOS/DESKTOP)에 대한 리프레시 토큰을 삭제합니다.
     * - jwt:refresh:{userId}:* 형태의 키를 모두 조회 후 일괄 삭제합니다.
     */
    public void deleteAllRefreshTokens(String userId) {
        String pattern = REFRESH_PREFIX + userId + ":" + "*";
        Set<String> keys = stringRedisTemplate.keys(pattern);
        if (!keys.isEmpty()) {
            stringRedisTemplate.delete(keys);
        }
    }

    /**
     * Redis 키 생성
     */
    private String buildKey(String userId, MobileType mobileType) {
        return REFRESH_PREFIX + userId + ":" + mobileType.name();
    }
}
