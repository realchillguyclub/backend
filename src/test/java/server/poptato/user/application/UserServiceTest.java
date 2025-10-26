package server.poptato.user.application;

import org.junit.jupiter.api.*;
import org.junit.jupiter.api.Nested;
import org.mockito.*;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.test.util.ReflectionTestUtils;
import server.poptato.auth.application.service.JwtService;
import server.poptato.category.domain.repository.CategoryRepository;
import server.poptato.configuration.ServiceTestConfig;
import server.poptato.global.exception.CustomException;
import server.poptato.user.api.request.UserCommentRequestDTO;
import server.poptato.user.api.request.UserDeleteRequestDTO;
import server.poptato.user.application.event.CreateUserCommentEvent;
import server.poptato.user.application.event.DeleteUserEvent;
import server.poptato.user.application.response.UserInfoResponseDto;
import server.poptato.user.application.service.UserService;
import server.poptato.user.domain.entity.Comment;
import server.poptato.user.domain.entity.Mobile;
import server.poptato.user.domain.entity.User;
import server.poptato.user.domain.repository.CommentRepository;
import server.poptato.user.domain.repository.DeleteReasonRepository;
import server.poptato.user.domain.repository.MobileRepository;
import server.poptato.user.domain.repository.UserRepository;
import server.poptato.user.domain.value.MobileType;
import server.poptato.user.domain.value.Reason;
import server.poptato.user.domain.value.SocialType;
import server.poptato.user.status.MobileErrorStatus;
import server.poptato.user.status.UserErrorStatus;
import server.poptato.user.validator.UserValidator;

import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.BDDMockito.*;
import static org.mockito.Mockito.never;

public class UserServiceTest extends ServiceTestConfig {

    @Mock
    JwtService jwtService;

    @Mock
    UserValidator userValidator;

    @Mock
    ApplicationEventPublisher eventPublisher;

    @Mock
    UserRepository userRepository;

    @Mock
    DeleteReasonRepository deleteReasonRepository;

    @Mock
    CategoryRepository categoryRepository;

    @Mock
    CommentRepository commentRepository;

    @Mock
    MobileRepository mobileRepository;

    @InjectMocks
    private UserService userService;

    @AfterEach
    void validateMockito() {
        Mockito.validateMockitoUsage();
    }

    private User user(Long id, String name) {
        User u = User.builder()
                .socialType(SocialType.KAKAO)
                .socialId("social-id")
                .name(name)
                .email("test@test.com")
                .imageUrl("https://image.com")
                .isPushAlarm(true)
                .build();
        ReflectionTestUtils.setField(u, "id", id);
        return u;
    }

    private Mobile mobile(Long userId) {
        return Mobile.builder()
                .userId(userId)
                .type(MobileType.ANDROID)
                .clientId("client-id")
                .build();
    }

    private Comment comment(Long id, Long userId, String content) {
        Comment c = Comment.builder()
                .userId(userId)
                .content(content)
                .build();
        ReflectionTestUtils.setField(c, "id", id);
        return c;
    }

    private UserDeleteRequestDTO requestDTO(List<Reason> reasons, String userInput) {
        return new UserDeleteRequestDTO(reasons, userInput);
    }

    private UserCommentRequestDTO requestDTO() {
        return mock(UserCommentRequestDTO.class);
    }


    @Nested
    @TestMethodOrder(MethodOrderer.DisplayName.class)
    @DisplayName("[SCN-SVC-USER-001] 사용자 탈퇴를 처리한다.")
    class DeleteUserScenario {

