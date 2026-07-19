package com.lampify.utils;

import com.google.api.client.googleapis.auth.oauth2.GoogleIdToken;
import com.google.api.client.googleapis.auth.oauth2.GoogleIdTokenVerifier;
import com.google.api.client.json.gson.GsonFactory;
import com.restfb.DefaultFacebookClient;
import com.restfb.FacebookClient;
import com.restfb.Version;
import com.restfb.types.User;
import lombok.Data;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.WebClient;

import java.io.IOException;
import java.security.GeneralSecurityException;
import java.util.Collections;

/**
 * OAuth Token Validators for Google and Facebook
 * Verifies tokens from social login providers
 */
@Component
public class OAuthTokenValidator {

    @Value("${app.oauth.google.client-id:#{null}}")
    private String googleClientId;

    @Value("${app.oauth.google.client-secret:#{null}}")
    private String googleClientSecret;

    @Value("${app.oauth.facebook.app-id:#{null}}")
    private String facebookAppId;

    @Value("${app.oauth.facebook.app-secret:#{null}}")
    private String facebookAppSecret;

    private final WebClient webClient;

    public OAuthTokenValidator(WebClient.Builder webClientBuilder) {
        this.webClient = webClientBuilder.build();
    }

    public boolean isGoogleConfigured() {
        return googleClientId != null && !googleClientId.isEmpty();
    }

    /**
     * Validates Google OAuth token and extracts user info.
     */
    public GoogleUserInfo validateGoogleToken(String token) {
        if (token == null || token.isEmpty()) {
            return null;
        }
        if (googleClientId == null || googleClientId.isEmpty()) {
            System.err.println("Google OAuth not configured: set GOOGLE_CLIENT_ID in .env or the environment");
            return null;
        }

        try {
            // Create verifier with Google's public keys
            GoogleIdTokenVerifier verifier = new GoogleIdTokenVerifier.Builder(
                    new com.google.api.client.http.javanet.NetHttpTransport(),
                    GsonFactory.getDefaultInstance()
            ).setAudience(Collections.singletonList(googleClientId))
                    .setIssuer("https://accounts.google.com")
                    .build();

            // Verify token signature and expiration
            GoogleIdToken idToken = verifier.verify(token);
            if (idToken == null) {
                return null;
            }

            // Extract user information
            GoogleIdToken.Payload payload = idToken.getPayload();
            return new GoogleUserInfo(
                    (String) payload.get("email"),
                    (String) payload.get("name"),
                    (String) payload.get("picture"),
                    payload.getSubject()  // Google user ID
            );
        } catch (Exception e) {
            System.err.println("Google token validation error: " + e.getMessage());
            return null;
        }
    }

    /**
     * Validates Facebook OAuth token and extracts user info
     * @param accessToken Access token from Facebook SDK
     * @return FacebookUserInfo if valid, null otherwise
     */
    public FacebookUserInfo validateFacebookToken(String accessToken) {
        if (accessToken == null || accessToken.isEmpty() || facebookAppId == null || facebookAppId.isEmpty()) {
            return null;
        }

        try {
            // Create Facebook client with app credentials
            FacebookClient facebookClient = new DefaultFacebookClient(
                    accessToken,
                    facebookAppSecret,
                    Version.LATEST
            );

            // Fetch authenticated user's info
            User user = facebookClient.fetchObject("me", User.class,
                    com.restfb.Parameter.with("fields", "id,name,email,picture")
            );

            if (user == null || user.getId() == null) {
                return null;
            }

            // Extract picture URL if available
            String pictureUrl = null;
            if (user.getPicture() != null && user.getPicture().getUrl() != null) {
                pictureUrl = user.getPicture().getUrl();
            }

            return new FacebookUserInfo(
                    user.getEmail(),
                    user.getName(),
                    pictureUrl,
                    user.getId()
            );
        } catch (Exception e) {
            System.err.println("Facebook token validation error: " + e.getMessage());
            return null;
        }
    }

    /**
     * Google user info extracted from verified token
     */
    @Data
    public static class GoogleUserInfo {
        private final String email;
        private final String name;
        private final String pictureUrl;
        private final String googleId;

        public GoogleUserInfo(String email, String name, String pictureUrl, String googleId) {
            this.email = email;
            this.name = name;
            this.pictureUrl = pictureUrl;
            this.googleId = googleId;
        }
    }

    /**
     * Facebook user info extracted from verified token
     */
    @Data
    public static class FacebookUserInfo {
        private final String email;
        private final String name;
        private final String pictureUrl;
        private final String facebookId;

        public FacebookUserInfo(String email, String name, String pictureUrl, String facebookId) {
            this.email = email;
            this.name = name;
            this.pictureUrl = pictureUrl;
            this.facebookId = facebookId;
        }
    }
}
