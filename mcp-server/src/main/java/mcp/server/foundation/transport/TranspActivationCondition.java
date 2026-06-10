package mcp.server.foundation.transport;

import org.springframework.context.annotation.Condition;
import org.springframework.context.annotation.ConditionContext;
import org.springframework.core.env.Environment;
import org.springframework.core.type.AnnotatedTypeMetadata;

public abstract class TranspActivationCondition implements Condition {

  private final TranspSelection expectedTransp;

  protected TranspActivationCondition(TranspSelection expectedTransp) {
    this.expectedTransp = expectedTransp;
  }

  @Override
  public boolean matches(ConditionContext context, AnnotatedTypeMetadata metadata) {

    Environment environment = context.getEnvironment();
    TranspSelectionSettings selectionSettings = TranspSelectionSupport.TranspSelectionResolve(
        environment.getProperty("mcp.transport.active", ""),
        environment.getProperty("mcp.transport.websocket.enabled", Boolean.class, false),
        environment.getProperty("mcp.transport.stdio.enabled", Boolean.class, false),
        environment.getProperty("mcp.transport.streamable-http.enabled", Boolean.class, false));

    return selectionSettings.activeTransp() == expectedTransp;
  }

  public static final class StreamableHTTPCondition extends TranspActivationCondition {

    public StreamableHTTPCondition() {
      super(TranspSelection.STREAMABLE_HTTP);
    }
  }

  public static final class StdioCondition extends TranspActivationCondition {

    public StdioCondition() {
      super(TranspSelection.STDIO);
    }
  }

  public static final class WebSocketCondition extends TranspActivationCondition {

    public WebSocketCondition() {
      super(TranspSelection.WEBSOCKET);
    }
  }

}