        @Test
        @DisplayName("[TC-DELETE-NORMAL-001] 유효한 사용자와 모바일이 존재하면 탈퇴 사유를 저장하고 관련 데이터 삭제 후 DeleteUserEvent를 발행한다")
        void deleteUser_normal_success() {
            // given
            Long userId = 100L;
            User found = user(userId, "tester");
            List<Reason> reasons = List.of(Reason.MISSING_FEATURES, Reason.NOT_USED_OFTEN);
            String userInput = "Hello world!!";
            UserDeleteRequestDTO request = requestDTO(reasons, userInput);

            given(userValidator.checkIsExistAndReturnUser(userId)).willReturn(found);
            given(mobileRepository.findTopByUserIdOrderByModifyDateDesc(found.getId()))
                    .willReturn(Optional.of(mobile(userId)));

            // when
            userService.deleteUser(userId, request);

            // then
            then(deleteReasonRepository).should(times(reasons.size() + 1)).save(any());
            then(userRepository).should().delete(found);
            then(categoryRepository).should().deleteByUserId(userId);
            then(jwtService).should().deleteRefreshToken(String.valueOf(userId));

            ArgumentCaptor<DeleteUserEvent> captor = ArgumentCaptor.forClass(DeleteUserEvent.class);
            then(eventPublisher).should().publishEvent(captor.capture());

            DeleteUserEvent deleteUserEvent = captor.getValue();
            assertThat(deleteUserEvent).isNotNull();
            assertThat(deleteUserEvent.userName()).isEqualTo(found.getName());
            assertThat(deleteUserEvent.mobileType()).isEqualTo(MobileType.ANDROID.toString());
            assertThat(deleteUserEvent.socialType()).isEqualTo(found.getSocialType().toString());

            List<String> expected = reasons.stream().map(Reason::getValue).collect(Collectors.toList());
            expected.add(userInput);
            assertThat(deleteUserEvent.deleteReasons()).containsExactlyInAnyOrderElementsOf(expected);
        }

        @Test
        @DisplayName("[TC-DELETE-EXCEPTION-001] 모바일 정보가 없으면 예외를 던진다")
        void deleteUser_mobileNotFound_throw() {
            // given
            Long userId = 101L;
            User found = user(userId, "tester");
            UserDeleteRequestDTO request = requestDTO(List.of(Reason.MISSING_FEATURES), "bye");

            given(userValidator.checkIsExistAndReturnUser(userId)).willReturn(found);
            given(mobileRepository.findTopByUserIdOrderByModifyDateDesc(found.getId()))
                    .willReturn(Optional.empty());

            // when & then
            assertThatThrownBy(() -> userService.deleteUser(userId, request))
                    .isInstanceOf(CustomException.class)
                    .hasMessageContaining(MobileErrorStatus._NOT_FOUND_MOBILE_BY_USER_ID.getMessage());

            then(deleteReasonRepository).shouldHaveNoInteractions();
            then(userRepository).shouldHaveNoInteractions();
            then(categoryRepository).shouldHaveNoInteractions();
            then(jwtService).shouldHaveNoInteractions();
            then(eventPublisher).should(never()).publishEvent(any());
        }

        @Test
        @DisplayName("[TC-DELETE-EXCEPTION-002] 사용자가 존재하지 않으면 예외를 던진다")
        void deleteUser_userNotFound_throw() {
            // given
            Long userId = 102L;
            UserDeleteRequestDTO request = requestDTO(List.of(Reason.NOT_USED_OFTEN), "bye");
            given(userValidator.checkIsExistAndReturnUser(userId))
                    .willThrow(new CustomException(UserErrorStatus._USER_NOT_EXIST));

            // when & then
            assertThatThrownBy(() -> userService.deleteUser(userId, request))
                    .isInstanceOf(CustomException.class)
                    .hasMessageContaining(UserErrorStatus._USER_NOT_EXIST.getMessage());

            then(mobileRepository).shouldHaveNoInteractions();
            then(deleteReasonRepository).shouldHaveNoInteractions();
            then(userRepository).shouldHaveNoInteractions();
            then(categoryRepository).shouldHaveNoInteractions();
            then(jwtService).shouldHaveNoInteractions();
            then(eventPublisher).shouldHaveNoInteractions();
        }

