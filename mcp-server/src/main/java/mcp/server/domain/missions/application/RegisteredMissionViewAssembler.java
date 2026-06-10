package mcp.server.domain.missions.application;

import java.time.Instant;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;

import org.springframework.stereotype.Component;

import mcp.server.domain.missions.persistence.MissionEntity;
import mcp.server.domain.missions.persistence.MissionProposalEntity;

@Component
public class RegisteredMissionViewAssembler {

        private static final DateTimeFormatter TIMESTAMP_FORMAT = DateTimeFormatter.ofPattern("yyyy-MM-dd | HH:mm:ss");

        public RegisteredMissionQuery.RegisteredMissionView toRegisteredView(
                        MissionProposalEntity entity,
                        MissionSpecification.SpecificationView specification) {
                return new RegisteredMissionQuery.RegisteredMissionView(
                                entity.getId(),
                                entity.getStatus(),
                                specification.customerName(),
                                specification,
                                formatInstant(entity.getCreatedAt()),
                                formatInstant(entity.getUpdatedAt()));
        }

        public RegisteredMissionQuery.RegisteredMissionView toRegisteredView(
                        MissionEntity mission,
                        MissionSpecification.SpecificationView specification) {
                return new RegisteredMissionQuery.RegisteredMissionView(
                                mission.getId(),
                                mission.getMissionAvailability(),
                                specification.customerName(),
                                specification,
                                null,
                                null);
        }

        private String formatInstant(Instant instant) {
                return instant == null
                                ? null
                                : TIMESTAMP_FORMAT.format(instant.atZone(ZoneId.systemDefault()));
        }
}
