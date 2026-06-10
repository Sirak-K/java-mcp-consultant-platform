package mcp.server.foundation.resource_interface;

import java.util.Map;

@FunctionalInterface
public interface ResrcProvid {

  Map<String, Object> ResourceProvRead();
}
