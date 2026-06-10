package mcp.server.domain.candidate_profiles.application.intake;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;

import mcp.server.domain.candidate_profiles.web.CandidateApplicationWebContract;
import mcp.server.domain.candidate_profiles.persistence.CandidateApplicationSnapshotEntity;
import mcp.server.domain.candidate_profiles.persistence.CandidateApplicationSnapshotJpaRepo;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import static mcp.server.domain.shared_kernel.validation.ApplicationInputValidation.safeText;

@Service
public class CandApplicationSnapshotService {

  private static final Logger log = LoggerFactory.getLogger(CandApplicationSnapshotService.class);

  private final CandidateApplicationSnapshotJpaRepo applicationSnapshotRepo;
  private final ObjectMapper objectMapper;

  public CandApplicationSnapshotService(
      CandidateApplicationSnapshotJpaRepo applicationSnapshotRepo,
      ObjectMapper objectMapper) {
    this.applicationSnapshotRepo = applicationSnapshotRepo;
    this.objectMapper = objectMapper;
  }

  public List<CandidateApplicationWebContract.CandidateApplicationView> findAllArchivedViews() {
    return applicationSnapshotRepo.findAllByOrderByCreatedAtDesc().stream()
        .map(this::archivedView)
        .toList();
  }

  public void saveInitialSubmissionSnapshot(CandidateApplicationWebContract.CandidateApplicationView view) {
    try {
      applicationSnapshotRepo.save(new CandidateApplicationSnapshotEntity(
          null,
          objectMapper.writeValueAsString(view),
          Instant.now()));
    } catch (JsonProcessingException exception) {
      log.warn("Candidate application archive snapshot could not be written for candidateProfileId={}: {}",
          view.id(),
          exception.getMessage());
    }
  }

  public int deleteSnapshotsForCandidateProfile(long candidateProfileId) {
    List<CandidateApplicationSnapshotEntity> matchingSnapshots = new ArrayList<>();
    for (CandidateApplicationSnapshotEntity snapshot : applicationSnapshotRepo.findAllByOrderByCreatedAtDesc()) {
      if (snapshotBelongsToCandidateProfile(snapshot, candidateProfileId)) {
        matchingSnapshots.add(snapshot);
      }
    }
    applicationSnapshotRepo.deleteAll(matchingSnapshots);
    return matchingSnapshots.size();
  }

  private CandidateApplicationWebContract.CandidateApplicationView archivedView(
      CandidateApplicationSnapshotEntity entity) {

    String snapshot = safeText(entity.getSubmissionSnapshotJson()).trim();
    try {
      return objectMapper.readValue(snapshot, CandidateApplicationWebContract.CandidateApplicationView.class);
    } catch (JsonProcessingException exception) {
      log.warn("Candidate application archive snapshot could not be read for id={}: {}",
          entity.getId(),
          exception.getMessage());
      throw new ResponseStatusException(
          HttpStatus.INTERNAL_SERVER_ERROR,
          "candidate application archive snapshot could not be read");
    }
  }

  private boolean snapshotBelongsToCandidateProfile(
      CandidateApplicationSnapshotEntity entity,
      long candidateProfileId) {

    String snapshot = safeText(entity.getSubmissionSnapshotJson()).trim();
    try {
      CandidateApplicationWebContract.CandidateApplicationView view = objectMapper.readValue(snapshot,
          CandidateApplicationWebContract.CandidateApplicationView.class);
      return view.id() == candidateProfileId;
    } catch (JsonProcessingException exception) {
      log.warn("Candidate application archive snapshot could not be checked for delete for id={}: {}",
          entity.getId(),
          exception.getMessage());
      return false;
    }
  }
}
