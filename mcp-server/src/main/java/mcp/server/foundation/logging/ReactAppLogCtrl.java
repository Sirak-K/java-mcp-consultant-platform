package mcp.server.foundation.logging;

import java.util.Objects;

import org.springframework.http.HttpStatus;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import mcp.server.foundation.security.request_binding.ReqsAuthBinding;

@RestController
public final class ReactAppLogCtrl {

  private final ReactAppLogService reactAppLogService;

  public ReactAppLogCtrl(ReactAppLogService reactAppLogService) {
    this.reactAppLogService = Objects.requireNonNull(reactAppLogService, "reactAppLogService");
  }

  @PostMapping("/api/ops/react-app-log")
  @ResponseStatus(HttpStatus.NO_CONTENT)
  public void recordReactAppLog(
      @RequestBody ReactAppLogEntry entry,
      @AuthenticationPrincipal ReqsAuthBinding requestAuthBinding) {

    reactAppLogService.ReactAppLogRecord(entry, requestAuthBinding);
  }
}
