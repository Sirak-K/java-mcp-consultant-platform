package mcp.server.foundation.tool_interface;

import java.util.List;
import java.util.Map;

public interface ToolInterface {

  String getName();

  String getDescription();

  default String getTitle() {
    return ToolMetadataSupport.ToolMetaSupDefaultTitle(getName());
  }

  Map<String, Object> getInputSchema();

  default Map<String, Object> getOutputSchema() {
    return ToolMetadataSupport.ToolMetaSupDefaultOutputSchema();
  }

  default boolean getReadOnlyHint() {
    return ToolMetadataSupport.ToolMetaSupDeriveReadOnlyHint(getName());
  }

  default boolean getDestructiveHint() {
    return ToolMetadataSupport.ToolMetaSupDeriveDestructiveHint(getName());
  }

  default boolean getIdempotentHint() {
    return ToolMetadataSupport.ToolMetaSupDeriveIdempotentHint(getName());
  }

  default boolean getOpenWorldHint() {
    return ToolMetadataSupport.ToolMetaSupDefaultOpenWorldHint();
  }

  default List<Map<String, Object>> getIcons() {
    return List.of();
  }

  default String getTaskSupport() {
    return ToolMetadataSupport.ToolMetaSupDefaultTaskSupport();
  }

  default List<String> getCategories() {
    return ToolMetadataSupport.ToolMetaSupDefaultCategories(getName());
  }

  default List<Map<String, Object>> getExamples() {
    return ToolMetadataSupport.ToolMetaSupGenerateExamples(getName(), getInputSchema());
  }

  ToolResponse execute(ToolReqs request) throws Exception;

  default ToolResponse execute(
      ToolReqs request,
      ToolExecCtx context) throws Exception {

    if (context != null) {
      context.ToolExecCtxThrowIfCancelled();
    }

    return execute(request);
  }
}
