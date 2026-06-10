package mcp.server.foundation.spring_integration;

import mcp.server.foundation.logging.AuditStructuredLogSink;
import mcp.server.foundation.logging.CanonicalLogPaths;
import mcp.server.foundation.logging.FileStructuredLogSink;
import mcp.server.foundation.logging.HumanReadableConsoleLogSink;
import mcp.server.foundation.logging.LogRotationPolicy;
import mcp.server.foundation.logging.OtlpStructuredLogSink;
import mcp.server.foundation.logging.RedactedStructuredLogJsonFormatter;
import mcp.server.foundation.logging.ServerLogger;
import mcp.server.foundation.logging.StructuredLogFormatter;
import mcp.server.foundation.logging.StructuredLogJsonCodec;
import mcp.server.foundation.logging.StructuredLogRedactor;
import mcp.server.foundation.logging.StructuredLogSink;
import mcp.server.foundation.logging.StructuredLogger;
import mcp.server.foundation.logging.StructuredLogTimestampFormatter;
import mcp.server.foundation.logging.TestStructuredLogSink;

import io.opentelemetry.sdk.logs.SdkLoggerProvider;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;

import java.io.PrintStream;
import java.util.List;

@Configuration
public class SpringLoggingCfg {

  @Bean
  public StructuredLogJsonCodec structuredLogJsonCodec(
      StructuredLogTimestampFormatter structuredLogTimestampFormatter) {
    return new StructuredLogJsonCodec(structuredLogTimestampFormatter);
  }

  @Bean
  @Profile("dev")
  public StructuredLogSink consoleStructuredLogSink(
      StructuredLogTimestampFormatter structuredLogTimestampFormatter,
      StructuredLogRedactor structuredLogRedactor,
      @Value("${mcp.transport.stdio.enabled:false}") boolean stdioEnabled,
      @Value("${mcp.transport.stdio.stderr-logging-enabled:true}") boolean stdioStderrLoggingEnabled) {
    return createConsoleStructuredLogSink(
        ServerLogger.Severity.INFO,
        structuredLogTimestampFormatter,
        structuredLogRedactor,
        stdioEnabled,
        stdioStderrLoggingEnabled,
        System.out,
        System.err);
  }

  @Bean
  @Profile("!dev")
  public StructuredLogSink prodConsoleStructuredLogSink(
      StructuredLogTimestampFormatter structuredLogTimestampFormatter,
      StructuredLogRedactor structuredLogRedactor,
      @Value("${mcp.transport.stdio.enabled:false}") boolean stdioEnabled,
      @Value("${mcp.transport.stdio.stderr-logging-enabled:true}") boolean stdioStderrLoggingEnabled) {
    return createConsoleStructuredLogSink(
        ServerLogger.Severity.WARN,
        structuredLogTimestampFormatter,
        structuredLogRedactor,
        stdioEnabled,
        stdioStderrLoggingEnabled,
        System.out,
        System.err);
  }

  @Bean
  public CanonicalLogPaths canonicalLogPaths() {
    return new CanonicalLogPaths();
  }

  @Bean
  public LogRotationPolicy localLogRotationPolicy(
      @Value("${mcp.observability.local-logs.rotation.max-file-size-bytes:1048576}") long maxFileSizeBytes,
      @Value("${mcp.observability.local-logs.rotation.max-history-files:5}") int maxHistoryFiles) {

    return new LogRotationPolicy(maxFileSizeBytes, maxHistoryFiles);
  }

  @Bean
  public StructuredLogTimestampFormatter structuredLogTimestampFormatter() {
    return new StructuredLogTimestampFormatter();
  }

