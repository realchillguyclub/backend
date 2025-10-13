package server.poptato.configuration;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.test.autoconfigure.data.redis.DataRedisTest;
// @ServiceConnection을 사용하기 위한 import
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.test.context.ActiveProfiles;
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.containers.output.Slf4jLogConsumer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

@DataRedisTest
@Testcontainers
@ActiveProfiles("unittest")
public abstract class RedisTestConfig {

    private static final Logger log = LoggerFactory.getLogger(RedisTestConfig.class);
    private static final String REDIS_IMAGE = "redis:alpine";

    @Container
    @ServiceConnection
    private static final GenericContainer<?> REDIS_CONTAINER =
            new GenericContainer<>(REDIS_IMAGE)
                    .withExposedPorts(6379)
                    .withLogConsumer(new Slf4jLogConsumer(log))
                    .withReuse(true);

}