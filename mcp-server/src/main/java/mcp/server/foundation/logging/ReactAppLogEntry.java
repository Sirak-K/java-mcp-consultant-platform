package mcp.server.foundation.logging;

import java.util.Map;

public record ReactAppLogEntry(
    String eventName,
    String route,
    Map<String, Object> details) {
}