        @Test
        @DisplayName("[TC-DELETE-REASON-001] 탈퇴 사유 입력 조합에 따라 DeleteReason 저장 횟수가 올바르게 달라진다")
        void deleteUser_reasonCombination_savesExpectedTimes() {
            // given
            Long userId = 103L;
            User found = user(userId, "tester");
            given(userValidator.checkIsExistAndReturnUser(userId)).willReturn(found);
            given(mobileRepository.findTopByUserIdOrderByModifyDateDesc(found.getId()))
                    .willReturn(Optional.of(mobile(userId)));

            // (a) reasons만 존재
            {
                reset(deleteReasonRepository, userRepository, categoryRepository, jwtService, eventPublisher);
                UserDeleteRequestDTO request = requestDTO(List.of(Reason.TOO_COMPLEX, Reason.NOT_USED_OFTEN), null);

                userService.deleteUser(userId, request);

                then(deleteReasonRepository).should(times(2)).save(any());
                then(userRepository).should().delete(found);
                then(categoryRepository).should().deleteByUserId(userId);
                then(jwtService).should().deleteRefreshToken(String.valueOf(userId));
                then(eventPublisher).should().publishEvent(any(DeleteUserEvent.class));
            }

            // (b) userInputReason만 존재(공백 아님)
            {
                reset(deleteReasonRepository, userRepository, categoryRepository, jwtService, eventPublisher);
                UserDeleteRequestDTO request = requestDTO(List.of(), "custom reason");

                userService.deleteUser(userId, request);

                then(deleteReasonRepository).should(times(1)).save(any());
                then(userRepository).should().delete(found);
                then(categoryRepository).should().deleteByUserId(userId);
                then(jwtService).should().deleteRefreshToken(String.valueOf(userId));
                then(eventPublisher).should().publishEvent(any(DeleteUserEvent.class));
            }

            // (c) 둘 다 없음 또는 blank
            {
                reset(deleteReasonRepository, userRepository, categoryRepository, jwtService, eventPublisher);
                UserDeleteRequestDTO request = requestDTO(List.of(), "   ");

                userService.deleteUser(userId, request);

                then(deleteReasonRepository).shouldHaveNoInteractions();
                then(userRepository).should().delete(found);
                then(categoryRepository).should().deleteByUserId(userId);
                then(jwtService).should().deleteRefreshToken(String.valueOf(userId));
                then(eventPublisher).should().publishEvent(any(DeleteUserEvent.class));
            }
        }
    }

    @Nested
    @TestMethodOrder(MethodOrderer.DisplayName.class)
    @DisplayName("[SCN-SVC-USER-002] 사용자 정보를 조회한다.")
    class GetUserInfoScenario {

        @Test
        @DisplayName("[TC-INFO-001] 존재하는 사용자의 정보를 조회하면 올바른 DTO를 반환한다")
        void getUserInfo_returnsDto() {
            // given
            Long userId = 200L;
            User found = user(userId, "tester");
            given(userValidator.checkIsExistAndReturnUser(userId)).willReturn(found);

            // when
            UserInfoResponseDto responseDto = userService.getUserInfo(userId);

            // then
            assertThat(responseDto).isNotNull();
            assertThat(responseDto.name()).isEqualTo("tester");
            assertThat(responseDto.email()).isEqualTo("test@test.com");
            assertThat(responseDto.imageUrl()).isEqualTo("https://image.com");

            then(userValidator).should().checkIsExistAndReturnUser(userId);
            then(userValidator).shouldHaveNoMoreInteractions();
        }

