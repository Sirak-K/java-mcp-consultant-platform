package mcp.server.foundation.logging;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Comparator;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;

import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import mcp.server.foundation.observability.context.ObservCtxFactory;
import mcp.server.foundation.security.request_binding.ReqsAuthBinding;

@Service
public final class ReactAppLogService {

  private static final DateTimeFormatter TIMESTAMP_FORMAT = DateTimeFormatter.ofPattern("yyyy-MM-dd | HH:mm:ss");
  private static final int MAX_FIELD_LENGTH = 180;
  private static final int MAX_DETAILS = 20;

  private final Path reactAppLogPath;
  private final ServerLogger serverLogger;
  private final ObservCtxFactory obsCtxFactory;

  public ReactAppLogService(
      CanonicalLogPaths canonicalLogPaths,
      ServerLogger serverLogger,
      ObservCtxFactory obsCtxFactory) {

    this.reactAppLogPath = Objects.requireNonNull(canonicalLogPaths, "canonicalLogPaths")
        .CanonicalLogPathsResolve("logs/react-app.log", "react-app.log");
    this.serverLogger = Objects.requireNonNull(serverLogger, "serverLogger");
    this.obsCtxFactory = Objects.requireNonNull(obsCtxFactory, "obsCtxFactory");
  }

  public void ReactAppLogRecord(
      ReactAppLogEntry entry,
      ReqsAuthBinding requestAuthBinding) {

    Objects.requireNonNull(entry, "entry");

    String line = String.join(
        " | ",
        TIMESTAMP_FORMAT.format(LocalDateTime.now()),
        "event=" + clean(entry.eventName()),
        "route=" + clean(entry.route()),
        "actor=" + clean(actorId(requestAuthBinding)),
        "details=" + details(entry.details()));

    try {
      Path parent = reactAppLogPath.getParent();
      if (parent != null) {
        Files.createDirectories(parent);
      }
      Files.writeString(
          reactAppLogPath,
          line + System.lineSeparator(),
          StandardCharsets.UTF_8,
          StandardOpenOption.CREATE,
          StandardOpenOption.APPEND);
    } catch (IOException exception) {
      serverLogger.ServerLogErrorObserved(
          ServerLogger.Component.RUNTIME,
          obsCtxFactory.ObservCtxFactoryCurrentOrEmpty(),
          "WRITE",
          "REACT_APP_LOG_WRITE_FAILED",
          "React app log write failed.",
          "REACT_APP_LOG_WRITE_FAILED",
          exception);
      throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, "React app log write failed", exception);
    }
  }

  private static String actorId(ReqsAuthBinding requestAuthBinding) {
    if (requestAuthBinding == null || requestAuthBinding.principalId() == null) {
      return "unknown";
    }
    return requestAuthBinding.principalId();
  }

  private static String details(Map<String, Object> details) {
    if (details == null || details.isEmpty()) {
      return "-";
    }

    return details.entrySet().stream()
        .sorted(Comparator.comparing(Map.Entry::getKey))
        .limit(MAX_DETAILS)
        .map(entry -> clean(entry.getKey()) + "=" + clean(String.valueOf(entry.getValue())))
        .collect(Collectors.joining(","));
  }

  private static String clean(String value) {
    String normalized = value == null ? "" : value
        .replace('\r', ' ')
        .replace('\n', ' ')
        .replace('|', '/')
        .trim();
    if (normalized.isBlank()) {
      return "-";
    }
    return normalized.length() <= MAX_FIELD_LENGTH
        ? normalized
        : normalized.substring(0, MAX_FIELD_LENGTH) + "...";
  }
}
