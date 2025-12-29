package server.poptato.app.domain.value;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
public enum UpdateEventType {
    VERSION_CHECK("VERSION_CHECK"),
    DOWNLOADED("DOWNLOADED"),
    UPDATE_SKIPPED("UPDATE_SKIPPED");

    private final String value;
}