  @Bean
  @ConditionalOnProperty(prefix = "mcp.observability.file-sink", name = "enabled", havingValue = "true")
  public StructuredLogSink fileStructuredLogSink(
      CanonicalLogPaths canonicalLogPaths,
      LogRotationPolicy localLogRotationPolicy,
      StructuredLogFormatter structuredLogFormatter,
      @Value("${mcp.observability.file-sink.all-path:logs/mcp-server.log}") String allPath,
      @Value("${mcp.observability.file-sink.error-path:logs/mcp-server-errors.log}") String errorPath,
      @Value("${mcp.observability.file-sink.reset-all-on-start:false}") boolean resetAllLogOnStart) {

    return new FileStructuredLogSink(
        canonicalLogPaths.CanonicalLogPathsResolve(allPath, "mcp-server.log"),
        canonicalLogPaths.CanonicalLogPathsResolve(errorPath, "mcp-server-errors.log"),
        localLogRotationPolicy,
        structuredLogFormatter,
        structuredLogFormatter,
        resetAllLogOnStart);
  }

  @Bean
  @ConditionalOnProperty(prefix = "mcp.observability.audit-sink", name = "enabled", havingValue = "true")
  public StructuredLogSink auditStructuredLogSink(
      CanonicalLogPaths canonicalLogPaths,
      LogRotationPolicy localLogRotationPolicy,
      StructuredLogFormatter structuredLogFormatter,
      @Value("${mcp.observability.audit-sink.path:logs/mcp-server-audit.log}") String auditPath) {

    return new AuditStructuredLogSink(
        canonicalLogPaths.CanonicalLogPathsResolve(auditPath, "mcp-server-audit.log"),
        localLogRotationPolicy,
        structuredLogFormatter);
  }

  @Bean
  @Profile("test")
  @ConditionalOnProperty(prefix = "mcp.observability.test-sink", name = "enabled", havingValue = "true")
  public StructuredLogSink testStructuredLogSink(
      CanonicalLogPaths canonicalLogPaths,
      LogRotationPolicy localLogRotationPolicy,
      StructuredLogFormatter structuredLogFormatter,
      @Value("${mcp.observability.test-sink.path:logs/mcp-server.log}") String testPath) {

    return new TestStructuredLogSink(
        canonicalLogPaths.CanonicalLogPathsResolve(testPath, "mcp-server.log"),
        localLogRotationPolicy,
        structuredLogFormatter);
  }

  @Bean
  @Profile("dev")
  public Object devStructuredFileSinkGuard(
      @Value("${mcp.observability.file-sink.enabled:true}") boolean fileSinkEnabled,
      @Value("${mcp.observability.audit-sink.enabled:true}") boolean auditSinkEnabled,
      @Value("${mcp.observability.file-sink.all-path:logs/mcp-server.log}") String allPath,
      @Value("${mcp.observability.file-sink.error-path:logs/mcp-server-errors.log}") String errorPath,
      @Value("${mcp.observability.audit-sink.path:logs/mcp-server-audit.log}") String auditPath) {

    if (!fileSinkEnabled) {
      throw new IllegalStateException("dev requires mcp.observability.file-sink.enabled=true");
    }

    if (!auditSinkEnabled) {
      throw new IllegalStateException("dev requires mcp.observability.audit-sink.enabled=true");
    }

    if (allPath == null || allPath.isBlank()) {
      throw new IllegalStateException("dev requires a non-blank all log path");
    }

    if (errorPath == null || errorPath.isBlank()) {
      throw new IllegalStateException("dev requires a non-blank error log path");
    }

    if (auditPath == null || auditPath.isBlank()) {
      throw new IllegalStateException("dev requires a non-blank audit log path");
    }

    return new Object();
  }

  @Bean
  @Profile("prod")
  public Object prodStructuredFileSinkGuard(
      @Value("${mcp.observability.file-sink.enabled:true}") boolean fileSinkEnabled,
      @Value("${mcp.observability.file-sink.all-path:logs/mcp-server.log}") String allPath,
      @Value("${mcp.observability.file-sink.error-path:logs/mcp-server-errors.log}") String errorPath) {

    if (!fileSinkEnabled) {
      throw new IllegalStateException("prod requires mcp.observability.file-sink.enabled=true");
    }

    if (allPath == null || allPath.isBlank()) {
      throw new IllegalStateException("prod requires a non-blank all log path");
    }

    if (errorPath == null || errorPath.isBlank()) {
      throw new IllegalStateException("prod requires a non-blank error log path");
    }

    return new Object();
  }

