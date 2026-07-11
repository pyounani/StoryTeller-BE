package com.cojac.storyteller.user.service;

import com.cojac.storyteller.common.mail.MailService;
import com.cojac.storyteller.common.redis.RedisService;
import com.cojac.storyteller.response.code.ErrorCode;
import com.cojac.storyteller.user.dto.*;
import com.cojac.storyteller.user.dto.oauth.GoogleLoginRequestDTO;
import com.cojac.storyteller.user.jwt.oauth.GoogleTokenVerifier;
import com.cojac.storyteller.user.dto.oauth.KakaoLoginRequestDTO;
import com.cojac.storyteller.user.entity.LocalUserEntity;
import com.cojac.storyteller.user.entity.SocialUserEntity;
import com.cojac.storyteller.user.exception.*;
import com.cojac.storyteller.user.jwt.JWTUtil;
import com.cojac.storyteller.user.repository.LocalUserRepository;
import com.cojac.storyteller.user.repository.SocialUserRepository;
import com.google.api.client.googleapis.auth.oauth2.GoogleIdToken;
import io.jsonwebtoken.ExpiredJwtException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.io.IOException;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.time.Duration;
import java.util.Random;

@Service
@RequiredArgsConstructor
public class UserService {

    private static final String REFRESH_TOKEN_PREFIX = "refresh_token:";
    private static final String EMAIL_CODE_PREFIX = "email_code:";
    private static final long ACCESS_TOKEN_EXPIRATION = 86400000L; // 24 hours
    private static final long REFRESH_TOKEN_EXPIRATION = 1209600000L; // 14 days

    private final LocalUserRepository localUserRepository;
    private final SocialUserRepository socialUserRepository;
    private final BCryptPasswordEncoder bCryptPasswordEncoder;
    private final JWTUtil jwtUtil;
    private final RedisService redisService;
    private final MailService mailService;
    private final GoogleTokenVerifier googleTokenVerifier;

    @Value("${spring.mail.auth-code-expiration-millis}")
    private long authCodeExpirationMillis;
    @Value("${google.client.id}")
    private String CLIENT_ID;

    /**
     * 카카오 소셜 로그인
     */
    @Transactional
    public SocialUserDTO kakaoLogin(KakaoLoginRequestDTO kakaoLoginRequestDTO, HttpServletResponse response) {
        return handleSocialLogin(
                "kakao_" + kakaoLoginRequestDTO.getId(),
                kakaoLoginRequestDTO.getNickname(),
                kakaoLoginRequestDTO.getEmail(),
                kakaoLoginRequestDTO.getRole(),
                response
        );
    }

    /**
     * 구글 소셜 로그인
     */
    @Transactional
    public SocialUserDTO googleLogin(GoogleLoginRequestDTO googleLoginRequestDTO, HttpServletResponse response) throws Exception {
        GoogleIdToken.Payload payload = googleTokenVerifier.verifyIdToken(googleLoginRequestDTO);


        return handleSocialLogin(
                "google_" + payload.getSubject(),
                (String) payload.get("name"),
                payload.getEmail(),
                googleLoginRequestDTO.getRole(),
                response
        );
    }

    private SocialUserDTO handleSocialLogin(String accountId, String nickname, String email, String role, HttpServletResponse response) {
        SocialUserEntity socialUserEntity = findOrCreateSocialUser(accountId, nickname, email, role);
        socialUserRepository.save(socialUserEntity);

        // 토큰 생성
        String accessToken = jwtUtil.createJwt("social", "access", accountId, role, ACCESS_TOKEN_EXPIRATION);
        String refreshToken = jwtUtil.createJwt("social", "refresh", accountId, role, REFRESH_TOKEN_EXPIRATION);

        // Redis에 refresh 토큰 저장
        String refreshTokenKey = REFRESH_TOKEN_PREFIX + accountId;
        redisService.setValues(refreshTokenKey, refreshToken, Duration.ofMillis(REFRESH_TOKEN_EXPIRATION));

        // 응답 헤더에 JWT 설정
        response.setHeader("access", accessToken);
        response.setHeader("refresh", refreshToken);

        return SocialUserDTO.mapToSocialUserDTO(socialUserEntity);
    }

