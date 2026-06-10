import type { MissionPresentationInput } from "~/missions/types";

export const MISSION_PRESENTATION_MAX_WORDS = 100;

export const MISSION_PRESENTATION_DEFAULTS: MissionPresentationInput = {
  oneDayAtWork:
    "Lorem ipsum dolor sit amet, consectetur adipiscing elit. Integer vitae sem at arcu luctus facilisis.",
  technicalLandscape:
    "Lorem ipsum dolor sit amet, consectetur adipiscing elit. Curabitur sed nibh ac justo tristique luctus.",
  whoWeAreLookingFor:
    "Lorem ipsum dolor sit amet, consectetur adipiscing elit. Praesent vitae risus eget mi posuere facilisis.",
  whatWeOffer:
    "Lorem ipsum dolor sit amet, consectetur adipiscing elit. Aenean vitae augue id mauris volutpat gravida.",
  aboutCustomer:
    "Lorem ipsum dolor sit amet, consectetur adipiscing elit. Donec non erat sed ipsum gravida consequat.",
  recruitmentProcess:
    "Lorem ipsum dolor sit amet, consectetur adipiscing elit. Sed porta nulla vitae libero cursus hendrerit.",
};

export const MISSION_PRESENTATION_FIELDS: Array<{
  key: keyof MissionPresentationInput;
  label: string;
}> = [
  { key: "oneDayAtWork", label: "En dag på jobbet" },
  { key: "technicalLandscape", label: "Vårt tekniska landskap" },
  { key: "whoWeAreLookingFor", label: "Vem söker vi?" },
  { key: "whatWeOffer", label: "Vad vi erbjuder" },
  { key: "aboutCustomer", label: "Om uppdragsgivaren" },
  { key: "recruitmentProcess", label: "Rekryteringsprocess" },
];

export function defaultMissionPresentation(): MissionPresentationInput {
  return { ...MISSION_PRESENTATION_DEFAULTS };
}
