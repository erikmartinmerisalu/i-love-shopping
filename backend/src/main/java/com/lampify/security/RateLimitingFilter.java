package com.lampify.security;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.LongSupplier;

@Component
public class RateLimitingFilter extends OncePerRequestFilter {

    private static class TokenBucket {
        private double tokens;
        private long lastRefillNanos;
        private boolean initialized;
    }

    private enum BucketGroup {
        AUTH,
        CONTACT
    }

    private final Map<String, TokenBucket> buckets = new ConcurrentHashMap<>();
    private LongSupplier nanoTime = System::nanoTime;

    @Value("${app.rate.limit.enabled:true}")
    private boolean enabled;

    @Value("${app.rate.limit.auth-capacity:10}")
    private int authCapacity;

    @Value("${app.rate.limit.auth-refill-seconds:6}")
    private int authRefillSeconds;

    @Value("${app.rate.limit.contact-capacity:5}")
    private int contactCapacity;

    @Value("${app.rate.limit.contact-refill-seconds:60}")
    private int contactRefillSeconds;

    void setNanoTime(LongSupplier nanoTime) {
        this.nanoTime = nanoTime;
    }

    @Override
    protected boolean shouldNotFilter(HttpServletRequest request) {
        if (!enabled) {
            return true;
        }
        return resolveGroup(request) == null;
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
            throws ServletException, IOException {

        BucketGroup group = resolveGroup(request);
        if (group == null) {
            filterChain.doFilter(request, response);
            return;
        }

        int capacity = group == BucketGroup.CONTACT ? contactCapacity : authCapacity;
        int refillSeconds = group == BucketGroup.CONTACT ? contactRefillSeconds : authRefillSeconds;
        String key = resolveClientIp(request) + ":" + group.name();
        TokenBucket bucket = buckets.computeIfAbsent(key, ignored -> new TokenBucket());

        ConsumeResult result = tryConsume(bucket, capacity, refillSeconds, nanoTime.getAsLong());
        if (!result.allowed()) {
            response.setStatus(HttpStatus.TOO_MANY_REQUESTS.value());
            response.setHeader("Retry-After", String.valueOf(result.retryAfterSeconds()));
            response.setContentType("application/json");
            response.getWriter().write("{\"message\":\"Too many requests. Please try again later.\"}");
            return;
        }

        filterChain.doFilter(request, response);
    }

    private ConsumeResult tryConsume(TokenBucket bucket, int capacity, int refillSeconds, long nowNanos) {
        synchronized (bucket) {
            if (!bucket.initialized) {
                bucket.tokens = capacity;
                bucket.lastRefillNanos = nowNanos;
                bucket.initialized = true;
            }

            double refillPerSecond = refillSeconds <= 0 ? 0 : 1.0 / refillSeconds;
            double elapsedSeconds = Math.max(0, (nowNanos - bucket.lastRefillNanos) / 1_000_000_000.0);
            bucket.tokens = Math.min(capacity, bucket.tokens + elapsedSeconds * refillPerSecond);
            bucket.lastRefillNanos = nowNanos;

            if (bucket.tokens >= 1.0) {
                bucket.tokens -= 1.0;
                return new ConsumeResult(true, 0);
            }

            int retryAfter = 1;
            if (refillPerSecond > 0) {
                retryAfter = Math.max(1, (int) Math.ceil((1.0 - bucket.tokens) / refillPerSecond));
            }
            return new ConsumeResult(false, retryAfter);
        }
    }

    private static BucketGroup resolveGroup(HttpServletRequest request) {
        String path = request.getRequestURI();
        if (path.contains("/auth/refresh")) {
            return null;
        }
        if (path.contains("/auth/")) {
            return BucketGroup.AUTH;
        }
        if ("POST".equalsIgnoreCase(request.getMethod()) && path.contains("/contact")) {
            return BucketGroup.CONTACT;
        }
        return null;
    }

    static String resolveClientIp(HttpServletRequest request) {
        String forwarded = request.getHeader("X-Forwarded-For");
        if (forwarded != null && !forwarded.isBlank() && !"unknown".equalsIgnoreCase(forwarded)) {
            return forwarded.split(",")[0].trim();
        }

        String realIp = request.getHeader("X-Real-IP");
        if (realIp != null && !realIp.isBlank() && !"unknown".equalsIgnoreCase(realIp)) {
            return realIp.trim();
        }

        return request.getRemoteAddr();
    }

    private record ConsumeResult(boolean allowed, int retryAfterSeconds) {
    }
}
