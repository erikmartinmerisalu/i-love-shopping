package com.lampify.utils;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.BodyInserters;
import org.springframework.web.reactive.function.client.WebClient;

import java.util.Arrays;

/**
 * Google reCAPTCHA v2 (checkbox) validation for registration.
 */
@Component
public class CaptchaValidator {

    @Value("${app.recaptcha.enabled:true}")
    private boolean captchaEnabled;

    @Value("${app.recaptcha.secret-key:#{null}}")
    private String secretKey;

    @Value("${app.recaptcha.verify-url}")
    private String verifyUrl;

    private final WebClient webClient;

    public CaptchaValidator(WebClient.Builder webClientBuilder) {
        this.webClient = webClientBuilder.build();
    }

    /**
     * Validates a reCAPTCHA v2 checkbox token from the frontend.
     */
    public boolean validateCaptcha(String captchaToken) {
        if (!isCaptchaRequired()) {
            return true;
        }

        if (captchaToken == null || captchaToken.trim().isEmpty()) {
            return false;
        }

        try {
            CaptchaResponse response = webClient.post()
                    .uri(verifyUrl)
                    .contentType(MediaType.APPLICATION_FORM_URLENCODED)
                    .body(BodyInserters.fromFormData("secret", secretKey).with("response", captchaToken))
                    .retrieve()
                    .bodyToMono(CaptchaResponse.class)
                    .block();

            if (response == null) {
                return false;
            }

            if (!response.isSuccess()) {
                System.err.println("CAPTCHA verification failed: "
                        + Arrays.toString(response.getErrorCodes())
                        + " (hostname=" + response.getHostname() + ")");
            }

            return response.isSuccess();
        } catch (Exception e) {
            System.err.println("CAPTCHA validation error: " + e.getMessage());
            return false;
        }
    }

    public boolean isCaptchaRequired() {
        return captchaEnabled && secretKey != null && !secretKey.isEmpty();
    }

    /**
     * Response from Google reCAPTCHA API
     */
    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class CaptchaResponse {
        private boolean success;
        private double score;
        private String action;
        @JsonProperty("challenge_ts")
        private String challengeTs;
        private String hostname;
        @JsonProperty("error-codes")
        private String[] errorCodes;

        public boolean isSuccess() {
            return success;
        }

        public double getScore() {
            return score;
        }
    }
}
