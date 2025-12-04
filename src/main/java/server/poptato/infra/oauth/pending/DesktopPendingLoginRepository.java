package server.poptato.infra.oauth.pending;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Repository;
import server.poptato.user.domain.value.SocialType;

import java.time.Duration;
import java.util.Optional;

@Slf4j
@Repository
@RequiredArgsConstructor
public class DesktopPendingLoginRepository {

    private final StringRedisTemplate stringRedisTemplate;

    public void save(String state, String accessToken, Duration ttl) {
        String value = SocialType.KAKAO + "|" + accessToken;
        String redisKey = buildKey(state);
        stringRedisTemplate.opsForValue().set(redisKey, value, ttl);
    }


    public Optional<PendingLogin> find(String state) {
        String value = stringRedisTemplate.opsForValue().get(buildKey(state));
        if (value == null) {
            return Optional.empty();
        }

        String[] parts = value.split("\\|", 2);
        return Optional.of(new PendingLogin(SocialType.valueOf(parts[0]), parts[1]));
    }

    public void delete(String state) {
        stringRedisTemplate.delete(buildKey(state));
    }

    private String buildKey(String state) {
        return "oauth:desktop:pending:" + state;
    }
}
