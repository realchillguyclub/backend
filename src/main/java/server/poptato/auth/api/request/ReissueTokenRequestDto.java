package server.poptato.auth.api.request;

import jakarta.validation.constraints.NotEmpty;
import server.poptato.auth.api.validation.ClientIdMatchesMobileType;
import server.poptato.auth.api.validation.HasMobileTypeAndClientId;
import server.poptato.user.domain.value.MobileType;

@ClientIdMatchesMobileType
public record ReissueTokenRequestDto(
    @NotEmpty(message = "토큰 재발급 시 accessToken은 필수입니다.")
    String accessToken,
    @NotEmpty(message = "토큰 재발급 시 refreshToken은 필수입니다.")
    String refreshToken,
    MobileType mobileType,
    String clientId
) implements HasMobileTypeAndClientId {
}
