package mcp.server.foundation.prompt_interface;

import java.util.Map;
import java.util.Objects;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Resolves prompt definitions into rendered prompt bodies.
 */
public final class PromptService {

  private static final Pattern PLACEHOLDER_PATTERN = Pattern.compile("\\{\\{\\s*([a-zA-Z0-9_.-]+)\\s*}}");

  private final PromptReg promptReg;
  private final PromptLoader promptLoader;

  public PromptService(
      PromptReg promptReg,
      PromptLoader promptLoader) {

    this.promptReg = Objects.requireNonNull(promptReg, "promptReg");
    this.promptLoader = Objects.requireNonNull(promptLoader, "promptLoader");
  }

  public PromptRenderResult PromptSvcRender(
      String promptName,
      Map<String, Object> arguments) {

    PromptDefin definition = promptReg.PromptRegGetDefin(
        PromptSupport.requireNonBlank(promptName, "promptName"));
    if (definition == null) {
      throw new IllegalArgumentException("Prompt not found: " + promptName);
    }

    Map<String, Object> safeArguments = PromptSupport.safeArguments(arguments);
    String template = promptLoader.PromptLoadReadText(definition.PromptDefGetResrcPath());
    String renderedText = PromptSvcInterpolate(template, safeArguments);

    return new PromptRenderResult(definition, renderedText, safeArguments);
  }

  String PromptSvcInterpolate(
      String template,
      Map<String, Object> arguments) {

    Objects.requireNonNull(template, "template");
    Objects.requireNonNull(arguments, "arguments");

    Matcher matcher = PLACEHOLDER_PATTERN.matcher(template);
    StringBuffer rendered = new StringBuffer();

    while (matcher.find()) {
      String argumentName = matcher.group(1);
      if (!arguments.containsKey(argumentName)) {
        throw new IllegalArgumentException("Missing prompt argument: " + argumentName);
      }

      Object value = arguments.get(argumentName);
      String replacement = value == null ? "" : String.valueOf(value);
      matcher.appendReplacement(rendered, Matcher.quoteReplacement(replacement));
    }

    matcher.appendTail(rendered);
    return rendered.toString();
  }
}
