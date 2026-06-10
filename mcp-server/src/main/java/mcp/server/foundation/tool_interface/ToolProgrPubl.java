package mcp.server.foundation.tool_interface;

import mcp.server.foundation.observability.context.ObservCtx;

public interface ToolProgrPubl {

  void ToolProgrPublPublish(
      ToolProgrUpd update,
      ObservCtx context);

  static ToolProgrPubl ToolProgrPublNoOp() {
    return (update, context) -> {
      // Intentionally no-op.
    };
  }
}
