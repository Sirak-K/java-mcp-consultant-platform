package mcp.server.foundation.server_process.orchestration;

import mcp.server.foundation.logging.ServerLogger;
import mcp.server.foundation.security.request_binding.ReqsLifecyReg;
import mcp.server.foundation.server_process.status.RTStatus;
import mcp.server.foundation.server_process.status.event.RTStatusEventPayl;
import mcp.server.foundation.server_process.status.event.RTStatusEventPubl;

import java.util.Objects;

/**
 * McpRTOrch
 *
 * Ren POJO (ingen Spring-koppling).
 *
 * Ansvar:
 * - Orkestrera runtime-livscykel (start/stop)
 * - Delegation till StartupMan + ShutdownMan
 * - Äger RTStatus transitions (strict)
 */
public final class McpRTOrch {

  private final RTStatus runtimeStatus;
  private final RTStatusEventPubl RTStatusEventPubl;
  private final StartupMan startupManager;
  private final ShutdownMan shutdownManager;
  private final OperatingModelReg operatingModelRegistry;
  private final ReqsLifecyReg requestLifecycleRegistry;
  private final RTMcpSessModelReg runtimeSessionModelRegistry;
  private final ServerLogger serverLogger;

  public McpRTOrch(
      RTStatus runtimeStatus,
      RTStatusEventPubl RTStatusEventPubl,
      StartupMan startupManager,
      ShutdownMan shutdownManager,
      OperatingModelReg operatingModelRegistry,
      ReqsLifecyReg requestLifecycleRegistry,
      RTMcpSessModelReg runtimeSessionModelRegistry,
      ServerLogger serverLogger) {

    this.runtimeStatus = Objects.requireNonNull(runtimeStatus, "runtimeStatus");
    this.RTStatusEventPubl = Objects.requireNonNull(RTStatusEventPubl, "RTStatusEventPubl");
    this.startupManager = Objects.requireNonNull(startupManager, "startupManager");
    this.shutdownManager = Objects.requireNonNull(shutdownManager, "shutdownManager");
    this.operatingModelRegistry = Objects.requireNonNull(operatingModelRegistry, "operatingModelRegistry");
    this.requestLifecycleRegistry = Objects.requireNonNull(requestLifecycleRegistry, "requestLifecycleRegistry");
    this.runtimeSessionModelRegistry = Objects.requireNonNull(runtimeSessionModelRegistry, "runtimeSessionModelRegistry");
    this.serverLogger = Objects.requireNonNull(serverLogger, "serverLogger");
  }

