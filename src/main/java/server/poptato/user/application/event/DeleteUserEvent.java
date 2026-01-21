package server.poptato.user.application.event;

import server.poptato.user.domain.entity.Mobile;
import server.poptato.user.domain.entity.User;
import server.poptato.user.domain.value.Reason;

import java.time.LocalDateTime;
import java.util.List;

public record DeleteUserEvent(
        String userName,
        String mobileType,
        String socialType,
        LocalDateTime createDate,
        List<String> deleteReasons

) {
    public static DeleteUserEvent from(User user, Mobile mobile, List<Reason> deleteReasons, String userInputReason) {
        List<String> reasonValues = (deleteReasons == null ? List.<Reason>of() : deleteReasons).stream()
                .map(Reason::getValue)
                .collect(java.util.stream.Collectors.toList());

        if (userInputReason != null && !userInputReason.isBlank()) {
            reasonValues.add(userInputReason);
        }

        return new DeleteUserEvent(
                user.getName(),
                mobile.getType().toString(),
                user.getSocialType().toString(),
                user.getCreateDate(),
                reasonValues
        );
    }
}
