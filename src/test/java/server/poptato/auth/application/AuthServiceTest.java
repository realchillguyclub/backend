package server.poptato.auth.application;

import org.junit.jupiter.api.*;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.test.util.ReflectionTestUtils;
import server.poptato.auth.api.request.FCMTokenRequestDto;
import server.poptato.auth.api.request.LoginRequestDto;
import server.poptato.auth.api.request.ReissueTokenRequestDto;
import server.poptato.auth.application.response.LoginResponseDto;
import server.poptato.auth.application.service.AuthService;
import server.poptato.auth.application.service.JwtService;
import server.poptato.auth.status.AuthErrorStatus;
import server.poptato.configuration.ServiceTestConfig;
import server.poptato.infra.lock.DistributedLockFacade;
import server.poptato.infra.lock.status.LockErrorStatus;
import server.poptato.infra.oauth.SocialService;
import server.poptato.infra.oauth.SocialServiceProvider;
import server.poptato.infra.oauth.SocialUserInfo;
import server.poptato.global.dto.TokenPair;
import server.poptato.global.exception.CustomException;
import server.poptato.user.application.event.CreateUserEvent;
import server.poptato.user.domain.entity.Mobile;
import server.poptato.user.domain.entity.User;
import server.poptato.user.domain.repository.MobileRepository;
import server.poptato.user.domain.repository.UserRepository;
import server.poptato.user.domain.value.MobileType;
import server.poptato.user.domain.value.SocialType;
import server.poptato.user.status.MobileErrorStatus;
import server.poptato.user.validator.UserValidator;

import java.util.Optional;
import java.util.function.Supplier;
import java.util.stream.Stream;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

class AuthServiceTest extends ServiceTestConfig {

    @Mock
    SocialServiceProvider socialServiceProvider;

    @Mock
    SocialService socialService;

    @Mock
    JwtService jwtService;

    @Mock
    UserRepository userRepository;

    @Mock
    MobileRepository mobileRepository;

    @Mock
    DistributedLockFacade distributedLockFacade;

    @Mock
    UserValidator userValidator;

    @Mock
    ApplicationEventPublisher eventPublisher;

    @InjectMocks
    private AuthService authService;

    private static LoginRequestDto loginRequestDto(SocialType socialType, MobileType mobileType, String clientId, String name) {
        return new LoginRequestDto(socialType, "access-token", mobileType, clientId, name, "test@test.com");
    }

    private static SocialUserInfo socialUserInfo() {
        return new SocialUserInfo("social-id", "테스터", "test@test.com", "https://image.com");
    }

    private static ReissueTokenRequestDto reissueTokenRequestDto(String refreshToken, MobileType mobileType, String clientId) {
        return new ReissueTokenRequestDto("access-token", refreshToken, mobileType, clientId);
    }

    private static Stream<MobileType> 디바이스() {
        return Stream.of(MobileType.ANDROID, MobileType.DESKTOP);
    }

    @Nested
    @TestMethodOrder(MethodOrderer.DisplayName.class)
    @DisplayName("[SCN-SVC-AUTH-001] 회원가입 및 로그인 시 신규 유저 생성, 유저 정보를 업데이트 한다.")
    class Login {

        @Test
        @DisplayName("[TC-LOGIN-001] ANDROID에서 신규 유저 소셜 로그인 시, 유저가 생성되어 저장되고 응답에 isNew=true 가 포함된다")
        void login_새로운_유저_로그인_성공_ANDROID() {
            //given
            Long userId = 1L;
            String clientId = "client-id";
            LoginRequestDto requestDto = loginRequestDto(SocialType.KAKAO, MobileType.ANDROID, clientId, "tester");
            SocialUserInfo userInfo = socialUserInfo();

            when(socialServiceProvider.getSocialService(requestDto.socialType())).thenReturn(socialService);
            when(socialService.getUserData(requestDto)).thenReturn(userInfo);

            when(distributedLockFacade.executeWithLock(eq(userInfo.socialId()), any()))
                    .thenAnswer(invocation -> {
                        Supplier<LoginResponseDto> supplier = invocation.getArgument(1);
                        return supplier.get();
                    });

            when(userRepository.findBySocialId(userInfo.socialId())).thenReturn(Optional.empty());

            ArgumentCaptor<User> userCaptor = ArgumentCaptor.forClass(User.class);
            when(userRepository.save(any(User.class)))
                    .thenAnswer(invocation -> {
                        User user = invocation.getArgument(0);
                        ReflectionTestUtils.setField(user, "id", userId); // ID 주입
                        return user;
                    });

            when(userRepository.count()).thenReturn(1L);
            when(mobileRepository.findByClientId(requestDto.clientId())).thenReturn(Optional.empty());
            doNothing().when(eventPublisher).publishEvent(any(CreateUserEvent.class));
            TokenPair tokenPair = new TokenPair("access-token", "refresh-token");
            when(jwtService.generateTokenPair(String.valueOf(userId))).thenReturn(tokenPair);

            // when
            LoginResponseDto responseDto = authService.login(requestDto);

            // then
            assertThat(responseDto).isNotNull();
            assertThat(responseDto.accessToken()).isEqualTo("access-token");
            assertThat(responseDto.refreshToken()).isEqualTo("refresh-token");
            assertThat(responseDto.userId()).isEqualTo(userId);
            assertThat(responseDto.isNewUser()).isTrue();

            verify(userRepository).save(userCaptor.capture());
            User savedUser = userCaptor.getValue();
            assertThat(savedUser.getId()).isEqualTo(userId);
            assertThat(savedUser.getEmail()).isEqualTo(userInfo.email());
            verify(eventPublisher).publishEvent(any(CreateUserEvent.class));
            verify(mobileRepository).save(any(Mobile.class));
        }

