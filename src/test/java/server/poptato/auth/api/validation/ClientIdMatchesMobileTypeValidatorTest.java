package server.poptato.auth.api.validation;

import jakarta.validation.Validation;
import jakarta.validation.Validator;
import jakarta.validation.ValidatorFactory;
import jakarta.validation.ConstraintViolation;
import org.junit.jupiter.api.*;
import server.poptato.user.domain.value.MobileType;

import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;

@TestMethodOrder(MethodOrderer.DisplayName.class)
@DisplayName("[SCN-API-AUTH-VALID-001] 로그인/로그아웃/토큰 갱신 시 mobileType과 clientId를 검증한다.")
class ClientIdMatchesMobileTypeValidatorTest {

    private static Validator validator;

    @BeforeAll
    static void setup() {
        ValidatorFactory factory = Validation.buildDefaultValidatorFactory();
        validator = factory.getValidator();
    }

    @ClientIdMatchesMobileType
    record HasMobileTypeAndClientIdTestDto(
            MobileType mobileType,
            String clientId
    ) implements HasMobileTypeAndClientId { }

    @Test
    @DisplayName("[TC-VALID-001] mobileType이 null일 때 검증이 실패하고, 에러 필드는 mobileType으로 지정된다")
    void mobileType_null_is_invalid_on_mobileType_field() {
        // Arrange
        HasMobileTypeAndClientIdTestDto dto = new HasMobileTypeAndClientIdTestDto(null, "client-id");

        // Act
        Set<ConstraintViolation<HasMobileTypeAndClientIdTestDto>> violations = validator.validate(dto);

        // Assert
        assertSingleViolationOn(violations, "mobileType", "로그인/회원가입/로그아웃 시 모바일 타입은 필수입니다.");
    }

    @Test
    @DisplayName("[TC-VALID-002] mobileType이 DESKTOP일 때 clientId가 null 또는 공백이면 검증이 통과한다")
    void desktop_without_clientId_ok() {
        // Arrange
        HasMobileTypeAndClientIdTestDto dto1 = new HasMobileTypeAndClientIdTestDto(MobileType.DESKTOP, null);
        HasMobileTypeAndClientIdTestDto dto2 = new HasMobileTypeAndClientIdTestDto(MobileType.DESKTOP, "");
        HasMobileTypeAndClientIdTestDto dto3 = new HasMobileTypeAndClientIdTestDto(MobileType.DESKTOP, "   ");

        // Act
        Set<ConstraintViolation<HasMobileTypeAndClientIdTestDto>> violations1 = validator.validate(dto1);
        Set<ConstraintViolation<HasMobileTypeAndClientIdTestDto>> violations2 = validator.validate(dto2);
        Set<ConstraintViolation<HasMobileTypeAndClientIdTestDto>> violations3 = validator.validate(dto3);

        // Assert
        assertThat(violations1).isEmpty();
        assertThat(violations2).isEmpty();
        assertThat(violations3).isEmpty();
    }

    @Test
    @DisplayName("[TC-VALID-003] mobileType이 DESKTOP일 때 clientId가 존재하면 검증이 실패하고, 에러 필드는 clientId로 지정된다")
    void desktop_with_clientId_invalid_on_clientId_field() {
        // Arrange
        HasMobileTypeAndClientIdTestDto dto = new HasMobileTypeAndClientIdTestDto(MobileType.DESKTOP, "client-id");

        // Act
        Set<ConstraintViolation<HasMobileTypeAndClientIdTestDto>> violations = validator.validate(dto);

        // Assert
        assertSingleViolationOn(violations, "clientId", "로그인/회원가입/로그아웃 시 DESKTOP은 clientId를 보내면 안 됩니다.");
    }

    @Test
    @DisplayName("[TC-VALID-004] DESKTOP이 아닌 mobileType일 때 clientId가 유효하면 검증이 통과한다")
    void mobile_requires_clientId_ok_when_present() {
        // Arrange
        HasMobileTypeAndClientIdTestDto dto = new HasMobileTypeAndClientIdTestDto(MobileType.ANDROID, "  client-id  ");

        // Act
        Set<ConstraintViolation<HasMobileTypeAndClientIdTestDto>> violations = validator.validate(dto);

        // Assert
        assertThat(violations).isEmpty();
    }

    @Test
    @DisplayName("[TC-VALID-005] DESKTOP이 아닌 mobileType일 때 clientId가 null 또는 공백이면 검증이 실패하고, 에러 필드는 clientId로 지정된다.")
    void mobile_requires_clientId_invalid_when_blank() {
        // Arrange
        HasMobileTypeAndClientIdTestDto nullClientId = new HasMobileTypeAndClientIdTestDto(MobileType.IOS, null);
        HasMobileTypeAndClientIdTestDto blankClientId = new HasMobileTypeAndClientIdTestDto(MobileType.IOS, "   ");

        // Act
        Set<ConstraintViolation<HasMobileTypeAndClientIdTestDto>> violations1 = validator.validate(nullClientId);
        Set<ConstraintViolation<HasMobileTypeAndClientIdTestDto>> violations2 = validator.validate(blankClientId);

        // Assert
        assertSingleViolationOn(violations1, "clientId",
                "로그인/회원가입/로그아웃 시 모바일 타입(" + nullClientId.mobileType() + ")은 clientId가 필수입니다.");
        assertSingleViolationOn(violations2, "clientId",
                "로그인/회원가입/로그아웃 시 모바일 타입(" + nullClientId.mobileType() + ")은 clientId가 필수입니다.");
    }

    private static void assertSingleViolationOn(
            Set<? extends ConstraintViolation<?>> violations,
            String field,
            String message
    ) {
        assertThat(violations).hasSize(1);
        ConstraintViolation<?> constraintViolation = violations.iterator().next();
        assertThat(constraintViolation.getPropertyPath().toString()).isEqualTo(field);
        assertThat(constraintViolation.getMessage()).isEqualTo(message);
    }
}
