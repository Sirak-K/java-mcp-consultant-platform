package mcp.server.foundation.tool_interface;

import mcp.server.foundation.rpc.RPCMetName;

import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

/**
 * ToolReg
 *
 * Hardening:
 * - Förhindrar att verktyg registreras med RPC-reserverade metodnamn.
 * - Förhindrar att domän-/andra verktyg "kolliderar" med foundation-prefixet
 * "system".
 * - Fortsätter vara deterministisk: duplikatnamn => fail-fast.
 */
public final class ToolReg {

  /**
   * Foundation-prefixet "system" är en unik routing-nyckel.
   * I nuvarande design tillåter vi endast explicit allowlistat foundation healthcheck-tool.
   * (Utökning till fler "system.*" görs explicit senare.)
   */
  private static final String RESERVED_FOUNDATION_PREFIX = "system";
  private static final Set<String> ALLOWED_SYSTEM_TOOLS = Set.of(RPCMetName.OPS_HEALTHCHECK);

  private final Map<String, ToolDefinition> definitions = new ConcurrentHashMap<>();

  public void ToolRegRegister(ToolDefinition definition) {

    Objects.requireNonNull(definition, "definition");

    String name = definition.ToolDefGetName();

    if (name == null || name.isBlank()) {
      throw new IllegalArgumentException("Tool name cannot be null or blank");
    }

    // 1) Blockera RPC-reserverade namn
    if (RPCMetName.RPCMetNameIsReservedToolName(name)) {
      throw new IllegalArgumentException("Tool name reserved by RPC layer: " + name);
    }

    // 2) Blockera foundation-prefixet "system" om det inte är explicit allowlisted.
    if (name.startsWith(RESERVED_FOUNDATION_PREFIX + ".") && !ALLOWED_SYSTEM_TOOLS.contains(name)) {
      throw new IllegalArgumentException(
          "Tool name under reserved prefix '" + RESERVED_FOUNDATION_PREFIX + "': " + name);
    }

    // 3) Fail-fast on duplicates
    ToolDefinition prev = definitions.putIfAbsent(name, definition);

    if (prev != null) {
      throw new IllegalStateException("Tool already registered: " + name);
    }
  }

  public ToolDefinition ToolRegGetDefin(String name) {
    return definitions.get(name);
  }

  public List<ToolDefinition> ToolRegListDefinitions() {
    return definitions.values().stream()
        .sorted(Comparator.comparing(ToolDefinition::ToolDefGetName))
        .toList();
  }

  public Set<String> ToolRegListNames() {
    return definitions.keySet();
  }

  public int ToolRegSize() {
    return definitions.size();
  }
}
