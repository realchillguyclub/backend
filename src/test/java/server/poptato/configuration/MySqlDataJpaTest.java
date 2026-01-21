package server.poptato.configuration;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;

/**
 * Testcontainers MySQL을 사용하는 @DataJpaTest 커스텀 어노테이션
 * <p>
 * @DataJpaTest의 기본 동작(H2 인메모리 DB 사용)을 비활성화하고,
 * Testcontainers MySQL 컨테이너를 사용하도록 설정합니다.
 */
@Target(ElementType.TYPE)
@Retention(RetentionPolicy.RUNTIME)
@DataJpaTest
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
public @interface MySqlDataJpaTest {
}
