package mcp.server.foundation.tool_interface;

import org.springframework.boot.context.properties.ConfigurationProperties;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Objects;

@ConfigurationProperties(prefix = "mcp.tools.execution")
public class ToolExecProperties {

  private int globalMaxConcurrency = 32;
  private final Defaults defaults = new Defaults();
  private final Map<String, OverridePolicy> overrides = new LinkedHashMap<>();

  public int getGlobalMaxConcurr() {
    return globalMaxConcurrency;
  }

  public void setGlobalMaxConcurr(int globalMaxConcurrency) {
    if (globalMaxConcurrency <= 0) {
      throw new IllegalArgumentException("globalMaxConcurrency must be > 0");
    }
    this.globalMaxConcurrency = globalMaxConcurrency;
  }

  public Defaults getDefaults() {
    return defaults;
  }

  public Map<String, OverridePolicy> getOverrides() {
    return overrides;
  }

  public ToolExecPolicy ToolExecPropertiesResolve(String toolName) {

    Objects.requireNonNull(toolName, "toolName");

    OverridePolicy overridePolicy = overrides.get(toolName);
    long timeoutMillis = overridePolicy != null && overridePolicy.getTimeoutMillis() != null
        ? overridePolicy.getTimeoutMillis()
        : defaults.getTimeoutMillis();
    int maxConcurrency = overridePolicy != null && overridePolicy.getMaxConcurr() != null
        ? overridePolicy.getMaxConcurr()
        : defaults.getMaxConcurr();
    boolean cancellable = overridePolicy != null && overridePolicy.getCancellable() != null
        ? overridePolicy.getCancellable()
        : defaults.isCancellable();
    boolean progressEnabled = overridePolicy != null && overridePolicy.getProgrEnabled() != null
        ? overridePolicy.getProgrEnabled()
        : defaults.isProgrEnabled();

    return new ToolExecPolicy(timeoutMillis, maxConcurrency, cancellable, progressEnabled);
  }

  public static final class Defaults {

    private long timeoutMillis = 10_000L;
    private int maxConcurrency = 32;
    private boolean cancellable = true;
    private boolean progressEnabled = false;

    public long getTimeoutMillis() {
      return timeoutMillis;
    }

    public void setTimeoutMillis(long timeoutMillis) {
      if (timeoutMillis <= 0L) {
        throw new IllegalArgumentException("timeoutMillis must be > 0");
      }
      this.timeoutMillis = timeoutMillis;
    }

    public int getMaxConcurr() {
      return maxConcurrency;
    }

    public void setMaxConcurr(int maxConcurrency) {
      if (maxConcurrency <= 0) {
        throw new IllegalArgumentException("maxConcurrency must be > 0");
      }
      this.maxConcurrency = maxConcurrency;
    }

    public boolean isCancellable() {
      return cancellable;
    }

    public void setCancellable(boolean cancellable) {
      this.cancellable = cancellable;
    }

    public boolean isProgrEnabled() {
      return progressEnabled;
    }

    public void setProgrEnabled(boolean progressEnabled) {
      this.progressEnabled = progressEnabled;
    }
  }

  public static final class OverridePolicy {

    private Long timeoutMillis;
    private Integer maxConcurrency;
    private Boolean cancellable;
    private Boolean progressEnabled;

    public Long getTimeoutMillis() {
      return timeoutMillis;
    }

    public void setTimeoutMillis(Long timeoutMillis) {
      if (timeoutMillis != null && timeoutMillis <= 0L) {
        throw new IllegalArgumentException("timeoutMillis must be > 0");
      }
      this.timeoutMillis = timeoutMillis;
    }

    public Integer getMaxConcurr() {
      return maxConcurrency;
    }

    public void setMaxConcurr(Integer maxConcurrency) {
      if (maxConcurrency != null && maxConcurrency <= 0) {
        throw new IllegalArgumentException("maxConcurrency must be > 0");
      }
      this.maxConcurrency = maxConcurrency;
    }

    public Boolean getCancellable() {
      return cancellable;
    }

    public void setCancellable(Boolean cancellable) {
      this.cancellable = cancellable;
    }

    public Boolean getProgrEnabled() {
      return progressEnabled;
    }

    public void setProgrEnabled(Boolean progressEnabled) {
      this.progressEnabled = progressEnabled;
    }
  }
}
