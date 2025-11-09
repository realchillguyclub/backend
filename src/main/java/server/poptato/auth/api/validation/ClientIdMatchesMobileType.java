package server.poptato.auth.api.validation;

import jakarta.validation.Constraint;
import jakarta.validation.Payload;
import java.lang.annotation.*;

@Documented
@Target({ElementType.TYPE})
@Retention(RetentionPolicy.RUNTIME)
@Constraint(validatedBy = ClientIdMatchesMobileTypeValidator.class)
public @interface ClientIdMatchesMobileType {
    String message() default "모바일 타입과 clientId 규칙이 일치하지 않습니다.";
    Class<?>[] groups() default {};
    Class<? extends Payload>[] payload() default {};
}