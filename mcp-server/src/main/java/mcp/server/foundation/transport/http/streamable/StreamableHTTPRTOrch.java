package mcp.server.foundation.transport.http.streamable;

import mcp.server.foundation.logging.ServerLogger;
import mcp.server.foundation.observability.context.ObservCtx;
import mcp.server.foundation.observability.context.ObservCtxFactory;
import mcp.server.foundation.observability.metrics.McpTelemMetrics;
import mcp.server.foundation.observability.metrics.RTMetrics;
import mcp.server.foundation.observability.transport.TranspSignalModel;
import mcp.server.foundation.server_process.orchestration.OperatingSurface;
import mcp.server.foundation.rpc.RPCJsonEntry;
import mcp.server.foundation.rpc.RPCMetName;
import mcp.server.foundation.rpc.RPCReqsPayl;
import mcp.server.foundation.rpc.RPCRouter;
import mcp.server.foundation.transport.TranspCapacityExceededExcep;
import mcp.server.foundation.transport.TranspOutbTelemSupport;
import mcp.server.foundation.transport.TranspSess;
import mcp.server.foundation.transport.http.shared.HTTPReqsMetadata;
import mcp.server.foundation.transport.http.shared.HTTPSessBindingReg;
import mcp.server.foundation.transport.http.shared.HTTPTranspCfg;
import mcp.server.foundation.transport.http.shared.HTTPTranspSess;
import mcp.server.foundation.transport.http.shared.HTTPTranspSupport;

import org.springframework.http.MediaType;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.io.IOException;
import java.util.NoSuchElementException;
import java.util.Objects;
import java.util.Queue;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.function.BiConsumer;
import java.util.function.Consumer;

final class StreamableHTTPRTOrch {

  private static final String METRIC_DUPLICATE_INFLIGHT_REJECTED_TOTAL =
      "http.requests.duplicate_inflight.rejected.total";
  private record PendingResponse(
      String correlationId,
      CompletableFuture<String> future) {
  }

  record PostResult(
      int statusCode,
      String responseBody,
      String mcpSessIdHeaderValue) {
  }

  private final HTTPTranspSupport httpTranspSupport;
  private final HTTPSessBindingReg httpSessBindingReg;
  private final RPCJsonEntry rpcJsonEntrypoint;
  private final ServerLogger logger;
  private final ObservCtxFactory obsCtxFactory;
  private final RTMetrics runtimeMetrics;
  private final McpTelemMetrics telemetryMetrics;

  private final ConcurrentHashMap<String, PendingResponse> pendingResponses = new ConcurrentHashMap<>();
  private final ConcurrentHashMap<String, SseEmitter> streamEmitters = new ConcurrentHashMap<>();
  private final ConcurrentHashMap<String, Queue<String>> bufferedNotifications = new ConcurrentHashMap<>();
  private final Set<String> sessionsWithOpenedStream = ConcurrentHashMap.newKeySet();

  private final AtomicBoolean started = new AtomicBoolean(false);

  private volatile BiConsumer<TranspSess, String> messageHandler = (session, payload) -> {
  };
  private volatile Consumer<TranspSess> sessionOpenHandler = session -> {
  };
  private volatile Consumer<TranspSess> sessionCloseHandler = session -> {
  };

  StreamableHTTPRTOrch(
      HTTPTranspSupport httpTranspSupport,
      HTTPSessBindingReg httpSessBindingReg,
      RPCJsonEntry rpcJsonEntrypoint,
      ServerLogger logger,
      ObservCtxFactory obsCtxFactory,
      RTMetrics runtimeMetrics,
      McpTelemMetrics telemetryMetrics) {

    this.httpTranspSupport = Objects.requireNonNull(httpTranspSupport, "httpTranspSupport");
    this.httpSessBindingReg = Objects.requireNonNull(httpSessBindingReg, "httpSessBindingReg");
    this.rpcJsonEntrypoint = Objects.requireNonNull(rpcJsonEntrypoint, "rpcJsonEntrypoint");
    this.logger = Objects.requireNonNull(logger, "logger");
    this.obsCtxFactory = Objects.requireNonNull(obsCtxFactory, "obsCtxFactory");
    this.runtimeMetrics = Objects.requireNonNull(runtimeMetrics, "runtimeMetrics");
    this.telemetryMetrics = Objects.requireNonNull(telemetryMetrics, "telemetryMetrics");
  }

