package mcp.server.foundation.transport;

import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Function;

/**
 * Shared dual-index registry support for transport bindings.
 */
public final class TranspSessBindRegSupport<CONNECTION_ID, SESSION> {

  private final ConcurrentHashMap<CONNECTION_ID, SESSION> sessionsByConnectionId = new ConcurrentHashMap<>();
  private final ConcurrentHashMap<String, CONNECTION_ID> connectionIdsByLogicalSessionId = new ConcurrentHashMap<>();
  private final Object lock = new Object();

  private final Function<SESSION, CONNECTION_ID> connectionIdExtractor;
  private final Function<SESSION, String> logicalSessionIdExtractor;

  public TranspSessBindRegSupport(
      Function<SESSION, CONNECTION_ID> connectionIdExtractor,
      Function<SESSION, String> logicalSessionIdExtractor) {

    this.connectionIdExtractor = Objects.requireNonNull(connectionIdExtractor, "connectionIdExtractor");
    this.logicalSessionIdExtractor = Objects.requireNonNull(logicalSessionIdExtractor, "logicalSessionIdExtractor");
  }

  public void register(SESSION session) {

    Objects.requireNonNull(session, "session");

    synchronized (lock) {
      CONNECTION_ID connectionId = Objects.requireNonNull(connectionIdExtractor.apply(session), "connectionId");
      String logicalSessionId = requireLogicalSessId(logicalSessionIdExtractor.apply(session));

      CONNECTION_ID previousConnectionId = connectionIdsByLogicalSessionId.put(logicalSessionId, connectionId);
      if (previousConnectionId != null && !previousConnectionId.equals(connectionId)) {
        sessionsByConnectionId.remove(previousConnectionId);
      }

      SESSION previousSession = sessionsByConnectionId.put(connectionId, session);
      if (previousSession != null) {
        String previousLogicalSessionId = logicalSessionIdExtractor.apply(previousSession);
        if (previousLogicalSessionId != null) {
          connectionIdsByLogicalSessionId.remove(previousLogicalSessionId.trim(), connectionId);
        }
      }
    }
  }

  public SESSION getByConnId(CONNECTION_ID connectionId) {
    Objects.requireNonNull(connectionId, "connectionId");
    return sessionsByConnectionId.get(connectionId);
  }

  public SESSION getByLogicalSessId(String logicalSessionId) {

    String normalized = normalizeLogicalSessId(logicalSessionId);
    if (normalized == null) {
      return null;
    }

    CONNECTION_ID connectionId = connectionIdsByLogicalSessionId.get(normalized);
    return connectionId == null ? null : sessionsByConnectionId.get(connectionId);
  }

  public SESSION unregisterByConnId(CONNECTION_ID connectionId) {

    Objects.requireNonNull(connectionId, "connectionId");

    synchronized (lock) {
      SESSION removed = sessionsByConnectionId.remove(connectionId);
      if (removed != null) {
        connectionIdsByLogicalSessionId.remove(
            requireLogicalSessId(logicalSessionIdExtractor.apply(removed)),
            connectionId);
      }
      return removed;
    }
  }

  public SESSION unregisterByLogicalSessId(String logicalSessionId) {

    String normalized = requireLogicalSessId(logicalSessionId);

    synchronized (lock) {
      CONNECTION_ID connectionId = connectionIdsByLogicalSessionId.remove(normalized);
      if (connectionId == null) {
        return null;
      }
      return sessionsByConnectionId.remove(connectionId);
    }
  }

  public boolean hasConn(CONNECTION_ID connectionId) {
    Objects.requireNonNull(connectionId, "connectionId");
    return sessionsByConnectionId.containsKey(connectionId);
  }

  public boolean hasLogicalSess(String logicalSessionId) {
    return normalizeLogicalSessId(logicalSessionId) != null
        && connectionIdsByLogicalSessionId.containsKey(logicalSessionId.trim());
  }

  public int getActiveBindingCount() {
    return sessionsByConnectionId.size();
  }

  public Map<CONNECTION_ID, String> getConnToLogicalSessSnapshot() {

    Map<CONNECTION_ID, String> snapshot = new ConcurrentHashMap<>();

    for (Map.Entry<CONNECTION_ID, SESSION> entry : sessionsByConnectionId.entrySet()) {
      snapshot.put(entry.getKey(), requireLogicalSessId(logicalSessionIdExtractor.apply(entry.getValue())));
    }

    return Map.copyOf(snapshot);
  }

  public Set<CONNECTION_ID> getConnIdsSnapshot() {
    return Set.copyOf(sessionsByConnectionId.keySet());
  }

  public void clearAll() {
    synchronized (lock) {
      sessionsByConnectionId.clear();
      connectionIdsByLogicalSessionId.clear();
    }
  }

  private static String requireLogicalSessId(String logicalSessionId) {
    String normalized = normalizeLogicalSessId(logicalSessionId);
    if (normalized == null) {
      throw new IllegalArgumentException("logicalSessionId must not be null or blank");
    }
    return normalized;
  }

  private static String normalizeLogicalSessId(String logicalSessionId) {
    if (logicalSessionId == null) {
      return null;
    }

    String normalized = logicalSessionId.trim();
    return normalized.isEmpty() ? null : normalized;
  }
}
