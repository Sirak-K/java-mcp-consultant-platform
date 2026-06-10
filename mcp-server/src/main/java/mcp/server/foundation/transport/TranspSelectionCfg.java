package mcp.server.foundation.transport;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.logging.Logger;

@Configuration
public class TranspSelectionCfg {

  private static final Logger LOGGER = Logger.getLogger(TranspSelectionCfg.class.getName());

  @Bean
  public TranspSelectionSettings transportSelectionSettings(
      @Value("${mcp.transport.active:}") String activeTranspRaw,
      @Value("${mcp.transport.websocket.enabled:false}") boolean websocketEnabled,
      @Value("${mcp.transport.stdio.enabled:false}") boolean stdioEnabled,
      @Value("${mcp.transport.streamable-http.enabled:false}") boolean streamableHttpEnabled) {

    TranspSelectionSettings settings = TranspSelectionSupport.TranspSelectionResolve(
        activeTranspRaw,
        websocketEnabled,
        stdioEnabled,
        streamableHttpEnabled);

    if (settings.propertySelectionMode()) {
      LOGGER.warning(
          "Transport boolean property "
              + settings.propertySelectionSourceProperty()
              + " selected active transport "
              + settings.TranspSelectionSettingsActiveTranspName()
              + ". Set mcp.transport.active instead.");
    }

    return settings;
  }
}
