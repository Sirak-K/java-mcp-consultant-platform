package mcp.server.foundation.security;

import mcp.server.foundation.transport.TranspSelectionSettings;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.logging.Logger;

@Configuration
public class NetwAuthCfg {

  private static final Logger LOGGER = Logger.getLogger(NetwAuthCfg.class.getName());

  @Bean
  public NetwAuthSettings networkAuthSettings(
      TranspSelectionSettings transportSelectionSettings,
      @Value("${mcp.security.network-auth.enabled:true}") boolean enabled,
      @Value("${mcp.security.network-auth.header-name:Authorization}") String headerName,
      @Value("${mcp.security.network-auth.scheme:Bearer}") String scheme,
      @Value("${mcp.security.network-auth.bearer-token:}") String bearerToken,
      @Value("${mcp.security.network-auth.bearer-token-transition:}") String bearerTokenTransition) {

    boolean isNetworkTransp = transportSelectionSettings.TranspSelectionSettingsIsNetworkTransp();
    if (enabled && !isNetworkTransp) {
      LOGGER.warning(
          "Network auth is configured as enabled but the active transport ("
              + transportSelectionSettings.TranspSelectionSettingsActiveTranspName()
              + ") is not a network transport. Auth will be silently disabled.");
    }

    return new NetwAuthSettings(
        enabled && isNetworkTransp,
        headerName,
        scheme,
        bearerToken,
        bearerTokenTransition);
  }

  @Bean
  public NetwAuthService networkAuthService(NetwAuthSettings settings) {
    return new NetwAuthService(settings);
  }
}
