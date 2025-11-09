package server.poptato.infra.firebase.application;

import java.time.LocalTime;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

import com.google.firebase.messaging.FirebaseMessagingException;
import com.google.firebase.messaging.MessagingErrorCode;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import server.poptato.infra.firebase.template.FcmNotificationTemplate;
import server.poptato.global.util.BatchUtil;
import server.poptato.todo.domain.entity.TimeAlarm;
import server.poptato.todo.domain.entity.Todo;
import server.poptato.todo.domain.repository.TimeAlarmRepository;
import server.poptato.todo.domain.repository.TodoRepository;
import server.poptato.todo.domain.value.TodayStatus;
import server.poptato.todo.domain.value.Type;
import server.poptato.user.domain.entity.Mobile;
import server.poptato.user.domain.entity.User;
import server.poptato.user.domain.repository.MobileRepository;
import server.poptato.user.domain.repository.UserRepository;

@Slf4j
@Service
@RequiredArgsConstructor
public class FcmNotificationBatchService {
    private final FcmService fcmService;
    private final FcmTokenService fcmTokenService;
    private final UserRepository userRepository;
    private final MobileRepository mobileRepository;
    private final TodoRepository todoRepository;
    private final TimeAlarmRepository timeAlarmRepository;

    @Value("${batch.size}")
    private int batchSize;

    /**
     * '오늘 할 일' 푸쉬알림을 전체 유저에게 전송한다.
     */
    @Async
    public void sendTodayTodosNotifications() {
        List<User> users = userRepository.findByIsPushAlarmTrue();
        BatchUtil.splitIntoBatches(users, batchSize).forEach(batch -> {
            for (User user : batch) {
                sendUserTodayTodosNotification(user);
            }
        });
    }

    /**
     * 유저에게 '오늘 할 일' 푸쉬알림을 전송한다.
     *
     * @param user 알림을 보낼 대상 유저
     */
    private void sendUserTodayTodosNotification(User user) {
		List<Todo> incompleteTodayTodos = todoRepository.findIncompleteTodayTodos(user.getId(), TodayStatus.INCOMPLETE);
		if (!incompleteTodayTodos.isEmpty()) {
			List<Mobile> mobiles = mobileRepository.findAllPushCapableByUserId(user.getId());
			for (Mobile mobile : mobiles) {
				for (Todo todo : incompleteTodayTodos) {
					String body = String.format(FcmNotificationTemplate.TODAY_TODOS.getBody(), todo.getContent());
					sendPushOrDeleteToken(
						mobile.getClientId(),
						FcmNotificationTemplate.TODAY_TODOS.getTitle(),
						body
					);
				}
			}
		}
    }

    /**
     * 하루 시작 푸쉬알림을, 아직 할 일이 없는 유저에게 전송한다.
     */
    @Async
    public void sendStartNotifications() {
        List<User> users = userRepository.findByIsPushAlarmTrue();
        BatchUtil.splitIntoBatches(users, batchSize).forEach(batch -> {
            for (User user : batch) {
				boolean hasIncompleteTodayTodos =
					todoRepository.existsByUserIdAndTypeAndTodayStatus(user.getId(), Type.TODAY, TodayStatus.INCOMPLETE);
				if (!hasIncompleteTodayTodos) {
					List<Mobile> mobiles = mobileRepository.findAllPushCapableByUserId(user.getId());
					for (Mobile mobile : mobiles) {
						sendPushOrDeleteToken(
							mobile.getClientId(),
							FcmNotificationTemplate.START_OF_DAY.getTitle(),
							FcmNotificationTemplate.START_OF_DAY.getBody()
						);
					}
				}
            }
        });
    }

    /**
     * 일과 정리 푸쉬알림을, 아직 할 일이 남은 유저에게 전송한다.
     */
    @Async
    public void sendEndOfDayNotifications() {
        List<User> users = userRepository.findByIsPushAlarmTrue();
        BatchUtil.splitIntoBatches(users, batchSize).forEach(batch -> {
            for (User user : batch) {
                boolean hasIncompleteTodayTodos =
                        todoRepository.existsByUserIdAndTypeAndTodayStatus(user.getId(), Type.TODAY, TodayStatus.INCOMPLETE);
                if (hasIncompleteTodayTodos) {
                    List<Mobile> mobiles = mobileRepository.findAllPushCapableByUserId(user.getId());
                    for (Mobile mobile : mobiles) {
                        sendPushOrDeleteToken(
                                mobile.getClientId(),
                                FcmNotificationTemplate.END_OF_DAY.getTitle(),
                                FcmNotificationTemplate.END_OF_DAY.getBody()
                        );
                    }
                }
            }
        });
    }

