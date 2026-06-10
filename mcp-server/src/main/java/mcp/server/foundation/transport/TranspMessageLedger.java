package mcp.server.foundation.transport;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Objects;

/**
 * Small bounded ledger for message accounting across transport flows.
 */
public final class TranspMessageLedger {

  private static final int DEFAULT_MAX_TRACKED_MESSAGES = 256;

  public enum MessageState {
    RECEIVED(false),
    ROUTED(false),
    RESPONDED(true),
    REJECTED(true),
    TIMED_OUT(true),
    ABORTED(true);

    private final boolean terminal;

    MessageState(boolean terminal) {
      this.terminal = terminal;
    }

    public boolean MsgStateIsTerminal() {
      return terminal;
    }
  }

  public record Snapshot(
      long receivedCount,
      long routedCount,
      long respondedCount,
      long rejectedCount,
      long timedOutCount,
      long abortedCount,
      String lastCorrelationId,
      Map<String, MessageState> recentStates) {
  }

  private final Object lock = new Object();
  private final int maxTrackedMessages;
  private final LinkedHashMap<String, MessageState> recentStates;

  private long receivedCount;
  private long routedCount;
  private long respondedCount;
  private long rejectedCount;
  private long timedOutCount;
  private long abortedCount;
  private String lastCorrelationId;

  public TranspMessageLedger() {
    this(DEFAULT_MAX_TRACKED_MESSAGES);
  }

  public TranspMessageLedger(int maxTrackedMessages) {

    if (maxTrackedMessages <= 0) {
      throw new IllegalArgumentException("maxTrackedMessages must be > 0");
    }

    this.maxTrackedMessages = maxTrackedMessages;
    this.recentStates = new LinkedHashMap<>(16, 0.75f, true) {
      @Override
      protected boolean removeEldestEntry(Map.Entry<String, MessageState> eldest) {
        return size() > TranspMessageLedger.this.maxTrackedMessages;
      }
    };
  }

  public void MsgLedgerRecordReceived(String correlationId) {
    MsgLedgerRecordState(correlationId, MessageState.RECEIVED);
  }

  public void MsgLedgerRecordRouted(String correlationId) {
    MsgLedgerRecordState(correlationId, MessageState.ROUTED);
  }

  public void MsgLedgerRecordResponded(String correlationId) {
    MsgLedgerRecordState(correlationId, MessageState.RESPONDED);
  }

  public void MsgLedgerRecordRejected(String correlationId) {
    MsgLedgerRecordState(correlationId, MessageState.REJECTED);
  }

  public void MsgLedgerRecordTimedOut(String correlationId) {
    MsgLedgerRecordState(correlationId, MessageState.TIMED_OUT);
  }

  public void MsgLedgerRecordAborted(String correlationId) {
    MsgLedgerRecordState(correlationId, MessageState.ABORTED);
  }

  public void MsgLedgerAbortOpenMessages() {

    synchronized (lock) {
      for (Map.Entry<String, MessageState> entry : recentStates.entrySet()) {
        if (!entry.getValue().MsgStateIsTerminal()) {
          entry.setValue(MessageState.ABORTED);
          abortedCount++;
          lastCorrelationId = entry.getKey();
        }
      }
    }
  }

  public MessageState MsgLedgerGetState(String correlationId) {

    String normalized = normalizeCorrelaId(correlationId);
    if (normalized == null) {
      return null;
    }

    synchronized (lock) {
      return recentStates.get(normalized);
    }
  }

  public Snapshot MsgLedgerSnapshot() {
    synchronized (lock) {
      return new Snapshot(
          receivedCount,
          routedCount,
          respondedCount,
          rejectedCount,
          timedOutCount,
          abortedCount,
          lastCorrelationId,
          Map.copyOf(recentStates));
    }
  }

  private void MsgLedgerRecordState(String correlationId, MessageState targetState) {

    Objects.requireNonNull(targetState, "targetState");

    String normalized = normalizeCorrelaId(correlationId);
    if (normalized == null) {
      return;
    }

    synchronized (lock) {
      MessageState currentState = recentStates.get(normalized);

      if (currentState == targetState) {
        lastCorrelationId = normalized;
        return;
      }

      if (currentState != null && currentState.MsgStateIsTerminal()) {
        lastCorrelationId = normalized;
        return;
      }

      recentStates.put(normalized, targetState);
      lastCorrelationId = normalized;
      incrementCounter(targetState);
    }
  }

  private void incrementCounter(MessageState targetState) {
    switch (targetState) {
      case RECEIVED -> receivedCount++;
      case ROUTED -> routedCount++;
      case RESPONDED -> respondedCount++;
      case REJECTED -> rejectedCount++;
      case TIMED_OUT -> timedOutCount++;
      case ABORTED -> abortedCount++;
    }
  }

  private static String normalizeCorrelaId(String correlationId) {

    if (correlationId == null) {
      return null;
    }

    String normalized = correlationId.trim();
    return normalized.isEmpty() ? null : normalized;
  }
}
