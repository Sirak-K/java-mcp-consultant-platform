package mcp.server.foundation.ai.generation;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

@Component
@ConfigurationProperties(prefix = "mcp.ai.generation")
public class AiGenerationRuntimeProperties {

  private String mcpEndpointUrl = "http://127.0.0.1:8080/mcp";
  private String mcpTransportKind = "streamable_http";
  private double mcpTimeoutSeconds = 30.0d;
  private final Runtime runtime = new Runtime();

  public String getMcpEndpointUrl() {
    return mcpEndpointUrl;
  }

  public void setMcpEndpointUrl(String mcpEndpointUrl) {
    this.mcpEndpointUrl = required(mcpEndpointUrl, "mcpEndpointUrl");
  }

  public String getMcpTransportKind() {
    return mcpTransportKind;
  }

  public void setMcpTransportKind(String mcpTransportKind) {
    this.mcpTransportKind = required(mcpTransportKind, "mcpTransportKind");
  }

  public double getMcpTimeoutSeconds() {
    return mcpTimeoutSeconds;
  }

  public void setMcpTimeoutSeconds(double mcpTimeoutSeconds) {
    if (mcpTimeoutSeconds <= 0.0d) {
      throw new IllegalArgumentException("mcpTimeoutSeconds must be greater than zero");
    }
    this.mcpTimeoutSeconds = mcpTimeoutSeconds;
  }

  public Runtime getRuntime() {
    return runtime;
  }

  public static final class Runtime {

    private String baseUrl = "http://127.0.0.1:8091";
    private String generationRunPath = "/candidate-presentation-generation/runs";
    private long connectTimeoutMillis = 1000L;
    private long requestTimeoutMillis = 5000L;

    public String getBaseUrl() {
      return baseUrl;
    }

    public void setBaseUrl(String baseUrl) {
      this.baseUrl = required(baseUrl, "baseUrl");
    }

    public String getGenerationRunPath() {
      return generationRunPath;
    }

    public void setGenerationRunPath(String generationRunPath) {
      this.generationRunPath = path(generationRunPath, "generationRunPath");
    }

    public long getConnectTimeoutMillis() {
      return connectTimeoutMillis;
    }

    public void setConnectTimeoutMillis(long connectTimeoutMillis) {
      if (connectTimeoutMillis <= 0L) {
        throw new IllegalArgumentException("connectTimeoutMillis must be greater than zero");
      }
      this.connectTimeoutMillis = connectTimeoutMillis;
    }

    public long getRequestTimeoutMillis() {
      return requestTimeoutMillis;
    }

    public void setRequestTimeoutMillis(long requestTimeoutMillis) {
      if (requestTimeoutMillis <= 0L) {
        throw new IllegalArgumentException("requestTimeoutMillis must be greater than zero");
      }
      this.requestTimeoutMillis = requestTimeoutMillis;
    }
  }

  private static String path(String value, String fieldName) {
    String requiredValue = required(value, fieldName);
    return requiredValue.startsWith("/") ? requiredValue : "/" + requiredValue;
  }

  private static String required(String value, String fieldName) {
    if (value == null || value.isBlank()) {
      throw new IllegalArgumentException(fieldName + " is required");
    }
    return value.trim();
  }
}
