package com.cojac.storyteller.unit.user;

import com.cojac.storyteller.common.mail.MailService;
import com.cojac.storyteller.common.redis.RedisService;
import com.cojac.storyteller.user.dto.*;
import com.cojac.storyteller.user.dto.oauth.GoogleLoginRequestDTO;
import com.cojac.storyteller.user.dto.oauth.KakaoLoginRequestDTO;
import com.cojac.storyteller.user.entity.LocalUserEntity;
import com.cojac.storyteller.user.entity.SocialUserEntity;
import com.cojac.storyteller.user.exception.DuplicateEmailException;
import com.cojac.storyteller.user.exception.DuplicateUsernameException;
import com.cojac.storyteller.user.exception.RequestParsingException;
import com.cojac.storyteller.user.jwt.JWTUtil;
import com.cojac.storyteller.user.repository.LocalUserRepository;
import com.cojac.storyteller.user.repository.SocialUserRepository;
import com.cojac.storyteller.user.jwt.oauth.GoogleTokenVerifier;
import com.cojac.storyteller.user.service.UserService;
import com.google.api.client.googleapis.auth.oauth2.GoogleIdToken;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.function.Executable;
import org.mockito.*;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;

import java.util.Optional;

import static com.cojac.storyteller.user.service.security.LogoutFilter.REFRESH_TOKEN_PREFIX;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

/**
 * 단위 테스트 클래스
 *
 * 이 클래스는 개별 단위(주로 서비스 또는 비즈니스 로직) 기능을 검증하기 위한 단위 테스트를 포함합니다.
 *
 * 주요 특징:
 * - 모의 객체(mock objects)를 사용하여 외부 의존성을 제거하고,테스트 대상 객체의 로직에만 집중합니다.
 * - 테스트의 독립성을 보장하여, 각 테스트가 서로에게 영향을 미치지 않도록 합니다.
 *
 * 테스트 전략:
 * - 간단한 기능이나 로직에 대한 테스트는 단위 테스트를 사용하십시오.
 * - 시스템의 전체적인 동작 및 상호작용을 검증하기 위해 통합 테스트를 활용하십시오.
 *
 * 참고: 단위 테스트는 실행 속도가 빠르며,
 *       전체 시스템의 동작보다는 개별 단위의 동작을 검증하는 데 중점을 둡니다.
 */
public class UserServiceUnitTest {

    @InjectMocks
    private UserService userService;

