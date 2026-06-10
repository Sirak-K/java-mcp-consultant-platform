package mcp.server.foundation.transport.stdio;

import mcp.server.foundation.transport.TranspAdap;
import mcp.server.foundation.transport.TranspCapacityProfile;
import mcp.server.foundation.transport.TranspSess;

import java.util.Map;
import java.util.function.BiConsumer;
import java.util.function.Consumer;

public final class STDIOTranspAdap implements TranspAdap {

  private static final String TRANSPORT_NAME = "stdio";

  private final STDIORTOrch runtime;

  public STDIOTranspAdap(STDIORTOrch runtime) {
    this.runtime = runtime;
  }

  @Override
  public String TranspAdapGetTranspName() {
    return TRANSPORT_NAME;
  }

  @Override
  public void TranspAdapStart() {
    runtime.STDIORTOrchStart();
  }

  @Override
  public void TranspAdapStop() {
    runtime.STDIORTOrchStop();
  }

  @Override
  public void TranspAdapSetMessageHandler(BiConsumer<TranspSess, String> handler) {
    runtime.STDIORTOrchSetMessageHandler(handler);
  }

  @Override
  public void TranspAdapSendTo(TranspSess session, String message) {
    runtime.STDIORTOrchSendTo(session, message);
  }

  @Override
  public TranspSess TranspAdapGetSessionById(String sessionId) {
    return runtime.STDIORTOrchGetSessById(sessionId);
  }

  @Override
  public boolean TranspAdapCloseSessById(String sessionId) {
    return runtime.STDIORTOrchCloseSess(sessionId);
  }

  @Override
  public Map<String, Long> TranspAdapDescribeCapacityProfile() {
    return TranspCapacityProfile.TransCapProfileSTDIOSingleConn();
  }

  @Override
  public void TranspAdapSetSessionOpenHandler(Consumer<TranspSess> handler) {
    runtime.STDIORTOrchSetOpenHandler(handler);
  }

  @Override
  public void TranspAdapSetSessionCloseHandler(Consumer<TranspSess> handler) {
    runtime.STDIORTOrchSetCloseHandler(handler);
  }
}
