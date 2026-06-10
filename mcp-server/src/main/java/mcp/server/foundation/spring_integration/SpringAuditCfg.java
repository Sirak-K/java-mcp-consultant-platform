package mcp.server.foundation.spring_integration;

import mcp.server.foundation.audit.AuditProperties;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.EnableScheduling;

/**
 * Spring configuration for the audit subsystem.
 * Enables @Scheduled (required for AuditRetentionJob) and binds AuditProperties.
 */
@Configuration
@EnableScheduling
@EnableConfigurationProperties(AuditProperties.class)
public class SpringAuditCfg {
}