  void StreamableHTTPRTStart() {
    started.set(true);
    runtimeMetrics.RTMetricsSetGauge("http.streams.active", 0L);
    runtimeMetrics.RTMetricsSetGauge("http.sessions.active", httpSessBindingReg.getActiveSessCount());
    runtimeMetrics.RTMetricsSetGauge("http.responses.pending", 0L);
    runtimeMetrics.RTMetricsSetGauge("http.notifications.buffered", 0L);
  }

  void StreamableHTTPRTStop() {

    started.set(false);

    for (String sessionId : streamEmitters.keySet()) {
      SseEmitter emitter = streamEmitters.remove(sessionId);
      if (emitter != null) {
        emitter.complete();
      }
    }

    for (String transportConnectionId : httpSessBindingReg.getTranspConnIdsSnapshot()) {
      HTTPTranspSess session = httpSessBindingReg.unregisterByTranspConnId(transportConnectionId);
      if (session != null) {
        session.HtsGetTranspSess().TranspSessClose();
        sessionCloseHandler.accept(session.HtsGetTranspSess());
      }
    }

    pendingResponses.clear();
    bufferedNotifications.clear();
    sessionsWithOpenedStream.clear();
    runtimeMetrics.RTMetricsSetGauge("http.streams.active", 0L);
    runtimeMetrics.RTMetricsSetGauge("http.sessions.active", 0L);
    runtimeMetrics.RTMetricsSetGauge("http.responses.pending", 0L);
    runtimeMetrics.RTMetricsSetGauge("http.notifications.buffered", 0L);
  }

  void StreamableHTTPRTSetMessageHandler(BiConsumer<TranspSess, String> handler) {
    this.messageHandler = Objects.requireNonNull(handler, "handler");
  }

  void StreamableHTTPRTSetSessOpenHandler(Consumer<TranspSess> handler) {
    this.sessionOpenHandler = Objects.requireNonNull(handler, "handler");
  }

  void StreamableHTTPRTSetSessCloseHandler(Consumer<TranspSess> handler) {
    this.sessionCloseHandler = Objects.requireNonNull(handler, "handler");
  }

  TranspSess StreamableHTTPRTGetSessById(String sessionId) {
    HTTPTranspSess session = httpSessBindingReg.getByMcpSessId(sessionId);
    return session == null ? null : session.HtsGetTranspSess();
  }

  boolean StreamableHTTPRTCloseSessById(String sessionId) {

    if (sessionId == null || sessionId.isBlank()) {
      return false;
    }

    HTTPTranspSess session = httpSessBindingReg.getByMcpSessId(sessionId);
    if (session == null) {
      return false;
    }

    return StreamableHTTPRTCloseSess(session);
  }

  int StreamableHTTPRTGetActiveSessCount() {
    return httpSessBindingReg.getActiveSessCount();
  }

  HTTPTranspSupport StreamableHTTPRTGetTranspSupport() {
    return httpTranspSupport;
  }

  PostResult StreamableHTTPRTHandlePost(
      String rawBody,
      HTTPReqsMetadata requestMetadata,
      String mcpSessIdHeader) throws TimeoutException {

    ensureStarted();

    RPCReqsPayl request = rpcJsonEntrypoint.RPCJsonEntryParseReqs(rawBody);

    HTTPTranspSess httpSession = resolveSessionForPost(request, requestMetadata, mcpSessIdHeader);
    TranspSess transportSession = httpSession.HtsGetTranspSess();

    runtimeMetrics.RTMetricsIncrementCounter("http.requests.in.total");
    logger.ServerLogInfoObserved(
        ServerLogger.Component.RUNTIME,
        obsCtxFactory.ObservCtxFactoryForRpc(
            httpSession,
            RPCRouter.RPCRouterResolveCorrelaId(request),
            request.RPCReqPlGetMet()),
        "RECEIVE",
        "HTTP_REQUEST_RECEIVED",
        "StreamableHTTPRTOrch: HTTP POST request received");

    if (request.RPCReqPlIsNotification()) {
      messageHandler.accept(transportSession, rawBody);
      return new PostResult(202, null, maybeSessionHeaderValue(request, httpSession, mcpSessIdHeader));
    }

    String correlationId = RPCRouter.RPCRouterResolveCorrelaId(request);
    String pendingKey = pendingKey(transportSession, correlationId);
    CompletableFuture<String> responseFuture = new CompletableFuture<>();
    StreamableHTTPRTRegisterPendingResponse(
        request,
        transportSession,
        correlationId,
        pendingKey,
        responseFuture);

    try {
      messageHandler.accept(transportSession, rawBody);
      String responseBody = responseFuture.get(
          httpTranspSupport.HTTPSupStreamablePostRespTimeoutMillis(),
          TimeUnit.MILLISECONDS);

      return new PostResult(
          200,
          responseBody,
          maybeSessionHeaderValue(request, httpSession, mcpSessIdHeader));
    } catch (TimeoutException timeoutException) {
      pendingResponses.remove(pendingKey);
      runtimeMetrics.RTMetricsSetGauge("http.responses.pending", pendingResponses.size());
      runtimeMetrics.RTMetricsIncrementCounter("http.transport.timeouts.total");
      transportSession.TranspSessRecordTimedOut(correlationId);
      throw timeoutException;
    } catch (Exception ex) {
      pendingResponses.remove(pendingKey);
      runtimeMetrics.RTMetricsSetGauge("http.responses.pending", pendingResponses.size());
      transportSession.TranspSessRecordAborted(correlationId);
      throw new IllegalStateException("Streamable HTTP request handling failed", ex);
    }
  }

