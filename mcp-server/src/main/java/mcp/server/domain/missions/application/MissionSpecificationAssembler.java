package mcp.server.domain.missions.application;

import java.time.LocalDate;
import java.util.List;

import org.springframework.stereotype.Component;

import mcp.server.domain.missions.application.intake.MissionProposalIntake;
import mcp.server.domain.missions.application.intake.MissionProposalValidationService;
import mcp.server.domain.missions.persistence.MissionEntity;
import mcp.server.domain.missions.persistence.MissionProposalEntity;
import mcp.server.domain.missions.persistence.MissionSlotEntity;
import static mcp.server.domain.shared_kernel.validation.ApplicationInputValidation.reject;
import static mcp.server.domain.shared_kernel.validation.ApplicationInputValidation.parseDate;

@Component
public class MissionSpecificationAssembler {

        private final MissionSlotSpecificationAssembler slotAssembler;
        private final MissionProposalValidationService proposalValidationService;

        public MissionSpecificationAssembler(
                        MissionSlotSpecificationAssembler slotAssembler,
                        MissionProposalValidationService proposalValidationService) {
                this.slotAssembler = slotAssembler;
                this.proposalValidationService = proposalValidationService;
        }

        public MissionSpecification.SpecificationView toSpecification(
                        MissionProposalIntake.ProposalInput request) {

                proposalValidationService.requireMissionProposal(request);
                LocalDate startDate = parseDate(request.startDate(), "startDate");
                LocalDate endDate = parseDate(request.endDate(), "endDate");
                if (endDate.isBefore(startDate)) {
                        throw reject("endDate must be the same as or after startDate");
                }

                List<MissionSpecification.SlotSpecificationView> missionSlots = slotAssembler
                                .toSpecificationViewsFromInputSlots(request.missionSlots());
                MissionSpecification.PresentationView presentation = toMissionPresentationView(
                                request.missionPresentation());

                return new MissionSpecification.SpecificationView(
                                request.customerName().trim(),
                                request.customerEmail().trim(),
                                request.missionTitle().trim(),
                                missionSlots,
                                startDate.toString(),
                                endDate.toString(),
                                request.workMode().trim().toUpperCase(),
                                presentation);
        }

        public MissionSpecification.SpecificationView toSpecification(
                        MissionProposalReview.ProposalEditInput request) {

                return toSpecification(new MissionProposalIntake.ProposalInput(
                                request.customerName(),
                                request.customerEmail(),
                                request.missionTitle(),
                                request.missionSlots(),
                                request.startDate(),
                                request.endDate(),
                                request.workMode(),
                                request.missionPresentation()));
        }

        public MissionSpecification.SpecificationView toSpecification(
                        MissionProposalEntity entity) {

                return new MissionSpecification.SpecificationView(
                                entity.getCustomerName(),
                                entity.getCustomerEmail(),
                                entity.getMissionTitle(),
                                slotAssembler.toSpecificationViewsFromProposalSlots(entity.getMissionSlots()),
                                entity.getStartDate().toString(),
                                entity.getEndDate().toString(),
                                entity.getWorkMode(),
                                toMissionPresentationView(
                                                entity.getPresentationOneDayAtWork(),
                                                entity.getPresentationTechnicalLandscape(),
                                                entity.getPresentationWhoWeAreLookingFor(),
                                                entity.getPresentationWhatWeOffer(),
                                                entity.getPresentationAboutCustomer(),
                                                entity.getPresentationRecruitmentProcess()));
        }

        public MissionSpecification.SpecificationView toSpecification(
                        MissionEntity mission,
                        List<MissionSlotEntity> slots) {

                return new MissionSpecification.SpecificationView(
                                mission.getCustomerName(),
                                mission.getCustomerEmail(),
                                mission.getMissionTitle(),
                                slotAssembler.toSpecificationViewsFromRegisteredSlots(slots),
                                mission.getStartDate().toString(),
                                mission.getEndDate().toString(),
                                mission.getWorkMode(),
                                toMissionPresentationView(
                                                mission.getPresentationOneDayAtWork(),
                                                mission.getPresentationTechnicalLandscape(),
                                                mission.getPresentationWhoWeAreLookingFor(),
                                                mission.getPresentationWhatWeOffer(),
                                                mission.getPresentationAboutCustomer(),
                                                mission.getPresentationRecruitmentProcess()));
        }

        private MissionSpecification.PresentationView toMissionPresentationView(
                        MissionSpecification.PresentationInput input) {

                MissionSpecification.PresentationInput normalized = normalizeMissionPresentation(
                                input);
                return new MissionSpecification.PresentationView(
                                normalized.oneDayAtWork(),
                                normalized.technicalLandscape(),
                                normalized.whoWeAreLookingFor(),
                                normalized.whatWeOffer(),
                                normalized.aboutCustomer(),
                                normalized.recruitmentProcess());
        }

        private MissionSpecification.PresentationView toMissionPresentationView(
                        String oneDayAtWork,
                        String technicalLandscape,
                        String whoWeAreLookingFor,
                        String whatWeOffer,
                        String aboutCustomer,
                        String recruitmentProcess) {

                return toMissionPresentationView(
                                new MissionSpecification.PresentationInput(
                                                oneDayAtWork,
                                                technicalLandscape,
                                                whoWeAreLookingFor,
                                                whatWeOffer,
                                                aboutCustomer,
                                                recruitmentProcess));
        }

        private MissionSpecification.PresentationInput normalizeMissionPresentation(
                        MissionSpecification.PresentationInput input) {

                MissionSpecification.PresentationInput defaults = MissionSpecification
                                .defaultPresentation();
                MissionSpecification.PresentationInput source = input == null ? defaults : input;
                return new MissionSpecification.PresentationInput(
                                normalizeMissionPresentationText(
                                                source.oneDayAtWork(),
                                                defaults.oneDayAtWork(),
                                                "missionPresentation.oneDayAtWork"),
                                normalizeMissionPresentationText(
                                                source.technicalLandscape(),
                                                defaults.technicalLandscape(),
                                                "missionPresentation.technicalLandscape"),
                                normalizeMissionPresentationText(
                                                source.whoWeAreLookingFor(),
                                                defaults.whoWeAreLookingFor(),
                                                "missionPresentation.whoWeAreLookingFor"),
                                normalizeMissionPresentationText(
                                                source.whatWeOffer(),
                                                defaults.whatWeOffer(),
                                                "missionPresentation.whatWeOffer"),
                                normalizeMissionPresentationText(
                                                source.aboutCustomer(),
                                                defaults.aboutCustomer(),
                                                "missionPresentation.aboutCustomer"),
                                normalizeMissionPresentationText(
                                                source.recruitmentProcess(),
                                                defaults.recruitmentProcess(),
                                                "missionPresentation.recruitmentProcess"));
        }

        private String normalizeMissionPresentationText(
                        String value,
                        String defaultValue,
                        String fieldName) {

                String normalized = value == null ? "" : value.trim().replaceAll("\\s+", " ");
                if (normalized.isBlank()) {
                        normalized = defaultValue;
                }
                if (wordCount(normalized) > MissionSpecification.PRESENTATION_MAX_WORDS) {
                        throw reject(fieldName + " must be 100 words or fewer");
                }
                return normalized;
        }

        private int wordCount(String value) {
                if (value == null || value.isBlank()) {
                        return 0;
                }
                return value.trim().split("\\s+").length;
        }
}
