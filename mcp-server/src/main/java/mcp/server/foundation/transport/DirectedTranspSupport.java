package mcp.server.foundation.transport;

/**
 * DirectedTranspSupport
 *
 * Kontrakt för transports som kan skicka meddelanden
 * direkt till specifik TranspSess.
 */
public interface DirectedTranspSupport {

  void sendTo(TranspSess session, String payload);
}
