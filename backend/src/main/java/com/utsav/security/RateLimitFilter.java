package com.utsav.security;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.time.Instant;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

/**
 * Simple in-memory rate limiter for auth and payment endpoints.
 * (Use Redis in production for multi-instance deployments.)
 */
@Component
public class RateLimitFilter extends OncePerRequestFilter {

  private static final int AUTH_LIMIT_PER_MINUTE = 20;
  private static final int PAYMENT_LIMIT_PER_MINUTE = 30;

  private final Map<String, Window> windows = new ConcurrentHashMap<>();

  @Override
  protected void doFilterInternal(
      HttpServletRequest request, HttpServletResponse response, FilterChain chain)
      throws ServletException, IOException {
    String path = request.getRequestURI();
    int limit = -1;
    if (path.startsWith("/api/auth/")) {
      limit = AUTH_LIMIT_PER_MINUTE;
    } else if (path.startsWith("/api/payments/") || path.startsWith("/api/webhooks/")) {
      limit = PAYMENT_LIMIT_PER_MINUTE;
    }
    if (limit > 0) {
      String key = clientKey(request) + ":" + path;
      Window window = windows.computeIfAbsent(key, k -> new Window());
      synchronized (window) {
        long now = Instant.now().getEpochSecond();
        if (now - window.startSecond >= 60) {
          window.startSecond = now;
          window.count = 0;
        }
        window.count++;
        if (window.count > limit) {
          response.sendError(HttpStatus.TOO_MANY_REQUESTS.value(), "rate limit exceeded");
          return;
        }
      }
    }
    chain.doFilter(request, response);
  }

  private String clientKey(HttpServletRequest request) {
    String forwarded = request.getHeader("X-Forwarded-For");
    if (forwarded != null && !forwarded.isBlank()) {
      return forwarded.split(",")[0].trim();
    }
    return request.getRemoteAddr();
  }

  private static class Window {
    long startSecond = Instant.now().getEpochSecond();
    int count = 0;
  }
}
