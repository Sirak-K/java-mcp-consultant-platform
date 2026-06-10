package mcp.server.foundation.logging;

import java.util.List;
import java.util.Objects;

/**
 * Human-readable aligned tabular formatter for local log files.
 */
public final class PipeDelimitedStructuredLogFormatter implements StructuredLogFormatter {

  private static final List<ColumnSpec> SERVER_LOG_COLUMNS = List.of(
      new ColumnSpec(Column.DATE, "DATE"),
      new ColumnSpec(Column.TIME, "TIME"),
      new ColumnSpec(Column.SEVERITY, "SEVERITY"),
      new ColumnSpec(Column.LAYER, "LAYER"),
      new ColumnSpec(Column.TRANSPORT, "TRANSPORT"),
      new ColumnSpec(Column.MCP_SESSION_ID, "SESSION"),
      new ColumnSpec(Column.RPC_CORRELATION_ID, "CORRELATION"),
      new ColumnSpec(Column.TRACE_ID, "TRACE"),
      new ColumnSpec(Column.SPAN_ID, "SPAN"),
      new ColumnSpec(Column.EVENT, "EVENT"),
      new ColumnSpec(Column.ACTION, "ACTION"),
      new ColumnSpec(Column.RPC_METHOD, "RPC"),
      new ColumnSpec(Column.TOOL_NAME, "TOOL"),
      new ColumnSpec(Column.MESSAGE, "MESSAGE"));

  private static final List<ColumnSpec> ERROR_LOG_COLUMNS = List.of(
      new ColumnSpec(Column.DATE, "DATE"),
      new ColumnSpec(Column.TIME, "TIME"),
      new ColumnSpec(Column.SEVERITY, "SEVERITY"),
      new ColumnSpec(Column.LAYER, "LAYER"),
      new ColumnSpec(Column.TRANSPORT, "TRANSPORT"),
      new ColumnSpec(Column.MCP_SESSION_ID, "SESSION"),
      new ColumnSpec(Column.RPC_CORRELATION_ID, "CORRELATION"),
      new ColumnSpec(Column.TRACE_ID, "TRACE"),
      new ColumnSpec(Column.SPAN_ID, "SPAN"),
      new ColumnSpec(Column.EVENT, "EVENT"),
      new ColumnSpec(Column.ACTION, "ACTION"),
      new ColumnSpec(Column.RPC_METHOD, "RPC"),
      new ColumnSpec(Column.TOOL_NAME, "TOOL"),
      new ColumnSpec(Column.ERROR_TYPE, "ERROR_TYPE"),
      new ColumnSpec(Column.MESSAGE, "MESSAGE"));

  private static final List<ColumnSpec> AUDIT_LOG_COLUMNS = List.of(
      new ColumnSpec(Column.DATE, "DATE"),
      new ColumnSpec(Column.TIME, "TIME"),
      new ColumnSpec(Column.SEVERITY, "SEVERITY"),
      new ColumnSpec(Column.LAYER, "LAYER"),
      new ColumnSpec(Column.TRANSPORT, "TRANSPORT"),
      new ColumnSpec(Column.MCP_SESSION_ID, "SESSION"),
      new ColumnSpec(Column.RPC_CORRELATION_ID, "CORRELATION"),
      new ColumnSpec(Column.TRACE_ID, "TRACE"),
      new ColumnSpec(Column.SPAN_ID, "SPAN"),
      new ColumnSpec(Column.EVENT, "EVENT"),
      new ColumnSpec(Column.ACTION, "ACTION"),
      new ColumnSpec(Column.MESSAGE, "MESSAGE"));

  private static final List<ColumnSpec> TEST_LOG_COLUMNS = List.of(
      new ColumnSpec(Column.DATE, "DATE"),
      new ColumnSpec(Column.TIME, "TIME"),
      new ColumnSpec(Column.SEVERITY, "SEVERITY"),
      new ColumnSpec(Column.TEST_SCOPE, "TEST_SCOPE"),
      new ColumnSpec(Column.EVENT, "EVENT"),
      new ColumnSpec(Column.MESSAGE, "MESSAGE"));

  public enum Column {
    DATE,
    TIME,
    SEVERITY,
    LAYER,
    TRANSPORT,
    MCP_SESSION_ID,
    RPC_CORRELATION_ID,
    TRACE_ID,
    SPAN_ID,
    EVENT,
    ACTION,
    RPC_METHOD,
    TOOL_NAME,
    ERROR_TYPE,
    MESSAGE,
    TEST_SCOPE
  }

  private static final String COLUMN_SEPARATOR = " | ";

  private final List<ColumnSpec> columns;
  private final StructuredLogTimestampFormatter timestampFormatter;
  private final StructuredLogRedactor redactor;

  public PipeDelimitedStructuredLogFormatter(
      List<ColumnSpec> columns,
      StructuredLogTimestampFormatter timestampFormatter,
      StructuredLogRedactor redactor) {

    this.columns = List.copyOf(Objects.requireNonNull(columns, "columns"));
    this.timestampFormatter = Objects.requireNonNull(timestampFormatter, "timestampFormatter");
    this.redactor = Objects.requireNonNull(redactor, "redactor");

    if (this.columns.isEmpty()) {
      throw new IllegalArgumentException("columns must not be empty");
    }
  }

  public static List<ColumnSpec> PipeDelimitedStructuredLogFormatterServerColumns() {
    return SERVER_LOG_COLUMNS;
  }

