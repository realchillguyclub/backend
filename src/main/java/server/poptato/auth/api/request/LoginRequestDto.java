package server.poptato.auth.api.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import server.poptato.auth.api.validation.ClientIdMatchesMobileType;
import server.poptato.auth.api.validation.HasMobileTypeAndClientId;
import server.poptato.user.domain.value.MobileType;
import server.poptato.user.domain.value.SocialType;

@ClientIdMatchesMobileType
public record LoginRequestDto(
        @NotNull(message = "로그인/회원가입 시 소셜 타입은 필수입니다.")
        SocialType socialType,
        @NotBlank(message = "로그인/회원가입 시 액세스 토큰은 필수입니다.")
        String accessToken,
        MobileType mobileType,
        String clientId,
        String name,
        String email
) implements HasMobileTypeAndClientId {
}
