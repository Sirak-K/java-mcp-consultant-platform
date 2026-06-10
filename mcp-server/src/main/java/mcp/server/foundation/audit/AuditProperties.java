package mcp.server.foundation.audit;

import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * Configuration properties for the audit subsystem.
 * Bound from the "audit" prefix in application.yml.
 */
@ConfigurationProperties(prefix = "audit")
public class AuditProperties {

    /** Number of days to retain audit entries. Default: 5 (dev phase). */
    private int retentionDays = 5;

    public int getRetentionDays() {
        return retentionDays;
    }

    public void setRetentionDays(int retentionDays) {
        this.retentionDays = retentionDays;
    }
}
