package server.poptato.auth.infra;

import lombok.RequiredArgsConstructor;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.concurrent.TimeUnit;

@Repository
@RequiredArgsConstructor
public class JwtRepository {

    private final StringRedisTemplate stringRedisTemplate;

    /**
     * 리프레시 토큰 저장
     *
     * @param userId      유저 ID
     * @param refreshToken 저장할 리프레시 토큰
     * @param ttlDays     만료 기간(일)
     */
    public void saveRefreshToken(String userId, String refreshToken, long ttlDays) {
        stringRedisTemplate.opsForValue().set(userId, refreshToken, ttlDays, TimeUnit.DAYS);
    }

    /**
     * 리프레시 토큰 조회
     *
     * @param userId 유저 ID
     * @return 저장된 리프레시 토큰(Optional)
     */
    public Optional<String> findRefreshToken(String userId) {
        String value = stringRedisTemplate.opsForValue().get(userId);
        return Optional.ofNullable(value);
    }

    /**
     * 리프레시 토큰 삭제
     *
     * @param userId 유저 ID
     */
    public void deleteRefreshToken(String userId) {
        stringRedisTemplate.delete(userId);
    }
}
