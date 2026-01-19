package server.poptato.auth.application.service;

import java.nio.charset.StandardCharsets;
import java.security.Key;
import java.time.Duration;
import java.time.LocalDateTime;
import java.util.Base64;
import java.util.Date;
import java.util.UUID;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.ExpiredJwtException;
import io.jsonwebtoken.Header;
import io.jsonwebtoken.IncorrectClaimException;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.MalformedJwtException;
import io.jsonwebtoken.UnsupportedJwtException;
import io.jsonwebtoken.security.Keys;
import io.jsonwebtoken.security.SignatureException;
import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import server.poptato.auth.domain.entity.RefreshToken;
import server.poptato.auth.domain.repository.RefreshTokenRepository;
import server.poptato.auth.domain.value.TokenStatus;
import server.poptato.auth.status.AuthErrorStatus;
import server.poptato.global.dto.TokenPair;
import server.poptato.global.exception.CustomException;
import server.poptato.user.domain.value.MobileType;

@Slf4j
@Service
@RequiredArgsConstructor
public class JwtService {

    @Value("${jwt.secret}")
    private String jwtSecret;

    private static final String ISS = "ILLDAN_API_SERVER";
    private static final String USER_ID = "USER_ID";
    private static final String ACCESS_TOKEN = "ACCESS_TOKEN";
    private static final String REFRESH_TOKEN = "REFRESH_TOKEN";
    private static final int GRACE_PERIOD_SECONDS = 3;
    public static final Duration ACCESS_TOKEN_EXPIRATION_MINUTE = Duration.ofMinutes(20);
    public static final Duration REFRESH_TOKEN_EXPIRATION_DAYS = Duration.ofDays(14);

    private final RefreshTokenRepository refreshTokenRepository;

    /**
     * JWT 비밀키를 Base64로 인코딩합니다.
     * 이 메서드는 클래스 초기화 시 실행됩니다.
     */
    @PostConstruct
    protected void init() {
        jwtSecret = Base64.getEncoder()
                .encodeToString(jwtSecret.getBytes(StandardCharsets.UTF_8));
    }

    /**
     * 액세스 토큰을 생성합니다.
     *
     * @param userId 토큰에 포함할 유저 ID
     * @return 생성된 액세스 토큰
     */
    public String createAccessToken(final String userId) {
        final Date now = new Date();
        return Jwts.builder()
                .setHeaderParam(Header.TYPE, Header.JWT_TYPE)
                .setIssuer(ISS)
                .setSubject(ACCESS_TOKEN)
                .setIssuedAt(now)
                .setExpiration(new Date(now.getTime() + ACCESS_TOKEN_EXPIRATION_MINUTE.toMillis()))
                .claim(USER_ID, userId)
                .signWith(getSigningKey())
                .compact();
    }

    /**
     * 리프레시 토큰을 생성합니다. (jti 포함)
     *
     * @param userId 토큰에 포함할 유저 ID
     * @param jti JWT 고유 식별자
     * @return 생성된 리프레시 토큰
     */
    public String createRefreshToken(final String userId, final String jti) {
        final Date now = new Date();
        return Jwts.builder()
                .setHeaderParam(Header.TYPE, Header.JWT_TYPE)
                .setIssuer(ISS)
                .setSubject(REFRESH_TOKEN)
                .setIssuedAt(now)
                .setExpiration(new Date(now.getTime() + REFRESH_TOKEN_EXPIRATION_DAYS.toMillis()))
                .setId(jti)
                .claim(USER_ID, userId)
                .signWith(getSigningKey())
                .compact();
    }

    /**
     * 액세스 토큰의 유효성을 검증하고 Claims를 반환합니다.
     * 유효하지 않거나 만료된 토큰인 경우 예외를 발생시킵니다.
     *
     * @param token 검증할 액세스 토큰
     * @return 토큰의 클레임 정보
     * @throws CustomException 액세스 토큰이 유효하지 않거나 만료된 경우
     */
    public Claims verifyAccessToken(final String token) {
        try {
            return getBody(token);
        } catch (ExpiredJwtException e) {
            throw new CustomException(AuthErrorStatus._EXPIRED_ACCESS_TOKEN);
        } catch (UnsupportedJwtException | SignatureException | MalformedJwtException | IncorrectClaimException e) {
            throw new CustomException(AuthErrorStatus._INVALID_ACCESS_TOKEN);
        }
    }

