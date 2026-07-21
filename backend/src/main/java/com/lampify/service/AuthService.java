package com.lampify.service;

import com.lampify.dto.AuthRequest;
import com.lampify.dto.AuthResponse;
import com.lampify.dto.TwoFactorRequest;
import com.lampify.entity.PasswordResetToken;
import com.lampify.entity.RefreshToken;
import com.lampify.entity.TwoFactorBackupCode;
import com.lampify.entity.User;
import com.lampify.repository.PasswordResetTokenRepository;
import com.lampify.repository.RefreshTokenRepository;
import com.lampify.repository.TwoFactorBackupCodeRepository;
import com.lampify.repository.UserRepository;
import com.lampify.security.JwtUtil;
import com.lampify.utils.CaptchaValidator;
import com.lampify.utils.OAuthTokenValidator;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Date;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Service
public class AuthService {

    private static final int BACKUP_CODE_COUNT = 8;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private RefreshTokenRepository refreshTokenRepository;

    @Autowired
    private PasswordResetTokenRepository passwordResetTokenRepository;

    @Autowired
    private TwoFactorBackupCodeRepository twoFactorBackupCodeRepository;

    @Autowired
    private JwtUtil jwtUtil;

    @Autowired
    private CaptchaValidator captchaValidator;

    @Autowired
    private OAuthTokenValidator oAuthValidator;

    @Autowired
    private EmailService emailService;

    @Autowired
    private TotpService totpService;

    @Autowired
    private TokenRevocationService tokenRevocationService;

    @Value("${app.security.password-recovery-expiration:3600000}")
    private long passwordRecoveryExpirationMs;

    private final BCryptPasswordEncoder passwordEncoder = new BCryptPasswordEncoder();

    @Transactional
    public AuthResponse register(AuthRequest request) {
        if (captchaValidator.isCaptchaRequired()) {
            if (!captchaValidator.validateCaptcha(request.getCaptchaChallenge())) {
                return failure("CAPTCHA validation failed. Please complete the CAPTCHA and try again.");
            }
        } else if (request.getCaptchaChallenge() != null && !request.getCaptchaChallenge().isEmpty()) {
            if (!captchaValidator.validateCaptcha(request.getCaptchaChallenge())) {
                return failure("CAPTCHA validation failed. Please try again.");
            }
        }

        if (!isValidEmail(request.getEmail())) {
            return failure("Invalid email format");
        }

        if (!isStrongPassword(request.getPassword())) {
            return failure("Password must include uppercase, lowercase, number, and special character");
        }

        if (!request.getPassword().equals(request.getConfirmPassword())) {
            return failure("Passwords do not match");
        }

        if (userRepository.existsByEmail(request.getEmail().trim().toLowerCase())) {
            return failure("Email already registered");
        }

        User user = new User();
        user.setEmail(request.getEmail().trim().toLowerCase());
        user.setPassword(passwordEncoder.encode(request.getPassword()));
        user.setUsername(request.getEmail().split("@", 2)[0]);
        user.setEnabled(true);
        userRepository.save(user);

        return issueTokens(user, "Account created successfully");
    }

    @Transactional
    public AuthResponse login(AuthRequest request) {
        if (!isValidEmail(request.getEmail())) {
            return failure("Invalid email format");
        }

        Optional<User> userOptional = userRepository.findByEmail(request.getEmail().trim().toLowerCase());
        if (userOptional.isEmpty()) {
            return failure("Invalid email or password");
        }

        User user = userOptional.get();
        if (!user.isEnabled()) {
            return failure("Account disabled");
        }

        if (!user.isPasswordLoginEnabled() || isSocialProvider(user.getProvider())) {
            return failure(formatSocialSignInMessage(user.getProvider()));
        }

        if (!passwordEncoder.matches(request.getPassword(), user.getPassword())) {
            return failure("Invalid email or password");
        }

        if (user.isTwoFactorEnabled()) {
            AuthResponse response = new AuthResponse();
            response.setSuccess(false);
            response.setMessage("Two-factor authentication required");
            response.setEmail(user.getEmail());
            response.setUsername(user.getUsername());
            response.setRequires2fa(true);
            response.setTwoFactorEnabled(true);
            return response;
        }

        return issueTokens(user, "Login successful");
    }

