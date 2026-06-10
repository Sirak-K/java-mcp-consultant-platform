package mcp.server.domain.candidate_profiles.persistence;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface CandidateApplicationSnapshotJpaRepo extends JpaRepository<CandidateApplicationSnapshotEntity, Long> {

    List<CandidateApplicationSnapshotEntity> findAllByOrderByCreatedAtDesc();
}
