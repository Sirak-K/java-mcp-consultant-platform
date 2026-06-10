package mcp.server.foundation.resource_interface;

import mcp.server.foundation.rpc.RPCJsonSeria;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

public final class ResrcDefin {

  public static final String MIME_TYPE_JSON = "application/json";

  private final String uri;
  private final String name;
  private final String title;
  private final String description;
  private final String mimeType;
  private final boolean dynamic;
  private final List<Map<String, Object>> icons;
  private final Map<String, Object> annotations;
  private final ResrcProvid provider;

  public ResrcDefin(
      String uri,
      String name,
      String description,
      String mimeType,
      boolean dynamic,
      ResrcProvid provider) {

    this(
        uri,
        name,
        name,
        description,
        mimeType,
        dynamic,
        List.of(),
        Map.of(),
        provider);
  }

  public ResrcDefin(
      String uri,
      String name,
      String title,
      String description,
      String mimeType,
      boolean dynamic,
      List<Map<String, Object>> icons,
      Map<String, Object> annotations,
      ResrcProvid provider) {

    this.uri = ResrcSupport.requireNonBlank(uri, "uri");
    this.name = ResrcSupport.requireNonBlank(name, "name");
    this.title = ResrcSupport.requireNonBlank(title, "title");
    this.description = ResrcSupport.requireNonBlank(description, "description");
    this.mimeType = ResrcSupport.requireNonBlank(mimeType, "mimeType");
    this.dynamic = dynamic;
    this.icons = copyMapList(icons, "icons");
    this.annotations = Map.copyOf(Objects.requireNonNull(annotations, "annotations"));
    this.provider = Objects.requireNonNull(provider, "provider");
  }

  public String ResrcDefGetUri() {
    return uri;
  }

  public String ResrcDefGetName() {
    return name;
  }

  public String ResrcDefGetTitle() {
    return title;
  }

  public boolean ResrcDefIsDynamic() {
    return dynamic;
  }

  public Map<String, Object> ResrcDefToMcpFormat() {

    LinkedHashMap<String, Object> result = new LinkedHashMap<>();
    result.put("uri", uri);
    result.put("name", name);
    result.put("title", title);
    result.put("description", description);
    result.put("mimeType", mimeType);
    if (!icons.isEmpty()) {
      result.put("icons", icons);
    }
    if (!annotations.isEmpty()) {
      result.put("annotations", annotations);
    }
    result.put("_meta", Map.of("dynamic", dynamic));
    return Map.copyOf(result);
  }

  public Map<String, Object> ResrcDefToMcpTemplateFormat() {

    LinkedHashMap<String, Object> result = new LinkedHashMap<>();
    result.put("uriTemplate", uri);
    result.put("name", name);
    result.put("title", title);
    result.put("description", description);
    result.put("mimeType", mimeType);
    if (!icons.isEmpty()) {
      result.put("icons", icons);
    }
    if (!annotations.isEmpty()) {
      result.put("annotations", annotations);
    }
    result.put("_meta", Map.of(
        "dynamic", dynamic,
        "sourceResourceUri", uri));
    return Map.copyOf(result);
  }

  public Map<String, Object> ResrcDefReadToMcpFormat(RPCJsonSeria serializer) {

    Objects.requireNonNull(serializer, "serializer");

    String text = serializer.JsonRPCSerSerialize(provider.ResourceProvRead());

    return Map.of(
        "contents", List.of(
            Map.of(
                "uri", uri,
                "mimeType", mimeType,
                "text", text)));
  }

  private static List<Map<String, Object>> copyMapList(
      List<Map<String, Object>> values,
      String fieldName) {

    Objects.requireNonNull(values, fieldName);
    return values.stream()
        .map(value -> Map.copyOf(Objects.requireNonNull(value, fieldName + " entry")))
        .toList();
  }
}