    @Mock
    private LocalUserRepository localUserRepository;
    @Mock
    private SocialUserRepository socialUserRepository;
    @Mock
    private JWTUtil jwtUtil;
    @Mock
    private RedisService redisService;
    @Mock
    private MailService mailService;
    @Mock
    private HttpServletRequest request;
    @Mock
    private HttpServletResponse response;
    @Mock
    private BCryptPasswordEncoder bCryptPasswordEncoder;
    @Mock
    private GoogleTokenVerifier googleTokenVerifier;
    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);

    }

    /**
     * 카카오 소셜 로그인
     */
    @Test
    @DisplayName("카카오 소셜 로그인 요청이 유효할 경우 SocialUserDTO 반환")
    void kakaoLogin_ShouldReturnSocialUserDTO_WhenValidKakaoLoginRequest() throws Exception {
        // given
        KakaoLoginRequestDTO kakaoLoginRequestDTO = new KakaoLoginRequestDTO("12345", "nickname", "email@example.com", "USER");
        HttpServletResponse response = mock(HttpServletResponse.class);

        SocialUserEntity mockUser = new SocialUserEntity("kakao_12345", "nickname", "email@example.com", "USER");

        when(socialUserRepository.findByAccountId("kakao_12345")).thenReturn(Optional.empty());
        when(socialUserRepository.save(any(SocialUserEntity.class))).thenReturn(mockUser);
        when(jwtUtil.createJwt(any(), any(), any(), any(), any())).thenReturn("accessToken", "refreshToken");

        // then
        SocialUserDTO result = userService.kakaoLogin(kakaoLoginRequestDTO, response);

        // when
        assertNotNull(result);
        assertEquals(mockUser.getAccountId(), result.getAccountId());
        verify(response).setHeader("access", "accessToken");
        verify(response).setHeader("refresh", "refreshToken");
    }

    /**
     * 구글 소셜 로그인
     */
    @Test
    public void testGoogleLogin() throws Exception {
        // Given
        GoogleLoginRequestDTO googleLoginRequestDTO = new GoogleLoginRequestDTO("mockIdToken", "ROLE_USER");

        // Mock GoogleIdToken.Payload
        GoogleIdToken.Payload payload = mock(GoogleIdToken.Payload.class);
        when(payload.getSubject()).thenReturn("1234567890");
        when(payload.get("name")).thenReturn("Test User");
        when(payload.getEmail()).thenReturn("test@example.com");

        // Mock TokenVerifier behavior
        when(googleTokenVerifier.verifyIdToken(googleLoginRequestDTO)).thenReturn(payload);

        // Mock SocialUserRepository behavior
        SocialUserEntity socialUserEntity = new SocialUserEntity("google_1234567890", "Test User", "test@example.com", "ROLE_USER");
        when(socialUserRepository.findByAccountId("google_1234567890")).thenReturn(Optional.empty());
        when(socialUserRepository.save(any(SocialUserEntity.class))).thenReturn(socialUserEntity);

        // When
        SocialUserDTO result = userService.googleLogin(googleLoginRequestDTO, response);

        // Then
        assertNotNull(result);
        assertEquals("google_1234567890", result.getAccountId());
        assertEquals("Test User", result.getNickname());
        assertEquals("test@example.com", result.getEmail());
    }

    /**
     * 회원 등록하기
     */
    @Test
    @DisplayName("회원 등록 요청이 유효할 경우 LocalUserDTO 반환")
    void registerUser_ShouldReturnLocalUserDTO_WhenValidCreateUserRequest() {
        CreateUserRequestDTO createUserRequestDTO = new CreateUserRequestDTO();
        createUserRequestDTO.setUsername("testUser");
        createUserRequestDTO.setPassword("password123");
        createUserRequestDTO.setEmail("test@example.com");
        createUserRequestDTO.setRole("USER");

        when(localUserRepository.existsByUsername("testUser")).thenReturn(false);
        when(bCryptPasswordEncoder.encode("password123")).thenReturn("hashedPassword");
        when(localUserRepository.save(any(LocalUserEntity.class))).thenReturn(new LocalUserEntity());

        LocalUserDTO result = userService.registerUser(createUserRequestDTO);

        assertNotNull(result);
        assertEquals("testUser", result.getUsername());
        verify(localUserRepository).save(any(LocalUserEntity.class));
    }

    @Test
    @DisplayName("존재하는 사용자 이름으로 회원 등록 요청 시 DuplicateUsernameException 발생")
    void registerUser_ShouldThrowDuplicateUsernameException_WhenUsernameExists() {
        // given
        CreateUserRequestDTO createUserRequestDTO = new CreateUserRequestDTO();
        createUserRequestDTO.setUsername("existingUser");
        createUserRequestDTO.setPassword("password123");
        createUserRequestDTO.setEmail("email@example.com");
        createUserRequestDTO.setRole("USER");

        when(localUserRepository.existsByUsername(createUserRequestDTO.getUsername())).thenReturn(true);

        // when & then
        assertThrows(DuplicateUsernameException.class, () -> {
            userService.registerUser(createUserRequestDTO);
        });

        verify(localUserRepository).existsByUsername(createUserRequestDTO.getUsername());
    }


    /**
     * 유저 아이디 중복 검증
     */
    @Test
    @DisplayName("사용자 이름이 사용 가능할 경우 인증 결과 true 반환")
    void verifiedUsername_ShouldReturnAuthResultTrue_WhenUsernameIsAvailable() {
        // given
        String username = "testUser";
        UsernameDTO usernameDTO = UsernameDTO.builder().username(username).build();
        when(localUserRepository.findByUsername(username)).thenReturn(Optional.empty());

        // when
        UsernameDTO result = userService.verifiedUsername(usernameDTO);

        // then
        assertTrue(result.isAuthResult());
    }

    @Test
    @DisplayName("사용자 이름이 사용 중일 경우 인증 결과 false 반환")
    void verifiedUsername_ShouldReturnAuthResultFalse_WhenUsernameIsTaken() {
        // given
        String username = "existingUser";
        UsernameDTO usernameDTO = UsernameDTO.builder().username(username).build();
        when(localUserRepository.findByUsername(username)).thenReturn(Optional.of(new LocalUserEntity()));

        // when
        UsernameDTO result = userService.verifiedUsername(usernameDTO);

        // then
        assertFalse(result.isAuthResult());
    }

    /**
     * 토큰 재발급
     */
    @Test
    @DisplayName("유효한 refresh token이 있을 경우 UserDTO 반환")
    void reissueToken_ShouldReturnUserDTO_WhenRefreshTokenIsValidAndLocalUser() throws Exception {
        // given
        String refreshToken = "validRefreshToken";
        String username = "username";
        ReissueDTO reissueDTO = new ReissueDTO(username, "someAccountId");
        LocalUserEntity mockUser = LocalUserEntity.builder()
                .username(username)
                .build();

        when(request.getHeader("refresh")).thenReturn(refreshToken);
        when(jwtUtil.getAuthenticationMethod(refreshToken)).thenReturn("local");
        when(jwtUtil.getCategory(refreshToken)).thenReturn("refresh");
        when(localUserRepository.findByUsername(username)).thenReturn(Optional.of(mockUser));
        String refreshTokenKey = REFRESH_TOKEN_PREFIX + username;
        when(redisService.getAndDelete(refreshTokenKey)).thenReturn(refreshToken);
        when(redisService.checkExistsValue(refreshToken)).thenReturn(true);
        when(jwtUtil.getUserKey(refreshToken)).thenReturn(username);
        when(jwtUtil.createJwt(any(), any(), any(), any(), any()))
                .thenReturn("newAccessToken", "newRefreshToken");

        // when
        UserDTO result = userService.reissueToken(request, response, reissueDTO);

        // then
        assertNotNull(result);
        verify(response).setHeader("access", "newAccessToken");
        verify(response).setHeader("refresh", "newRefreshToken");
    }

    @Test
    @DisplayName("유효한 refresh token이 있을 경우 SocialUserDTO 반환")
    void reissueToken_ShouldReturnUserDTO_WhenRefreshTokenIsValidAndSocialUser() throws Exception {
        // given
        String refreshToken = "validRefreshToken";
        String accountId = "accountId";
        ReissueDTO reissueDTO = new ReissueDTO("username", accountId);
        SocialUserEntity mockUser = SocialUserEntity.builder()
                .accountId(accountId)
                .build();

        when(request.getHeader("refresh")).thenReturn(refreshToken);
        when(jwtUtil.getAuthenticationMethod(refreshToken)).thenReturn("social");
        when(jwtUtil.getCategory(refreshToken)).thenReturn("refresh");
        when(socialUserRepository.findByAccountId(accountId)).thenReturn(Optional.of(mockUser));
        String refreshTokenKey = REFRESH_TOKEN_PREFIX + accountId;
        when(redisService.getAndDelete(refreshTokenKey)).thenReturn(refreshToken);
        when(redisService.checkExistsValue(refreshToken)).thenReturn(true);
        when(jwtUtil.getUserKey(refreshToken)).thenReturn(accountId);
        when(jwtUtil.createJwt(any(), any(), any(), any(), any()))
                .thenReturn("newAccessToken", "newRefreshToken");

        // when
        UserDTO result = userService.reissueToken(request, response, reissueDTO);

        // then
        assertNotNull(result);
        verify(response).setHeader("access", "newAccessToken");
        verify(response).setHeader("refresh", "newRefreshToken");
    }

    @Test
    @DisplayName("refresh token이 유효하지 않을 경우 예외 발생")
    void reissueToken_ShouldThrowException_WhenRefreshTokenIsInvalid() {
        // given
        String refreshToken = null;
        when(request.getHeader("refresh")).thenReturn(refreshToken);

        // when
        Executable executable = () -> userService.reissueToken(request, response, new ReissueDTO());

        // then
        assertThrows(RequestParsingException.class, executable);
    }

    @Test
    @DisplayName("Redis에 토큰이 없을 경우 예외 발생 (getAndDelete → \"false\")")
    void reissueToken_ShouldThrowException_WhenTokenNotInRedis() {
        // given
        String refreshToken = "validRefreshToken";
        String username = "username";
        ReissueDTO reissueDTO = new ReissueDTO(username, "someAccountId");

        when(request.getHeader("refresh")).thenReturn(refreshToken);
        when(jwtUtil.getAuthenticationMethod(refreshToken)).thenReturn("local");
        when(jwtUtil.getCategory(refreshToken)).thenReturn("refresh");
        String refreshTokenKey = REFRESH_TOKEN_PREFIX + username;
        when(redisService.getAndDelete(refreshTokenKey)).thenReturn("false"); // 키 없음
        when(redisService.checkExistsValue("false")).thenReturn(false);

        // when & then
        assertThrows(RequestParsingException.class,
                () -> userService.reissueToken(request, response, reissueDTO));
    }

    @Test
    @DisplayName("Redis 저장 토큰과 요청 토큰이 다를 경우 예외 발생 (구 토큰 재사용 차단)")
    void reissueToken_ShouldThrowException_WhenStoredTokenMismatch() {
        // given
        String requestToken = "requestToken";
        String storedToken = "differentToken"; // 다른 기기에서 재발급된 최신 토큰
        String username = "username";
        ReissueDTO reissueDTO = new ReissueDTO(username, "someAccountId");

        when(request.getHeader("refresh")).thenReturn(requestToken);
        when(jwtUtil.getAuthenticationMethod(requestToken)).thenReturn("local");
        when(jwtUtil.getCategory(requestToken)).thenReturn("refresh");
        String refreshTokenKey = REFRESH_TOKEN_PREFIX + username;
        when(redisService.getAndDelete(refreshTokenKey)).thenReturn(storedToken);
        when(redisService.checkExistsValue(storedToken)).thenReturn(true);

        // when & then
        assertThrows(RequestParsingException.class,
                () -> userService.reissueToken(request, response, reissueDTO));
    }

    /**
     * 인증 코드 검증을 위한 이메일 전송
     */
    @Test
    @DisplayName("중복되지 않은 이메일로 인증 코드 전송")
    void sendCodeToEmail_ShouldSendEmail_WhenEmailIsNotDuplicated() {
        // given
        String email = "test@example.com";
        when(localUserRepository.existsByEmail(email)).thenReturn(false);
        when(socialUserRepository.existsByEmail(email)).thenReturn(false);

        // when
        userService.sendCodeToEmail(email);

        // then
        verify(mailService).sendEmail(any(), any(), any());
    }

    @Test
    @DisplayName("중복된 이메일로 인증 코드 전송 시 예외 발생")
    void sendCodeToEmail_ShouldThrowException_WhenEmailIsDuplicated() {
        // given
        String email = "duplicate@example.com";
        when(localUserRepository.existsByEmail(email)).thenReturn(true);
        when(socialUserRepository.existsByEmail(email)).thenReturn(false);

        // when
        Executable executable = () -> userService.sendCodeToEmail(email);

        // then
        assertThrows(DuplicateEmailException.class, executable);
    }

    /**
     * 인증 코드 검증하기
     */
    @Test
    @DisplayName("유효한 인증 코드로 인증 결과 true 반환")
    void verifiedCode_ShouldReturnAuthResultTrue_WhenCodeIsValid() {
        // given
        String email = "test@example.com";
        String authCode = "123456";
        when(redisService.getValues("email_code:" + email)).thenReturn(authCode);
        when(redisService.checkExistsValue(authCode)).thenReturn(true);

        // when
        EmailDTO result = userService.verifiedCode(email, authCode);

        // then
        assertTrue(result.isAuthResult());
    }

    @Test
    @DisplayName("유효하지 않은 인증 코드로 인증 결과 false 반환")
    void verifiedCode_ShouldReturnAuthResultFalse_WhenCodeIsInvalid() {
        // given
        String email = "test@example.com";
        String authCode = "123456";
        when(redisService.getValues("email_code:" + email)).thenReturn("wrongCode");
        when(redisService.checkExistsValue(any())).thenReturn(true);

        // when
        EmailDTO result = userService.verifiedCode(email, authCode);

        // then
        assertFalse(result.isAuthResult());
    }
}
