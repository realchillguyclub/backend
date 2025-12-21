package server.poptato.auth.api.request;

import server.poptato.auth.api.validation.ClientIdMatchesMobileType;
import server.poptato.auth.api.validation.HasMobileTypeAndClientId;
import server.poptato.user.domain.value.MobileType;

@ClientIdMatchesMobileType
public record FCMTokenRequestDto(
        MobileType mobileType,
        String clientId
) implements HasMobileTypeAndClientId {
}
