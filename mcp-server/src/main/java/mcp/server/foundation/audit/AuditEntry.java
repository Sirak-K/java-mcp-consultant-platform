package mcp.server.foundation.audit;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import java.time.Instant;

@Entity
@Table(name = "audit_entry", schema = "runtime")
public class AuditEntry {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "audit_id")
    private Long auditId;

    @Enumerated(EnumType.STRING)
    @Column(name = "audit_category", nullable = false, length = 64)
    private AuditCategory auditCategory;

    @Column(name = "event_type", nullable = false, length = 128)
    private String eventType;

    @Column(name = "outcome", nullable = false, length = 32)
    private String outcome;

    @Column(name = "principal_id", length = 256)
    private String principalId;

    @Column(name = "tenant_id", length = 256)
    private String tenantId;

    @Column(name = "session_id", length = 64)
    private String sessionId;

    @Column(name = "request_id", length = 256)
    private String requestId;

    @Column(name = "record_type", length = 64)
    private String recordType;

    @Column(name = "record_id")
    private Long recordId;

    @Column(name = "detail", columnDefinition = "TEXT")
    private String detail;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    protected AuditEntry() {}

    public AuditEntry(AuditCategory auditCategory, String eventType, String outcome,
                      String principalId, String tenantId, String sessionId,
                      String requestId, String recordType, Long recordId,
                      String detail, Instant createdAt) {
        this.auditCategory = auditCategory;
        this.eventType = eventType;
        this.outcome = outcome;
        this.principalId = principalId;
        this.tenantId = tenantId;
        this.sessionId = sessionId;
        this.requestId = requestId;
        this.recordType = recordType;
        this.recordId = recordId;
        this.detail = detail;
        this.createdAt = createdAt;
    }

    public Long getAuditId()                { return auditId; }
    public AuditCategory getAuditCategory() { return auditCategory; }
    public String getEventType()            { return eventType; }
    public String getOutcome()              { return outcome; }
    public String getPrincipalId()          { return principalId; }
    public String getTenantId()             { return tenantId; }
    public String getSessionId()            { return sessionId; }
    public String getRequestId()            { return requestId; }
    public String getRecordType()           { return recordType; }
    public Long getRecordId()               { return recordId; }
    public String getDetail()               { return detail; }
    public Instant getCreatedAt()           { return createdAt; }
}