  SseEmitter StreamableHTTPRTOpenStream(
      String mcpSessIdHeader,
      HTTPReqsMetadata requestMetadata) {

    ensureStarted();

    HTTPTranspSess session = requireExistingSess(mcpSessIdHeader);
    session.HtsUpdLastReqsMetadata(requestMetadata);

    if (!sessionsWithOpenedStream.add(session.HtsGetMcpSessId())) {
      runtimeMetrics.RTMetricsIncrementCounter("http.reconnects.total");
    }

    if (!streamEmitters.containsKey(session.HtsGetMcpSessId())
        && streamEmitters.size() >= httpTranspSupport.HTTPSupStreamableMaxActiveStreams()) {
      runtimeMetrics.RTMetricsIncrementCounter("http.capacity.rejected.total");
      throw new TranspCapacityExceededExcep("Streamable HTTP stream capacity reached");
    }

    SseEmitter previousEmitter = streamEmitters.remove(session.HtsGetMcpSessId());
    if (previousEmitter != null) {
      previousEmitter.complete();
    }

    SseEmitter emitter = new SseEmitter(0L);
    streamEmitters.put(session.HtsGetMcpSessId(), emitter);
    runtimeMetrics.RTMetricsSetGauge("http.streams.active", streamEmitters.size());

    ObservCtx context = obsCtxFactory.ObservCtxFactoryFromHTTPTranspSess(session);
    logger.ServerLogInfoObserved(
        ServerLogger.Component.RUNTIME,
        context,
        "OPEN",
        "HTTP_STREAM_OPENED",
        "StreamableHTTPRTOrch: SSE stream opened");

    emitter.onCompletion(() -> StreamableHTTPRTHandleEmitterClosed(session.HtsGetMcpSessId(), context));
    emitter.onTimeout(() -> StreamableHTTPRTHandleEmitterClosed(session.HtsGetMcpSessId(), context));
    emitter.onError(ex -> StreamableHTTPRTHandleEmitterError(session, ex));

    try {
      emitter.send(SseEmitter.event()
          .id(sseEventId(session))
          .name("ready")
          .reconnectTime(1_000L)
          .data("{\"ready\":true}", MediaType.APPLICATION_JSON));
    } catch (IOException ex) {
      throw new IllegalStateException("Failed to initialize SSE stream", ex);
    }

    StreamableHTTPRTFlushBufferedNotifications(session, emitter);
    return emitter;
  }

  boolean StreamableHTTPRTHandleDelete(String mcpSessIdHeader) {

    ensureStarted();

    HTTPTranspSess session = requireExistingSess(mcpSessIdHeader);
    return StreamableHTTPRTCloseSess(session);
  }

  boolean StreamableHTTPRTCompletePendingResponse(
      TranspSess session,
      String responseJson,
      String responseCorrelationId) {

    PendingResponse pendingResponse = pendingResponses.remove(pendingKey(session, responseCorrelationId));
    if (pendingResponse == null) {
      return false;
    }

    long sendStartedAt = System.nanoTime();
    pendingResponse.future().complete(responseJson);
    session.TranspSessRecordResponded(pendingResponse.correlationId());
    runtimeMetrics.RTMetricsSetGauge("http.responses.pending", pendingResponses.size());
    TranspOutbTelemSupport.TranspOutbTelemRecordDuration(
        runtimeMetrics,
        "http.transport.outbound.duration",
        sendStartedAt);
    return true;
  }

