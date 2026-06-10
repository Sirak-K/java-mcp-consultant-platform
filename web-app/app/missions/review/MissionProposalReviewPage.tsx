import { useEffect, useState } from "react";
import { missionsApi, missionsApiPaths } from "~/missions/api/missionsApi";
import { referenceDataApi } from "~/reference_data/api/referenceDataApi";
import type { MissionProposalEditInput, MissionProposalView } from "~/missions/types";
import type { MarketplaceReferenceData } from "~/reference_data/types";
import { OperationsIntakeReviewQueue } from "~/system_operations/review/OperationsIntakeReviewQueue";
import {
  buildMissionProposalEditInput as buildEditInput,
  buildMissionProposalEditWorkingCopy as buildEditWorkingCopy,
} from "~/missions/proposal_form/missionProposalFormState";
import { MissionProposalReviewEditForm } from "~/missions/review/MissionProposalReviewEditForm";
import { MissionProposalReviewMatches } from "~/missions/review/MissionProposalReviewMatches";
import { MissionProposalReviewSummary } from "~/missions/review/MissionProposalReviewSummary";

const missionProposalContractPath = `${missionsApiPaths.missionProposals}/{id}`;
const contract = [
  `GET ${missionsApiPaths.missionProposals} -> MissionProposalView[]`,
  `PUT ${missionProposalContractPath} -> MissionProposalView`,
  `PUT ${missionProposalContractPath}/approve -> MissionProposalView`,
  `PUT ${missionProposalContractPath}/reject -> MissionProposalView`,
];

function missionProposalTitle(item: MissionProposalView): string {
  return `Uppdragsunderlag #${item.id}`;
}

function missionProposalStatusView(item: MissionProposalView) {
  if (item.status === "SUBMITTED") {
    return {
      label: "Väntar på Godkännande",
      className: "chip-status-submitted",
    };
  }

  return { label: item.status };
}

function isMissionProposalReviewComplete(item: MissionProposalView): boolean {
  return item.status === "APPROVED";
}

export default function MissionProposalReviewPage() {
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
      MissionProposalView,
      MissionProposalEditInput
    >
      title={"Uppdragsgranskning"}
      description={"Inskickade uppdragsförfrågningar."}
      contentClassName="mx-auto w-full max-w-6xl"
      contract={contract}
      loadItems={missionsApi.listMissionProposals}
      getItemId={(item) => item.id}
      getItemTitle={missionProposalTitle}
      getItemStatusView={missionProposalStatusView}
      isItemReviewComplete={isMissionProposalReviewComplete}
      editItem={missionsApi.editMissionProposal}
      approveItem={missionsApi.approveMissionProposal}
      rejectItem={missionsApi.rejectMissionProposal}
      buildEditInput={buildEditInput}
      buildEditWorkingCopy={buildEditWorkingCopy}
      renderSummary={(item) => <MissionProposalReviewSummary item={item} />}
      renderMatches={(item) => <MissionProposalReviewMatches item={item} />}
      renderEditFields={(_item, editWorkingCopy, updateEditWorkingCopy) => (
        <MissionProposalReviewEditForm
          editWorkingCopy={editWorkingCopy}
          updateEditWorkingCopy={updateEditWorkingCopy}
          referenceData={referenceData}
        />
      )}
    />
  );
}
