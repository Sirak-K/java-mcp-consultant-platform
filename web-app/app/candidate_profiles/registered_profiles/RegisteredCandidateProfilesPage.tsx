import { useEffect, useState } from "react";
import {
  candidateProfilesApi,
  candidateProfilesApiPaths,
} from "~/candidate_profiles/api/candidateProfilesApi";
import {
  buildCandidateApplicationEditInput,
  buildCandidateApplicationEditWorkingCopy,
} from "~/candidate_profiles/profile_form/candidateProfileFormState";
import { CandidateProfileReviewMissionMatches } from "~/candidate_profiles/profile_review/CandidateProfileReviewMissionMatches";
import { CandidateProfileReviewSummary } from "~/candidate_profiles/profile_review/CandidateProfileReviewSummary";
import type {
  CandidateApplicationEditInput,
  CandidateProfileReviewItem,
} from "~/candidate_profiles/types";
import { referenceDataApi } from "~/reference_data/api/referenceDataApi";
import type { MarketplaceReferenceData } from "~/reference_data/types";
import { OperationsIntakeReviewQueue } from "~/system_operations/review/OperationsIntakeReviewQueue";
import { RegisteredCandidateProfileEditForm } from "./RegisteredCandidateProfileEditForm";

const registeredCandidateProfileContractPath = `${candidateProfilesApiPaths.registeredCandidateProfiles}/{candidateProfileId}`;

const registeredProfileContract = [
  `GET ${candidateProfilesApiPaths.registeredCandidateProfiles} -> RegisteredCandidateProfileView[]`,
  `PUT ${registeredCandidateProfileContractPath} -> RegisteredCandidateProfileView`,
  `DELETE ${registeredCandidateProfileContractPath} -> 204 No Content`,
];

function reviewItemId(item: CandidateProfileReviewItem): number {
  return "candidateProfileId" in item ? item.candidateProfileId : item.id;
}

function registeredCandidateProfileTitle(
  item: CandidateProfileReviewItem,
): string {
  return `Kandidat #${reviewItemId(item)}`;
}

export default function RegisteredCandidateProfilesPage() {
  const [referenceData, setReferenceData] =
    useState<MarketplaceReferenceData | null>(null);

  useEffect(() => {
    let mounted = true;
    referenceDataApi.referenceData().then((data) => {
      if (mounted) {
        setReferenceData(data);
      }
    });
    return () => {
      mounted = false;
    };
  }, []);

  return (
    <OperationsIntakeReviewQueue<
      CandidateProfileReviewItem,
      CandidateApplicationEditInput
    >
      title="Kandidatpool"
      description=""
      contentClassName="mx-auto w-full max-w-6xl"
      contract={registeredProfileContract}
      loadItems={candidateProfilesApi.listRegisteredCandidateProfiles}
      getItemId={reviewItemId}
      getItemTitle={registeredCandidateProfileTitle}
      editItem={candidateProfilesApi.editRegisteredCandidateProfile}
      deleteAction={{
        deleteItem: candidateProfilesApi.deleteRegisteredCandidateProfile,
        getConfirmationMessage: (item) =>
          `Ta bort ${registeredCandidateProfileTitle(item)}? Associerad kandidatprofil, matchningar och artefakter tas också bort.`,
        errorMessage: "Kunde inte ta bort kandidatprofilen.",
      }}
      buildEditInput={buildCandidateApplicationEditInput}
      buildEditWorkingCopy={buildCandidateApplicationEditWorkingCopy}
      renderSummary={(item) => (
        <CandidateProfileReviewSummary item={item} isRegisteredProfiles />
      )}
      renderMatches={(item) => (
        <CandidateProfileReviewMissionMatches item={item} />
      )}
      renderEditFields={(_item, editWorkingCopy, updateEditWorkingCopy) => (
        <RegisteredCandidateProfileEditForm
          editWorkingCopy={editWorkingCopy}
          updateEditWorkingCopy={updateEditWorkingCopy}
          referenceData={referenceData}
        />
      )}
    />
  );
}
