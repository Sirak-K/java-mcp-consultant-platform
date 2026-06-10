package mcp.server.foundation.audit;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;

/**
 * Synchronous audit service.
 * Each record method runs in its own REQUIRES_NEW transaction so the audit
 * entry is committed even if the calling transaction rolls back.
 */
@Service
public class AuditService {

    private final AuditEntryJpaRepo repo;

    public AuditService(AuditEntryJpaRepo repo) {
        this.repo = repo;
    }

    /** Category: IDENTITY_TENANT - session lifecycle events. */
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void recordIdentityEvent(
            String eventType,
            String outcome,
            String principalId,
            String tenantId,
            String sessionId,
            String requestId,
            String detail) {
        repo.save(new AuditEntry(
                AuditCategory.IDENTITY_TENANT,
                eventType, outcome,
                principalId, tenantId, sessionId,
                requestId, null, null,
                detail, Instant.now()));
    }

    /** Category: ENFORCEMENT_ACCESS - tenant guard allow/deny decisions. */
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void recordEnforcementEvent(
            String eventType,
            String outcome,
            String tenantId,
            String requestId,
            String detail) {
        repo.save(new AuditEntry(
                AuditCategory.ENFORCEMENT_ACCESS,
                eventType, outcome,
                null, tenantId, null,
                requestId, null, null,
                detail, Instant.now()));
    }

    /** Category: OPERATIONAL_BUSINESS - business state changes. */
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void recordBusinessEvent(
            String eventType,
            String outcome,
            String tenantId,
            String recordType,
            Long recordId,
            String detail) {
        repo.save(new AuditEntry(
                AuditCategory.OPERATIONAL_BUSINESS,
                eventType, outcome,
                null, tenantId, null,
                null, recordType, recordId,
                detail, Instant.now()));
    }
}
