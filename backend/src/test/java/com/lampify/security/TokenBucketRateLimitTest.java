package com.lampify.security;

import jakarta.servlet.FilterChain;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.test.util.ReflectionTestUtils;

import java.io.PrintWriter;
import java.io.StringWriter;
import java.util.concurrent.atomic.AtomicLong;

import static org.mockito.Mockito.*;
import static org.mockito.Mockito.lenient;

@ExtendWith(MockitoExtension.class)
class TokenBucketRateLimitTest {

    @Mock
    private HttpServletRequest request;

    @Mock
    private HttpServletResponse response;

    @Mock
    private FilterChain filterChain;

    private RateLimitingFilter filter;
    private final AtomicLong nowNanos = new AtomicLong(0);

    @BeforeEach
    void setUp() throws Exception {
        filter = new RateLimitingFilter();
        filter.setNanoTime(nowNanos::get);
        ReflectionTestUtils.setField(filter, "enabled", true);
        ReflectionTestUtils.setField(filter, "authCapacity", 2);
        ReflectionTestUtils.setField(filter, "authRefillSeconds", 1);
        ReflectionTestUtils.setField(filter, "contactCapacity", 5);
        ReflectionTestUtils.setField(filter, "contactRefillSeconds", 60);
        lenient().when(request.getRequestURI()).thenReturn("/api/auth/login");
        lenient().when(request.getRemoteAddr()).thenReturn("203.0.113.50");
        lenient().when(response.getWriter()).thenReturn(new PrintWriter(new StringWriter()));
    }

    @Test
    void burstIsAllowedThenBlockedUntilRefill() throws Exception {
        filter.doFilter(request, response, filterChain);
        filter.doFilter(request, response, filterChain);
        filter.doFilter(request, response, filterChain);

        verify(filterChain, times(2)).doFilter(request, response);
        verify(response).setStatus(HttpStatus.TOO_MANY_REQUESTS.value());

        nowNanos.addAndGet(1_000_000_000L);
        filter.doFilter(request, response, filterChain);

        verify(filterChain, times(3)).doFilter(request, response);
    }

    @Test
    void loginAndRegisterShareTheSameAuthBucket() throws Exception {
        when(request.getRequestURI()).thenReturn("/api/auth/login");
        filter.doFilter(request, response, filterChain);
        when(request.getRequestURI()).thenReturn("/api/auth/register");
        filter.doFilter(request, response, filterChain);
        filter.doFilter(request, response, filterChain);

        verify(filterChain, times(2)).doFilter(request, response);
        verify(response).setStatus(HttpStatus.TOO_MANY_REQUESTS.value());
    }
}
