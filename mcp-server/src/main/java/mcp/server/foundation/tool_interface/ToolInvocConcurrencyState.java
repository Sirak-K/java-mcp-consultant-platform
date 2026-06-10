package mcp.server.foundation.tool_interface;

import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Future;
import java.util.concurrent.Semaphore;

final class ToolInvocConcurrencyState {

  private final Semaphore globalPostInitConcurrency;
  private final int globalMaxConcurrency;
  private final Map<String, Semaphore> perToolConcurrency = new ConcurrentHashMap<>();
  private final Map<String, ToolInvocRunningState> runningInvocations = new ConcurrentHashMap<>();

  ToolInvocConcurrencyState(int globalMaxConcurrency) {
    this.globalPostInitConcurrency = new Semaphore(globalMaxConcurrency, true);
    this.globalMaxConcurrency = globalMaxConcurrency;
  }

  boolean tryAcquireGlobal() {
    return globalPostInitConcurrency.tryAcquire();
  }

  void releaseGlobal() {
    globalPostInitConcurrency.release();
  }

  Semaphore toolSemaphore(ToolDefinition definition) {
    return perToolConcurrency.computeIfAbsent(
        definition.ToolDefGetName(),
        ignored -> new Semaphore(definition.ToolDefGetExecPolicy().maxConcurrency(), true));
  }

  void registerRunning(ToolInvocRunningState runningInvocation) {
    runningInvocations.put(runningInvocation.requestId(), runningInvocation);
  }

  void registerFuture(String requestId, Future<ToolResponse> future) {
    runningInvocations.computeIfPresent(
        requestId,
        (ignored, runningInvocation) -> runningInvocation.withFuture(future));
  }

  void removeRunning(String requestId) {
    runningInvocations.remove(requestId);
  }

  ToolInvocRunningState runningInvocation(String requestId) {
    return runningInvocations.get(requestId);
  }

  List<ToolInvocEngine.ActiveInvocationSnapshot> runningInvocationsSnapshot() {
    return runningInvocations.values().stream()
        .map(invocation -> new ToolInvocEngine.ActiveInvocationSnapshot(
            invocation.requestId(),
            invocation.toolName(),
            invocation.policy().timeoutMillis(),
            invocation.policy().cancellable(),
            invocation.policy().progressEnabled()))
        .sorted(java.util.Comparator
            .comparing(ToolInvocEngine.ActiveInvocationSnapshot::toolName)
            .thenComparing(ToolInvocEngine.ActiveInvocationSnapshot::requestId))
        .toList();
  }

  ToolInvocEngine.ConcurrSnapshot concurrencySnapshot(List<ToolDefinition> definitions) {
    Map<String, Long> activeExecutionsByTool = runningInvocations.values().stream()
        .collect(java.util.stream.Collectors.groupingBy(
            ToolInvocRunningState::toolName,
            java.util.stream.Collectors.counting()));

    List<ToolInvocEngine.ToolConcurrSnapshot> tools = definitions.stream()
        .map(definition -> {
          ToolExecPolicy policy = definition.ToolDefGetExecPolicy();
          return new ToolInvocEngine.ToolConcurrSnapshot(
              definition.ToolDefGetName(),
              policy.timeoutMillis(),
              policy.maxConcurrency(),
              policy.cancellable(),
              policy.progressEnabled(),
              activeExecutionsByTool.getOrDefault(definition.ToolDefGetName(), 0L).intValue());
        })
        .sorted(java.util.Comparator.comparing(ToolInvocEngine.ToolConcurrSnapshot::toolName))
        .toList();

    return new ToolInvocEngine.ConcurrSnapshot(
        globalMaxConcurrency,
        globalPostInitConcurrency.availablePermits(),
        tools);
  }

  int globalMaxConcurrency() {
    return globalMaxConcurrency;
  }

  int globalAvailablePermits() {
    return globalPostInitConcurrency.availablePermits();
  }
}
