package mcp.server.domain.candidate_profiles.application.intake;

import org.springframework.stereotype.Service;

import mcp.server.domain.candidate_profiles.web.CandidateApplicationWebContract;
import static mcp.server.domain.shared_kernel.validation.ApplicationInputValidation.reject;
import static mcp.server.domain.shared_kernel.validation.ApplicationInputValidation.requireEmail;
import static mcp.server.domain.shared_kernel.validation.ApplicationInputValidation.requireText;

@Service
public class CandApplicationValidationService {
  public void requireCandidateProfileRequest(CandidateApplicationWebContract.CandidateApplicationInput request) {
    if (request == null) {
      throw reject("candidateApplication is required");
    }
    requireEmail(request.contactEmail(), "contactEmail is required");
    requireText(request.cvFileName(), "cvFileName is required");
    requireText(request.cvContentType(), "cvContentType is required");
    if (request.cvSizeBytes() != null && request.cvSizeBytes() < 0) {
      throw reject("cvSizeBytes must be zero or positive");
    }
  }
}
