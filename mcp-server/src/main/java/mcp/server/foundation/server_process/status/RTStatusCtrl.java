package mcp.server.foundation.server_process.status;

import mcp.server.foundation.transport.TranspAdap;
import mcp.server.foundation.transport.websocket.WsTranspAdap;
import mcp.server.foundation.transport.websocket.WsTranspStatus;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Objects;

/**
 * RTStatusCtrl
 *
 * Ansvar:
 * - Exponerar deterministisk liveness endpoint
 * - Returnerar alltid 200 om servern är igång
 * - Exponerar WS-transportstatus (server truth)
 *
 * Transport abstraction:
 * - Beroende på TranspAdap (abstraktion)
 * - Ingen hård koppling till WebSocket-implementation
 */
@RestController
public class RTStatusCtrl {

  private final TranspAdap transportAdapter;

  public RTStatusCtrl(TranspAdap transportAdapter) {
    this.transportAdapter = Objects.requireNonNull(transportAdapter, "transportAdapter");
  }

  /**
   * Liveness endpoint.
   */
  @GetMapping("/liveness")
  public ResponseEntity<String> RTStatusCtrlLiveness() {
    return ResponseEntity.ok("MCP_SERVER_ALIVE");
  }

  /**
   * Server truth endpoint for WS transport.
   *
   * sessionCount = number of active physical WebSocket sessions currently open.
   *
   * Om annan transport används i framtiden returneras 0
   * (deterministiskt standardbeteende).
   */
  @GetMapping("/runtime/ws-transport")
  public ResponseEntity<WsTranspStatus> RTStatusCtrlWsTranspStatus() {

    int count = 0;

    if (transportAdapter instanceof WsTranspAdap wsAdapter) {
      count = wsAdapter.WSTrGetActiveWsSessCount();
    }

    return ResponseEntity.ok(new WsTranspStatus(count));
  }
}
