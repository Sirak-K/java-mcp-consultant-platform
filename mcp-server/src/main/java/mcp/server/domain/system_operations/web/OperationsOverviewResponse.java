package mcp.server.domain.system_operations.web;

public record OperationsOverviewResponse(
    int activeSessions,
    String lastSessionAt,
    long customerCount,
    long candidateCount,
    long totalMissions) {
}