    @Transactional
    public AuthResponse verifyTwoFactorLogin(TwoFactorRequest request) {
        Optional<User> userOptional = userRepository.findByEmail(request.getEmail().trim().toLowerCase());
        if (userOptional.isEmpty()) {
            return failure("Invalid email or password");
        }

        User user = userOptional.get();
        if (!verifyPasswordIfRequired(user, request.getPassword())) {
            return failure("Invalid email or password");
        }

        if (!user.isTwoFactorEnabled()) {
            return failure("Two-factor authentication is not enabled for this account");
        }

        if (!verifyTwoFactorCode(user, request.getTwoFactorCode())) {
            return failure("Invalid two-factor authentication code");
        }

        return issueTokens(user, "Login successful");
    }

    @Transactional
    public AuthResponse refreshToken(String refreshTokenValue) {
        if (refreshTokenValue == null || refreshTokenValue.isBlank()) {
            return failure("Refresh token is required");
        }

        Optional<RefreshToken> tokenOptional = refreshTokenRepository.findByToken(refreshTokenValue);
        if (tokenOptional.isEmpty()) {
            return failure("Refresh token not found");
        }

        RefreshToken token = tokenOptional.get();
        if (token.isRevoked() || token.isUsed() || token.getExpiresAt().isBefore(Instant.now())) {
            return failure("Refresh token has been invalidated");
        }

        token.setUsed(true);
        token.setRevoked(true);
        refreshTokenRepository.save(token);

        User user = token.getUser();
        return issueTokens(user, "Token refreshed");
    }

    @Transactional
    public AuthResponse logout(String refreshTokenValue, String accessToken) {
        if (refreshTokenValue != null) {
            refreshTokenRepository.findByToken(refreshTokenValue).ifPresent(token -> {
                token.setRevoked(true);
                token.setUsed(true);
                refreshTokenRepository.save(token);
            });
        }

        revokeAccessTokenIfPresent(accessToken);
        return success("Logged out successfully");
    }

    @Transactional
    public AuthResponse requestPasswordReset(String email) {
        String normalizedEmail = email.trim().toLowerCase();
        Optional<User> userOptional = userRepository.findByEmail(normalizedEmail);

        if (userOptional.isPresent()) {
            User user = userOptional.get();
            if (!canResetPassword(user)) {
                return failure(formatSocialSignInMessage(user.getProvider())
                        + " Password reset is not available for social sign-in accounts.");
            }

            passwordResetTokenRepository.deleteByUserId(user.getId());

            PasswordResetToken resetToken = new PasswordResetToken();
            resetToken.setUser(user);
            resetToken.setToken(UUID.randomUUID().toString());
            resetToken.setExpiresAt(Instant.now().plusMillis(passwordRecoveryExpirationMs));
            passwordResetTokenRepository.save(resetToken);

            emailService.sendPasswordResetEmail(user.getEmail(), resetToken.getToken());
        }

        return success("If the account exists, a reset link has been sent");
    }

    @Transactional
    public AuthResponse resetPassword(String resetToken, String newPassword) {
        if (!isStrongPassword(newPassword)) {
            return failure("Password must include uppercase, lowercase, number, and special character");
        }

        Optional<PasswordResetToken> tokenOptional = passwordResetTokenRepository.findByToken(resetToken);
        if (tokenOptional.isEmpty()) {
            return failure("Invalid or expired reset token");
        }

        PasswordResetToken token = tokenOptional.get();
        if (token.isUsed() || token.getExpiresAt().isBefore(Instant.now())) {
            return failure("Invalid or expired reset token");
        }

        User user = token.getUser();
        if (!canResetPassword(user)) {
            return failure("Password reset is not available for social sign-in accounts");
        }

        user.setPassword(passwordEncoder.encode(newPassword));
        user.setPasswordLoginEnabled(true);
        userRepository.save(user);

        token.setUsed(true);
        passwordResetTokenRepository.save(token);
        refreshTokenRepository.deleteByUser(user);

        return success("Password reset successfully");
    }

    @Transactional
    public AuthResponse oauthLogin(String provider, String accessToken) {
        if (provider == null || accessToken == null) {
            return failure("Provider and token are required");
        }

        provider = provider.toLowerCase();

        if ("google".equals(provider)) {
            return handleGoogleLogin(accessToken);
        } else if ("facebook".equals(provider)) {
            return handleFacebookLogin(accessToken);
        }

        return failure("Unsupported provider: " + provider);
    }

