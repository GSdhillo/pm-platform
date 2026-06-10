package com.gurjeet.pm.common.idempotency;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.gurjeet.pm.adapter.out.persistence.IdempotencyKeyRepository;
import com.gurjeet.pm.domain.model.IdempotencyKey;
import com.gurjeet.pm.common.security.AuthUser;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.core.annotation.Order;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;
import org.springframework.web.util.ContentCachingResponseWrapper;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.util.HexFormat;
import java.util.Optional;

@Component
@Order(20)
public class IdempotencyFilter extends OncePerRequestFilter {
    public static final String HEADER = "Idempotency-Key";
    private final IdempotencyKeyRepository repository;
    private final ObjectMapper objectMapper;

    public IdempotencyFilter(IdempotencyKeyRepository repository, ObjectMapper objectMapper) {
        this.repository = repository;
        this.objectMapper = objectMapper;
    }

    @Override
    protected boolean shouldNotFilter(HttpServletRequest request) {
        return !"POST".equals(request.getMethod())
                || request.getHeader(HEADER) == null
                || !request.getRequestURI().startsWith("/api/v1/");
    }

    @Override
    protected void doFilterInternal(HttpServletRequest req, HttpServletResponse res, FilterChain chain)
            throws ServletException, IOException {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth == null || !(auth.getPrincipal() instanceof AuthUser user)) {
            chain.doFilter(req, res);
            return;
        }
        String idemKey = user.id() + ":" + req.getHeader(HEADER);
        CachedBodyRequest cached = new CachedBodyRequest(req);
        String requestHash = sha256(req.getMethod() + req.getRequestURI() + new String(cached.body(), StandardCharsets.UTF_8));

        Optional<IdempotencyKey> existing = repository.findById(idemKey);
        if (existing.isPresent()) {
            IdempotencyKey stored = existing.get();
            if (!stored.getRequestHash().equals(requestHash)) {
                res.setStatus(422);
                res.setContentType("application/json");
                res.getWriter().write("{\"code\":\"IDEMPOTENCY_KEY_REUSED\",\"message\":\"Idempotency-Key was already used with a different request body.\"}");
                return;
            }
            if (stored.getResponseStatus() != null) {
                res.setStatus(stored.getResponseStatus());
                res.setContentType("application/json");
                res.setHeader("X-Idempotent-Replay", "true");
                res.getWriter().write(stored.getResponseBody() == null ? "" : stored.getResponseBody().toString());
                return;
            }
        }

        ContentCachingResponseWrapper wrappedRes = new ContentCachingResponseWrapper(res);
        chain.doFilter(cached, wrappedRes);

        try {
            IdempotencyKey record = existing.orElseGet(() -> new IdempotencyKey(idemKey, user.id(), requestHash));
            record.setResponseStatus(wrappedRes.getStatus());
            String body = new String(wrappedRes.getContentAsByteArray(), StandardCharsets.UTF_8);
            if (!body.isBlank()) {
                try {
                    JsonNode node = objectMapper.readTree(body);
                    record.setResponseBody(node);
                } catch (Exception ignore) {  }
            }
            repository.save(record);
        } catch (DataIntegrityViolationException raced) {

        }
        wrappedRes.copyBodyToResponse();
    }

    private String sha256(String input) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            return HexFormat.of().formatHex(digest.digest(input.getBytes(StandardCharsets.UTF_8)));
        } catch (Exception e) {
            throw new IllegalStateException(e);
        }
    }
}
