package com.gurjeet.pm.common.ratelimit;

import com.gurjeet.pm.common.security.AuthUser;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.annotation.Order;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.time.Duration;

@Component
@Order(10)
public class RateLimitFilter extends OncePerRequestFilter {
    private final StringRedisTemplate redis;
    private final int readPerMinute;
    private final int writePerMinute;

    public RateLimitFilter(StringRedisTemplate redis,
                           @Value("${app.rate-limit.default-per-minute}") int readPerMinute,
                           @Value("${app.rate-limit.write-per-minute}") int writePerMinute) {
        this.redis = redis;
        this.readPerMinute = readPerMinute;
        this.writePerMinute = writePerMinute;
    }

    @Override
    protected boolean shouldNotFilter(HttpServletRequest request) {
        String p = request.getRequestURI();
        return !p.startsWith("/api/v1/") || p.startsWith("/api/v1/auth/");
    }

    @Override
    protected void doFilterInternal(HttpServletRequest req, HttpServletResponse res, FilterChain chain)
            throws ServletException, IOException {
        boolean write = !req.getMethod().equals("GET");
        int limit = write ? writePerMinute : readPerMinute;
        String who = principalKey(req);
        long window = System.currentTimeMillis() / 60_000;
        String key = "rl:" + who + ":" + (write ? "w" : "r") + ":" + window;

        Long count = redis.opsForValue().increment(key);
        if (count != null && count == 1L) redis.expire(key, Duration.ofSeconds(65));

        if (count != null && count > limit) {
            res.setStatus(429);
            res.setContentType("application/json");
            res.getWriter().write("{\"code\":\"RATE_LIMITED\",\"message\":\"Too many requests. Limit: "
                    + limit + "/min for " + (write ? "writes" : "reads") + ".\"}");
            return;
        }
        res.setHeader("X-RateLimit-Limit", String.valueOf(limit));
        res.setHeader("X-RateLimit-Remaining", String.valueOf(Math.max(0, limit - (count == null ? 0 : count))));
        chain.doFilter(req, res);
    }

    private String principalKey(HttpServletRequest req) {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth != null && auth.getPrincipal() instanceof AuthUser u) return "u:" + u.id();
        return "ip:" + req.getRemoteAddr();
    }
}
