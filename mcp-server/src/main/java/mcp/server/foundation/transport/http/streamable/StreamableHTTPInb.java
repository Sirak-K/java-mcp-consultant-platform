package mcp.server.foundation.transport.http.streamable;

import mcp.server.foundation.logging.ServerLogger;
import mcp.server.foundation.observability.metrics.McpTelemMetrics;
import mcp.server.foundation.observability.metrics.RTMetrics;
import mcp.server.foundation.observability.transport.TranspSignalModel;
import mcp.server.foundation.rpc.RPCJsonEntry;
import mcp.server.foundation.rpc.RPCReqsPayl;
import mcp.server.foundation.rpc.RPCRespPayl;
import mcp.server.foundation.rpc.RPCRouter;
import mcp.server.foundation.security.NetwAuthHTTPResp;
import mcp.server.foundation.security.NetwAuthService;
import mcp.server.foundation.security.StreamableHTTPRateLimiter;
import mcp.server.foundation.security.TranspAuthExcep;
import mcp.server.foundation.security.TranspRateLimitExcep;
import mcp.server.foundation.transport.TranspRPCRespFactory;
import mcp.server.foundation.transport.TranspCapacityExceededExcep;
import mcp.server.foundation.transport.http.shared.HTTPReqsMetadata;
import mcp.server.foundation.transport.http.shared.HTTPTranspCfg;
import mcp.server.foundation.transport.http.shared.HTTPTranspSupport;

import jakarta.servlet.http.HttpServletRequest;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.util.Objects;
import java.util.NoSuchElementException;
import java.util.concurrent.TimeoutException;

final class StreamableHTTPInb {

  private final StreamableHTTPRTOrch runtime;
  private final HTTPTranspSupport httpTranspSupport;
  private final RPCJsonEntry rpcJsonEntrypoint;
  private final ServerLogger logger;
  private final NetwAuthService authService;
  private final StreamableHTTPRateLimiter rateLimiter;
  private final RTMetrics runtimeMetrics;
  private final McpTelemMetrics telemetryMetrics;

  StreamableHTTPInb(
      StreamableHTTPRTOrch runtime,
      HTTPTranspSupport httpTranspSupport,
      RPCJsonEntry rpcJsonEntrypoint,
      ServerLogger logger,
      NetwAuthService authService,
      StreamableHTTPRateLimiter rateLimiter,
      RTMetrics runtimeMetrics,
      McpTelemMetrics telemetryMetrics) {

    this.runtime = Objects.requireNonNull(runtime, "runtime");
    this.httpTranspSupport = Objects.requireNonNull(httpTranspSupport, "httpTranspSupport");
    this.rpcJsonEntrypoint = Objects.requireNonNull(rpcJsonEntrypoint, "rpcJsonEntrypoint");
    this.logger = Objects.requireNonNull(logger, "logger");
    this.authService = Objects.requireNonNull(authService, "authService");
    this.rateLimiter = Objects.requireNonNull(rateLimiter, "rateLimiter");
    this.runtimeMetrics = Objects.requireNonNull(runtimeMetrics, "runtimeMetrics");
    this.telemetryMetrics = Objects.requireNonNull(telemetryMetrics, "telemetryMetrics");
  }

