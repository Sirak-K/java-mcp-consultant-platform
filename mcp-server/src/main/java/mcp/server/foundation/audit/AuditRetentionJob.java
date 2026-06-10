package mcp.server.foundation.audit;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.time.temporal.ChronoUnit;

/**
 * Scheduled cleanup job that deletes audit entries older than the configured TTL.
 * Runs daily at 02:00.
 */
@Component
public class AuditRetentionJob {

    private static final Logger log = LoggerFactory.getLogger(AuditRetentionJob.class);

    private final AuditEntryJpaRepo repo;
    private final AuditProperties properties;

    public AuditRetentionJob(AuditEntryJpaRepo repo, AuditProperties properties) {
        this.repo = repo;
        this.properties = properties;
    }

    @Scheduled(cron = "0 0 2 * * *")
    @Transactional
    public void purgeExpiredEntries() {
        Instant cutoff = Instant.now().minus(properties.getRetentionDays(), ChronoUnit.DAYS);
        log.info("AuditRetentionJob: purging audit entries older than {} (retention-days={})",
                cutoff, properties.getRetentionDays());
        repo.deleteByCreatedAtBefore(cutoff);
    }
}
