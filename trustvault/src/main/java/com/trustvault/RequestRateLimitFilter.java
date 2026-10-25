package com.trustvault;

import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.Locale;
import java.util.Map;
import java.util.regex.Pattern;

import org.springframework.web.filter.OncePerRequestFilter;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;

/**
 * Per-client token bucket limits for login and API requests.
 *
 * This in-process limiter is appropriate for the current single-instance
 * Render deployment. Multi-instance deployments must use shared state.
 */
public final class RequestRateLimitFilter extends OncePerRequestFilter {

    private static final int MAX_BUCKETS = 10_000;
    private static final Pattern IP_LITERAL_CHARACTERS =
            Pattern.compile("[0-9a-fA-F:.]+");

    private final int apiRequestsPerMinute;
    private final int loginRequestsPerMinute;
    private final Map<String, TokenBucket> buckets =
            new LinkedHashMap<>(256, 0.75f, true);

    public RequestRateLimitFilter(
            int apiRequestsPerMinute,
            int loginRequestsPerMinute) {
        if (apiRequestsPerMinute < 1 || loginRequestsPerMinute < 1) {
            throw new IllegalArgumentException(
                    "Rate limits must be positive numbers");
        }
        this.apiRequestsPerMinute = apiRequestsPerMinute;
        this.loginRequestsPerMinute = loginRequestsPerMinute;
    }

    @Override
    protected boolean shouldNotFilter(HttpServletRequest request) {
        String path = request.getServletPath();
        return !"/login".equals(path)
                && !"/api".equals(path)
                && !path.startsWith("/api/");
    }

    @Override
    protected void doFilterInternal(
            HttpServletRequest request,
            HttpServletResponse response,
            FilterChain filterChain) throws ServletException, IOException {

        boolean login = "/login".equals(request.getServletPath());
        int limit = login ? loginRequestsPerMinute : apiRequestsPerMinute;
        String bucketKey = (login ? "login:" : "api:") + clientAddress(request);
        long now = System.currentTimeMillis();
        long retryAfterMillis = acquire(bucketKey, now, limit);

        if (retryAfterMillis > 0) {
            long retryAfterSeconds = Math.max(
                    1, (retryAfterMillis + 999) / 1000);
            response.setStatus(429);
            response.setHeader("Retry-After", Long.toString(retryAfterSeconds));
            response.setHeader("Cache-Control", "no-store");
            response.setContentType("application/json");
            response.setCharacterEncoding("UTF-8");
            response.getWriter().write(
                    "{\"message\":\"Too many requests. Please retry later.\"}");
            return;
        }

        filterChain.doFilter(request, response);
    }

    private long acquire(String key, long now, int limit) {
        TokenBucket bucket;
        synchronized (buckets) {
            bucket = buckets.get(key);
            if (bucket == null) {
                if (buckets.size() >= MAX_BUCKETS) {
                    Iterator<String> oldest = buckets.keySet().iterator();
                    if (oldest.hasNext()) {
                        oldest.next();
                        oldest.remove();
                    }
                }
                bucket = new TokenBucket(limit, now);
                buckets.put(key, bucket);
            }
        }
        return bucket.acquire(limit, now);
    }

    private String clientAddress(HttpServletRequest request) {
        // Render forwards the original client address in X-Forwarded-For.
        // Use the first hop and accept only IP-literal characters so this
        // value cannot trigger DNS lookups or become an unbounded key.
        String forwardedFor = request.getHeader("X-Forwarded-For");
        if (forwardedFor != null && !forwardedFor.isBlank()) {
            String firstHop = forwardedFor.split(",", 2)[0].trim();
            if (isIpLiteral(firstHop)) {
                return firstHop.toLowerCase(Locale.ROOT);
            }
        }

        String remoteAddress = request.getRemoteAddr();
        return remoteAddress == null || remoteAddress.isBlank()
                ? "unknown"
                : remoteAddress.toLowerCase(Locale.ROOT);
    }

    private boolean isIpLiteral(String value) {
        return value.length() <= 64
                && IP_LITERAL_CHARACTERS.matcher(value).matches()
                && (value.indexOf('.') >= 0 || value.indexOf(':') >= 0);
    }

    private static final class TokenBucket {
        private double tokens;
        private long lastRefillAt;

        private TokenBucket(int capacity, long now) {
            this.tokens = capacity;
            this.lastRefillAt = now;
        }

        private synchronized long acquire(int capacity, long now) {
            long elapsed = Math.max(0, now - lastRefillAt);
            double refillPerMillisecond = capacity / 60_000.0;
            tokens = Math.min(capacity, tokens + elapsed * refillPerMillisecond);
            lastRefillAt = now;

            if (tokens >= 1) {
                tokens -= 1;
                return 0;
            }

            return (long) Math.ceil((1 - tokens) / refillPerMillisecond);
        }

    }
}
