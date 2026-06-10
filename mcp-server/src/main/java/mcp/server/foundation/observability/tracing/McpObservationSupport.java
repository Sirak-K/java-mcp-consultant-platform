package mcp.server.foundation.observability.tracing;

import io.micrometer.common.KeyValue;
import io.micrometer.observation.Observation;
import io.micrometer.observation.ObservationRegistry;
import mcp.server.foundation.observability.context.McpCorrelaFieldCatal;
import mcp.server.foundation.observability.context.ObservCtx;

import java.util.Objects;

/**
 * Creates standard observations around MCP-specific flows while preserving the
 * existing internal observation context model.
 */
public final class McpObservationSupport {

  private final ObservationRegistry observationRegistry;

  public McpObservationSupport(ObservationRegistry observationRegistry) {
    this.observationRegistry = Objects.requireNonNull(observationRegistry, "observationRegistry");
  }

  public Observation McpObsStartRPCObservation(ObservCtx context, String rpcMet) {
    return McpObsStartObservation(
        McpObservationCatal.MCP_RPC_ROUTE,
        "mcp rpc " + McpObsNormalizeName(rpcMet, "unknown"),
        context,
        rpcMet,
        null,
        null,
        null);
  }

  public Observation McpObsStartToolObservation(ObservCtx context, String toolName) {
    return McpObsStartObservation(
        McpObservationCatal.MCP_TOOL_INVOKE,
        "mcp tool " + McpObsNormalizeName(toolName, "unknown"),
        context,
        null,
        toolName,
        null,
        null);
  }

  public Observation McpObsStartPersistenceObservation(
      ObservCtx context,
      String repository,
      String operation) {
    return McpObsStartObservation(
        McpObservationCatal.MCP_PERSISTENCE_CALL,
        "mcp persistence " + McpObsNormalizeName(repository, "unknown"),
        context,
        null,
        null,
        repository,
        operation);
  }

  public void McpObsMarkErr(Observation observation, Throwable throwable, String errorType) {

    if (observation == null) {
      return;
    }

    if (errorType != null && !errorType.isBlank()) {
      observation.lowCardinalityKeyValue(KeyValue.of(McpObservationCatal.FIELD_ERROR_TYPE, errorType));
    }

    if (throwable != null) {
      observation.error(throwable);
    }
  }

  private Observation McpObsStartObservation(
      String name,
      String contextualName,
      ObservCtx context,
      String rpcMet,
      String toolName,
      String repository,
      String operation) {

    Observation observation = Observation.createNotStarted(name, observationRegistry)
        .contextualName(contextualName);

    if (context != null) {
      McpObsLowCardinality(observation, McpObservationCatal.FIELD_TRANSPORT, context.ObservCtxGetTranspName());
      McpObsLowCardinality(observation, McpObservationCatal.FIELD_SESSION_PHASE, context.ObservCtxGetSessPhase());
      McpObsLowCardinality(observation, McpObservationCatal.FIELD_ERROR_TYPE, context.ObservCtxGetErrType());
      McpObsHighCardinality(observation, McpCorrelaFieldCatal.MCP_SESSION_ID, context.ObservCtxGetMcpSessId());
      McpObsHighCardinality(observation, McpCorrelaFieldCatal.MCP_WS_CONNECTION_ID, context.ObservCtxGetWsConnId());
      McpObsHighCardinality(observation, McpCorrelaFieldCatal.MCP_RPC_CORRELATION_ID, context.ObservCtxGetRPCCorrelaId());
      McpObsHighCardinality(observation, McpCorrelaFieldCatal.CLIENT_ADDRESS, context.ObservCtxGetClientAddress());
    }

    McpObsLowCardinality(observation, McpObservationCatal.FIELD_RPC_METHOD, rpcMet);
    McpObsLowCardinality(observation, McpObservationCatal.FIELD_TOOL_NAME, toolName);
    McpObsLowCardinality(observation, McpObservationCatal.FIELD_PERSISTENCE_REPOSITORY, repository);
    McpObsLowCardinality(observation, McpObservationCatal.FIELD_PERSISTENCE_OPERATION, operation);

    return observation.start();
  }

  private static void McpObsLowCardinality(Observation observation, String key, String value) {
    if (value != null && !value.isBlank()) {
      observation.lowCardinalityKeyValue(KeyValue.of(key, value));
    }
  }

  private static void McpObsHighCardinality(Observation observation, String key, String value) {
    if (value != null && !value.isBlank()) {
      observation.highCardinalityKeyValue(KeyValue.of(key, value));
    }
  }

  private static String McpObsNormalizeName(String rawValue, String fallback) {
    if (rawValue == null || rawValue.isBlank()) {
      return fallback;
    }

    return rawValue.trim().replace(' ', '_');
  }
}
