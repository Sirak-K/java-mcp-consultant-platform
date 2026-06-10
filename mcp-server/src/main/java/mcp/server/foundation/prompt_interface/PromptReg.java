package mcp.server.foundation.prompt_interface;

import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Deterministic prompt registry.
 */
public final class PromptReg {

  private final Map<String, PromptDefin> definitions = new ConcurrentHashMap<>();

  public void PromptRegRegister(PromptDefin definition) {

    Objects.requireNonNull(definition, "definition");

    PromptDefin previous = definitions.putIfAbsent(
        definition.PromptDefGetName(),
        definition);

    if (previous != null) {
      throw new IllegalStateException("Prompt already registered: " + definition.PromptDefGetName());
    }
  }

  public PromptDefin PromptRegGetDefin(String name) {
    return definitions.get(name);
  }

  public List<PromptDefin> PromptRegListDefinitions() {
    return definitions.values().stream()
        .sorted(Comparator.comparing(PromptDefin::PromptDefGetName))
        .toList();
  }

  public int PromptRegSize() {
    return definitions.size();
  }
}
