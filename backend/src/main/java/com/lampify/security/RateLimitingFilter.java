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
import java.util.concurrent.atomic.AtomicInteger;

@Component
public class RateLimitingFilter extends OncePerRequestFilter {

    private static class WindowState {
        private final AtomicInteger count = new AtomicInteger(0);
        private volatile long windowStartMs = System.currentTimeMillis();
    }

    private final Map<String, WindowState> windows = new ConcurrentHashMap<>();

    @Value("${app.rate.limit.enabled:true}")
    private boolean enabled;

    @Value("${app.rate.limit.default-limit:60}")
    private int limit;

    @Value("${app.rate.limit.window-duration:60}")
    private int windowSeconds;

    @Override
    protected boolean shouldNotFilter(HttpServletRequest request) {
        if (!enabled) {
            return true;
        }
        String path = request.getRequestURI();
        return !path.contains("/auth/");
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
            throws ServletException, IOException {

        String key = request.getRemoteAddr() + ":" + request.getRequestURI();
        WindowState state = windows.computeIfAbsent(key, ignored -> new WindowState());

        long now = System.currentTimeMillis();
        long windowMs = windowSeconds * 1000L;

        synchronized (state) {
            if (now - state.windowStartMs >= windowMs) {
                state.count.set(0);
                state.windowStartMs = now;
            }

            if (state.count.incrementAndGet() > limit) {
                response.setStatus(HttpStatus.TOO_MANY_REQUESTS.value());
                response.setContentType("application/json");
                response.getWriter().write("{\"message\":\"Too many requests. Please try again later.\"}");
                return;
            }
        }

        filterChain.doFilter(request, response);
    }
}
