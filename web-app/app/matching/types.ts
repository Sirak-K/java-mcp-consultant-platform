import type {
  RegisteredCandidateProfileCardView,
} from "~/candidate_profiles/types";
import type { MissionSkillRequirementView } from "~/missions/types";

export interface MatchViewerCandidateMatchView {
  matchId: string | number;
  score?: number | null;
  scoreLabel?: string | null;
  roleMatched?: boolean | null;
  workModeMatched?: boolean | null;
  matchedSkillCount?: number | null;
  requiredSkillCount?: number | null;
  matchedSkills?: string[] | null;
  candidateCard: RegisteredCandidateProfileCardView;
}

export interface MatchScoreBreakdownFactorView {
  factor: string;
  matched: boolean;
  matchedCount: number;
  requiredCount: number;
  scorePerInstance: number;
  points: number;
  evidence: string[];
  note: string;
}

export interface MatchScoreBreakdownView {
  matchId: string | number;
  score: number;
  scoreLabel: string;
  discoveryThreshold: number;
  passedDiscoveryThreshold: boolean;
  decision: string;
  factors: MatchScoreBreakdownFactorView[];
  matchedSkills: string[];
  missingOrWeakFactors: string[];
  matchedAt?: string | null;
}

export interface MatchViewerMissionSlotView {
  missionSlotId?: number | null;
  missionSlotNumber: number;
  roleTitle?: string | null;
  requiredRoleExperienceYears?: number | null;
  requiredSkills?: MissionSkillRequirementView[] | null;
  matches: MatchViewerCandidateMatchView[];
}

export interface MatchViewerMissionView {
  missionId: number;
  missionTitle?: string | null;
  customerEmail?: string | null;
  customerName?: string | null;
  status?: string | null;
  startDate?: string | null;
  endDate?: string | null;
  workMode?: string | null;
  slots: MatchViewerMissionSlotView[];
}

export interface MatchViewerView {
  generatedAt?: string | null;
  missions: MatchViewerMissionView[];
}
