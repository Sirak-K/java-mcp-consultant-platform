package mcp.server.foundation.transport.http.streamable;

import mcp.server.foundation.transport.TranspAdap;
import mcp.server.foundation.transport.TranspCapacityProfile;
import mcp.server.foundation.transport.TranspSess;
import mcp.server.foundation.transport.http.shared.HTTPTranspCfg;

import java.util.Map;
import java.util.Objects;
import java.util.function.BiConsumer;
import java.util.function.Consumer;

public final class StreamableHTTPTranspAdap implements TranspAdap {

  private final StreamableHTTPRTOrch runtime;
  private final StreamableHTTPOutb outbound;

  public StreamableHTTPTranspAdap(
      StreamableHTTPRTOrch runtime,
      StreamableHTTPOutb outbound) {

    this.runtime = Objects.requireNonNull(runtime, "runtime");
    this.outbound = Objects.requireNonNull(outbound, "outbound");
  }

  @Override
  public String TranspAdapGetTranspName() {
    return HTTPTranspCfg.TRANSPORT_STREAMABLE_HTTP;
  }

  @Override
  public void TranspAdapStart() {
    runtime.StreamableHTTPRTStart();
  }

  @Override
  public void TranspAdapStop() {
    runtime.StreamableHTTPRTStop();
  }

  @Override
  public void TranspAdapSetMessageHandler(BiConsumer<TranspSess, String> handler) {
    runtime.StreamableHTTPRTSetMessageHandler(handler);
  }

  @Override
  public void TranspAdapSendTo(TranspSess session, String message) {
    outbound.StrHttpOutSendTo(session, message);
  }

  @Override
  public TranspSess TranspAdapGetSessionById(String sessionId) {
    return runtime.StreamableHTTPRTGetSessById(sessionId);
  }

  @Override
  public boolean TranspAdapCloseSessById(String sessionId) {
    return runtime.StreamableHTTPRTCloseSessById(sessionId);
  }

  @Override
  public Map<String, Long> TranspAdapDescribeCapacityProfile() {
    var support = runtime.StreamableHTTPRTGetTranspSupport();
    return TranspCapacityProfile.TransCapProfileHttpStreamable(
        support.HTTPSupStreamableMaxActiveSessions(),
        support.HTTPSupStreamableMaxActiveStreams(),
        support.HTTPSupStreamableMaxBufferedOutbMessagesPerSess(),
        support.HTTPSupStreamablePostRespTimeoutMillis());
  }

  @Override
  public void TranspAdapSetSessionOpenHandler(Consumer<TranspSess> handler) {
    runtime.StreamableHTTPRTSetSessOpenHandler(handler);
  }

  @Override
  public void TranspAdapSetSessionCloseHandler(Consumer<TranspSess> handler) {
    runtime.StreamableHTTPRTSetSessCloseHandler(handler);
  }

  public int StrHTTPTrGetActiveSessCount() {
    return runtime.StreamableHTTPRTGetActiveSessCount();
  }
}