  void StreamableHTTPRTPublishNotification(
      TranspSess session,
      String notificationJson) {

    HTTPTranspSess httpSession = requireExistingSess(session.TranspSessGetMcpSessId());
    Queue<String> queue = bufferedNotifications.computeIfAbsent(
        httpSession.HtsGetMcpSessId(),
        ignored -> new ConcurrentLinkedQueue<>());

    SseEmitter emitter = streamEmitters.get(httpSession.HtsGetMcpSessId());

    if (emitter == null) {
      StreamableHTTPRTEnqueueBufferedNotification(httpSession, queue, notificationJson);
      return;
    }

    try {
      long sendStartedAt = System.nanoTime();
      emitter.send(SseEmitter.event()
          .id(sseEventId(httpSession))
          .name(HTTPTranspSupport.SSE_EVENT_MESSAGE)
          .data(notificationJson, MediaType.APPLICATION_JSON));

      TranspOutbTelemSupport.TranspOutbTelemRecordSent(
          runtimeMetrics,
          HTTPTranspCfg.TRANSPORT_STREAMABLE_HTTP,
          "http.transport.outbound.duration",
          sendStartedAt);
      runtimeMetrics.RTMetricsSetGauge("http.notifications.buffered", StreamableHTTPRTCountBufferedNotifications());
      logger.ServerLogInfoObserved(
          ServerLogger.Component.RUNTIME,
          obsCtxFactory.ObservCtxFactoryFromHTTPTranspSess(httpSession),
          "SEND",
          "HTTP_RESPONSE_SENT",
          "StreamableHTTPRTOrch: SSE notification sent");
    } catch (IOException ex) {
      StreamableHTTPRTEnqueueBufferedNotification(httpSession, queue, notificationJson);
      streamEmitters.remove(httpSession.HtsGetMcpSessId(), emitter);
      TranspOutbTelemSupport.TranspOutbTelemRecordErr(
          runtimeMetrics,
          telemetryMetrics,
          obsCtxFactory.ObservCtxFactoryWithErrType(
              obsCtxFactory.ObservCtxFactoryFromHTTPTranspSess(httpSession),
              "TRANSPORT_ERROR"),
          HTTPTranspCfg.TRANSPORT_STREAMABLE_HTTP);
      runtimeMetrics.RTMetricsSetGauge("http.streams.active", streamEmitters.size());
      logger.ServerLogErrorObserved(
          ServerLogger.Component.RUNTIME,
          obsCtxFactory.ObservCtxFactoryWithErrType(
              obsCtxFactory.ObservCtxFactoryFromHTTPTranspSess(httpSession),
              "TRANSPORT_ERROR"),
          "ERROR",
          "HTTP_TRANSPORT_ERROR",
          "StreamableHTTPRTOrch: failed to send SSE notification: " + ex.getMessage(),
          "TRANSPORT_ERROR",
          ex);
    }
  }

  private HTTPTranspSess resolveSessionForPost(
      RPCReqsPayl request,
      HTTPReqsMetadata requestMetadata,
      String mcpSessIdHeader) {

    Objects.requireNonNull(request, "request");
    Objects.requireNonNull(requestMetadata, "requestMetadata");

    if (RPCMetName.INITIALIZE.equals(request.RPCReqPlGetMet())) {
      if (StreamableHTTPProtocolGuard.hasSessionHeader(mcpSessIdHeader)) {
        throw new IllegalArgumentException(RPCMetName.INITIALIZE + " must not include Mcp-Session-Id");
      }

      if (httpSessBindingReg.getActiveSessCount() >= httpTranspSupport.HTTPSupStreamableMaxActiveSessions()) {
        runtimeMetrics.RTMetricsIncrementCounter("http.capacity.rejected.total");
        throw new TranspCapacityExceededExcep("Streamable HTTP session capacity reached");
      }

      HTTPTranspSess session = httpTranspSupport.HttpSupCreateSession(
          HTTPTranspCfg.TRANSPORT_STREAMABLE_HTTP,
          requestMetadata,
          OperatingSurface.MCP_DIRECT);

      httpSessBindingReg.register(session);
      runtimeMetrics.RTMetricsSetGauge("http.sessions.active", httpSessBindingReg.getActiveSessCount());
      sessionOpenHandler.accept(session.HtsGetTranspSess());
      return session;
    }

    HTTPTranspSess session = requireExistingSess(mcpSessIdHeader);
    session.HtsUpdLastReqsMetadata(requestMetadata);
    return session;
  }

