import {
  candidateProfilesApi,
  candidateProfilesApiPaths,
} from "~/candidate_profiles/api/candidateProfilesApi";
import { CandidateProfileReviewMissionMatches } from "~/candidate_profiles/profile_review/CandidateProfileReviewMissionMatches";
import { CandidateProfileReviewSummary } from "~/candidate_profiles/profile_review/CandidateProfileReviewSummary";
import type { CandidateApplicationView } from "~/candidate_profiles/types";
import { OperationsIntakeReviewQueue } from "~/system_operations/review/OperationsIntakeReviewQueue";

const candidateApplicationReviewContract = [
  `GET ${candidateProfilesApiPaths.candidateApplications} -> CandidateApplicationView[]`,
];

export default function CandidateApplicationReviewPage() {
  return (
    <OperationsIntakeReviewQueue<CandidateApplicationView, never>
      title="Kandidatansökningar"
      description="Inskickade kandidatansökningar."
      contentClassName="mx-auto w-full max-w-6xl"
      contract={candidateApplicationReviewContract}
      loadItems={candidateProfilesApi.listCandidateApplications}
      getItemId={(item) => item.id}
      getItemTitle={(item) => `Kandidatansökan #${item.id}`}
      renderSummary={(item) => (
        <CandidateProfileReviewSummary
          item={item}
          isRegisteredProfiles={false}
        />
      )}
      renderMatches={(item) => (
        <CandidateProfileReviewMissionMatches item={item} />
      )}
    />
  );
}