  ResponseEntity<String> StrHttpInHandlePost(
      HttpServletRequest request,
      String rawBody) {

    HTTPReqsMetadata requestMetadata = httpTranspSupport.HTTPSupBuildMetadata(request);
    RPCReqsPayl parsedRequest = null;

    try {
      validateTransportReqs(request, requestMetadata);
      parsedRequest = parseReqsOrThrowBadReqs(rawBody);
      StreamableHTTPProtocolGuard.validatePostAcceptHeader(request);
      String sessionHeader = validateProtocolReqs(request);

      StreamableHTTPRTOrch.PostResult result = runtime.StreamableHTTPRTHandlePost(
          rawBody,
          requestMetadata,
          sessionHeader);

      ResponseEntity.BodyBuilder builder = ResponseEntity.status(result.statusCode());

      if (result.mcpSessIdHeaderValue() != null) {
        builder.header(HTTPTranspSupport.HEADER_MCP_SESSION_ID, result.mcpSessIdHeaderValue());
      }

      if (result.responseBody() == null) {
        return builder.build();
      }

      return builder
          .contentType(MediaType.APPLICATION_JSON)
          .body(result.responseBody());
    } catch (TimeoutException ex) {
      return ResponseEntity.status(HttpStatus.GATEWAY_TIMEOUT)
          .contentType(MediaType.APPLICATION_JSON)
          .body(serializeTimeoutErr(parsedRequest));
    } catch (TranspCapacityExceededExcep ex) {
      return ResponseEntity.status(HttpStatus.TOO_MANY_REQUESTS)
          .contentType(MediaType.APPLICATION_JSON)
          .body(serializeOverloadErr(parsedRequest));
    } catch (TranspRateLimitExcep ex) {
      return ResponseEntity.status(HttpStatus.TOO_MANY_REQUESTS)
          .contentType(MediaType.APPLICATION_JSON)
          .body(serializeOverloadErr(parsedRequest));
    } catch (NoSuchElementException ex) {
      return ResponseEntity.status(HttpStatus.NOT_FOUND)
          .contentType(MediaType.APPLICATION_JSON)
          .body(serializeParseOrValidationErr(parsedRequest, ex));
    } catch (StreamableHTTPAccessDeniedExcep ex) {
      return ResponseEntity.status(HttpStatus.FORBIDDEN)
          .contentType(MediaType.TEXT_PLAIN)
          .body("Forbidden");
    } catch (IllegalArgumentException ex) {
      return ResponseEntity.badRequest()
          .contentType(MediaType.APPLICATION_JSON)
          .body(serializeParseOrValidationErr(parsedRequest, ex));
    } catch (TranspAuthExcep ex) {
      return NetwAuthHTTPResp.NetAuthUnauthorizedText(authService, ex);
    } catch (RuntimeException ex) {
      return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
          .contentType(MediaType.APPLICATION_JSON)
          .body(serializeParseOrValidationErr(ex));
    }
  }

  ResponseEntity<SseEmitter> StrHTTPInHandleGet(HttpServletRequest request) {

    HTTPReqsMetadata requestMetadata = httpTranspSupport.HTTPSupBuildMetadata(request);

    try {
      validateTransportReqs(request, requestMetadata);
      StreamableHTTPProtocolGuard.validateGetAcceptHeader(request);
      String sessionHeader = validateProtocolReqs(request);

      SseEmitter emitter = runtime.StreamableHTTPRTOpenStream(
          sessionHeader,
          requestMetadata);

      return ResponseEntity.ok()
          .header(HttpHeaders.CACHE_CONTROL, "no-store")
          .contentType(MediaType.TEXT_EVENT_STREAM)
          .body(emitter);
    } catch (StreamableHTTPAccessDeniedExcep ex) {
      return ResponseEntity.status(HttpStatus.FORBIDDEN).build();
    } catch (IllegalArgumentException ex) {
      return ResponseEntity.status(HttpStatus.BAD_REQUEST).build();
    } catch (NoSuchElementException ex) {
      return ResponseEntity.status(HttpStatus.NOT_FOUND).build();
    } catch (TranspCapacityExceededExcep ex) {
      return ResponseEntity.status(HttpStatus.TOO_MANY_REQUESTS).build();
    } catch (TranspRateLimitExcep ex) {
      return ResponseEntity.status(HttpStatus.TOO_MANY_REQUESTS).build();
    } catch (TranspAuthExcep ex) {
      return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
          .headers(NetwAuthHTTPResp.NetAuthUnauthorizedHeaders(authService, ex))
          .build();
    } catch (RuntimeException ex) {
      logger.ServerLogErrorStructured(
          ServerLogger.Component.RUNTIME,
          "N/A",
          "UNBOUND",
          "N/A",
          "StreamableHTTPInb: GET failed: " + ex.getMessage(),
          ex);
      return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
    }
  }

  ResponseEntity<Void> StrHTTPInHandleDelete(HttpServletRequest request) {

    HTTPReqsMetadata requestMetadata = httpTranspSupport.HTTPSupBuildMetadata(request);

    try {
      validateTransportReqs(request, requestMetadata);
      String sessionHeader = validateProtocolReqs(request);

      if (!httpTranspSupport.HTTPSupAllowSessDelete()) {
        return ResponseEntity.status(HttpStatus.METHOD_NOT_ALLOWED).build();
      }

      boolean deleted = runtime.StreamableHTTPRTHandleDelete(sessionHeader);
      return deleted
          ? ResponseEntity.noContent().build()
          : ResponseEntity.notFound().build();
    } catch (NoSuchElementException ex) {
      return ResponseEntity.notFound().build();
    } catch (TranspCapacityExceededExcep ex) {
      return ResponseEntity.status(HttpStatus.TOO_MANY_REQUESTS).build();
    } catch (TranspRateLimitExcep ex) {
      return ResponseEntity.status(HttpStatus.TOO_MANY_REQUESTS).build();
    } catch (StreamableHTTPAccessDeniedExcep ex) {
      return ResponseEntity.status(HttpStatus.FORBIDDEN).build();
    } catch (IllegalArgumentException ex) {
      return ResponseEntity.badRequest().build();
    } catch (TranspAuthExcep ex) {
      return NetwAuthHTTPResp.NetAuthUnauthorizedEmpty(authService, ex);
    }
  }