  private HTTPTranspSess requireExistingSess(String mcpSessIdHeader) {

    String requiredSessionHeader = StreamableHTTPProtocolGuard.requireSessionHeader(mcpSessIdHeader);

    HTTPTranspSess session = httpSessBindingReg.getByMcpSessId(requiredSessionHeader);
    if (session == null) {
      throw new NoSuchElementException("Unknown MCP session: " + requiredSessionHeader);
    }

    return session;
  }

  private boolean StreamableHTTPRTCloseSess(HTTPTranspSess session) {

    HTTPTranspSess removed = httpSessBindingReg.unregisterByMcpSessId(session.HtsGetMcpSessIdObject());
    if (removed == null) {
      return false;
    }

    SseEmitter emitter = streamEmitters.remove(removed.HtsGetMcpSessId());
    if (emitter != null) {
      emitter.complete();
    }

    bufferedNotifications.remove(removed.HtsGetMcpSessId());
    sessionsWithOpenedStream.remove(removed.HtsGetMcpSessId());
    String pendingPrefix = removed.HtsGetTranspConnId() + "|";
    pendingResponses.entrySet().removeIf(entry -> {
      if (!entry.getKey().startsWith(pendingPrefix)) {
        return false;
      }

      PendingResponse pendingResponse = entry.getValue();
      if (pendingResponse != null) {
        removed.HtsGetTranspSess().TranspSessRecordAborted(pendingResponse.correlationId());
        pendingResponse.future().completeExceptionally(
            new IllegalStateException("Streamable HTTP session closed before response delivery"));
      }

      return true;
    });
    runtimeMetrics.RTMetricsSetGauge("http.responses.pending", pendingResponses.size());
    removed.HtsGetTranspSess().TranspSessClose();
    runtimeMetrics.RTMetricsIncrementCounter(
        TranspSignalModel.TransSigSessClosedMetricName(HTTPTranspCfg.TRANSPORT_STREAMABLE_HTTP));
    sessionCloseHandler.accept(removed.HtsGetTranspSess());
    runtimeMetrics.RTMetricsSetGauge("http.sessions.active", httpSessBindingReg.getActiveSessCount());
    runtimeMetrics.RTMetricsSetGauge("http.streams.active", streamEmitters.size());
    runtimeMetrics.RTMetricsSetGauge("http.notifications.buffered", StreamableHTTPRTCountBufferedNotifications());
    return true;
  }

  private void StreamableHTTPRTFlushBufferedNotifications(
      HTTPTranspSess session,
      SseEmitter emitter) {

    Queue<String> queue = bufferedNotifications.computeIfAbsent(
        session.HtsGetMcpSessId(),
        ignored -> new ConcurrentLinkedQueue<>());

    String notificationJson;
    while ((notificationJson = queue.poll()) != null) {
      try {
        long sendStartedAt = System.nanoTime();
        emitter.send(SseEmitter.event()
            .id(sseEventId(session))
            .name(HTTPTranspSupport.SSE_EVENT_MESSAGE)
            .data(notificationJson, MediaType.APPLICATION_JSON));
        TranspOutbTelemSupport.TranspOutbTelemRecordDuration(
            runtimeMetrics,
            "http.transport.outbound.duration",
            sendStartedAt);
      } catch (IOException ex) {
        StreamableHTTPRTEnqueueBufferedNotification(session, queue, notificationJson);
        throw new IllegalStateException("Failed to flush buffered SSE notifications", ex);
      }
    }

    runtimeMetrics.RTMetricsSetGauge("http.notifications.buffered", StreamableHTTPRTCountBufferedNotifications());
  }

  private void StreamableHTTPRTHandleEmitterClosed(
      String mcpSessId,
      ObservCtx context) {

    streamEmitters.remove(mcpSessId);
    runtimeMetrics.RTMetricsSetGauge("http.streams.active", streamEmitters.size());

    logger.ServerLogInfoObserved(
        ServerLogger.Component.RUNTIME,
        context,
        "CLOSE",
        "HTTP_STREAM_CLOSED",
        "StreamableHTTPRTOrch: SSE stream closed");
  }

