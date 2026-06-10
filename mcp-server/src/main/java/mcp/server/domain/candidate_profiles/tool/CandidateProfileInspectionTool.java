package mcp.server.domain.candidate_profiles.tool;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;

import mcp.server.domain.candidate_profiles.application.intake.CandidateApplicationIntakeService;
import mcp.server.domain.candidate_profiles.application.registered_profiles.RegisteredCandidateProfileService;
import mcp.server.domain.candidate_profiles.web.CandidateApplicationWebContract;
import mcp.server.foundation.tool_interface.ToolInterface;
import mcp.server.foundation.tool_interface.ToolReqs;
import mcp.server.foundation.tool_interface.ToolResponse;
import org.springframework.stereotype.Component;

import static mcp.server.foundation.tool_interface.ToolInputSchemaSupport.arraySchema;
import static mcp.server.foundation.tool_interface.ToolInputSchemaSupport.closedObjectSchema;
import static mcp.server.foundation.tool_interface.ToolInputSchemaSupport.integerSchema;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

@Component
public final class CandidateProfileInspectionTool {

  public static final String APPLICATION_ARCHIVE_TOOL_NAME = "candidateApplication.inspectArchive";
  public static final String REGISTERED_PROFILE_TOOL_NAME = "registeredCandidateProfile.inspect";

  private final CandidateApplicationIntakeService candidateApplicationIntakeService;
  private final RegisteredCandidateProfileService registeredCandidateProfileService;
  private final ObjectMapper objectMapper;

  public CandidateProfileInspectionTool(
      CandidateApplicationIntakeService candidateApplicationIntakeService,
      RegisteredCandidateProfileService registeredCandidateProfileService,
      ObjectMapper objectMapper) {
    this.candidateApplicationIntakeService = Objects.requireNonNull(
        candidateApplicationIntakeService,
        "candidateApplicationIntakeService");
    this.registeredCandidateProfileService = Objects.requireNonNull(
        registeredCandidateProfileService,
        "registeredCandidateProfileService");
    this.objectMapper = Objects.requireNonNull(objectMapper, "objectMapper");
  }

  public ToolInterface inspectApplicationArchiveTool() {
    return new InspectApplicationArchiveImpl();
  }

  public ToolInterface inspectRegisteredProfilesTool() {
    return new InspectRegisteredProfilesImpl();
  }

  private final class InspectApplicationArchiveImpl implements ToolInterface {

    @Override
    public String getName() {
      return APPLICATION_ARCHIVE_TOOL_NAME;
    }

    @Override
    public String getDescription() {
      return "Inspect archived Candidate Applications from the immutable submission snapshot surface. "
          + "Use this read-only capability when an MCP client needs candidate application history without creating "
          + "or editing candidate profiles.";
    }

    @Override
    public Map<String, Object> getInputSchema() {
      return closedObjectSchema(Map.of(), List.of());
    }

    @Override
    public Map<String, Object> getOutputSchema() {
      return candidateListOutputSchema("applications");
    }

    @Override
    public boolean getDestructiveHint() {
      return false;
    }

    @Override
    public boolean getIdempotentHint() {
      return true;
    }

    @Override
    public ToolResponse execute(ToolReqs req) {
      List<CandidateApplicationWebContract.CandidateApplicationView> applications = candidateApplicationIntakeService
          .candidateApplicationsArchive();
      return ToolResponse.ToolRespStructured(
          candidateListContent("applications", applications),
          "Candidate Application archive inspected from immutable submission snapshots.");
    }
  }

  private final class InspectRegisteredProfilesImpl implements ToolInterface {

    @Override
    public String getName() {
      return REGISTERED_PROFILE_TOOL_NAME;
    }

    @Override
    public String getDescription() {
      return "Inspect Registered Candidate Profiles from the canonical candidate profile surface. "
          + "Use this read-only capability when an MCP client needs registered candidate profile data without "
          + "running match discovery or editing profile fields.";
    }

    @Override
    public Map<String, Object> getInputSchema() {
      return closedObjectSchema(Map.of(), List.of());
    }

    @Override
    public Map<String, Object> getOutputSchema() {
      return candidateListOutputSchema("profiles");
    }

    @Override
    public boolean getDestructiveHint() {
      return false;
    }

    @Override
    public boolean getIdempotentHint() {
      return true;
    }

    @Override
    public ToolResponse execute(ToolReqs req) {
      List<CandidateApplicationWebContract.CandidateApplicationView> profiles = registeredCandidateProfileService
          .registeredCandidateProfiles();
      return ToolResponse.ToolRespStructured(
          candidateListContent("profiles", profiles),
          "Registered Candidate Profiles inspected without running match discovery.");
    }
  }

  private Map<String, Object> candidateListContent(
      String listKey,
      List<CandidateApplicationWebContract.CandidateApplicationView> candidates) {
    LinkedHashMap<String, Object> structuredContent = new LinkedHashMap<>();
    structuredContent.put("count", candidates.size());
    structuredContent.put(
        listKey,
        objectMapper.convertValue(
            candidates,
            new TypeReference<List<LinkedHashMap<String, Object>>>() {
            }));
    return Map.copyOf(structuredContent);
  }

  private Map<String, Object> candidateListOutputSchema(String listKey) {
    return closedObjectSchema(
        Map.of(
            "count", integerSchema(),
            listKey, arraySchema(Map.of("type", "object", "additionalProperties", true))),
        List.of("count", listKey));
  }

}
