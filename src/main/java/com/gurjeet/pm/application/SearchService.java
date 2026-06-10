package com.gurjeet.pm.application;

import com.gurjeet.pm.common.error.BadRequestException;
import jakarta.persistence.EntityManager;
import jakarta.persistence.Query;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Service
public class SearchService {
    private static final Pattern CLAUSE = Pattern.compile("(\\w+)\\s*=\\s*\"?([^\"]+?)\"?\\s*$");
    private static final Set<String> FIELDS = Set.of("status", "assignee", "type", "priority", "label", "project");

    private final EntityManager entityManager;
    private final AccessService accessService;

    public SearchService(EntityManager entityManager, AccessService accessService) {
        this.entityManager = entityManager;
        this.accessService = accessService;
    }

    public record SearchHit(UUID id, String issueKey, String title, String type, String priority,
                            String status, String assignee, Instant createdAt) {}
    public record SearchPage(List<SearchHit> hits, String nextCursor) {}

    @Transactional(readOnly = true)
    public SearchPage search(UUID userId, String freeText, String structuredQuery, UUID projectId,
                             String cursor, int limit) {
        List<UUID> visible = accessService.visibleProjectIds(userId);
        if (visible.isEmpty()) return new SearchPage(List.of(), null);
        if (projectId != null) {
            if (!visible.contains(projectId)) return new SearchPage(List.of(), null);
            visible = List.of(projectId);
        }
        int pageSize = Math.min(Math.max(limit, 1), 50);

        StringBuilder sql = new StringBuilder("""
            SELECT i.id, i.issue_key, i.title, i.type, i.priority, ws.name AS status,
                   COALESCE(a.display_name, '') AS assignee, i.created_at
            FROM issues i
            JOIN workflow_statuses ws ON ws.id = i.status_id
            LEFT JOIN users a ON a.id = i.assignee_id
            JOIN projects p ON p.id = i.project_id
            WHERE i.project_id IN (:projectIds)
            """);
        Map<String, Object> params = new HashMap<>();
        params.put("projectIds", visible);

        if (freeText != null && !freeText.isBlank()) {
            sql.append("""
                AND (i.search_vector @@ plainto_tsquery('english', :q)
                     OR EXISTS (SELECT 1 FROM comments c
                                WHERE c.issue_id = i.id
                                  AND c.search_vector @@ plainto_tsquery('english', :q)))
                """);
            params.put("q", freeText);
        }
        if (structuredQuery != null && !structuredQuery.isBlank()) {
            applyStructured(structuredQuery, sql, params);
        }
        if (cursor != null && !cursor.isBlank()) {
            Cursor decoded = Cursor.decode(cursor);
            sql.append(" AND (i.created_at, i.id) < (CAST(:cursorTs AS timestamptz), CAST(:cursorId AS uuid)) ");
            params.put("cursorTs", decoded.createdAt().toString());
            params.put("cursorId", decoded.id().toString());
        }
        sql.append(" ORDER BY i.created_at DESC, i.id DESC LIMIT ").append(pageSize + 1);

        Query query = entityManager.createNativeQuery(sql.toString());
        params.forEach(query::setParameter);

        @SuppressWarnings("unchecked")
        List<Object[]> rows = query.getResultList();
        List<SearchHit> hits = new ArrayList<>();
        for (Object[] row : rows) {
            hits.add(new SearchHit(
                    (UUID) row[0], (String) row[1], (String) row[2], (String) row[3], (String) row[4],
                    (String) row[5], (String) row[6], toInstant(row[7])));
        }
        String next = null;
        if (hits.size() > pageSize) {
            hits = hits.subList(0, pageSize);
            SearchHit last = hits.get(hits.size() - 1);
            next = new Cursor(last.createdAt(), last.id()).encode();
        }
        return new SearchPage(hits, next);
    }

    private void applyStructured(String structuredQuery, StringBuilder sql, Map<String, Object> params) {
        String[] clauses = structuredQuery.split("(?i)\\s+AND\\s+");
        int n = 0;
        for (String raw : clauses) {
            Matcher matcher = CLAUSE.matcher(raw.trim());
            if (!matcher.matches()) {
                throw new BadRequestException("Cannot parse clause: \"" + raw.trim()
                        + "\". Expected: field = \"value\" with fields " + FIELDS);
            }
            String field = matcher.group(1).toLowerCase();
            String value = matcher.group(2).trim();
            if (!FIELDS.contains(field)) {
                throw new BadRequestException("Unknown search field \"" + field + "\". Supported: " + FIELDS);
            }
            String param = "sq" + (n++);
            switch (field) {
                case "status" -> { sql.append(" AND ws.name ILIKE :").append(param); params.put(param, value); }
                case "assignee" -> { sql.append(" AND a.display_name ILIKE :").append(param); params.put(param, "%" + value + "%"); }
                case "type" -> { sql.append(" AND i.type = :").append(param); params.put(param, value.toUpperCase()); }
                case "priority" -> { sql.append(" AND i.priority = :").append(param); params.put(param, value.toUpperCase()); }
                case "label" -> { sql.append(" AND i.labels @> CAST(:").append(param).append(" AS jsonb)"); params.put(param, "[\"" + value + "\"]"); }
                case "project" -> { sql.append(" AND p.project_key = :").append(param); params.put(param, value.toUpperCase()); }
                default -> { }
            }
        }
    }

    private Instant toInstant(Object value) {
        if (value instanceof Instant i) return i;
        if (value instanceof java.sql.Timestamp ts) return ts.toInstant();
        if (value instanceof java.time.OffsetDateTime odt) return odt.toInstant();
        return Instant.now();
    }

    record Cursor(Instant createdAt, UUID id) {
        String encode() {
            return Base64.getUrlEncoder().withoutPadding()
                    .encodeToString((createdAt.toEpochMilli() + "|" + id).getBytes(StandardCharsets.UTF_8));
        }
        static Cursor decode(String encoded) {
            try {
                String[] parts = new String(Base64.getUrlDecoder().decode(encoded), StandardCharsets.UTF_8).split("\\|");
                return new Cursor(Instant.ofEpochMilli(Long.parseLong(parts[0])), UUID.fromString(parts[1]));
            } catch (Exception e) {
                throw new BadRequestException("Invalid cursor");
            }
        }
    }
}
