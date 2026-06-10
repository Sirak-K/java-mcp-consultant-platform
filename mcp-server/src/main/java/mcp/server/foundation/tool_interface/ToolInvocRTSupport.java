package mcp.server.foundation.tool_interface;

import mcp.server.foundation.observability.context.ObservCtx;
import mcp.server.foundation.observability.context.ObservCtxFactory;
import mcp.server.foundation.rpc.RPCMappedExcep;

import java.util.UUID;
import java.util.concurrent.TimeUnit;

final class ToolInvocRTSupport {

  private static final ObservCtxFactory OBS_CONTEXT_FACTORY = new ObservCtxFactory();

  private ToolInvocRTSupport() {
  }

  static ObservCtx buildToolCtx(ObservCtx context, String method) {

    if (context == null) {
      return OBS_CONTEXT_FACTORY.ObservCtxFactoryWithRequestStartNano(
          OBS_CONTEXT_FACTORY.ObservCtxFactoryWithToolName(
              OBS_CONTEXT_FACTORY.ObservCtxFactoryEmpty(),
              method),
          System.nanoTime());
    }

    return OBS_CONTEXT_FACTORY.ObservCtxFactoryWithRequestStartNano(
        OBS_CONTEXT_FACTORY.ObservCtxFactoryWithToolName(context, method),
        System.nanoTime());
  }

  static String resolveReqsId(ObservCtx context) {
    if (context != null && context.ObservCtxGetRPCCorrelaId() != null && !context.ObservCtxGetRPCCorrelaId().isBlank()) {
      return context.ObservCtxGetRPCCorrelaId();
    }
    return UUID.randomUUID().toString();
  }

  static Long durationFrom(ObservCtx context) {

    if (context == null || context.ObservCtxGetReqsStartNano() == null) {
      return null;
    }

    return TimeUnit.NANOSECONDS.toMillis(System.nanoTime() - context.ObservCtxGetReqsStartNano());
  }

  static long elapsedMillis(long startedAt) {
    return TimeUnit.NANOSECONDS.toMillis(Math.max(0L, System.nanoTime() - startedAt));
  }

  static boolean looksCancelled(RPCMappedExcep mappedException) {
    return mappedException.getMessage() != null
        && mappedException.getMessage().toLowerCase().contains("cancel");
  }
}
