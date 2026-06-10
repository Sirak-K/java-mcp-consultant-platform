package mcp.server.foundation.support;

import java.util.Map;

public final class ListLimitSupport {

  public static final int DEFAULT_LIMIT = 100;
  public static final int MAX_LIMIT = 500;

  private ListLimitSupport() {
  }

  public static int ListLimitResolveRequested(Integer requestedLimit) {

    if (requestedLimit == null || requestedLimit <= 0) {
      return DEFAULT_LIMIT;
    }

    return Math.min(requestedLimit, MAX_LIMIT);
  }

  public static int ListLimitCap(int limit) {

    if (limit <= 0) {
      return 1;
    }

    return Math.min(limit, MAX_LIMIT);
  }

  public static Map<String, Object> ListLimitSchema() {
    return Map.of(
        "type", "integer",
        "minimum", 1,
        "maximum", MAX_LIMIT,
        "default", DEFAULT_LIMIT);
  }
}