  /**
   * STOPPED -> STARTING -> RUNNING
   * STARTING -> FAILED (exception)
   */
  public synchronized void McpRTOrchStartRT() {

    RTStatus.ServerState cur = runtimeStatus.RTStatusGet();

    if (cur == RTStatus.ServerState.RUNNING) {
      serverLogger.ServerLogInfoObserved(
          ServerLogger.Component.RUNTIME,
          null,
          "START",
          "RUNTIME_START_SKIPPED",
          "McpRTOrch: Start requested but already RUNNING (idempotent).");
      return;
    }

    if (cur == RTStatus.ServerState.STOPPED) {
      runtimeStatus.RTStatusMarkStarting();
      RTStatusEventPubl.publish(new RTStatusEventPayl(runtimeStatus.RTStatusGet()));
      cur = runtimeStatus.RTStatusGet();
    }

    if (cur != RTStatus.ServerState.STARTING) {
      throw new IllegalStateException("McpRTOrchStartRT illegal when state=" + cur);
    }

    serverLogger.ServerLogInfoObserved(
        ServerLogger.Component.RUNTIME,
        null,
        "START",
        "RUNTIME_STARTING",
        "McpRTOrch: Starting runtime");

    serverLogger.ServerLogInfoObserved(
        ServerLogger.Component.RUNTIME,
        null,
        "START",
        "OPERATING_MODEL_READY",
        "McpRTOrch: Operating model loaded: "
            + operatingModelRegistry.OperatingModelRegDescribe());

    serverLogger.ServerLogInfoObserved(
        ServerLogger.Component.RUNTIME,
        null,
        "START",
        "REQUEST_LIFECYCLE_MODEL_READY",
        "McpRTOrch: Request lifecycle model loaded: "
            + requestLifecycleRegistry.ReqsLifecyRegDescribe());

    serverLogger.ServerLogInfoObserved(
        ServerLogger.Component.RUNTIME,
        null,
        "START",
        "RUNTIME_SESSION_MODEL_READY",
        "McpRTOrch: Runtime session model loaded: "
            + runtimeSessionModelRegistry.RTMcpSessModelRegDescribe());

    try {

      boolean ok = startupManager.StartupManStart();

      if (!ok) {
        runtimeStatus.RTStatusMarkFailed();
        RTStatusEventPubl.publish(new RTStatusEventPayl(runtimeStatus.RTStatusGet()));
        throw new IllegalStateException("StartupMan reported ok=false (unexpected)");
      }

      runtimeStatus.RTStatusMarkRunning();
      RTStatusEventPubl.publish(new RTStatusEventPayl(runtimeStatus.RTStatusGet()));

      serverLogger.ServerLogInfoObserved(
          ServerLogger.Component.RUNTIME,
          null,
          "START",
          "RUNTIME_RUNNING",
          "McpRTOrch: Runtime now RUNNING");

    } catch (RuntimeException ex) {

      runtimeStatus.RTStatusMarkFailed();
      RTStatusEventPubl.publish(new RTStatusEventPayl(runtimeStatus.RTStatusGet()));

      serverLogger.ServerLogErrorObserved(
          ServerLogger.Component.RUNTIME,
          null,
          "ERROR",
          "RUNTIME_START_FAILED",
          "McpRTOrch: runtime failed during start: " + ex.getMessage(),
          "STARTUP_FAILURE",
          ex);

      throw ex;
    }
  }

  /**
   * RUNNING -> STOPPING -> STOPPED
   */
  public synchronized void McpRTOrchStopRT() {

    RTStatus.ServerState cur = runtimeStatus.RTStatusGet();

    if (cur == RTStatus.ServerState.STOPPED) {
      serverLogger.ServerLogInfoObserved(
          ServerLogger.Component.RUNTIME,
          null,
          "STOP",
          "RUNTIME_STOP_SKIPPED",
          "McpRTOrch: Stop requested but already STOPPED (idempotent).");
      return;
    }

    if (cur != RTStatus.ServerState.RUNNING) {
      throw new IllegalStateException("McpRTOrchStopRT illegal when state=" + cur);
    }

    runtimeStatus.RTStatusMarkStopping();
    RTStatusEventPubl.publish(new RTStatusEventPayl(runtimeStatus.RTStatusGet()));

    serverLogger.ServerLogInfoObserved(
        ServerLogger.Component.RUNTIME,
        null,
        "STOP",
        "RUNTIME_STOPPING",
        "McpRTOrch: stopping runtime");

    try {

      shutdownManager.ShutdownManStop();

      runtimeStatus.RTStatusMarkStopped();
      RTStatusEventPubl.publish(new RTStatusEventPayl(runtimeStatus.RTStatusGet()));

      serverLogger.ServerLogInfoObserved(
          ServerLogger.Component.RUNTIME,
          null,
          "STOP",
          "RUNTIME_STOPPED",
          "McpRTOrch: runtime STOPPED");

    } catch (RuntimeException ex) {

      runtimeStatus.RTStatusMarkFailed();
      RTStatusEventPubl.publish(new RTStatusEventPayl(runtimeStatus.RTStatusGet()));

      serverLogger.ServerLogErrorObserved(
          ServerLogger.Component.RUNTIME,
          null,
          "ERROR",
          "RUNTIME_STOP_FAILED",
          "McpRTOrch: runtime failed during stop: " + ex.getMessage(),
          "SHUTDOWN_FAILURE",
          ex);

      throw ex;
    }
  }
}
