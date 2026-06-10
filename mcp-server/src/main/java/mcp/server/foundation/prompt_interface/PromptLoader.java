package mcp.server.foundation.prompt_interface;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;

/**
 * Loads prompt bodies from classpath resources.
 */
public final class PromptLoader {

  public String PromptLoadReadText(String resourcePath) {

    String normalizedPath = PromptLoadNormalizePath(resourcePath);

    try (InputStream inputStream = getClass().getClassLoader().getResourceAsStream(normalizedPath)) {
      if (inputStream == null) {
        throw new IllegalArgumentException("Prompt resource not found: " + normalizedPath);
      }

      return new String(inputStream.readAllBytes(), StandardCharsets.UTF_8);
    } catch (IOException ex) {
      throw new IllegalStateException("Could not read prompt resource: " + normalizedPath, ex);
    }
  }

  String PromptLoadNormalizePath(String resourcePath) {

    return PromptSupport.normalizeResourcePath(resourcePath);
  }
}