    /**
     * 시간이 설정된 할 일이 있는 유저에게 1시간 전 푸쉬알림을 전송한다.
     */
    @Async
    public void sendTimeDeadlineNotifications() {
        LocalTime from = LocalTime.now().withSecond(0).withNano(0);
        LocalTime to = from.plusHours(1);

        List<TimeAlarm> alarms = timeAlarmRepository.findPushEnabledAlarms(from, to);

        Map<Long, List<TimeAlarm>> alarmsByUser = alarms.stream()
                .collect(Collectors.groupingBy(TimeAlarm::getUserId));

        List<Long> userIds = alarmsByUser.keySet().stream().toList();
        BatchUtil.splitIntoBatches(userIds, batchSize).forEach(batch -> {
            for (Long userId : batch) {
                sendUserTimeNotification(userId, alarmsByUser.get(userId));
            }
        });
    }

    /**
     * 특정 유저의 시간 알림 목록에 대해 푸쉬알림을 전송한다.
     *
     * @param userId 유저 아이디
     * @param alarmList 알림을 보낼 목록
     */
    private void sendUserTimeNotification(Long userId, List<TimeAlarm> alarmList) {
        List<Mobile> mobiles = mobileRepository.findAllPushCapableByUserId(userId);
        for (Mobile mobile : mobiles) {
            for (TimeAlarm timeAlarm : alarmList) {
                todoRepository.findById(timeAlarm.getTodoId())
                        .ifPresent(todo -> {
                            sendPushOrDeleteToken(
                                    mobile.getClientId(),
                                    FcmNotificationTemplate.TIME_DEADLINE.getTitle(),
                                    todo.getContent());
                            timeAlarm.updateNotified(true);
                            timeAlarmRepository.save(timeAlarm);
                        });
            }
        }
    }

    /**
     * 이벤트 푸쉬알림을 전체 유저에게 전송한다.
     *
     * @param pushAlarmTitle 알림 제목
     * @param pushAlarmContent 알림 내용
     */
    @Async
    public void sendEventNotifications(String pushAlarmTitle, String pushAlarmContent) {
        List<User> users = userRepository.findByIsPushAlarmTrue();

        BatchUtil.splitIntoBatches(users, batchSize).forEach(batch -> {
            for (User user : batch) {
                List<Mobile> mobiles = mobileRepository.findAllPushCapableByUserId(user.getId());
                if (mobiles.isEmpty()) continue;

                for (Mobile mobile : mobiles) {
                    sendPushOrDeleteToken(
                            mobile.getClientId(),
                            pushAlarmTitle,
                            pushAlarmContent
                    );
                }
            }
        });
    }

    /**
     * FCM 푸쉬알림을 전송하거나, 유효하지 않은 토큰인 경우 삭제한다.
     *
     * @param clientId FCM 토큰 (디바이스 식별자)
     * @param title 알림 제목
     * @param body 알림 본문
     */
    private void sendPushOrDeleteToken(String clientId, String title, String body) {
        try {
            fcmService.sendPushNotification(clientId, title, body);
        } catch (FirebaseMessagingException e) {
            MessagingErrorCode code = e.getMessagingErrorCode();

            if (code == MessagingErrorCode.INVALID_ARGUMENT || code == MessagingErrorCode.UNREGISTERED) {
                log.warn("❌ 유효하지 않은 FCM 토큰 - clientId: {}, code: {}", clientId, code);
                fcmTokenService.deleteInvalidToken(clientId);
            } else {
                log.error("❌ FCM 전송 중 예외 발생 - clientId: {}, code: {}, message: {}", clientId, code, e.getMessage());
                throw new RuntimeException(e);
            }
        }
    }
}