    @Transactional
    public AuthResponse setupTwoFactor(String email, String password) {
        Optional<User> userOptional = userRepository.findByEmail(email.trim().toLowerCase());
        if (userOptional.isEmpty()) {
            return failure("User not found");
        }

        User user = userOptional.get();
        if (!verifyPasswordIfRequired(user, password)) {
            return failure("Invalid credentials");
        }

        if (user.isTwoFactorEnabled()) {
            return failure("Two-factor authentication is already enabled");
        }

        String secret = totpService.generateSecret();
        user.setTwoFactorSecret(secret);
        user.setTwoFactorEnabled(false);
        userRepository.save(user);

        twoFactorBackupCodeRepository.deleteByUser(user);
        List<String> backupCodes = totpService.generateBackupCodes(BACKUP_CODE_COUNT);
        for (String code : backupCodes) {
            TwoFactorBackupCode backupCode = new TwoFactorBackupCode();
            backupCode.setUser(user);
            backupCode.setCodeHash(passwordEncoder.encode(code));
            twoFactorBackupCodeRepository.save(backupCode);
        }

        AuthResponse response = success("Scan the QR code and verify with your authenticator app");
        populateUserDetails(response, user);
        response.setQrCodeUri(totpService.generateQrCodeDataUri(user.getEmail(), secret));
        response.setBackupCodes(backupCodes);
        return response;
    }

    @Transactional
    public AuthResponse verifyTwoFactorSetup(TwoFactorRequest request) {
        Optional<User> userOptional = userRepository.findByEmail(request.getEmail().trim().toLowerCase());
        if (userOptional.isEmpty()) {
            return failure("User not found");
        }

        User user = userOptional.get();
        if (!verifyPasswordIfRequired(user, request.getPassword())) {
            return failure("Invalid credentials");
        }

        if (user.getTwoFactorSecret() == null || !totpService.verifyCode(user.getTwoFactorSecret(), request.getTwoFactorCode())) {
            return failure("Invalid two-factor authentication code");
        }

        user.setTwoFactorEnabled(true);
        userRepository.save(user);

        AuthResponse response = success("Two-factor authentication enabled");
        populateUserDetails(response, user);
        response.setTwoFactorEnabled(true);
        return response;
    }

    @Transactional
    public AuthResponse disableTwoFactor(TwoFactorRequest request) {
        Optional<User> userOptional = userRepository.findByEmail(request.getEmail().trim().toLowerCase());
        if (userOptional.isEmpty()) {
            return failure("User not found");
        }

        User user = userOptional.get();
        if (!verifyPasswordIfRequired(user, request.getPassword())) {
            return failure("Invalid credentials");
        }

        if (!user.isTwoFactorEnabled()) {
            return failure("Two-factor authentication is not enabled");
        }

        if (!verifyTwoFactorCode(user, request.getTwoFactorCode())) {
            return failure("Invalid two-factor authentication code");
        }

        user.setTwoFactorEnabled(false);
        user.setTwoFactorSecret(null);
        userRepository.save(user);
        twoFactorBackupCodeRepository.deleteByUser(user);

        AuthResponse response = success("Two-factor authentication disabled");
        populateUserDetails(response, user);
        response.setTwoFactorEnabled(false);
        return response;
    }

    private AuthResponse handleGoogleLogin(String idToken) {
        if (!oAuthValidator.isGoogleConfigured()) {
            return failure("Google login is not configured on the server. Set GOOGLE_CLIENT_ID in the project .env file.");
        }
        OAuthTokenValidator.GoogleUserInfo userInfo = oAuthValidator.validateGoogleToken(idToken);
        if (userInfo == null) {
            return failure("Invalid Google token");
        }
        return createOrLoginOAuthUser("google", userInfo.getEmail(), userInfo.getName());
    }

    private AuthResponse handleFacebookLogin(String accessToken) {
        OAuthTokenValidator.FacebookUserInfo userInfo = oAuthValidator.validateFacebookToken(accessToken);
        if (userInfo == null) {
            return failure("Invalid Facebook token");
        }
        return createOrLoginOAuthUser("facebook", userInfo.getEmail(), userInfo.getName());
    }

    private AuthResponse createOrLoginOAuthUser(String provider, String email, String name) {
        if (email == null || email.isEmpty()) {
            return failure("Email not provided by OAuth provider");
        }

        email = email.trim().toLowerCase();
        final String normalizedEmail = email;

        Optional<User> userOptional = userRepository.findByEmail(normalizedEmail);
        User user = userOptional.orElseGet(() -> {
            User created = new User();
            created.setEmail(normalizedEmail);
            created.setUsername(name != null && !name.isBlank() ? name : normalizedEmail.split("@", 2)[0]);
            created.setPassword(passwordEncoder.encode(UUID.randomUUID().toString()));
            created.setProvider(provider);
            created.setPasswordLoginEnabled(false);
            created.setEnabled(true);
            return userRepository.save(created);
        });

        if (userOptional.isPresent() && isSocialProvider(user.getProvider())
                && user.getProvider().equalsIgnoreCase(provider)
                && user.isPasswordLoginEnabled()) {
            user.setPasswordLoginEnabled(false);
            userRepository.save(user);
        }

        if (user.isTwoFactorEnabled()) {
            AuthResponse response = new AuthResponse();
            response.setSuccess(false);
            response.setMessage("Two-factor authentication required");
            populateUserDetails(response, user);
            response.setRequires2fa(true);
            response.setTwoFactorEnabled(true);
            return response;
        }

        return issueTokens(user, "OAuth login successful");
    }

