package mcp.server.domain.missions.web;

import java.util.List;
import java.util.Objects;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

import mcp.server.domain.missions.application.MissionQueryService;

@RestController
public final class RegisteredMissionQueryController {

  private final MissionQueryService missionQueryService;

  public RegisteredMissionQueryController(MissionQueryService missionQueryService) {
    this.missionQueryService = Objects.requireNonNull(missionQueryService, "missionQueryService");
  }

  @GetMapping("/api/ops/registered-missions")
  public List<RegisteredMissionWebContract.RegisteredMissionView> registeredMissions() {
    return missionQueryService.registeredMissions().stream()
        .map(RegisteredMissionWebContract::fromApplication)
        .toList();
  }
}