  private void StreamableHTTPRTHandleEmitterError(
      HTTPTranspSess session,
      Throwable throwable) {

    streamEmitters.remove(session.HtsGetMcpSessId());
    TranspOutbTelemSupport.TranspOutbTelemRecordErr(
        runtimeMetrics,
        telemetryMetrics,
        obsCtxFactory.ObservCtxFactoryWithErrType(
            obsCtxFactory.ObservCtxFactoryFromHTTPTranspSess(session),
            "TRANSPORT_ERROR"),
        HTTPTranspCfg.TRANSPORT_STREAMABLE_HTTP);
    runtimeMetrics.RTMetricsSetGauge("http.streams.active", streamEmitters.size());

    logger.ServerLogErrorObserved(
        ServerLogger.Component.RUNTIME,
        obsCtxFactory.ObservCtxFactoryWithErrType(
            obsCtxFactory.ObservCtxFactoryFromHTTPTranspSess(session),
            "TRANSPORT_ERROR"),
        "ERROR",
        "HTTP_TRANSPORT_ERROR",
        "StreamableHTTPRTOrch: SSE stream failed: " + throwable.getMessage(),
        "TRANSPORT_ERROR",
        throwable);
  }

  private String maybeSessionHeaderValue(
      RPCReqsPayl request,
      HTTPTranspSess session,
      String mcpSessIdHeader) {

    if (!RPCMetName.INITIALIZE.equals(request.RPCReqPlGetMet())) {
      return null;
    }

    if (StreamableHTTPProtocolGuard.hasSessionHeader(mcpSessIdHeader)) {
      return null;
    }

    return session.HtsGetMcpSessId();
  }

  private static String pendingKey(TranspSess session, String correlationId) {
    return session.TranspSessGetTranspConnId() + "|" + correlationId;
  }

  private static String sseEventId(HTTPTranspSess session) {
    return session.HtsGetMcpSessId() + ":" + UUID.randomUUID();
  }

  private void StreamableHTTPRTRegisterPendingResponse(
      RPCReqsPayl request,
      TranspSess transportSession,
      String correlationId,
      String pendingKey,
      CompletableFuture<String> responseFuture) {

    PendingResponse existing = pendingResponses.putIfAbsent(
        pendingKey,
        new PendingResponse(correlationId, responseFuture));

    if (existing == null) {
      runtimeMetrics.RTMetricsSetGauge("http.responses.pending", pendingResponses.size());
      return;
    }

    transportSession.TranspSessRecordRejected(correlationId);
    runtimeMetrics.RTMetricsIncrementCounter(METRIC_DUPLICATE_INFLIGHT_REJECTED_TOTAL);

    logger.ServerLogWarnObserved(
        ServerLogger.Component.RUNTIME,
        obsCtxFactory.ObservCtxFactoryWithErrType(
            obsCtxFactory.ObservCtxFactoryForRpc(
                transportSession,
                correlationId,
                request.RPCReqPlGetMet()),
            "DUPLICATE_REQUEST"),
        "REJECT",
        "HTTP_DUPLICATE_INFLIGHT_REQUEST_REJECTED",
        "StreamableHTTPRTOrch: duplicate in-flight request id rejected for correlationId=" + correlationId,
        "DUPLICATE_REQUEST");

    throw new IllegalArgumentException(
        "Duplicate in-flight request id for streamable HTTP session: " + correlationId);
  }

  private void StreamableHTTPRTEnqueueBufferedNotification(
      HTTPTranspSess session,
      Queue<String> queue,
      String notificationJson) {

    if (queue.size() >= httpTranspSupport.HTTPSupStreamableMaxBufferedOutbMessagesPerSess()) {
      runtimeMetrics.RTMetricsIncrementCounter("http.capacity.rejected.total");
      StreamableHTTPRTCloseSess(session);
      throw new TranspCapacityExceededExcep(
          "Streamable HTTP buffered outbound capacity reached for session " + session.HtsGetMcpSessId());
    }

    queue.offer(notificationJson);
    runtimeMetrics.RTMetricsSetGauge("http.notifications.buffered", StreamableHTTPRTCountBufferedNotifications());
  }

  private void ensureStarted() {
    if (!started.get()) {
      throw new IllegalStateException("Streamable HTTP transport is not started");
    }
  }

  private long StreamableHTTPRTCountBufferedNotifications() {

    long totalBuffered = 0L;

    for (Queue<String> queue : bufferedNotifications.values()) {
      totalBuffered += queue.size();
    }

    return totalBuffered;
  }
}
