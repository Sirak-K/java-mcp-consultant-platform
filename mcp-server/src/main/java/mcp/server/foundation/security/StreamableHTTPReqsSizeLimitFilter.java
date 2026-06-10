package mcp.server.foundation.security;

import mcp.server.foundation.logging.ServerLogger;
import mcp.server.foundation.observability.metrics.McpTelemMetrics;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ReadListener;
import jakarta.servlet.ServletException;
import jakarta.servlet.ServletInputStream;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletRequestWrapper;
import jakarta.servlet.http.HttpServletResponse;

import org.springframework.http.MediaType;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.util.Objects;

public final class StreamableHTTPReqsSizeLimitFilter extends OncePerRequestFilter {

  private static final String MESSAGE_PAYLOAD_TOO_LARGE = "Payload Too Large";
  private static final String EVENT_REQUEST_SIZE_LIMIT_DENIED = "REQUEST_SIZE_LIMIT_DENIED";

  private final long maxRequestBodyBytes;
  private final ServerLogger logger;
  private final McpTelemMetrics telemetryMetrics;

  StreamableHTTPReqsSizeLimitFilter(
      StreamableHTTPReqsSizeLimitSettings settings,
      ServerLogger logger,
      McpTelemMetrics telemetryMetrics) {
    this.maxRequestBodyBytes = Objects.requireNonNull(settings, "settings").maxRequestBodyBytes();
    this.logger = Objects.requireNonNull(logger, "logger");
    this.telemetryMetrics = Objects.requireNonNull(telemetryMetrics, "telemetryMetrics");
  }

  @Override
  protected void doFilterInternal(
      HttpServletRequest request,
      HttpServletResponse response,
      FilterChain filterChain) throws ServletException, IOException {

    // Ingress request-size limiting is transport backpressure protection.
    // It is intentionally separate from identity-based rate limiting and from
    // higher-level throttling or load-shedding decisions.

    if (!"POST".equalsIgnoreCase(request.getMethod())) {
      filterChain.doFilter(request, response);
      return;
    }

    long contentLength = request.getContentLengthLong();
    if (contentLength > maxRequestBodyBytes) {
      emitReqsSizeDenied(request, contentLength);
      writePayloadTooLarge(response);
      return;
    }

    SizeLimitedHttpServletRequest wrappedRequest = new SizeLimitedHttpServletRequest(request, maxRequestBodyBytes);
    try {
      filterChain.doFilter(wrappedRequest, response);
    } catch (ReqsBodyTooLargeIOException ex) {
      emitReqsSizeDenied(request, null);
      writePayloadTooLarge(response);
    }
  }

  private static void writePayloadTooLarge(HttpServletResponse response) throws IOException {
    if (response.isCommitted()) {
      return;
    }

    response.reset();
    response.setStatus(HttpServletResponse.SC_REQUEST_ENTITY_TOO_LARGE);
    response.setCharacterEncoding(StandardCharsets.UTF_8.name());
    response.setContentType(MediaType.TEXT_PLAIN_VALUE);
    response.getWriter().write(MESSAGE_PAYLOAD_TOO_LARGE);
    response.getWriter().flush();
  }

  private void emitReqsSizeDenied(HttpServletRequest request, Long declaredContentLength) {
    String contentLengthSummary = declaredContentLength == null || declaredContentLength < 0L
        ? "unknown"
        : Long.toString(declaredContentLength);

    telemetryMetrics.McpTelemIncrementSecurityDenied("streamable-http", EVENT_REQUEST_SIZE_LIMIT_DENIED);
    logger.ServerLogSecurityAuditWarnDeniedObserved(
        ServerLogger.Component.RUNTIME,
        null,
        EVENT_REQUEST_SIZE_LIMIT_DENIED,
        "StreamableHTTPReqsSizeLimitFilter: denied oversized streamable HTTP request from client "
            + request.getRemoteAddr()
            + " contentLength="
            + contentLengthSummary
            + " maxRequestBodyBytes="
            + maxRequestBodyBytes);
  }

  private static final class SizeLimitedHttpServletRequest extends HttpServletRequestWrapper {

    private final long maxRequestBodyBytes;
    private ServletInputStream inputStream;
    private BufferedReader reader;

    SizeLimitedHttpServletRequest(
        HttpServletRequest request,
        long maxRequestBodyBytes) {

      super(request);
      this.maxRequestBodyBytes = maxRequestBodyBytes;
    }

    @Override
    public ServletInputStream getInputStream() throws IOException {
      if (reader != null) {
        throw new IllegalStateException("getReader() has already been called for this request");
      }

      if (inputStream == null) {
        inputStream = new SizeLimitedServletInputStream(super.getInputStream(), maxRequestBodyBytes);
      }

      return inputStream;
    }

    @Override
    public BufferedReader getReader() throws IOException {
      if (reader == null) {
        Charset charset = getCharacterEncoding() == null
            ? StandardCharsets.UTF_8
            : Charset.forName(getCharacterEncoding());
        reader = new BufferedReader(new InputStreamReader(getInputStream(), charset));
      }

      return reader;
    }
  }

  private static final class SizeLimitedServletInputStream extends ServletInputStream {

    private final ServletInputStream delegate;
    private final long maxRequestBodyBytes;
    private long bytesRead;

    SizeLimitedServletInputStream(
        ServletInputStream delegate,
        long maxRequestBodyBytes) {

      this.delegate = Objects.requireNonNull(delegate, "delegate");
      this.maxRequestBodyBytes = maxRequestBodyBytes;
    }

    @Override
    public int read() throws IOException {
      int value = delegate.read();
      if (value >= 0) {
        incrementBytesRead(1L);
      }
      return value;
    }

    @Override
    public int read(byte[] b, int off, int len) throws IOException {
      int count = delegate.read(b, off, len);
      if (count > 0) {
        incrementBytesRead(count);
      }
      return count;
    }

    @Override
    public boolean isFinished() {
      return delegate.isFinished();
    }

    @Override
    public boolean isReady() {
      return delegate.isReady();
    }

    @Override
    public void setReadListener(ReadListener readListener) {
      delegate.setReadListener(readListener);
    }

    private void incrementBytesRead(long additionalBytes) throws IOException {
      bytesRead += additionalBytes;
      if (bytesRead > maxRequestBodyBytes) {
        throw new ReqsBodyTooLargeIOException();
      }
    }
  }

  private static final class ReqsBodyTooLargeIOException extends IOException {
    private static final long serialVersionUID = 1L;
  }
}
