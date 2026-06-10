package mcp.server.foundation.tool_interface;

import mcp.server.foundation.support.ListLimitSupport;

import java.time.LocalDate;
import java.util.Map;
import java.util.Objects;
import java.util.function.Function;

/**
 * Boundary-oriented helpers for parsing tool request arguments and schemas.
 */
public final class ToolReqsSupport {

  private ToolReqsSupport() {
  }

  public static Map<String, Object> ToolReqSupportListLimitSchema() {
    return ListLimitSupport.ListLimitSchema();
  }

  public static int ToolReqSupportResolveLimit(ToolReqs request) {
    Objects.requireNonNull(request, "request");
    return ListLimitSupport.ListLimitResolveRequested(
        request.ToolReqParam("limit").ToolParamAsInt(null));
  }

  public static long ToolReqSupportRequiredLong(ToolReqs request, String name) {
    return ToolReqSupportParam(request, name).ToolParamAsLong();
  }

  public static String ToolReqSupportRequiredText(ToolReqs request, String name) {
    return ToolReqSupportParam(request, name).ToolParamAsText();
  }

  public static String ToolReqSupportOptionalText(ToolReqs request, String name) {
    return ToolReqSupportParam(request, name).ToolParamAsText(null);
  }

  public static String ToolReqSupportTextOrDefault(
      ToolReqs request,
      String name,
      String defaultValue) {

    return ToolReqSupportParam(request, name).ToolParamAsText(defaultValue);
  }

  public static Integer ToolReqSupportOptionalInt(ToolReqs request, String name) {
    return ToolReqSupportParam(request, name).ToolParamAsInt(null);
  }

  public static <ID> ID ToolReqSupportRequiredId(
      ToolReqs request,
      String name,
      Function<Long, ID> idFactory) {

    Objects.requireNonNull(idFactory, "idFactory");
    return idFactory.apply(ToolReqSupportRequiredLong(request, name));
  }

  public static <E extends Enum<E>> E ToolReqSupportOptionalEnum(
      ToolReqs request,
      String name,
      Class<E> enumType) {

    Objects.requireNonNull(enumType, "enumType");
    String raw = ToolReqSupportOptionalText(request, name);
    return raw == null ? null : Enum.valueOf(enumType, raw);
  }

  public static LocalDate ToolReqSupportOptionalLocalDate(ToolReqs request, String name) {
    String raw = ToolReqSupportOptionalText(request, name);
    return raw == null ? null : LocalDate.parse(raw);
  }

  private static ToolParam ToolReqSupportParam(ToolReqs request, String name) {
    Objects.requireNonNull(request, "request");
    if (name == null || name.isBlank()) {
      throw new IllegalArgumentException("name must not be blank");
    }
    return request.ToolReqParam(name);
  }
}