    private boolean verifyPasswordIfRequired(User user, String password) {
        if (!user.isPasswordLoginEnabled() || isSocialProvider(user.getProvider())) {
            return true;
        }
        return password != null && !password.isBlank() && passwordEncoder.matches(password, user.getPassword());
    }

    private void populateUserDetails(AuthResponse response, User user) {
        response.setEmail(user.getEmail());
        response.setUsername(user.getUsername());
        response.setProvider(user.getProvider());
        response.setOauthAccount(isSocialProvider(user.getProvider()) || !user.isPasswordLoginEnabled());
    }

    private boolean canResetPassword(User user) {
        if (!user.isPasswordLoginEnabled()) {
            return false;
        }
        return !isSocialProvider(user.getProvider());
    }

    private boolean isSocialProvider(String provider) {
        if (provider == null || provider.isBlank()) {
            return false;
        }
        return switch (provider.toLowerCase()) {
            case "google", "facebook" -> true;
            default -> false;
        };
    }

    private String formatSocialSignInMessage(String provider) {
        if (provider == null || provider.isBlank()) {
            return "This account uses social sign-in.";
        }
        return switch (provider.toLowerCase()) {
            case "google" -> "This account uses Google sign-in.";
            case "facebook" -> "This account uses Facebook sign-in.";
            default -> "This account uses " + provider + " sign-in.";
        };
    }

    private boolean verifyTwoFactorCode(User user, String code) {
        if (code == null || code.isBlank()) {
            return false;
        }

        if (totpService.verifyCode(user.getTwoFactorSecret(), code)) {
            return true;
        }

        return twoFactorBackupCodeRepository.findByUserAndUsedFalse(user).stream()
                .filter(backupCode -> passwordEncoder.matches(code.trim(), backupCode.getCodeHash()))
                .findFirst()
                .map(backupCode -> {
                    backupCode.setUsed(true);
                    twoFactorBackupCodeRepository.save(backupCode);
                    return true;
                })
                .orElse(false);
    }

    private void revokeAccessTokenIfPresent(String accessToken) {
        if (accessToken == null || accessToken.isBlank() || !jwtUtil.validateToken(accessToken)) {
            return;
        }

        String jti = jwtUtil.getJtiFromToken(accessToken);
        Date expiration = jwtUtil.getExpirationFromToken(accessToken);
        tokenRevocationService.revokeAccessToken(jti, expiration.toInstant());
    }

    private AuthResponse issueTokens(User user, String message) {
        String accessToken = jwtUtil.generateToken(user.getEmail());
        String refreshToken = createRefreshToken(user);

        AuthResponse response = success(message);
        populateUserDetails(response, user);
        response.setAccessToken(accessToken);
        response.setRefreshToken(refreshToken);
        response.setTwoFactorEnabled(user.isTwoFactorEnabled());
        return response;
    }

    private String createRefreshToken(User user) {
        RefreshToken token = new RefreshToken();
        token.setUser(user);
        token.setToken(UUID.randomUUID().toString());
        token.setExpiresAt(Instant.now().plus(7, ChronoUnit.DAYS));
        token.setRevoked(false);
        token.setUsed(false);
        refreshTokenRepository.save(token);
        return token.getToken();
    }

    private AuthResponse success(String message) {
        AuthResponse response = new AuthResponse();
        response.setSuccess(true);
        response.setMessage(message);
        return response;
    }

    private AuthResponse failure(String message) {
        AuthResponse response = new AuthResponse();
        response.setSuccess(false);
        response.setMessage(message);
        return response;
    }

    boolean isValidEmail(String email) {
        return email != null && email.length() >= 8 && email.contains("@") && email.contains(".");
    }

    boolean isStrongPassword(String password) {
        if (password == null || password.length() < 8) {
            return false;
        }
        boolean hasUpper = password.chars().anyMatch(Character::isUpperCase);
        boolean hasLower = password.chars().anyMatch(Character::isLowerCase);
        boolean hasDigit = password.chars().anyMatch(Character::isDigit);
        boolean hasSpecial = password.chars().anyMatch(ch -> !Character.isLetterOrDigit(ch));
        return hasUpper && hasLower && hasDigit && hasSpecial;
    }
}