        @Test
        @DisplayName("[TC-LOGIN-002] DESKTOP에서 신규 유저 소셜 로그인 시, 유저가 생성되어 저장되고 응답에 isNew=true 가 포함된다")
        void login_새로운_유저_로그인_성공_DESKTOP() {
            //given
            Long userId = 1L;
            LoginRequestDto requestDto = loginRequestDto(SocialType.KAKAO, MobileType.DESKTOP, null, "tester");
            SocialUserInfo userInfo = socialUserInfo();

            when(socialServiceProvider.getSocialService(requestDto.socialType())).thenReturn(socialService);
            when(socialService.getUserData(requestDto)).thenReturn(userInfo);

            when(distributedLockFacade.executeWithLock(eq(userInfo.socialId()), any()))
                    .thenAnswer(invocation -> {
                        Supplier<LoginResponseDto> supplier = invocation.getArgument(1);
                        return supplier.get();
                    });

            when(userRepository.findBySocialId(userInfo.socialId())).thenReturn(Optional.empty());

            ArgumentCaptor<User> userCaptor = ArgumentCaptor.forClass(User.class);
            when(userRepository.save(any(User.class)))
                    .thenAnswer(invocation -> {
                        User user = invocation.getArgument(0);
                        ReflectionTestUtils.setField(user, "id", userId);
                        return user;
                    });

            when(userRepository.count()).thenReturn(1L);
            when(mobileRepository.findByUserIdAndType(userId, requestDto.mobileType())).thenReturn(Optional.empty());
            doNothing().when(eventPublisher).publishEvent(any(CreateUserEvent.class));
            TokenPair tokenPair = new TokenPair("access-token", "refresh-token");
            when(jwtService.generateTokenPair(String.valueOf(userId))).thenReturn(tokenPair);

            // when
            LoginResponseDto responseDto = authService.login(requestDto);

            // then
            assertThat(responseDto).isNotNull();
            assertThat(responseDto.accessToken()).isEqualTo("access-token");
            assertThat(responseDto.refreshToken()).isEqualTo("refresh-token");
            assertThat(responseDto.userId()).isEqualTo(userId);
            assertThat(responseDto.isNewUser()).isTrue();

            verify(userRepository).save(userCaptor.capture());
            User savedUser = userCaptor.getValue();
            assertThat(savedUser.getId()).isEqualTo(userId);
            assertThat(savedUser.getEmail()).isEqualTo(userInfo.email());
            verify(eventPublisher).publishEvent(any(CreateUserEvent.class));
            verify(mobileRepository).save(any(Mobile.class));
        }

        @Test
        @DisplayName("[TC-LOGIN-003] 신규 유저 애플 로그인 시, name이 null 인 경우 AuthErrorStatus._HAS_NOT_NEW_APPLE_USER_NAME 예외가 발생한다")
        void login_새로운_APPLE_유저_로그인_실패() {
            //given
            LoginRequestDto requestDto = loginRequestDto(SocialType.APPLE, MobileType.IOS, "client-id", null);
            SocialUserInfo userInfo = socialUserInfo();
            when(socialServiceProvider.getSocialService(requestDto.socialType())).thenReturn(socialService);
            when(socialService.getUserData(requestDto)).thenReturn(userInfo);

            when(distributedLockFacade.executeWithLock(eq(userInfo.socialId()), any()))
                    .thenAnswer(invocation -> {
                        Supplier<LoginResponseDto> supplier = invocation.getArgument(1);
                        return supplier.get();
                    });

            when(userRepository.findBySocialId(userInfo.socialId())).thenReturn(Optional.empty());

            // when
            CustomException exception = assertThrows(CustomException.class, () -> authService.login(requestDto));

            // then
            assertThat(exception.getErrorCode()).isEqualTo(AuthErrorStatus._HAS_NOT_NEW_APPLE_USER_NAME);
        }