    /**
     * 리프레쉬 토큰의 유효성을 검증하고 Claims를 반환합니다.
     * 유효하지 않거나 만료된 토큰인 경우 예외를 발생시킵니다.
     *
     * @param token 검증할 리프레쉬 토큰
     * @return 토큰의 클레임 정보
     * @throws CustomException 리프레쉬 토큰이 유효하지 않거나 만료된 경우
     */
    public Claims verifyRefreshToken(final String token) {
        try {
            return getBody(token);
        } catch (ExpiredJwtException e) {
            throw new CustomException(AuthErrorStatus._EXPIRED_REFRESH_TOKEN);
        } catch (UnsupportedJwtException | SignatureException | MalformedJwtException | IncorrectClaimException e) {
            throw new CustomException(AuthErrorStatus._INVALID_REFRESH_TOKEN);
        }
    }

    /**
     * 토큰에서 유저 ID를 추출합니다.
     *
     * @param token JWT 토큰
     * @return 토큰에 포함된 유저 ID
     */
    public String getUserIdInToken(final String token) {
        final Claims claims = getBody(token);
        return (String) claims.get(USER_ID);
    }

    /**
     * 토큰에서 jti를 추출합니다.
     *
     * @param token JWT 토큰
     * @return 토큰에 포함된 jti
     */
    public String getJtiFromToken(final String token) {
        final Claims claims = getBody(token);
        return claims.getId();
    }

    /**
     * 액세스 토큰과 리프레시 토큰으로 구성된 토큰 페어를 생성합니다.
     * 생성된 리프레시 토큰은 DB에 저장됩니다.
     *
     * @param userId     유저 ID
     * @param mobileType 모바일 타입
     * @param clientId   클라이언트 ID (모바일용)
     * @param userIp     클라이언트 IP
     * @param userAgent  User-Agent
     * @return 생성된 토큰 페어 (액세스 토큰, 리프레시 토큰)
     */
    @Transactional
    public TokenPair generateTokenPair(final Long userId, final MobileType mobileType,
                                       final String clientId, final String userIp, final String userAgent) {
        final String accessToken = createAccessToken(String.valueOf(userId));
        final String jti = UUID.randomUUID().toString();
        final String refreshToken = createRefreshToken(String.valueOf(userId), jti);

        LocalDateTime now = LocalDateTime.now();
        LocalDateTime expiryAt = now.plus(REFRESH_TOKEN_EXPIRATION_DAYS);

        RefreshToken token = RefreshToken.create(
                userId, jti, mobileType, clientId, refreshToken,
                now, expiryAt, userIp, userAgent
        );
        refreshTokenRepository.save(token);

        return new TokenPair(accessToken, refreshToken);
    }

    /**
     * DB에 저장된 리프레시 토큰을 검증합니다.
     * - ACTIVE 상태: 정상 진행
     * - ROTATED 상태 + Grace Period 내: 중복 요청으로 간주 (429)
     * - ROTATED 상태 + Grace Period 초과: 토큰 재사용 공격 탐지 → family 전체 revoke (401)
     * - 그 외 상태: 이미 사용된 토큰 (401)
     *
     * @param jti          토큰의 jti
     * @param refreshToken 입력받은 리프레시 토큰
     * @return 조회된 RefreshToken 엔티티
     * @throws CustomException 토큰 상태에 따른 예외 발생
     */
    @Transactional
    public RefreshToken validateAndGetRefreshToken(final String jti, final String refreshToken) {
        RefreshToken storedToken = refreshTokenRepository.findByJti(jti)
                .orElseThrow(() -> new CustomException(AuthErrorStatus._EXPIRED_OR_NOT_FOUND_REFRESH_TOKEN));

        if (!storedToken.getTokenValue().equals(refreshToken)) {
            throw new CustomException(AuthErrorStatus._DIFFERENT_REFRESH_TOKEN);
        }

        if (storedToken.isActive()) {
            return storedToken;
        }

        if (storedToken.getStatus() == TokenStatus.ROTATED) {
            if (isWithinGracePeriod(storedToken)) {
                throw new CustomException(AuthErrorStatus._DUPLICATED_REFRESH_REQUEST);
            }
            log.warn("[Token Reuse Detected] familyId={}, jti={}, userId={}",
                    storedToken.getFamilyId(), jti, storedToken.getUserId());
            refreshTokenRepository.revokeAllByFamilyId(storedToken.getFamilyId());
            throw new CustomException(AuthErrorStatus._TOKEN_REUSE_DETECTED);
        }

        throw new CustomException(AuthErrorStatus._ALREADY_USED_REFRESH_TOKEN);
    }