    private SocialUserEntity findOrCreateSocialUser(String accountId, String nickname, String email, String role) {
        return socialUserRepository.findByAccountId(accountId)
                .map(existingUser -> {
                    existingUser.updateUsername(nickname);
                    existingUser.updateEmail(email);
                    return existingUser;
                })
                .orElseGet(() -> new SocialUserEntity(accountId, nickname, email, role));
    }

    /**
     * 회원 등록하기
     */
    @Transactional
    public LocalUserDTO registerUser(CreateUserRequestDTO createUserRequestDTO) {
        String username = createUserRequestDTO.getUsername();
        String role = createUserRequestDTO.getRole();
        String email = createUserRequestDTO.getEmail();
        String encryptedPassword = bCryptPasswordEncoder.encode(createUserRequestDTO.getPassword());

        if (localUserRepository.existsByUsername(username)) {
            throw new DuplicateUsernameException(ErrorCode.DUPLICATE_USERNAME);
        }

        LocalUserEntity localUserEntity = LocalUserEntity.builder()
                .username(username)
                .encryptedPassword(encryptedPassword)
                .email(email)
                .role(role)
                .build();
        localUserRepository.save(localUserEntity);

        return LocalUserDTO.builder()
                .id(localUserEntity.getId())
                .username(username)
                .email(email)
                .role(role)
                .build();
    }

    /**
     * 유저 아이디 중복 검증
     */
    public UsernameDTO verifiedUsername(UsernameDTO usernameDTO) {
        String username = usernameDTO.getUsername();
        boolean authResult = localUserRepository.findByUsername(username)
                .map(user -> false)
                .orElse(true);
        return new UsernameDTO(username, authResult);
    }

    /**
     * 토큰 재발급
     */
    public UserDTO reissueToken(HttpServletRequest request, HttpServletResponse response, ReissueDTO reissueDTO) throws IOException {
        String refreshToken = getRefreshTokenFromRequest(request);
        validateToken(refreshToken);
        validateCategory(refreshToken);

        String authenticationMethod = jwtUtil.getAuthenticationMethod(refreshToken);
        if (authenticationMethod.equals("local")) {
            // 자체 로그인 사용자 검증
            return authenticateLocalUser(response, reissueDTO, refreshToken);
        } else if (authenticationMethod.equals("social")) {
            // 소셜 사용자 검증
            return authenticateSocialUser(response, reissueDTO, refreshToken);
        }
        else {
            throw new RequestParsingException(ErrorCode.INVALID_REFRESH_TOKEN);
        }
    }

    /**
     * 인증 코드 검증을 위한 이메일 전송
     */
    public void sendCodeToEmail(String toEmail) {
        this.checkDuplicatedEmail(toEmail);
        String title = "StoryTeller 이메일 인증 번호";
        String authCode = this.createCode();
        mailService.sendEmail(toEmail, title, authCode);

        // 이메일 인증 요청 시 인증 번호 Redis에 저장 ( key = "email_code:" + Email / value = AuthCode )
        redisService.setValues(EMAIL_CODE_PREFIX + toEmail,
                authCode, Duration.ofMillis(authCodeExpirationMillis));
    }

    /**
     * 인증 코드 검증하기
     */
    public EmailDTO verifiedCode(String email, String authCode) {
        this.checkDuplicatedEmail(email);
        String redisAuthCode = redisService.getValues(EMAIL_CODE_PREFIX + email);
        boolean authResult = redisService.checkExistsValue(redisAuthCode) && redisAuthCode.equals(authCode);

        return new EmailDTO(email, authCode, authResult);
    }

    private String getRefreshTokenFromRequest(HttpServletRequest request) {
        String refreshToken = request.getHeader("refresh");
        if (refreshToken == null) {
            throw new RequestParsingException(ErrorCode.TOKEN_MISSING);
        }
        return refreshToken;
    }

    private void validateToken(String refreshToken) {
        try {
            jwtUtil.isExpired(refreshToken);
        } catch (ExpiredJwtException e) {
            throw new AccessTokenExpiredException(ErrorCode.TOKEN_EXPIRED);
        }
    }

