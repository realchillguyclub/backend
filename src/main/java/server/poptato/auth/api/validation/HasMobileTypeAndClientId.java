package server.poptato.auth.api.validation;

import server.poptato.user.domain.value.MobileType;

public interface HasMobileTypeAndClientId {
    MobileType mobileType();
    String clientId();
}
