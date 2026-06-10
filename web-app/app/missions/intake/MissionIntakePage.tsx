import { Link } from "react-router";
import {
  MissionIntakeBasicsSection,
  MissionIntakeCustomerSection,
  MissionIntakePresentationSection,
  MissionIntakeSubmitSection,
} from "~/missions/intake/MissionIntakeFormSections";
import { MissionIntakePreviewPanel } from "~/missions/intake/MissionIntakePreviewPanel";
import { MissionIntakeSlotsSection } from "~/missions/intake/MissionIntakeSlotsSection";
import { MISSION_INTAKE_CHANGE_REQUEST_CONTACT_EMAIL } from "~/missions/intake/missionIntakeContact";
import { useMissionIntakeWorkflow } from "~/missions/intake/useMissionIntakeWorkflow";

export default function MissionIntakePage() {
  const {
    form,
    referenceData,
    loadingReferences,
    submitting,
    error,
    receiptId,
    roleRequirementsText,
    roleRequirementsWords,
    roleRequirementsMaxWords,
    missionTitleWords,
    missionTitleMaxWords,
    previewingRequirements,
    requirementsPreview,
    requirementsPreviewError,
    primarySkillRows,
    secondarySkillRows,
    roleTitle,
    updateField,
    updatePresentationField,
    updateMissionSlot,
    updateSkillRequirement,
    addMissionSlot,
    removeMissionSlot,
    addSkillRequirement,
    removeSkillRequirement,
    previewFieldClass,
    updateRoleRequirementsText,
    previewRoleRequirements,
    submitForm,
  } = useMissionIntakeWorkflow();

  return (
    <div className="min-h-screen px-6 py-8">
      <main className="mx-auto max-w-5xl">
        <Link to="/" className="text-link text-sm font-medium">
          Till startsidan
        </Link>

        <section className="mt-8">
          <p className="text-label text-xs font-semibold uppercase tracking-[0.22em]">
            Hitta Konsult
          </p>
          <h1 className="text-title mt-3 text-4xl font-semibold">
            Registrering av Uppdrag
          </h1>
          <p className="text-muted mt-4 max-w-2xl text-sm leading-6">
            En position representerar en specifik konsultplats inom uppdraget.
          </p>
        </section>

        <form
          onSubmit={submitForm}
          className="panel mt-8 grid gap-6 rounded-2xl p-6"
        >
          <MissionIntakeCustomerSection
            form={form}
            previewFieldClass={previewFieldClass}
            updateField={updateField}
          />

          <MissionIntakeBasicsSection
            form={form}
            missionTitleWords={missionTitleWords}
            missionTitleMaxWords={missionTitleMaxWords}
            previewFieldClass={previewFieldClass}
            updateField={updateField}
          />

          <MissionIntakePreviewPanel
            form={form}
            roleRequirementsText={roleRequirementsText}
            roleRequirementsWords={roleRequirementsWords}
            roleRequirementsMaxWords={roleRequirementsMaxWords}
            previewingRequirements={previewingRequirements}
            loadingReferences={loadingReferences}
            requirementsPreview={requirementsPreview}
            requirementsPreviewError={requirementsPreviewError}
            primarySkillRows={primarySkillRows}
            secondarySkillRows={secondarySkillRows}
            roleTitle={roleTitle}
            onRoleRequirementsTextChange={updateRoleRequirementsText}
            onPreviewRoleRequirements={previewRoleRequirements}
          />

          <MissionIntakeSlotsSection
            form={form}
            referenceData={referenceData}
            loadingReferences={loadingReferences}
            previewFieldClass={previewFieldClass}
            updateMissionSlot={updateMissionSlot}
            updateSkillRequirement={updateSkillRequirement}
            addMissionSlot={addMissionSlot}
            removeMissionSlot={removeMissionSlot}
            addSkillRequirement={addSkillRequirement}
            removeSkillRequirement={removeSkillRequirement}
          />

          <MissionIntakePresentationSection
            form={form}
            updatePresentationField={updatePresentationField}
          />

          <MissionIntakeSubmitSection
            error={error}
            receiptId={receiptId}
            contactEmail={MISSION_INTAKE_CHANGE_REQUEST_CONTACT_EMAIL}
            submitting={submitting}
            loadingReferences={loadingReferences}
          />
        </form>
      </main>
    </div>
  );
}
