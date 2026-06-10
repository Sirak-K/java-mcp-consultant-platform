package mcp.server.domain.candidate_presentation.resource;

import mcp.server.domain.candidate_presentation.application.generation.CandidatePresentationGenerationContractService;
import mcp.server.foundation.resource_interface.ResrcProvid;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Objects;

public final class CandidatePresentationGenerationContractResourceProvider implements ResrcProvid {

  private final String resourceUri;
  private final String resourceName;
  private final CandidatePresentationGenerationContractService generationContractService;

  public CandidatePresentationGenerationContractResourceProvider(
      String resourceUri,
      String resourceName,
      CandidatePresentationGenerationContractService generationContractService) {
    this.resourceUri = Objects.requireNonNull(resourceUri, "resourceUri");
    this.resourceName = Objects.requireNonNull(resourceName, "resourceName");
    this.generationContractService = Objects.requireNonNull(
        generationContractService,
        "generationContractService");
  }

  @Override
  public Map<String, Object> ResourceProvRead() {
    LinkedHashMap<String, Object> result = new LinkedHashMap<>();
    result.put("uri", resourceUri);
    result.put("resource", resourceName);
    result.put("trinityLayer", "Resources");
    result.putAll(generationContractService.generationContractPayload());
    return Map.copyOf(result);
  }
}
