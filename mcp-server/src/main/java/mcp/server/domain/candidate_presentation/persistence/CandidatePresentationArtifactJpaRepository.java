package mcp.server.domain.candidate_presentation.persistence;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;

public interface CandidatePresentationArtifactJpaRepository
    extends JpaRepository<CandidatePresentationArtifactEntity, Long> {

  List<CandidatePresentationArtifactEntity> findAllByOrderByUpdatedAtDesc();

  Optional<CandidatePresentationArtifactEntity> findBySourceCandidateToSlotMatchId(Long sourceCandidateToSlotMatchId);

  List<CandidatePresentationArtifactEntity> findAllByCandProfileIdOrderByUpdatedAtDesc(Long candProfileId);

  List<CandidatePresentationArtifactEntity> findAllByMissionIdOrderByUpdatedAtDesc(Long missionId);
}