        @Test
        @DisplayName("[TC-LOGIN-004] 기존 유저 소셜 로그인 시, 유저의 정보(이미지url, fcm토큰)가 업데이트 되고 응답에 isNew=false가 포함된다")
        void login_존재하는_유저_로그인_성공() {
            //given
            String clientId = "client-id";
            LoginRequestDto requestDto = loginRequestDto(SocialType.KAKAO, MobileType.ANDROID, clientId, "tester");
            SocialUserInfo userInfo = socialUserInfo();
            Long userId = 1L;

            User existingUser = User.createUser(requestDto, userInfo, "https://image.com");
            ReflectionTestUtils.setField(existingUser, "id", userId);

            when(socialServiceProvider.getSocialService(requestDto.socialType())).thenReturn(socialService);
            when(socialService.getUserData(requestDto)).thenReturn(userInfo);

            when(distributedLockFacade.executeWithLock(eq(userInfo.socialId()), any()))
                    .thenAnswer(invocation -> {
                        Supplier<LoginResponseDto> supplier = invocation.getArgument(1);
                        return supplier.get();
                    });

            when(userRepository.findBySocialId(userInfo.socialId())).thenReturn(Optional.of(existingUser));
            when(mobileRepository.findByClientId(clientId)).thenReturn(Optional.of(mock(Mobile.class)));
            when(jwtService.generateTokenPair(String.valueOf(userId)))
                    .thenReturn(new TokenPair("access-token", "refresh-token"));

            // when
            LoginResponseDto response = authService.login(requestDto);

            // then
            assertThat(response.userId()).isEqualTo(userId);
            assertThat(response.isNewUser()).isFalse();
            assertThat(response.accessToken()).isEqualTo("access-token");
            assertThat(response.refreshToken()).isEqualTo("refresh-token");

            verify(userRepository).findBySocialId(userInfo.socialId());
            verify(jwtService).generateTokenPair(String.valueOf(userId));
            verify(mobileRepository).findByClientId(clientId);
            verify(userRepository, never()).save(existingUser);
        }

        @Test
        @DisplayName("[TC-SVC-LOGIN-005] 로그인 시 락 획득에 실패하면 AuthErrorStatus._SIGNUP_IN_PROGRESS 예외가 발생한다")
        void login_락_획득_실패_시_SIGNUP_IN_PROGRESS_예외_발생() {
            // given
            LoginRequestDto requestDto = new LoginRequestDto(SocialType.KAKAO, "access-token", MobileType.ANDROID, "client-id", null, null);
            SocialUserInfo userInfo = socialUserInfo();

            when(socialServiceProvider.getSocialService(requestDto.socialType())).thenReturn(socialService);
            when(socialService.getUserData(requestDto)).thenReturn(userInfo);

            when(distributedLockFacade.executeWithLock(eq(userInfo.socialId()), any()))
                    .thenThrow(new CustomException(LockErrorStatus._LOCK_ACQUISITION_FAILED));

            // when
            CustomException exception = assertThrows(CustomException.class, () -> authService.login(requestDto));

            // then
            assertThat(exception.getErrorCode()).isEqualTo(AuthErrorStatus._SIGNUP_IN_PROGRESS);
        }
    }

    @ParameterizedTest
    @MethodSource("디바이스")
    @DisplayName("[SCN-SVC-AUTH-002][TC-LOGOUT-001] 로그아웃시 정상적으로 로그아웃된다")
    void logout_로그아웃_성공(MobileType mobileType) {
        //given
        Long userId = 1L;
        FCMTokenRequestDto requestDto = new FCMTokenRequestDto(mobileType, "client-id");

        doNothing().when(userValidator).checkIsExistUser(userId);

        if (requestDto.mobileType() == MobileType.DESKTOP) {
            doNothing().when(mobileRepository).deleteByUserIdAndType(userId, requestDto.mobileType());
        } else {
            doNothing().when(mobileRepository).deleteByClientId(requestDto.clientId());
        }
        doNothing().when(jwtService).deleteRefreshToken(String.valueOf(userId));

        //when
        authService.logout(userId, requestDto);

        //then
        verify(userValidator).checkIsExistUser(userId);
        if (requestDto.mobileType() == MobileType.DESKTOP) {
            verify(mobileRepository).deleteByUserIdAndType(userId, requestDto.mobileType());
        } else {
            verify(mobileRepository).deleteByClientId(requestDto.clientId());
        }
        verify(jwtService).deleteRefreshToken(String.valueOf(userId));
    }

    @Nested
    @TestMethodOrder(MethodOrderer.DisplayName.class)
    @DisplayName("[SCN-SVC-AUTH-003] 토큰을 갱신한다")
    class RefreshToken {

