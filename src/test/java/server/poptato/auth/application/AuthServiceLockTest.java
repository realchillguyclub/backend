package server.poptato.auth.application;

import org.assertj.core.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.context.annotation.Import;
import org.springframework.data.redis.core.StringRedisTemplate;
import server.poptato.auth.api.request.LoginRequestDto;
import server.poptato.auth.application.response.LoginResponseDto;
import server.poptato.auth.application.service.AuthService;
import server.poptato.auth.application.service.JwtService;
import server.poptato.auth.status.AuthErrorStatus;
import server.poptato.configuration.RedisTestConfig;


import server.poptato.global.dto.TokenPair;
import server.poptato.global.exception.CustomException;
import server.poptato.infra.lock.DistributedLockFacade;
import server.poptato.infra.lock.LettuceLockRepository;
import server.poptato.infra.oauth.SocialService;
import server.poptato.infra.oauth.SocialServiceProvider;
import server.poptato.infra.oauth.SocialUserInfo;
import server.poptato.user.application.event.CreateUserEvent;
import server.poptato.user.domain.entity.User;
import server.poptato.user.domain.repository.MobileRepository;
import server.poptato.user.domain.repository.UserRepository;
import server.poptato.user.domain.value.MobileType;
import server.poptato.user.domain.value.SocialType;
import server.poptato.user.validator.UserValidator;

import java.util.List;
import java.util.Optional;
import java.util.concurrent.*;
import java.util.stream.IntStream;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.when;

@Import({LettuceLockRepository.class, DistributedLockFacade.class})
class AuthServiceLockTest extends RedisTestConfig {

    private AuthService authService;

    @Autowired
    DistributedLockFacade distributedLockFacade;

    @Autowired
    StringRedisTemplate stringRedisTemplate;

    @MockBean
    JwtService jwtService;

    @MockBean
    SocialServiceProvider socialServiceProvider;

    @MockBean
    UserValidator userValidator;

    @MockBean
    ApplicationEventPublisher eventPublisher;

    @MockBean
    UserRepository userRepository;

    @MockBean
    MobileRepository mobileRepository;

    @MockBean
    SocialService socialService;

    private static String lockKey(String socialId) {
        return "lock:" + socialId;
    }

    @BeforeEach
    void setUp() {
        this.authService = new AuthService(
                jwtService,
                socialServiceProvider,
                userValidator,
                eventPublisher,
                userRepository,
                mobileRepository,
                distributedLockFacade
        );
    }

    @BeforeEach
    void cleanRedis() {
        org.junit.jupiter.api.Assertions.assertNotNull(stringRedisTemplate.getConnectionFactory());
        stringRedisTemplate.getConnectionFactory().getConnection().serverCommands().flushAll();
    }

    @Test
    @DisplayName("[SCN-SVC-AUTH-001][TC-SVC-LOGIN-005] 신규 유저에 대해 동시에 여러번 요청 시 유저 등록은 한 번만 성공한다")
    void login_동시에_여러번_신규_유저_요청시_한번만_저장() throws InterruptedException, TimeoutException {
        // given
        final String socialId = "concurrent-integration-test-id";
        final int threadCount = 24;
        SocialUserInfo socialUserInfo = new SocialUserInfo(socialId, "tester", "test@test.com", "https://image.com");
        LoginRequestDto requestDto = new LoginRequestDto(SocialType.KAKAO, "access-token", MobileType.ANDROID, "client-id", "테스터", "test@test.com");
        TokenPair tokenPair = new TokenPair("access-token", "refresh-token");

        when(socialServiceProvider.getSocialService(any())).thenReturn(socialService);
        when(socialService.getUserData(any())).thenReturn(socialUserInfo);
        when(userRepository.findBySocialId(socialId)).thenReturn(Optional.empty());

        User savedUser = Mockito.mock(User.class);
        when(savedUser.getId()).thenReturn(100L);
        when(savedUser.getSocialType()).thenReturn(SocialType.KAKAO);
        when(userRepository.save(any(User.class))).thenReturn(savedUser);
        when(userRepository.count()).thenReturn(1L);
        when(jwtService.generateTokenPair(anyString(), any())).thenReturn(tokenPair);

        // when
        ExecutorService pool = Executors.newFixedThreadPool(threadCount);
        CyclicBarrier barrier = new CyclicBarrier(threadCount);

        List<Callable<LoginResponseDto>> tasks = IntStream.range(0, threadCount)
                .mapToObj(i -> (Callable<LoginResponseDto>) () -> {
                    barrier.await(2, TimeUnit.SECONDS);
                    return authService.login(requestDto);
                }).toList();

        List<Future<LoginResponseDto>> futures = pool.invokeAll(tasks, 10, TimeUnit.SECONDS);

        int success = 0;
        int failed = 0;
        for (Future<LoginResponseDto> future : futures) {
            try {
                LoginResponseDto loginResponseDto = future.get(7, TimeUnit.SECONDS);
                Assertions.assertThat(loginResponseDto).isNotNull();
                success++;
            } catch (ExecutionException e) {
                Assertions.assertThat(e.getCause()).isInstanceOf(CustomException.class);
                Assertions.assertThat(((CustomException)e.getCause()).getErrorCode())
                        .isEqualTo(AuthErrorStatus._SIGNUP_IN_PROGRESS);
                failed++;
            }
        }
        pool.shutdown();

        // then
        Assertions.assertThat(pool.awaitTermination(15, TimeUnit.SECONDS)).isTrue();

        Assertions.assertThat(success).isEqualTo(1);
        Assertions.assertThat(failed).isEqualTo(threadCount - 1);

        Mockito.verify(userRepository, Mockito.times(1)).findBySocialId(socialId);
        Mockito.verify(userRepository, Mockito.times(1)).save(any(User.class));
        Mockito.verify(userRepository, Mockito.times(1)).count();
        Mockito.verify(eventPublisher, Mockito.times(1)).publishEvent(any(CreateUserEvent.class));

        Assertions.assertThat(stringRedisTemplate.hasKey(lockKey(socialId))).isFalse();
    }
}