    /**
     * Grace Period 이내인지 확인합니다.
     */
    private boolean isWithinGracePeriod(RefreshToken token) {
        return token.getLastUsedAt() != null &&
                token.getLastUsedAt().plusSeconds(GRACE_PERIOD_SECONDS).isAfter(LocalDateTime.now());
    }

    /**
     * Token Rotation을 수행합니다.
     * 기존 토큰을 ROTATED 상태로 변경하고 새 토큰을 생성합니다.
     *
     * @param oldToken    기존 RefreshToken 엔티티
     * @param userIp      클라이언트 IP
     * @param userAgent   User-Agent
     * @return 새로운 토큰 페어
     */
    @Transactional
    public TokenPair rotateToken(final RefreshToken oldToken, final String userIp, final String userAgent) {
        oldToken.markAsRotated(userIp);
        refreshTokenRepository.save(oldToken);

        final String accessToken = createAccessToken(String.valueOf(oldToken.getUserId()));
        final String newJti = UUID.randomUUID().toString();
        final String newRefreshToken = createRefreshToken(String.valueOf(oldToken.getUserId()), newJti);

        LocalDateTime now = LocalDateTime.now();
        LocalDateTime expiryAt = now.plus(REFRESH_TOKEN_EXPIRATION_DAYS);

        RefreshToken newToken = oldToken.rotate(newJti, newRefreshToken, now, expiryAt, userIp, userAgent);
        refreshTokenRepository.save(newToken);

        return new TokenPair(accessToken, newRefreshToken);
    }

    /**
     * 특정 사용자 + 디바이스의 리프레시 토큰을 폐기합니다.
     *
     * @param userId     유저 ID
     * @param mobileType 모바일 타입
     */
    @Transactional
    public void revokeRefreshToken(final Long userId, final MobileType mobileType) {
        refreshTokenRepository.revokeByUserIdAndMobileType(userId, mobileType);
    }

    /**
     * 특정 유저의 모든 기기(ANDROID/IOS/DESKTOP)에 대한 리프레시 토큰을 폐기합니다.
     *
     * @param userId 유저 ID
     */
    @Transactional
    public void revokeAllRefreshTokens(final Long userId) {
        refreshTokenRepository.revokeAllByUserId(userId);
    }

    /**
     * JWT 토큰의 클레임 정보를 파싱하여 반환합니다.
     *
     * @param token 파싱할 JWT 토큰
     * @return 토큰의 클레임 정보
     */
    private Claims getBody(final String token) {
        return Jwts.parserBuilder()
                .setSigningKey(getSigningKey())
                .requireIssuer(ISS)
                .build()
                .parseClaimsJws(token)
                .getBody();
    }

    /**
     * JWT 서명에 사용할 키를 반환합니다.
     *
     * @return 서명 키
     */
    private Key getSigningKey() {
        final byte[] keyBytes = jwtSecret.getBytes(StandardCharsets.UTF_8);
        return Keys.hmacShaKeyFor(keyBytes);
    }

    /**
     * Authorization 헤더에서 사용자 ID를 추출합니다.
     * JWT 토큰을 검증하고, 유효한 경우 토큰에서 사용자 ID를 가져옵니다.
     *
     * @param authorization 요청 헤더의 Authorization (Bearer 토큰)
     * @return 토큰에서 추출된 사용자 ID
     * @throws CustomException 토큰이 없거나 유효하지 않은 경우 예외 발생
     */
    public Long extractUserIdFromToken(String authorization) {
        if (authorization == null || !authorization.startsWith("Bearer ")) {
            throw new CustomException(AuthErrorStatus._NOT_EXIST_ACCESS_TOKEN);
        }
        String token = authorization.substring("Bearer ".length());
        Claims claims = verifyAccessToken(token);
        return Long.parseLong((String) claims.get(USER_ID));
    }
}