    private void validateCategory(String refreshToken) {
        String category = jwtUtil.getCategory(refreshToken);
        if (!category.equals("refresh")) {
            throw new RequestParsingException(ErrorCode.INVALID_REFRESH_TOKEN);
        }
    }

    private UserDTO authenticateLocalUser(HttpServletResponse response, ReissueDTO reissueDTO, String refreshToken) {
        String refreshTokenKey = REFRESH_TOKEN_PREFIX + reissueDTO.getUsername();

        // 원자적 GET+DEL: 동시 요청 중 첫 번째만 storedToken을 가져감 (Race Condition 방지)
        // 저장된 토큰과 요청 토큰 비교로 구 토큰 재사용 차단
        String storedToken = redisService.getAndDelete(refreshTokenKey);
        if (!redisService.checkExistsValue(storedToken) || !storedToken.equals(refreshToken)) {
            throw new RequestParsingException(ErrorCode.INVALID_REFRESH_TOKEN);
        }

        String username = jwtUtil.getUserKey(refreshToken);
        String role = jwtUtil.getRole(refreshToken);

        String newAccess = jwtUtil.createJwt("local", "access", username, role, ACCESS_TOKEN_EXPIRATION);
        String newRefresh = jwtUtil.createJwt("local", "refresh", username, role, REFRESH_TOKEN_EXPIRATION);

        // TTL 포함 저장 — 재발급 후에도 14일 만료 유지
        redisService.setValues(refreshTokenKey, newRefresh, Duration.ofMillis(REFRESH_TOKEN_EXPIRATION));

        response.setHeader("access", newAccess);
        response.setHeader("refresh", newRefresh);

        LocalUserEntity userEntity = localUserRepository.findByUsername(username)
                .orElseThrow(() -> new UserNotFoundException(ErrorCode.USER_NOT_FOUND));

        return LocalUserDTO.builder()
                .id(userEntity.getId())
                .username(username)
                .email(userEntity.getEmail())
                .role(role)
                .build();
    }

    private UserDTO authenticateSocialUser(HttpServletResponse response, ReissueDTO reissueDTO, String refreshToken) {
        String refreshTokenKey = REFRESH_TOKEN_PREFIX + reissueDTO.getAccountId();

        String storedToken = redisService.getAndDelete(refreshTokenKey);
        if (!redisService.checkExistsValue(storedToken) || !storedToken.equals(refreshToken)) {
            throw new RequestParsingException(ErrorCode.INVALID_REFRESH_TOKEN);
        }

        String accountId = jwtUtil.getUserKey(refreshToken);
        String role = jwtUtil.getRole(refreshToken);

        String newAccess = jwtUtil.createJwt("social", "access", accountId, role, ACCESS_TOKEN_EXPIRATION);
        String newRefresh = jwtUtil.createJwt("social", "refresh", accountId, role, REFRESH_TOKEN_EXPIRATION);

        redisService.setValues(refreshTokenKey, newRefresh, Duration.ofMillis(REFRESH_TOKEN_EXPIRATION));

        response.setHeader("access", newAccess);
        response.setHeader("refresh", newRefresh);

        SocialUserEntity socialUserEntity = socialUserRepository.findByAccountId(accountId)
                .orElseThrow(() -> new UserNotFoundException(ErrorCode.USER_NOT_FOUND));

        return SocialUserDTO.mapToSocialUserDTO(socialUserEntity);
    }

    private void checkDuplicatedEmail(String email) {
        if (localUserRepository.existsByEmail(email) || socialUserRepository.existsByEmail(email)) {
            throw new DuplicateEmailException(ErrorCode.DUPLICATE_EMAIL);
        }
    }

    private String createCode() {
        int len = 6;
        try {
            Random random = SecureRandom.getInstanceStrong();
            StringBuilder builder = new StringBuilder();
            for (int i = 0; i < len; i++) {
                builder.append(random.nextInt(10));
            }
            return builder.toString();
        } catch (NoSuchAlgorithmException e) {
            throw new BusinessLogicException(ErrorCode.NO_SUCH_ALGORITHM);
        }
    }
}