  private void validateTransportReqs(
      HttpServletRequest request,
      HTTPReqsMetadata requestMetadata) {

    validateRateLimit(requestMetadata);
    validateCommonReqs(requestMetadata);
    validateAuth(request);
  }

  private String validateProtocolReqs(HttpServletRequest request) {
    String sessionHeader = StreamableHTTPProtocolGuard.sessionHeader(request);
    StreamableHTTPProtocolGuard.validateAfterInit(httpTranspSupport, request, sessionHeader);
    return sessionHeader;
  }

  private void validateRateLimit(HTTPReqsMetadata requestMetadata) {
    try {
      rateLimiter.RateLimiterAssert(requestMetadata.HTTPReqMetaPreferredClientAddress());
    } catch (TranspRateLimitExcep ex) {
      runtimeMetrics.RTMetricsIncrementCounter("http.rate.limit.denied.total");
      telemetryMetrics.McpTelemIncrementSecurityDenied(
          HTTPTranspCfg.TRANSPORT_STREAMABLE_HTTP,
          "RATE_LIMIT_DENIED");
      logger.ServerLogSecurityAuditWarnDeniedObserved(
          ServerLogger.Component.RUNTIME,
          null,
          "RATE_LIMIT_DENIED",
          "StreamableHTTPInb: rate limit exceeded for client "
              + requestMetadata.HTTPReqMetaPreferredClientAddress());
      throw ex;
    }
  }

  private void validateAuth(HttpServletRequest request) {
    try {
      authService.NetAuthAssertAuthorized(
          request.getHeader(authService.NetAuthHeaderName()),
          HTTPTranspCfg.TRANSPORT_STREAMABLE_HTTP);
    } catch (TranspAuthExcep ex) {
      runtimeMetrics.RTMetricsIncrementCounter(
          TranspSignalModel.TransSigAuthDeniedMetricName(HTTPTranspCfg.TRANSPORT_STREAMABLE_HTTP));
      telemetryMetrics.McpTelemIncrementAuthDenied(
          HTTPTranspCfg.TRANSPORT_STREAMABLE_HTTP,
          ex.TransAuthExcepGetReason().name());
      telemetryMetrics.McpTelemIncrementSecurityDenied(
          HTTPTranspCfg.TRANSPORT_STREAMABLE_HTTP,
          "AUTH_DENIED");
      logger.ServerLogSecurityAuditWarnDeniedObserved(
          ServerLogger.Component.RUNTIME,
          null,
          "AUTH_DENIED",
          "StreamableHTTPInb: streamable HTTP auth denied reason="
              + ex.TransAuthExcepGetReason().name());
      throw ex;
    }
  }

  private void validateCommonReqs(HTTPReqsMetadata requestMetadata) {
    httpTranspSupport.HTTPSupAssertSupportedTransp(HTTPTranspCfg.TRANSPORT_STREAMABLE_HTTP);
    tryAssertTrustedEdge(requestMetadata);
    tryAssertLocalhostAllowed(requestMetadata);
    tryAssertOriginAllowed(requestMetadata);
    tryAssertHostAllowed(requestMetadata);
  }

  private void tryAssertTrustedEdge(HTTPReqsMetadata requestMetadata) {
    try {
      httpTranspSupport.HttpSupAssertTrustedEdgeSatisfied(
          HTTPTranspCfg.TRANSPORT_STREAMABLE_HTTP, requestMetadata);
    } catch (IllegalArgumentException ex) {
      emitAccessDenied(requestMetadata, "EDGE_TRUST_DENIED");
      throw new StreamableHTTPAccessDeniedExcep(ex);
    }
  }

