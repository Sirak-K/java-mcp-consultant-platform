# Builds the backend image from source so the container does not depend on a locally generated target/ jar.
FROM --platform=linux/amd64 maven:3.9.11-eclipse-temurin-21 AS build

WORKDIR /workspace

COPY pom.xml .
COPY mcp-server/pom.xml mcp-server/pom.xml
RUN mvn -B -q -pl mcp-server -am dependency:go-offline

COPY mcp-server/src mcp-server/src
RUN mvn -B -q -pl mcp-server -am -DskipTests package

# Prepare writable runtime paths before switching to a minimal runtime image.
FROM --platform=linux/amd64 busybox:1.36.1-musl AS runtime-prep

RUN mkdir -p /app /var/opt/mcp/logs && chown -R 65532:65532 /app /var/opt/mcp

# Platform-pinned to prevent accidental cross-platform layer pulls.
# Pin to a digest at release time when the public image promotion process is established.
FROM --platform=linux/amd64 gcr.io/distroless/java21-debian12:nonroot

ARG APP_VERSION=dev
ARG VCS_REF=unknown

WORKDIR /app

LABEL org.opencontainers.image.title="java-mcp-consultant-platform" \
      org.opencontainers.image.description="Portfolio-grade Java and Spring Boot MCP server platform." \
      org.opencontainers.image.version="${APP_VERSION}" \
      org.opencontainers.image.revision="${VCS_REF}"

ENV SPRING_PROFILES_ACTIVE=prod \
    JAVA_TOOL_OPTIONS=-Djava.io.tmpdir=/tmp \
    MCP_TRANSPORT_ACTIVE=streamable-http \
    MCP_TRANSPORT_STREAMABLE_HTTP_ENABLED=true \
    MCP_TRANSPORT_STREAMABLE_HTTP_ENDPOINT_PATH=/mcp \
    MCP_TRANSPORT_STREAMABLE_HTTP_REQUIRE_ORIGIN_VALIDATION=true \
    MCP_TRANSPORT_STREAMABLE_HTTP_LOCALHOST_ONLY=false \
    MCP_TRANSPORT_WEBSOCKET_ENABLED=false \
    MCP_TRANSPORT_STDIO_ENABLED=false \
    MCP_FILE_SINK_ALL_PATH=/var/opt/mcp/logs/mcp-server.log \
    MCP_FILE_SINK_ERROR_PATH=/var/opt/mcp/logs/mcp-server-errors.log \
    MCP_AUDIT_SINK_PATH=/var/opt/mcp/logs/mcp-server-audit.log \
    SERVER_PORT=8080

COPY --from=runtime-prep --chown=65532:65532 /app /app
COPY --from=runtime-prep --chown=65532:65532 /var/opt/mcp /var/opt/mcp
COPY --from=build --chown=65532:65532 /workspace/mcp-server/target/mcp-server.jar /app/app.jar
COPY --chown=65532:65532 catalogs /app/catalogs

EXPOSE 8080

VOLUME ["/var/opt/mcp/logs"]

ENTRYPOINT ["java", "-jar", "/app/app.jar"]
