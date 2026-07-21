package com.lampify.service;

import com.lampify.dto.AuthRequest;
import com.lampify.dto.AuthResponse;
import com.lampify.dto.TwoFactorRequest;
import com.lampify.entity.PasswordResetToken;
import com.lampify.entity.RefreshToken;
import com.lampify.entity.User;
import com.lampify.repository.PasswordResetTokenRepository;
import com.lampify.repository.RefreshTokenRepository;
import com.lampify.repository.TwoFactorBackupCodeRepository;
import com.lampify.repository.UserRepository;
import com.lampify.security.JwtUtil;
import com.lampify.utils.CaptchaValidator;
import com.lampify.utils.OAuthTokenValidator;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.test.util.ReflectionTestUtils;

import java.time.Instant;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class AuthServiceTest {

    @Mock
    private UserRepository userRepository;

    @Mock
    private RefreshTokenRepository refreshTokenRepository;

    @Mock
    private PasswordResetTokenRepository passwordResetTokenRepository;

    @Mock
    private TwoFactorBackupCodeRepository twoFactorBackupCodeRepository;

    @Mock
    private JwtUtil jwtUtil;

    @Mock
    private CaptchaValidator captchaValidator;

    @Mock
    private OAuthTokenValidator oAuthValidator;

    @Mock
    private EmailService emailService;

    @Mock
    private TotpService totpService;

    @Mock
    private TokenRevocationService tokenRevocationService;

    @InjectMocks
    private AuthService authService;

    private final BCryptPasswordEncoder passwordEncoder = new BCryptPasswordEncoder();

    @BeforeEach
    void setUp() {
        ReflectionTestUtils.setField(authService, "passwordRecoveryExpirationMs", 3_600_000L);
    }

    @Test
    void registerRejectsWeakPassword() {
        AuthRequest request = new AuthRequest();
        request.setEmail("user@example.com");
        request.setPassword("password");
        request.setConfirmPassword("password");

        AuthResponse response = authService.register(request);

        assertFalse(response.isSuccess());
        assertTrue(response.getMessage().contains("uppercase"));
    }

    @Test
    void registerRejectsInvalidEmail() {
        AuthRequest request = new AuthRequest();
        request.setEmail("bad");
        request.setPassword("StrongP@ss1");
        request.setConfirmPassword("StrongP@ss1");

        AuthResponse response = authService.register(request);

        assertFalse(response.isSuccess());
        assertEquals("Invalid email format", response.getMessage());
    }

    @Test
    void registerRejectsDuplicateEmail() {
        AuthRequest request = new AuthRequest();
        request.setEmail("user@example.com");
        request.setPassword("StrongP@ss1");
        request.setConfirmPassword("StrongP@ss1");

        when(userRepository.existsByEmail("user@example.com")).thenReturn(true);

        AuthResponse response = authService.register(request);

        assertFalse(response.isSuccess());
        assertEquals("Email already registered", response.getMessage());
    }

    @Test
    void registerStoresBcryptHash() {
        AuthRequest request = new AuthRequest();
        request.setEmail("user@example.com");
        request.setPassword("StrongP@ss1");
        request.setConfirmPassword("StrongP@ss1");

        when(userRepository.existsByEmail("user@example.com")).thenReturn(false);
        when(userRepository.save(any(User.class))).thenAnswer(invocation -> {
            User user = invocation.getArgument(0);
            user.setId(1L);
            return user;
        });
        when(jwtUtil.generateToken(anyString())).thenReturn("access-token");
        when(refreshTokenRepository.save(any(RefreshToken.class))).thenAnswer(invocation -> {
            RefreshToken token = invocation.getArgument(0);
            token.setToken("refresh-token");
            return token;
        });

        authService.register(request);

        ArgumentCaptor<User> userCaptor = ArgumentCaptor.forClass(User.class);
        verify(userRepository).save(userCaptor.capture());
        String storedPassword = userCaptor.getValue().getPassword();
        assertNotEquals("StrongP@ss1", storedPassword);
        assertTrue(passwordEncoder.matches("StrongP@ss1", storedPassword));
    }

    @Test
    void refreshTokenMarksOldTokenUsedAndIssuesNewToken() {
        User user = buildUser();
        RefreshToken existing = new RefreshToken();
        existing.setUser(user);
        existing.setToken("old-refresh");
        existing.setExpiresAt(Instant.now().plusSeconds(3600));
        existing.setRevoked(false);
        existing.setUsed(false);

        when(refreshTokenRepository.findByToken("old-refresh")).thenReturn(Optional.of(existing));
        when(jwtUtil.generateToken(user.getEmail())).thenReturn("new-access");
        when(refreshTokenRepository.save(any(RefreshToken.class))).thenAnswer(invocation -> invocation.getArgument(0));

        AuthResponse response = authService.refreshToken("old-refresh");

        assertTrue(response.isSuccess());
        assertTrue(existing.isUsed());
        assertTrue(existing.isRevoked());
        assertEquals("new-access", response.getAccessToken());
        verify(refreshTokenRepository, times(2)).save(any(RefreshToken.class));
    }

    @Test
    void refreshTokenRejectsReusedToken() {
        RefreshToken existing = new RefreshToken();
        existing.setUsed(true);
        existing.setRevoked(true);
        existing.setExpiresAt(Instant.now().plusSeconds(3600));

        when(refreshTokenRepository.findByToken("used-token")).thenReturn(Optional.of(existing));

        AuthResponse response = authService.refreshToken("used-token");

        assertFalse(response.isSuccess());
        assertEquals("Refresh token has been invalidated", response.getMessage());
    }

    @Test
    void logoutRevokesRefreshTokenAndAccessToken() {
        RefreshToken refreshToken = new RefreshToken();
        refreshToken.setToken("refresh-token");

        when(refreshTokenRepository.findByToken("refresh-token")).thenReturn(Optional.of(refreshToken));
        when(jwtUtil.validateToken("access-token")).thenReturn(true);
        when(jwtUtil.getJtiFromToken("access-token")).thenReturn("jti-123");
        when(jwtUtil.getExpirationFromToken("access-token")).thenReturn(new java.util.Date(System.currentTimeMillis() + 60_000));

        AuthResponse response = authService.logout("refresh-token", "access-token");

        assertTrue(response.isSuccess());
        assertTrue(refreshToken.isRevoked());
        verify(tokenRevocationService).revokeAccessToken(eq("jti-123"), any(Instant.class));
    }

    @Test
    void requestPasswordResetCreatesTokenAndSendsEmail() {
        User user = buildUser();
        when(userRepository.findByEmail("user@example.com")).thenReturn(Optional.of(user));

        AuthResponse response = authService.requestPasswordReset("user@example.com");

        assertTrue(response.isSuccess());
        verify(passwordResetTokenRepository).deleteByUserId(1L);
        verify(passwordResetTokenRepository).save(any(PasswordResetToken.class));
        verify(emailService).sendPasswordResetEmail(eq("user@example.com"), anyString());
    }

    @Test
    void requestPasswordResetRejectsSocialSignInAccount() {
        User user = buildUser();
        user.setProvider("google");
        when(userRepository.findByEmail("user@example.com")).thenReturn(Optional.of(user));

        AuthResponse response = authService.requestPasswordReset("user@example.com");

        assertFalse(response.isSuccess());
        assertTrue(response.getMessage().contains("Google sign-in"));
        verify(emailService, never()).sendPasswordResetEmail(anyString(), anyString());
        verify(passwordResetTokenRepository, never()).save(any());
    }

    @Test
    void loginRejectsSocialSignInAccountWithPasswordForm() {
        User user = buildUser();
        user.setProvider("google");
        user.setPassword(passwordEncoder.encode("StrongP@ss1"));

        when(userRepository.findByEmail("user@example.com")).thenReturn(Optional.of(user));

        AuthRequest request = new AuthRequest();
        request.setEmail("user@example.com");
        request.setPassword("StrongP@ss1");

        AuthResponse response = authService.login(request);

        assertFalse(response.isSuccess());
        assertTrue(response.getMessage().contains("Google sign-in"));
    }

    @Test
    void resetPasswordUpdatesUserPassword() {
        User user = buildUser();
        user.setPassword(passwordEncoder.encode("OldPass1!"));

        PasswordResetToken token = new PasswordResetToken();
        token.setUser(user);
        token.setToken("reset-token");
        token.setExpiresAt(Instant.now().plusSeconds(3600));
        token.setUsed(false);

        when(passwordResetTokenRepository.findByToken("reset-token")).thenReturn(Optional.of(token));

        AuthResponse response = authService.resetPassword("reset-token", "NewPass1!");

        assertTrue(response.isSuccess());
        assertTrue(passwordEncoder.matches("NewPass1!", user.getPassword()));
        assertTrue(token.isUsed());
        verify(refreshTokenRepository).deleteByUser(user);
    }

    @Test
    void loginRequiresTwoFactorWhenEnabled() {
        User user = buildUser();
        user.setPassword(passwordEncoder.encode("StrongP@ss1"));
        user.setTwoFactorEnabled(true);

        when(userRepository.findByEmail("user@example.com")).thenReturn(Optional.of(user));

        AuthRequest request = new AuthRequest();
        request.setEmail("user@example.com");
        request.setPassword("StrongP@ss1");

        AuthResponse response = authService.login(request);

        assertFalse(response.isSuccess());
        assertTrue(response.isRequires2fa());
    }

    @Test
    void verifyTwoFactorLoginIssuesTokens() {
        User user = buildUser();
        user.setPassword(passwordEncoder.encode("StrongP@ss1"));
        user.setTwoFactorEnabled(true);
        user.setTwoFactorSecret("secret");

        when(userRepository.findByEmail("user@example.com")).thenReturn(Optional.of(user));
        when(totpService.verifyCode("secret", "123456")).thenReturn(true);
        when(jwtUtil.generateToken(user.getEmail())).thenReturn("access-token");
        when(refreshTokenRepository.save(any(RefreshToken.class))).thenAnswer(invocation -> {
            RefreshToken token = invocation.getArgument(0);
            token.setToken("refresh-token");
            return token;
        });

        TwoFactorRequest request = new TwoFactorRequest();
        request.setEmail("user@example.com");
        request.setPassword("StrongP@ss1");
        request.setTwoFactorCode("123456");

        AuthResponse response = authService.verifyTwoFactorLogin(request);

        assertTrue(response.isSuccess());
        assertEquals("access-token", response.getAccessToken());
    }

    private User buildUser() {
        User user = new User();
        user.setId(1L);
        user.setEmail("user@example.com");
        user.setUsername("user");
        user.setEnabled(true);
        user.setPasswordLoginEnabled(true);
        return user;
    }
}