  @Bean
  @Profile("prod")
  public Object prodAuditStructuredSinkGuard(
      @Value("${mcp.observability.audit-sink.enabled:true}") boolean auditSinkEnabled,
      @Value("${mcp.observability.audit-sink.path:logs/mcp-server-audit.log}") String auditPath) {

    if (auditSinkEnabled && (auditPath == null || auditPath.isBlank())) {
      throw new IllegalStateException("prod requires a non-blank audit log path when audit sink is enabled");
    }

    return new Object();
  }

  @Bean
  @Profile("test")
  public Object testStructuredSinkGuard(
      @Value("${mcp.observability.file-sink.enabled:false}") boolean fileSinkEnabled,
      @Value("${mcp.observability.audit-sink.enabled:false}") boolean auditSinkEnabled,
      @Value("${mcp.observability.test-sink.enabled:true}") boolean testSinkEnabled,
      @Value("${mcp.observability.test-sink.path:logs/mcp-server.log}") String testPath) {

    if (fileSinkEnabled) {
      throw new IllegalStateException("test must keep mcp.observability.file-sink.enabled=false");
    }

    if (auditSinkEnabled) {
      throw new IllegalStateException("test must keep mcp.observability.audit-sink.enabled=false");
    }

    if (!testSinkEnabled) {
      throw new IllegalStateException("test requires mcp.observability.test-sink.enabled=true");
    }

    if (testPath == null || testPath.isBlank()) {
      throw new IllegalStateException("test requires a non-blank test log path");
    }

    return new Object();
  }

  @Bean
  public StructuredLogRedactor structuredLogRedactor() {
    return new StructuredLogRedactor();
  }

  @Bean
  public StructuredLogFormatter structuredLogFormatter(
      StructuredLogJsonCodec codec,
      StructuredLogRedactor redactor) {

    return new RedactedStructuredLogJsonFormatter(codec, redactor);
  }

  @Bean
  public StructuredLogger structuredLogger(List<StructuredLogSink> sinks) {
    return new StructuredLogger(sinks);
  }

  @Bean
  public ServerLogger serverLogger(StructuredLogger structuredLogger) {
    return new ServerLogger(structuredLogger);
  }

  @Bean
  @ConditionalOnProperty(prefix = "mcp.observability.otlp-log-sink", name = "enabled", havingValue = "true")
  public StructuredLogSink otlpStructuredLogSink(
      SdkLoggerProvider otlpSdkLoggerProvider,
      StructuredLogRedactor structuredLogRedactor,
      @Value("${spring.application.name:mcp-server}") String applicationName) {

    return new OtlpStructuredLogSink(
        otlpSdkLoggerProvider.get(applicationName + ".structured-logs"),
        structuredLogRedactor);
  }

  private StructuredLogSink createConsoleStructuredLogSink(
      ServerLogger.Severity minimumSeverity,
      StructuredLogTimestampFormatter structuredLogTimestampFormatter,
      StructuredLogRedactor structuredLogRedactor,
      boolean stdioEnabled,
      boolean stdioStderrLoggingEnabled,
      PrintStream standardStream,
      PrintStream errorStream) {

    if (stdioEnabled) {
      if (!stdioStderrLoggingEnabled) {
        return (event, throwable) -> {
          // STDIO reserves stdout for MCP frames, so console logging can be suppressed entirely.
        };
      }

      return new HumanReadableConsoleLogSink(
          minimumSeverity,
          structuredLogTimestampFormatter,
          structuredLogRedactor,
          errorStream,
          errorStream);
    }

    return new HumanReadableConsoleLogSink(
        minimumSeverity,
        structuredLogTimestampFormatter,
        structuredLogRedactor,
        standardStream,
        errorStream);
  }
}
