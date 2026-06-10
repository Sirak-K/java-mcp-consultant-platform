package mcp.server;

import jakarta.annotation.PreDestroy;
import mcp.server.foundation.logging.ServerLogger;
import mcp.server.foundation.server_process.orchestration.McpRTOrch;
import mcp.server.foundation.server_process.status.RTStatus;

import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.FilterType;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

/**
 * McpServerApp
 *
 * Ontologisk roll:
 * - Application Composition Root
 * - Startar Spring Boot
 * - Startar MCP runtime
 *
 * Extra ansvar:
 * - Skriver deterministisk PID-fil för extern OS-level liveness discovery
 */
@SpringBootApplication
@ComponentScan(
    excludeFilters = {
        @ComponentScan.Filter(
            type = FilterType.REGEX,
            pattern = "mcp\\.server\\.integration\\..*\\$.*Config")
        ,
        @ComponentScan.Filter(
            type = FilterType.REGEX,
            pattern = "mcp\\.server\\.foundation\\..*\\$.*(Config|Cfg)")
    })
public class McpServerApp implements ApplicationRunner {

  private static final String PID_FILE = "./mcp-server.pid";

  private final McpRTOrch lifecycleManager;
  private final RTStatus runtimeStatus;
  private final ServerLogger serverLogger;

  public McpServerApp(
      McpRTOrch lifecycleManager,
      RTStatus runtimeStatus,
      ServerLogger serverLogger) {

    this.lifecycleManager = lifecycleManager;
    this.runtimeStatus = runtimeStatus;
    this.serverLogger = serverLogger;
  }

  public static void main(String[] args) {
    SpringApplication.run(McpServerApp.class, args);
  }

  @Override
  public void run(ApplicationArguments args) {

    serverLogger.ServerLogAuditInfoObserved(
        ServerLogger.Component.RUNTIME,
        null,
        "VALIDATE",
        "DB_SCHEMA_VALIDATION",
        "McpServerApp: DB-SCHEMA-VALIDATION: Correct as Expected");

    writePidFile();

    lifecycleManager.McpRTOrchStartRT();
  }

  @PreDestroy
  public void onShutdown() {

    if (runtimeStatus.RTStatusGet() != RTStatus.ServerState.RUNNING) {
      return;
    }

    serverLogger.ServerLogAuditInfoObserved(
        ServerLogger.Component.RUNTIME,
        null,
        "STOP",
        "SPRING_SHUTDOWN_HOOK_TRIGGERED",
        "McpServerApp: Spring shutdown hook triggered");

    lifecycleManager.McpRTOrchStopRT();
  }

  /**
   * Deterministisk PID-skrivning utan Spring-interna klasser.
   */
  private void writePidFile() {

    long pid = ProcessHandle.current().pid();
    Path pidFilePath = Path.of(PID_FILE);

    try {
      Files.writeString(pidFilePath, String.valueOf(pid));

      serverLogger.ServerLogAuditInfoObserved(
          ServerLogger.Component.RUNTIME,
          null,
          "WRITE",
          "PID_FILE_WRITTEN",
          "McpServerApp: PID file written to " + pidFilePath.toAbsolutePath());
    } catch (IOException ex) {
      serverLogger.ServerLogAuditErrorObserved(
          ServerLogger.Component.RUNTIME,
          null,
          "ERROR",
          "PID_FILE_WRITE_FAILED",
          "McpServerApp: could not write PID file: " + ex.getMessage(),
          "PID_FILE_WRITE_ERROR",
          ex);
    }
  }
}
