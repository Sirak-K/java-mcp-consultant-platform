package mcp.server.foundation.audit;

import org.springframework.data.jpa.repository.JpaRepository;

import java.time.Instant;
import java.util.List;

public interface AuditEntryJpaRepo extends JpaRepository<AuditEntry, Long> {

    void deleteByCreatedAtBefore(Instant cutoff);

    /** Returns the 25 most recent audit entries, newest first. */
    List<AuditEntry> findTop25ByOrderByCreatedAtDesc();
}
