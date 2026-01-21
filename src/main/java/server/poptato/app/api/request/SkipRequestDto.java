package server.poptato.app.api.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import server.poptato.app.domain.value.Platform;

public record SkipRequestDto(
        @NotBlank(message = "currentVersion은 필수입니다.")
        String currentVersion,

        @NotBlank(message = "targetVersion은 필수입니다.")
        String targetVersion,

        @NotNull(message = "platform은 필수입니다.")
        Platform platform
) {
}
