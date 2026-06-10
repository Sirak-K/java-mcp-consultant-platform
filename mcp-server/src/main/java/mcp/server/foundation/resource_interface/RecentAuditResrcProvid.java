package mcp.server.foundation.resource_interface;

import mcp.server.foundation.audit.AuditEntry;
import mcp.server.foundation.audit.AuditEntryJpaRepo;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.Instant;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

/**
 * MCP Resource: recent audit entries
 *
 * Exposes the 25 most recent audit entries (newest first) as passive context.
 * MCP clients and ops tooling can read recent system activity without issuing
 * a tool call or querying the database directly.
 *
 * Results are cached for 30 seconds to avoid repeated DB queries on
 * back-to-back resource reads.
 * The TTL is short enough to reflect recent activity while eliminating
 * per-request DB overhead.
 *
 * Content is dynamic — the cache is invalidated and rebuilt after expiry.
 */
public final class RecentAuditResrcProvid implements ResrcProvid {

    private static final Logger log = LoggerFactory.getLogger(RecentAuditResrcProvid.class);

    private static final long CACHE_TTL_MILLIS = 30_000L; // 30 seconds

    private final String resourceName;
    private final AuditEntryJpaRepo auditRepo;

    private volatile Map<String, Object> cachedResult;
    private volatile long cacheExpiresAt = 0L;

    public RecentAuditResrcProvid(
            String resourceName,
            AuditEntryJpaRepo auditRepo) {
        this.resourceName = Objects.requireNonNull(resourceName, "resourceName");
        this.auditRepo = Objects.requireNonNull(auditRepo, "auditRepo");
    }

    @Override
    public Map<String, Object> ResourceProvRead() {
        long now = System.currentTimeMillis();
        if (cachedResult != null && now <= cacheExpiresAt) {
            return cachedResult;
        }
        try {
            Map<String, Object> fresh = buildPayload(now + CACHE_TTL_MILLIS);
            cachedResult   = fresh;
            cacheExpiresAt = now + CACHE_TTL_MILLIS;
            return fresh;
        } catch (Exception e) {
            // Do NOT cache error responses — next read retries the DB.
            log.warn("AUDIT_RESOURCE_DB_ERROR error={}", e.getMessage());
            return Map.of(
                "resource",      resourceName,
                "entryCount",    0,
                "entries",       List.of(),
                "dataFreshAsOf", Instant.now().toString(),
                "error",         "db-unavailable"
            );
        }
    }

    private Map<String, Object> buildPayload(long cacheExpiresAtMillis) {
        List<AuditEntry> entries = auditRepo.findTop25ByOrderByCreatedAtDesc();

        List<Map<String, Object>> serialized = entries.stream()
                .map(e -> {
                    Map<String, Object> m = new LinkedHashMap<>();
                    m.put("auditId",   e.getAuditId());
                    m.put("category",  e.getAuditCategory() != null ? e.getAuditCategory().name() : null);
                    m.put("eventType", e.getEventType());
                    m.put("outcome",   e.getOutcome());
                    m.put("tenantId",  e.getTenantId());
                    m.put("sessionId", e.getSessionId());
                    m.put("createdAt", e.getCreatedAt() != null ? e.getCreatedAt().toString() : null);
                    return Collections.unmodifiableMap(m);
                })
                .toList();

        return Map.of(
                "resource",       resourceName,
                "entryCount",     serialized.size(),
                "entries",        serialized,
                "dataFreshAsOf",  Instant.now().toString(),
                "cacheExpiresAt", Instant.ofEpochMilli(cacheExpiresAtMillis).toString()
        );
    }
}
