package server.poptato.configuration;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.boot.test.autoconfigure.orm.jpa.TestEntityManager;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.test.context.ActiveProfiles;
import org.testcontainers.containers.MySQLContainer;
import org.testcontainers.containers.output.Slf4jLogConsumer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

@DataJpaTest
@Testcontainers
@ActiveProfiles("unittest")
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
public abstract class DatabaseTestConfig {

    private static final Logger log = LoggerFactory.getLogger(DatabaseTestConfig.class);
    private static final String MYSQL_IMAGE = "mysql:8.0.42";

    @Autowired
    protected TestEntityManager tem;


    @Container
    @ServiceConnection
    private static final MySQLContainer<?> MYSQL_CONTAINER =
            new MySQLContainer<>(MYSQL_IMAGE)
                    .withCommand("--default-time-zone=+09:00")
                    .withLogConsumer(new Slf4jLogConsumer(log))
                    .withReuse(true);

}