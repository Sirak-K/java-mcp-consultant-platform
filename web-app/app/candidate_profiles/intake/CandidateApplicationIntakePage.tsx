import { Link } from "react-router";
import { CandidateApplicationCertificationSection } from "~/candidate_profiles/intake/CandidateApplicationCertificationSection";
import { CandidateApplicationConsentSection } from "~/candidate_profiles/intake/CandidateApplicationConsentSection";
import { CandidateApplicationContactCvSection } from "~/candidate_profiles/intake/CandidateApplicationContactCvSection";
import { CandidateApplicationEducationSection } from "~/candidate_profiles/intake/CandidateApplicationEducationSection";
import { CandidateApplicationGeneratedSummarySection } from "~/candidate_profiles/intake/CandidateApplicationGeneratedSummarySection";
import { CandidateApplicationPersonalInfoSection } from "~/candidate_profiles/intake/CandidateApplicationPersonalInfoSection";
import { CandidateApplicationProfileSection } from "~/candidate_profiles/intake/CandidateApplicationProfileSection";
import { CandidateApplicationWorkExperienceSection } from "~/candidate_profiles/intake/CandidateApplicationWorkExperienceSection";
import { CandidateApplicationWorkPreferencesSection } from "~/candidate_profiles/intake/CandidateApplicationWorkPreferencesSection";
import { useCandidateApplicationIntakeWorkflow } from "~/candidate_profiles/intake/useCandidateApplicationIntakeWorkflow";

export default function CandidateApplicationIntakePage() {
  const {
    form,
    cvProfile,
    workExperiences,
    educations,
    candidateRoles,
    candidateSkills,
    certifications,
    certificationFeedback,
    referenceData,
    loadingReferences,
    previewingCv,
    cvPreviewStatus,
    cvPreviewError,
    submitting,
    error,
    receiptId,
    languageWorkingCopy,
    generatedSummary,
    updateField,
    updateCvMetadata,
    setLanguageWorkingCopy,
    updateProfileField,
    commitLabelWorkingCopy,
    removeLabelValue,
    updateCandidateRole,
    addCandidateRole,
    removeCandidateRole,
    updateCandidateSkill,
    selectCandidateSkill,
    addCandidateSkill,
    removeCandidateSkill,
    addWorkExperience,
    removeWorkExperience,
    updateWorkExperienceField,
    updateCurrentWorkExperience,
    selectCompanyIdentity,
    addEducation,
    removeEducation,
    updateEducationField,
    updateCurrentEducation,
    addCertification,
    removeCertification,
    updateCertificationField,
    updateCertificationDocument,
    hasSummarySourceData,
    generateStructuredSummary,
    submitForm,
    stripCompetencyYearThresholds,
  } = useCandidateApplicationIntakeWorkflow();

  return (
    <div className="min-h-screen px-6 py-8">
      <main className="mx-auto max-w-4xl">
        <Link to="/" className="text-link text-sm font-medium">
          Till startsidan
        </Link>

        <section className="mt-8">
          <p className="text-label text-xs font-semibold uppercase tracking-[0.22em]">
            Hitta Uppdrag
          </p>
          <h1 className="text-title mt-3 text-4xl font-semibold">
            Konsultregistrering
          </h1>
          <p className="text-muted mt-4 max-w-2xl text-sm leading-6">
            Ladda upp CV-fil för extraktion. PDF stöds direkt.
          </p>
        </section>

        <form
          onSubmit={submitForm}
          className="panel mt-8 grid gap-5 rounded-2xl p-6"
        >
          <CandidateApplicationContactCvSection
            form={form}
            previewingCv={previewingCv}
            cvPreviewStatus={cvPreviewStatus}
            cvPreviewError={cvPreviewError}
            updateField={updateField}
            updateCvMetadata={updateCvMetadata}
          />

          <CandidateApplicationPersonalInfoSection
            cvProfile={cvProfile}
            languageWorkingCopy={languageWorkingCopy}
            setLanguageWorkingCopy={setLanguageWorkingCopy}
            updateProfileField={updateProfileField}
            commitLabelWorkingCopy={commitLabelWorkingCopy}
            removeLabelValue={removeLabelValue}
          />

          <CandidateApplicationProfileSection
            cvProfile={cvProfile}
            candidateRoles={candidateRoles}
            candidateSkills={candidateSkills}
            referenceData={referenceData}
            loadingReferences={loadingReferences}
            updateProfileField={updateProfileField}
            updateCandidateRole={updateCandidateRole}
            addCandidateRole={addCandidateRole}
            removeCandidateRole={removeCandidateRole}
            updateCandidateSkill={updateCandidateSkill}
            selectCandidateSkill={selectCandidateSkill}
            addCandidateSkill={addCandidateSkill}
            removeCandidateSkill={removeCandidateSkill}
          />

          <CandidateApplicationWorkExperienceSection
            workExperiences={workExperiences}
            addWorkExperience={addWorkExperience}
            removeWorkExperience={removeWorkExperience}
            updateWorkExperienceField={updateWorkExperienceField}
            updateCurrentWorkExperience={updateCurrentWorkExperience}
            selectCompanyIdentity={selectCompanyIdentity}
          />

          <CandidateApplicationWorkPreferencesSection
            cvProfile={cvProfile}
            updateProfileField={updateProfileField}
          />

          <CandidateApplicationEducationSection
            educations={educations}
            addEducation={addEducation}
            removeEducation={removeEducation}
            updateEducationField={updateEducationField}
            updateCurrentEducation={updateCurrentEducation}
          />

          <CandidateApplicationCertificationSection
            certifications={certifications}
            certificationFeedback={certificationFeedback}
            addCertification={addCertification}
            removeCertification={removeCertification}
            updateCertificationField={updateCertificationField}
            updateCertificationDocument={updateCertificationDocument}
          />

          <CandidateApplicationConsentSection
            cvProfile={cvProfile}
            updateProfileField={updateProfileField}
          />

          <CandidateApplicationGeneratedSummarySection
            generatedSummary={generatedSummary}
            previewingCv={previewingCv}
            hasSummarySourceData={hasSummarySourceData}
            generateStructuredSummary={generateStructuredSummary}
            stripCompetencyYearThresholds={stripCompetencyYearThresholds}
          />

          {error && <p className="text-danger text-sm">{error}</p>}
          {receiptId && (
            <p className="text-success text-sm">
              Kandidatprofilen är registrerad med Referens: {receiptId}.
            </p>
          )}

          <button
            type="submit"
            disabled={submitting}
            className="btn btn-main justify-self-start rounded px-5 py-2 text-sm font-semibold"
          >
            {submitting ? "Registrerar..." : "Registrera Kandidatprofil"}
          </button>
        </form>
      </main>
    </div>
  );
}
