package mcp.server.foundation.resource_interface;

import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.ConcurrentHashMap;

public final class ResrcReg {

  private final Map<String, ResrcDefin> definitions = new ConcurrentHashMap<>();

  public void ResrcRegRegister(ResrcDefin definition) {

    Objects.requireNonNull(definition, "definition");

    String uri = ResrcSupport.requireResourceUri(definition.ResrcDefGetUri());

    ResrcDefin previous = definitions.putIfAbsent(uri, definition);

    if (previous != null) {
      throw new IllegalStateException("Resource already registered: " + uri);
    }
  }

  public ResrcDefin ResrcRegGetDefin(String uri) {
    return definitions.get(uri);
  }

  public List<ResrcDefin> ResrcRegListDefinitions() {
    return definitions.values().stream()
        .sorted(Comparator.comparing(ResrcDefin::ResrcDefGetUri))
        .toList();
  }

  public int ResrcRegSize() {
    return definitions.size();
  }
}
