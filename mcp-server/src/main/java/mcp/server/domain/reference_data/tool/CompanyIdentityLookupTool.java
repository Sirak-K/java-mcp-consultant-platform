package mcp.server.domain.reference_data.tool;

import mcp.server.domain.reference_data.application.CompanyIdentityLookupService;
import mcp.server.domain.reference_data.application.CompanyIdentityLookupService.CompanyIdentityOption;
import mcp.server.foundation.tool_interface.ToolInterface;
import mcp.server.foundation.tool_interface.ToolReqs;
import mcp.server.foundation.tool_interface.ToolResponse;
import org.springframework.stereotype.Component;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

import static mcp.server.foundation.tool_interface.ToolInputSchemaSupport.arraySchema;
import static mcp.server.foundation.tool_interface.ToolInputSchemaSupport.closedObjectSchema;
import static mcp.server.foundation.tool_interface.ToolInputSchemaSupport.stringSchema;
import static mcp.server.foundation.tool_interface.ToolReqsSupport.ToolReqSupportRequiredText;

@Component
public final class CompanyIdentityLookupTool {

  public static final String LOOKUP_TOOL_NAME = "companyIdentity.lookup";
  private static final String COMPANY_NAME_ARGUMENT = "companyName";

  private final CompanyIdentityLookupService companyIdentityLookupService;

  public CompanyIdentityLookupTool(CompanyIdentityLookupService companyIdentityLookupService) {
    this.companyIdentityLookupService = Objects.requireNonNull(companyIdentityLookupService,
        "companyIdentityLookupService");
  }

  public ToolInterface lookupTool() {
    return new LookupImpl();
  }

  private final class LookupImpl implements ToolInterface {

    @Override
    public String getName() {
      return LOOKUP_TOOL_NAME;
    }

    @Override
    public String getDescription() {
      return "Lookup Swedish company identity fields from the local company identity lookup table. "
          + "Use this read-only capability when an MCP client needs organisation name, organisation number, city "
          + "or candidate options for a company name without uploading a CV.";
    }

    @Override
    public Map<String, Object> getInputSchema() {
      return closedObjectSchema(
          Map.of(
              COMPANY_NAME_ARGUMENT,
              Map.of(
                  "type", "string",
                  "description", "Company name to resolve against the local company identity lookup table")),
          List.of(COMPANY_NAME_ARGUMENT));
    }

    @Override
    public Map<String, Object> getOutputSchema() {
      return closedObjectSchema(
          Map.of(
              "organisationName", stringSchema(),
              "organisationNumber", stringSchema(),
              "organisationCity", stringSchema(),
              "options", arraySchema(companyIdentityOptionSchema())),
          List.of("organisationName", "organisationNumber", "organisationCity", "options"));
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
      String companyName = ToolReqSupportRequiredText(req, COMPANY_NAME_ARGUMENT);
      CompanyIdentityLookupService.CompanyIdentityResolution resolution = companyIdentityLookupService
          .resolve(companyName);
      return ToolResponse.ToolRespStructured(
          toStructuredContent(resolution),
          "Company identity lookup completed from local deterministic lookup data.");
    }
  }

  private Map<String, Object> toStructuredContent(
      CompanyIdentityLookupService.CompanyIdentityResolution resolution) {
    LinkedHashMap<String, Object> structuredContent = new LinkedHashMap<>();
    structuredContent.put("organisationName", resolution.organisationName());
    structuredContent.put("organisationNumber", resolution.organisationNumber());
    structuredContent.put("organisationCity", resolution.organisationCity());
    structuredContent.put(
        "options",
        resolution.options().stream()
            .map(this::toOption)
            .toList());
    return Map.copyOf(structuredContent);
  }

  private Map<String, Object> toOption(
      CompanyIdentityOption option) {
    return Map.of(
        "organisationName", option.organisationName(),
        "organisationNumber", option.organisationNumber(),
        "organisationCity", option.organisationCity());
  }

  private Map<String, Object> companyIdentityOptionSchema() {
    return closedObjectSchema(
        Map.of(
            "organisationName", stringSchema(),
            "organisationNumber", stringSchema(),
            "organisationCity", stringSchema()),
        List.of("organisationName", "organisationNumber", "organisationCity"));
  }

}
