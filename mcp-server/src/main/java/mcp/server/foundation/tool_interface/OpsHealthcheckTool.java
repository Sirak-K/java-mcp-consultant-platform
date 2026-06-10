package mcp.server.foundation.tool_interface;

import mcp.server.foundation.rpc.RPCMetName;

import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;

/**
 * OpsHealthcheckTool
 *
 * Domänneutral liveness capability.
 *
 * Kontrakt:
 * - Namn hämtas från RPCMetName.
 * - Parametrar: inga
 * - Returnerar konstant payload (deterministisk)
 */
@Component
public final class OpsHealthcheckTool implements ToolInterface {

  @Override
  public String getName() {
    return RPCMetName.OPS_HEALTHCHECK;
  }

  @Override
  public String getDescription() {
    return "Foundation liveness ping capability";
  }

  @Override
  public Map<String, Object> getInputSchema() {
    // Parametrar = inga
    return Map.of(
        "type", "object",
        "properties", Map.of());
  }

  @Override
  public Map<String, Object> getOutputSchema() {
    return Map.of(
        "type", "object",
        "required", List.of("pong"),
        "properties", Map.of(
            "pong", Map.of("type", "boolean")));
  }

  @Override
  public ToolResponse execute(ToolReqs request) {

    // Konstant deterministisk payload för functional liveness.
    return ToolResponse.ToolRespSuccess(
        Map.of("pong", true));
  }
}