  private void tryAssertLocalhostAllowed(HTTPReqsMetadata requestMetadata) {
    try {
      httpTranspSupport.HttpSupAssertLocalhostAllowed(
          HTTPTranspCfg.TRANSPORT_STREAMABLE_HTTP, requestMetadata);
    } catch (IllegalArgumentException ex) {
      emitAccessDenied(requestMetadata, "LOCALHOST_POLICY_DENIED");
      throw new StreamableHTTPAccessDeniedExcep(ex);
    }
  }

  private void tryAssertOriginAllowed(HTTPReqsMetadata requestMetadata) {
    try {
      httpTranspSupport.HttpSupAssertOriginAllowed(
          HTTPTranspCfg.TRANSPORT_STREAMABLE_HTTP, requestMetadata);
    } catch (IllegalArgumentException ex) {
      emitAccessDenied(requestMetadata, "ORIGIN_DENIED");
      throw new StreamableHTTPAccessDeniedExcep(ex);
    }
  }

  private void tryAssertHostAllowed(HTTPReqsMetadata requestMetadata) {
    try {
      httpTranspSupport.HttpSupAssertHostAllowed(
          HTTPTranspCfg.TRANSPORT_STREAMABLE_HTTP, requestMetadata);
    } catch (IllegalArgumentException ex) {
      emitAccessDenied(requestMetadata, "HOST_DENIED");
      throw new StreamableHTTPAccessDeniedExcep(ex);
    }
  }

  private void emitAccessDenied(HTTPReqsMetadata requestMetadata, String eventName) {
    runtimeMetrics.RTMetricsIncrementCounter("http.access.denied.total");
    telemetryMetrics.McpTelemIncrementSecurityDenied(
        HTTPTranspCfg.TRANSPORT_STREAMABLE_HTTP,
        eventName);
    logger.ServerLogSecurityAuditWarnDeniedObserved(
        ServerLogger.Component.RUNTIME,
        null,
        eventName,
        "StreamableHTTPInb: request denied by access policy ["
            + eventName + "] for client "
            + requestMetadata.HTTPReqMetaPreferredClientAddress());
  }

  private RPCReqsPayl parseReqsOrThrowBadReqs(String rawBody) {

    try {
      return rpcJsonEntrypoint.RPCJsonEntryParseReqs(rawBody);
    } catch (RuntimeException ex) {
      throw new IllegalArgumentException("Malformed JSON-RPC request: " + ex.getMessage(), ex);
    }
  }

  private String serializeParseOrValidationErr(RuntimeException ex) {
    RPCRespPayl errorPayload = RPCRouter.RPCRouterMapParseRTExcepToResp(ex);
    return rpcJsonEntrypoint.RPCJsonEntryToRespJson(errorPayload);
  }

  private String serializeParseOrValidationErr(
      RPCReqsPayl parsedRequest,
      RuntimeException ex) {

    if (parsedRequest == null) {
      return serializeParseOrValidationErr(ex);
    }

    RPCRespPayl errorPayload = RPCRouter.RPCRouterMapRuntimeExcepToResponse(ex, parsedRequest);
    if (errorPayload == null) {
      return serializeParseOrValidationErr(ex);
    }

    return rpcJsonEntrypoint.RPCJsonEntryToRespJson(errorPayload);
  }

  private String serializeTimeoutErr(RPCReqsPayl parsedRequest) {

    if (parsedRequest == null) {
      return rpcJsonEntrypoint.RPCJsonEntryToRespJson(
          TranspRPCRespFactory.parseErr(
              new IllegalArgumentException(TranspRPCRespFactory.MESSAGE_TRANSPORT_TIMEOUT)));
    }

    return rpcJsonEntrypoint.RPCJsonEntryToRespJson(
        TranspRPCRespFactory.timeout(parsedRequest));
  }

  private String serializeOverloadErr(RPCReqsPayl parsedRequest) {

    if (parsedRequest == null) {
      return rpcJsonEntrypoint.RPCJsonEntryToRespJson(
          TranspRPCRespFactory.parseErr(
              new IllegalArgumentException(TranspRPCRespFactory.MESSAGE_TRANSPORT_OVERLOADED)));
    }

    return rpcJsonEntrypoint.RPCJsonEntryToRespJson(
        TranspRPCRespFactory.overload(parsedRequest));
  }

  private static final class StreamableHTTPAccessDeniedExcep extends RuntimeException {

    private StreamableHTTPAccessDeniedExcep(Throwable cause) {
      super(cause);
    }
  }
}