        @Test
        @DisplayName("[TC-JWT-001] 유효한 리프레시 토큰을 기반으로 새로운 토큰 페어를 생성하여 반환한다.")
        void 토큰갱신_성공_새토큰페어반환() {
            // given
            Long userId = 100L;
            String refreshToken = "refresh-old";
            String clientId = "client-ok";

            ReissueTokenRequestDto dto = reissueTokenRequestDto(refreshToken, MobileType.ANDROID, clientId);

            doNothing().when(jwtService).verifyRefreshToken(refreshToken);
            when(jwtService.getUserIdInToken(refreshToken)).thenReturn(String.valueOf(userId));
            doNothing().when(jwtService).compareRefreshToken(String.valueOf(userId), refreshToken);
            doNothing().when(userValidator).checkIsExistUser(userId);

            Mobile mobile = mock(Mobile.class);
            when(mobileRepository.findByClientId(clientId)).thenReturn(Optional.of(mobile));

            TokenPair newPair = new TokenPair("access-new", "refresh-new");
            when(jwtService.generateTokenPair(String.valueOf(userId))).thenReturn(newPair);
            doNothing().when(jwtService).saveRefreshToken(String.valueOf(userId), "refresh-new");

            // when
            TokenPair result = authService.refresh(dto);

            // then
            assertThat(result.accessToken()).isEqualTo("access-new");
            assertThat(result.refreshToken()).isEqualTo("refresh-new");

            verify(jwtService).verifyRefreshToken(refreshToken);
            verify(jwtService).getUserIdInToken(refreshToken);
            verify(jwtService).compareRefreshToken(String.valueOf(userId), refreshToken);
            verify(userValidator).checkIsExistUser(userId);

            verify(mobileRepository).findByClientId(clientId);
            verify(mobileRepository, never()).findByUserIdAndType(anyLong(), any());

            verify(mobile).updateModifiedDate();
            verify(jwtService).generateTokenPair(String.valueOf(userId));
            verify(jwtService).saveRefreshToken(String.valueOf(userId), "refresh-new");
        }

        @Test
        @DisplayName("[TC-MOBILE-001] 존재하는 Mobile일 경우 접속한 날짜로 수정일을 변경한다")
        void 모바일_존재시_수정일_갱신() {
            // given
            Long userId = 200L;
            String refreshToken = "refresh-old";
            String clientId = "client-id";
            ReissueTokenRequestDto dto = reissueTokenRequestDto(refreshToken, MobileType.DESKTOP, clientId);

            doNothing().when(jwtService).verifyRefreshToken(refreshToken);
            when(jwtService.getUserIdInToken(refreshToken)).thenReturn(String.valueOf(userId));
            doNothing().when(jwtService).compareRefreshToken(String.valueOf(userId), refreshToken);
            doNothing().when(userValidator).checkIsExistUser(userId);

            Mobile mobile = mock(Mobile.class);
            when(mobileRepository.findByUserIdAndType(userId, MobileType.DESKTOP)).thenReturn(Optional.of(mobile));

            TokenPair pair = new TokenPair("access-new", "refresh-new");
            when(jwtService.generateTokenPair(String.valueOf(userId))).thenReturn(pair);
            doNothing().when(jwtService).saveRefreshToken(String.valueOf(userId), "refresh-new");

            // when
            TokenPair result = authService.refresh(dto);

            // then
            assertThat(result).isNotNull();
            verify(mobile).updateModifiedDate();
        }

        @Test
        @DisplayName("[TC-MOBILE-002] 존재하지 않는 Mobile일 경우 MobileErrorStatus._NOT_EXIST_MOBILE 예외가 발생한다")
        void 모바일_없으면_예외() {
            // given
            Long userId = 300L;
            String refreshToken = "refresh-token";
            String clientId = "client-id";
            ReissueTokenRequestDto dto = reissueTokenRequestDto(refreshToken, MobileType.DESKTOP, clientId);

            doNothing().when(jwtService).verifyRefreshToken(refreshToken);
            when(jwtService.getUserIdInToken(refreshToken)).thenReturn(String.valueOf(userId));
            doNothing().when(jwtService).compareRefreshToken(String.valueOf(userId), refreshToken);
            doNothing().when(userValidator).checkIsExistUser(userId);

            when(mobileRepository.findByUserIdAndType(userId, MobileType.DESKTOP)).thenReturn(Optional.empty());

            // when
            CustomException ex = assertThrows(CustomException.class, () -> authService.refresh(dto));

            // then
            assertThat(ex.getErrorCode()).isEqualTo(MobileErrorStatus._NOT_EXIST_MOBILE);

            verify(mobileRepository).findByUserIdAndType(userId, MobileType.DESKTOP);
            verify(mobileRepository, never()).findByClientId(anyString());

            verify(jwtService, never()).generateTokenPair(anyString());
            verify(jwtService, never()).saveRefreshToken(anyString(), anyString());
        }
    }
}