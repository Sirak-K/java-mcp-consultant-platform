package mcp.server.foundation.transport.http.streamable;

import jakarta.servlet.http.HttpServletRequest;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.util.Objects;

@RestController
@ConditionalOnProperty(
    prefix = "mcp.transport.streamable-http",
    name = "enabled",
    havingValue = "true")
@RequestMapping("${mcp.transport.streamable-http.endpoint-path:/mcp}")
public class StreamableHTTPCtrl {

  private final StreamableHTTPInb inbound;

  public StreamableHTTPCtrl(StreamableHTTPInb inbound) {
    this.inbound = Objects.requireNonNull(inbound, "inbound");
  }

  @PostMapping
  public ResponseEntity<String> StrHttpCtrlPost(
      HttpServletRequest request,
      @RequestBody(required = false) String rawBody) {
    return inbound.StrHttpInHandlePost(request, rawBody == null ? "" : rawBody);
  }

  @GetMapping
  public ResponseEntity<SseEmitter> StrHTTPCtrlGet(HttpServletRequest request) {
    return inbound.StrHTTPInHandleGet(request);
  }

  @DeleteMapping
  public ResponseEntity<Void> StrHTTPCtrlDelete(HttpServletRequest request) {
    return inbound.StrHTTPInHandleDelete(request);
  }
}