  public static List<ColumnSpec> PipeDelimitedStructuredLogFormatterErrColumns() {
    return ERROR_LOG_COLUMNS;
  }

  public static List<ColumnSpec> PipeDelimitedStructuredLogFormatterAuditColumns() {
    return AUDIT_LOG_COLUMNS;
  }

  public static List<ColumnSpec> PipeDelimitedStructuredLogFormatterTestColumns() {
    return TEST_LOG_COLUMNS;
  }

  @Override
  public String StructuredLogHeader() {
    return String.join(COLUMN_SEPARATOR, PipeDelimitedStructuredLogFormatterHeaderCells());
  }

  @Override
  public String StructuredLogFormat(
      StructuredLogEvent event,
      Throwable throwable) {

    return String.join(COLUMN_SEPARATOR, PipeDelimitedStructuredLogFormatterCells(event, throwable));
  }

  public List<String> PipeDelimitedStructuredLogFormatterHeaderCells() {
    return columns.stream()
        .map(ColumnSpec::header)
        .toList();
  }

  public List<String> PipeDelimitedStructuredLogFormatterCells(
      StructuredLogEvent event,
      Throwable throwable) {

    Objects.requireNonNull(event, "event");

    return columns.stream()
        .map(column -> redactor.StructuredLogRedact(formatColumn(column.column(), event)))
        .toList();
  }

  public String PipeDelimitedStructuredLogFormatterRenderTable(List<List<String>> rows) {
    Objects.requireNonNull(rows, "rows");

    List<String> headerCells = PipeDelimitedStructuredLogFormatterHeaderCells();
    int[] widths = PipeDelimitedStructuredLogFormatterWidths(headerCells, rows);
    StringBuilder content = new StringBuilder()
        .append(renderRow(headerCells, widths))
        .append(System.lineSeparator())
        .append(renderSeparator(widths));

    for (List<String> row : rows) {
      content.append(System.lineSeparator()).append(renderRow(row, widths));
    }

    return content.toString();
  }

  private String formatColumn(Column column, StructuredLogEvent event) {
    return switch (column) {
      case DATE -> sanitize(timestampFormatter.StructuredLogTimestampFormatDate(event.StructuredLogEvtGetTimestamp()));
      case TIME -> sanitize(timestampFormatter.StructuredLogTimestampFormatTime(event.StructuredLogEvtGetTimestamp()));
      case SEVERITY -> sanitize(event.StructuredLogEvtGetSeverity());
      case LAYER -> sanitize(event.StructuredLogEvtGetLayer());
      case TRANSPORT -> sanitize(event.StructuredLogEvtGetTranspName());
      case MCP_SESSION_ID -> sanitize(event.StructuredLogEvtGetMcpSessId());
      case RPC_CORRELATION_ID -> sanitize(event.StructuredLogEvtGetRPCCorrelaId());
      case TRACE_ID -> sanitize(event.StructuredLogEvtGetTraceId());
      case SPAN_ID -> sanitize(event.StructuredLogEvtGetSpanId());
      case EVENT -> sanitize(event.StructuredLogEvtGetEventName());
      case ACTION -> sanitize(event.StructuredLogEvtGetAction());
      case RPC_METHOD -> sanitize(event.StructuredLogEvtGetRPCMet());
      case TOOL_NAME -> sanitize(event.StructuredLogEvtGetToolName());
      case ERROR_TYPE -> sanitize(event.StructuredLogEvtGetErrType());
      case MESSAGE -> sanitize(event.StructuredLogEvtGetMessage());
      case TEST_SCOPE -> sanitize(event.StructuredLogEvtGetLayer());
    };
  }

  private static int[] PipeDelimitedStructuredLogFormatterWidths(List<String> headers, List<List<String>> rows) {
    int[] widths = new int[headers.size()];

    for (int index = 0; index < headers.size(); index++) {
      widths[index] = headers.get(index).length();
    }

    for (List<String> row : rows) {
      for (int index = 0; index < headers.size(); index++) {
        String cell = index < row.size() ? row.get(index) : "";
        widths[index] = Math.max(widths[index], cell.length());
      }
    }

    return widths;
  }

  private static String renderRow(List<String> cells, int[] widths) {
    StringBuilder row = new StringBuilder();

    for (int index = 0; index < widths.length; index++) {
      if (index > 0) {
        row.append(COLUMN_SEPARATOR);
      }

      String cell = index < cells.size() ? cells.get(index) : "";
      row.append(String.format("%-" + widths[index] + "s", cell));
    }

    return row.toString();
  }

  private static String renderSeparator(int[] widths) {
    StringBuilder separator = new StringBuilder();

    for (int index = 0; index < widths.length; index++) {
      if (index > 0) {
        separator.append(COLUMN_SEPARATOR);
      }
      separator.append("-".repeat(Math.max(1, widths[index])));
    }

    return separator.toString();
  }

  private static String sanitize(String value) {
    if (value == null) {
      return "";
    }

    return value
        .replace("\r\n", "\\n")
        .replace('\r', '\n')
        .replace("\n", "\\n")
        .replace("|", "/")
        .trim();
  }

  public record ColumnSpec(
      Column column,
      String header) {

    public ColumnSpec {
      Objects.requireNonNull(column, "column");
      Objects.requireNonNull(header, "header");
    }
  }
}