        @Test
        @DisplayName("[TC-INFO-EXCEPTION-001] 사용자가 존재하지 않으면 예외를 던진다")
        void getUserInfo_userNotFound_throws() {
            // given
            Long userId = 201L;
            willThrow(new CustomException(UserErrorStatus._USER_NOT_EXIST))
                    .given(userValidator).checkIsExistAndReturnUser(userId);

            // when & then
            assertThatThrownBy(() -> userService.getUserInfo(userId))
                    .isInstanceOf(CustomException.class);

            then(userValidator).should().checkIsExistAndReturnUser(userId);
            then(userValidator).shouldHaveNoMoreInteractions();
        }
    }

    @Nested
    @TestMethodOrder(MethodOrderer.DisplayName.class)
    @DisplayName("[SCN-SVC-USER-003] 사용자 의견을 저장하고 이벤트를 발행한다.")
    class CreateAndSendUserCommentScenario {

        @Test
        @DisplayName("[TC-COMMENT-NORMAL-001] 유효한 사용자와 모바일이 존재하면 코멘트를 저장하고 CreateUserCommentEvent를 발행한다")
        void createComment_normal_success() {
            // given
            Long userId = 300L;
            User found = user(userId, "tester");
            UserCommentRequestDTO requestDTO = requestDTO();
            Comment created = comment(10L, userId, "great app!");

            given(userValidator.checkIsExistAndReturnUser(userId)).willReturn(found);
            try (MockedStatic<Comment> mocked = Mockito.mockStatic(Comment.class)) {
                mocked.when(() -> Comment.createComment(requestDTO, userId)).thenReturn(created);
                given(commentRepository.save(created)).willReturn(created);
                given(mobileRepository.findTopByUserIdOrderByModifyDateDesc(found.getId()))
                        .willReturn(Optional.of(mobile(userId)));

                // when
                userService.createAndSendUserComment(userId, requestDTO);

                // then
                then(commentRepository).should().save(created);
                then(eventPublisher).should().publishEvent(any(CreateUserCommentEvent.class));
                then(mobileRepository).should().findTopByUserIdOrderByModifyDateDesc(userId);
            }
        }

        @Test
        @DisplayName("[TC-COMMENT-EXCEPTION-001] 사용자가 존재하지 않으면 예외를 던진다")
        void createComment_userNotFound_throw() {
            // given
            Long userId = 301L;
            UserCommentRequestDTO requestDTO = requestDTO();
            willThrow(new CustomException(UserErrorStatus._USER_NOT_EXIST))
                    .given(userValidator).checkIsExistAndReturnUser(userId);

            // when & then
            assertThatThrownBy(() -> userService.createAndSendUserComment(userId, requestDTO))
                    .isInstanceOf(CustomException.class);

            then(commentRepository).shouldHaveNoInteractions();
            then(mobileRepository).shouldHaveNoInteractions();
            then(eventPublisher).shouldHaveNoInteractions();
        }

        @Test
        @DisplayName("[TC-COMMENT-EXCEPTION-002] 코멘트는 저장되지만 모바일 정보가 없으면 예외를 던진다")
        void createComment_mobileNotFound_throw() {
            // given
            Long userId = 302L;
            User found = user(userId, "tester");
            UserCommentRequestDTO requestDTO = requestDTO();
            Comment created = comment(11L, userId, "any");

            given(userValidator.checkIsExistAndReturnUser(userId)).willReturn(found);

            try (MockedStatic<Comment> mocked = Mockito.mockStatic(Comment.class)) {
                mocked.when(() -> Comment.createComment(requestDTO, userId)).thenReturn(created);
                given(commentRepository.save(created)).willReturn(created);
                given(mobileRepository.findTopByUserIdOrderByModifyDateDesc(found.getId()))
                        .willReturn(Optional.empty());

                // when & then
                assertThatThrownBy(() -> userService.createAndSendUserComment(userId, requestDTO))
                        .isInstanceOf(CustomException.class)
                        .hasMessageContaining(MobileErrorStatus._NOT_FOUND_MOBILE_BY_USER_ID.getMessage());

                then(commentRepository).should().save(created);
                then(eventPublisher).should(never()).publishEvent(any());
            }
        }
    }
}
