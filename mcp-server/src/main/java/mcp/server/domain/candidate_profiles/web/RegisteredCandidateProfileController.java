package mcp.server.domain.candidate_profiles.web;

import mcp.server.domain.candidate_profiles.application.registered_profiles.RegisteredCandidateProfileService;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.Objects;

@RestController
public final class RegisteredCandidateProfileController {

  private final RegisteredCandidateProfileService registeredCandidateProfileService;

  public RegisteredCandidateProfileController(
      RegisteredCandidateProfileService registeredCandidateProfileService) {
    this.registeredCandidateProfileService = Objects.requireNonNull(
        registeredCandidateProfileService,
        "registeredCandidateProfileService");
  }

  @GetMapping("/api/ops/registered-candidate-profiles")
  public List<CandidateApplicationWebContract.CandidateApplicationView> registeredCandidateProfiles() {
    return registeredCandidateProfileService.registeredCandidateProfiles();
  }

  @GetMapping("/api/ops/registered-candidate-profile-cards")
  public List<RegisteredCandidateProfileWebContract.RegisteredCandidateProfileCardView> registeredCandidateProfileCards() {
    return registeredCandidateProfileService.registeredCandidateProfileCards();
  }

  @PutMapping("/api/ops/registered-candidate-profiles/{id}")
  public CandidateApplicationWebContract.CandidateApplicationView editRegisteredCandidateProfile(
      @PathVariable("id") long id,
      @RequestBody RegisteredCandidateProfileWebContract.RegisteredCandidateProfileEditInput request) {
    return registeredCandidateProfileService.editRegisteredCandidateProfile(id, request);
  }

  @DeleteMapping("/api/ops/registered-candidate-profiles/{id}")
  @ResponseStatus(HttpStatus.NO_CONTENT)
  public void deleteRegisteredCandidateProfile(@PathVariable("id") long id) {
    registeredCandidateProfileService.deleteRegisteredCandidateProfile(id);
  }
}
