package server.poptato.auth.api.validation;

import jakarta.validation.ConstraintValidator;
import jakarta.validation.ConstraintValidatorContext;
import server.poptato.user.domain.value.MobileType;

public class ClientIdMatchesMobileTypeValidator
        implements ConstraintValidator<ClientIdMatchesMobileType, HasMobileTypeAndClientId> {

    @Override
    public boolean isValid(HasMobileTypeAndClientId dto, ConstraintValidatorContext constraintValidatorContext) {
        if (dto == null) {
            return true;
        }

        MobileType mobileType = dto.mobileType();
        String clientId = dto.clientId();
        String trimmed = (clientId == null) ? null : clientId.trim();

        boolean ok;
        String message = null;
        String errorField = "clientId";

        if (mobileType == null) {
            ok = false;
            message = "로그인/회원가입/로그아웃 시 모바일 타입은 필수입니다.";
            errorField = "mobileType";
        } else if (mobileType == MobileType.DESKTOP) {
            ok = (trimmed == null || trimmed.isEmpty());
            if (!ok) message = "로그인/회원가입/로그아웃 시 DESKTOP은 clientId를 보내면 안 됩니다.";
        } else {
            ok = (trimmed != null && !trimmed.isEmpty());
            if (!ok) message = "로그인/회원가입/로그아웃 시 모바일 타입(" + mobileType + ")은 clientId가 필수입니다.";
        }

        if (!ok) {
            constraintValidatorContext.disableDefaultConstraintViolation();
            constraintValidatorContext.buildConstraintViolationWithTemplate(message)
                    .addPropertyNode(errorField)
                    .addConstraintViolation();
        }
        return ok;
    }
}
