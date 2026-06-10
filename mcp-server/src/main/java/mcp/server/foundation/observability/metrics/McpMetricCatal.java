package mcp.server.foundation.observability.metrics;

public final class McpMetricCatal {

    public static final String MCP_RPC_REQUESTS_TOTAL = "mcp.rpc.requests.total";
    public static final String MCP_RPC_DURATION = "mcp.rpc.duration";
    public static final String MCP_RUNTIME_READINESS_STATE = "mcp.runtime.readiness.state";
    public static final String MCP_RUNTIME_LIVENESS_STATE = "mcp.runtime.liveness.state";
    public static final String MCP_SESSIONS_LOGICAL_ACTIVE = "mcp.sessions.logical.active";
    public static final String MCP_BINDINGS_ACTIVE = "mcp.bindings.active";
    public static final String MCP_SENTINEL_SUBSCRIBERS_ACTIVE = "mcp.sentinel.subscribers.active";
    public static final String MCP_SESSION_ACTIVE = "mcp.session.active";
    public static final String MCP_SESSION_REQUESTS_TOTAL = "mcp.session.requests.total";
    public static final String MCP_SESSION_RESPONSES_TOTAL = "mcp.session.responses.total";
    public static final String MCP_SESSION_TOOL_INVOCATIONS_SUCCESS_TOTAL = "mcp.session.tool.invocations.success.total";
    public static final String MCP_SESSION_CREATED_TIMESTAMP_SECONDS = "mcp.session.created.timestamp.seconds";
    public static final String MCP_SESSION_LAST_ACTIVITY_TIMESTAMP_SECONDS = "mcp.session.last.activity.timestamp.seconds";
    public static final String MCP_SESSION_CURRENT_PHASE_STARTED_TIMESTAMP_SECONDS = "mcp.session.current.phase.started.timestamp.seconds";
    public static final String MCP_SESSION_PHASE_DURATION_SECONDS_TOTAL = "mcp.session.phase.duration.seconds.total";
    public static final String MCP_SESSION_CLOSES_TOTAL = "mcp.session.closes.total";
    public static final String MCP_SERVER_PROCESS_CREATED_TIMESTAMP_SECONDS = "mcp.server.process.created.timestamp.seconds";
    public static final String MCP_SERVER_PROCESS_LAST_STOPPED_TIMESTAMP_SECONDS = "mcp.server.process.last.stopped.timestamp.seconds";
    public static final String MCP_SERVER_LAST_SESSION_CREATED_TIMESTAMP_SECONDS = "mcp.server.last.session.created.timestamp.seconds";
    public static final String MCP_SERVER_LAST_SESSION_CLOSED_TIMESTAMP_SECONDS = "mcp.server.last.session.closed.timestamp.seconds";

    public static final String MCP_TOOL_INVOCATIONS_TOTAL = "mcp.tool.invocations.total";
    public static final String MCP_TOOL_DURATION = "mcp.tool.duration";
    public static final String MCP_TOOL_QUEUE_WAIT_DURATION = "mcp.tool.queue.wait.duration";
    public static final String MCP_TOOL_REJECTIONS_TOTAL = "mcp.tool.rejections.total";
    public static final String MCP_TOOL_CANCELLATIONS_TOTAL = "mcp.tool.cancellations.total";
    public static final String MCP_TOOL_TIMEOUTS_TOTAL = "mcp.tool.timeouts.total";
    public static final String MCP_TOOL_EXECUTIONS_ACTIVE = "mcp.tool.executions.active";
    public static final String MCP_TOOL_GLOBAL_CONCURRENCY_ACTIVE = "mcp.tool.global.concurrency.active";
    public static final String MCP_TOOL_GLOBAL_CONCURRENCY_LIMIT = "mcp.tool.global.concurrency.limit";
    public static final String MCP_TOOL_GLOBAL_CONCURRENCY_UTILIZATION = "mcp.tool.global.concurrency.utilization";
    public static final String MCP_TOOL_CONCURRENCY_ACTIVE = "mcp.tool.concurrency.active";
    public static final String MCP_TOOL_CONCURRENCY_LIMIT = "mcp.tool.concurrency.limit";
    public static final String MCP_TOOL_CONCURRENCY_UTILIZATION = "mcp.tool.concurrency.utilization";

    public static final String MCP_TRANSPORT_ERRORS_TOTAL = "mcp.transport.errors.total";
    public static final String MCP_AUTH_DENIALS_TOTAL = "mcp.auth.denials.total";
    public static final String MCP_SECURITY_DENIALS_TOTAL = "mcp.security.denials.total";

    public static final String MCP_PERSISTENCE_CALLS_TOTAL = "mcp.persistence.calls.total";
    public static final String MCP_PERSISTENCE_DURATION = "mcp.persistence.duration";
    public static final String MCP_RUNTIME_PERSISTENCE_CALLS_TOTAL = "runtime.persistence.calls.total";
    public static final String MCP_RUNTIME_PERSISTENCE_FAILURES_TOTAL = "runtime.persistence.failures.total";
    public static final String MCP_RUNTIME_PERSISTENCE_DURATION = "runtime.persistence.duration";
    public static final String MCP_RUNTIME_PERSISTENCE_OPERATION_DURATION_PREFIX = "runtime.persistence.";
    public static final String MCP_RUNTIME_PERSISTENCE_OPERATION_DURATION_SUFFIX = ".duration";

    private McpMetricCatal() {
    }
}
