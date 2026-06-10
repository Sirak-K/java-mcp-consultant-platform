package mcp.server.foundation.spring_integration;

import jakarta.annotation.PostConstruct;
import java.util.List;
import java.util.Objects;

import mcp.server.foundation.prompt_interface.PromptDefin;
import mcp.server.foundation.prompt_interface.PromptDefinProvid;
import mcp.server.foundation.prompt_interface.PromptReg;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

/**
 * MCP prompt registration hook.
 */
@Component
public class McpPromptsRegistrar {

  private static final Logger log = LoggerFactory.getLogger(McpPromptsRegistrar.class);

  private final PromptReg promptReg;
  private final List<PromptDefinProvid> promptDefinitionProviders;

  public McpPromptsRegistrar(
      PromptReg promptReg,
      List<PromptDefinProvid> promptDefinitionProviders) {

    this.promptReg = Objects.requireNonNull(promptReg, "promptReg");
    this.promptDefinitionProviders = List.copyOf(
        Objects.requireNonNull(promptDefinitionProviders, "promptDefinitionProviders"));
  }

  @PostConstruct
  public void registerPrompts() {
    int registeredCount = 0;
    for (PromptDefinProvid provider : promptDefinitionProviders) {
      List<PromptDefin> definitions = Objects.requireNonNull(
          provider.PromptDefinProvidListDefinitions(),
          "prompt provider definitions");
      for (PromptDefin definition : definitions) {
        promptReg.PromptRegRegister(definition);
        registeredCount++;
      }
    }

    log.info("[McpPromptsRegistrar] MCP prompt providers count={}, registered count={}, registry size={}",
        promptDefinitionProviders.size(),
        registeredCount,
        promptReg.PromptRegSize());
  }
}
