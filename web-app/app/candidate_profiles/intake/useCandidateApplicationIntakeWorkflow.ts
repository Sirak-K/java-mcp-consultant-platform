import { type ChangeEvent, type FormEvent, useEffect, useState } from "react";
import { candidateProfilesApi } from "~/candidate_profiles/api/candidateProfilesApi";
import {
  buildCandidateApplicationGeneratedSummary,
  hasCandidateApplicationSummarySourceData,
  stripCandidateCompetencyYearThresholds,
} from "~/candidate_profiles/intake/candidateApplicationGeneratedSummary";
import {
  mergeCandidateCvProfile,
  mergeCandidateRoles,
  mergeCandidateSkills,
  mergeCertificationFiles,
  mergeCertifications,
  mergeEducations,
  mergeWorkExperiences,
} from "~/candidate_profiles/intake/candidateApplicationCvPreviewMerge";
import { referenceDataApi } from "~/reference_data/api/referenceDataApi";
import {
  currentlyStudyingAvailable,
  endDateHasPassed,
  parseCandidateSkillSelection,
} from "~/candidate_profiles/profile_form/candidateProfileFormState";
import {
  blankCandidateCertification,
  blankCandidateEducation,
  blankCandidateWorkExperience,
  defaultCandidateRoleFormFields,
  defaultCandidateSkillFormFields,
  hasCertificationData,
  hasEducationData,
  hasSkillReferenceOptions,
  hasWorkExperienceData,
  initialCandidateApplicationForm,
  initialCandidateCvProfile,
  initialCandidateSummary,
  joinLabelValues,
  mergeText,
  normalizeLocationFlexibility,
  parseLabelValues,
  type CandidateApplicationFormFields,
  type CandidateCertificationFormFields as CertificationFields,
  type CandidateCvProfileFormFields as CvProfileFields,
  type CandidateEducationFormFields as EducationFields,
  type CandidateRoleFormFields as CandidateRoleFields,
  type CandidateSkillFormFields as CandidateSkillFields,
  type CandidateSummaryFormFields as CandidateSummaryFields,
  type CandidateWorkExperienceFormFields as WorkExperienceFields,
} from "~/candidate_profiles/intake/candidateApplicationIntakeState";
import type {
  CandidateRoleWorkingCopyInput,
  CandidateSkillWorkingCopyInput,
  CandidateCvProfileWorkingCopyInput,
  CandidateCvPreviewView,
} from "~/candidate_profiles/types";
import type { MarketplaceReferenceData } from "~/reference_data/types";
import type { ApiError } from "~/shared/api/apiErrors";

export function useCandidateApplicationIntakeWorkflow() {
  const [form, setForm] = useState<CandidateApplicationFormFields>(
    initialCandidateApplicationForm,
  );
  const [cvProfile, setCvProfile] = useState<CvProfileFields>(
    initialCandidateCvProfile,
  );
  const [workExperiences, setWorkExperiences] = useState<
    WorkExperienceFields[]
  >([{ ...blankCandidateWorkExperience }]);
  const [educations, setEducations] = useState<EducationFields[]>([
    { ...blankCandidateEducation },
  ]);
  const [candidateRoles, setCandidateRoles] = useState<CandidateRoleFields[]>(
    [],
  );
  const [candidateSkills, setCandidateSkills] = useState<
    CandidateSkillFields[]
  >([]);
  const [certifications, setCertifications] = useState<CertificationFields[]>(
    [],
  );
  const [certificateFiles, setCertificateFiles] = useState<Array<File | null>>(
    [],
  );
  const [certificationFeedback, setCertificationFeedback] = useState<
    string | null
  >(null);
  const [referenceData, setReferenceData] =
    useState<MarketplaceReferenceData | null>(null);
  const [loadingReferences, setLoadingReferences] = useState(true);
  const [previewingCv, setPreviewingCv] = useState(false);
  const [cvPreviewStatus, setCvPreviewStatus] = useState<string | null>(null);
  const [cvPreviewError, setCvPreviewError] = useState<string | null>(null);
  const [submitting, setSubmitting] = useState(false);
  const [error, setError] = useState<string | null>(null);
  const [receiptId, setReceiptId] = useState<number | null>(null);
  const [selectedFile, setSelectedFile] = useState<File | null>(null);
  const [languageWorkingCopy, setLanguageWorkingCopy] = useState("");
  const [generatedSummary, setGeneratedSummary] =
    useState<CandidateSummaryFields>(initialCandidateSummary);

  useEffect(() => {
    let mounted = true;
    referenceDataApi
      .referenceData()
      .then((data) => {
        if (mounted) {
          setReferenceData(data);
        }
      })
      .catch((err: ApiError) => {
        if (mounted) {
          setError(err.message ?? "Kunde inte ladda referensdata.");
        }
      })
      .finally(() => {
        if (mounted) {
          setLoadingReferences(false);
        }
      });

    return () => {
      mounted = false;
    };
  }, []);

  async function ensureSkillReferenceOptionsLoaded() {
    if (hasSkillReferenceOptions(referenceData)) {
      return;
    }
    setLoadingReferences(true);
    try {
      const data = await referenceDataApi.referenceData();
      setReferenceData(data);
      if (!hasSkillReferenceOptions(data)) {
        throw new Error(
          "Referensdata saknar kompetenser eller kompetensnivåer.",
        );
      }
    } catch (err) {
      const apiError = err as ApiError;
      setError(apiError.message ?? "Kunde inte ladda referensdata.");
      throw err;
    } finally {
      setLoadingReferences(false);
    }
  }

  function updateField<K extends keyof CandidateApplicationFormFields>(
    name: K,
    value: CandidateApplicationFormFields[K],
  ) {
    setForm((current) => ({ ...current, [name]: value }));
  }

  function updateProfileField<K extends keyof CvProfileFields>(
    name: K,
    value: CvProfileFields[K],
  ) {
    setCvProfile((current) => ({ ...current, [name]: value }));
  }

  function setLabelField(name: "languages", values: string[]) {
    updateProfileField(name, joinLabelValues(values));
  }

  function commitLabelWorkingCopy(
    name: "languages",
    workingCopyValue: string,
    clearWorkingCopy: () => void,
  ) {
    const workingCopyLabels = parseLabelValues(workingCopyValue);
    if (workingCopyLabels.length === 0) {
      clearWorkingCopy();
      return;
    }
    const currentLabels = parseLabelValues(cvProfile[name]);
    setLabelField(name, [...currentLabels, ...workingCopyLabels]);
    clearWorkingCopy();
  }

  function removeLabelValue(name: "languages", valueToRemove: string) {
    setLabelField(
      name,
      parseLabelValues(cvProfile[name]).filter(
        (item) =>
          item.toLocaleLowerCase() !== valueToRemove.toLocaleLowerCase(),
      ),
    );
  }

  function updateCandidateRole<K extends keyof CandidateRoleFields>(
    index: number,
    name: K,
    value: CandidateRoleFields[K],
  ) {
    setCandidateRoles((current) =>
      current.map((item, itemIndex) =>
        itemIndex === index ? { ...item, [name]: value } : item,
      ),
    );
  }

  function addCandidateRole() {
    setCandidateRoles((current) => [
      ...current,
      defaultCandidateRoleFormFields(referenceData),
    ]);
  }

  function removeCandidateRole(index: number) {
    setCandidateRoles((current) =>
      current.filter((_, itemIndex) => itemIndex !== index),
    );
  }

  function updateCandidateSkill<K extends keyof CandidateSkillFields>(
    index: number,
    name: K,
    value: CandidateSkillFields[K],
  ) {
    setCandidateSkills((current) =>
      current.map((item, itemIndex) =>
        itemIndex === index ? { ...item, [name]: value } : item,
      ),
    );
  }

  function addCandidateSkill() {
    setCandidateSkills((current) => [
      ...current,
      defaultCandidateSkillFormFields(referenceData),
    ]);
  }

  function removeCandidateSkill(index: number) {
    setCandidateSkills((current) =>
      current.filter((_, itemIndex) => itemIndex !== index),
    );
  }

  function selectCandidateSkill(index: number, value: string) {
    const { skillCategory, skillId } = parseCandidateSkillSelection(value);
    setCandidateSkills((current) =>
      current.map((item, itemIndex) =>
        itemIndex === index ? { ...item, skillId, skillCategory } : item,
      ),
    );
  }

  function updateWorkExperienceField<K extends keyof WorkExperienceFields>(
    index: number,
    name: K,
    value: WorkExperienceFields[K],
  ) {
    setWorkExperiences((current) =>
      current.map((item, itemIndex) =>
        itemIndex === index
          ? {
              ...item,
              [name]: value,
              currentlyHere:
                name === "endDate" && endDateHasPassed(String(value))
                  ? false
                  : item.currentlyHere,
            }
          : item,
      ),
    );
  }

  function updateCurrentWorkExperience(index: number, currentlyHere: boolean) {
    setWorkExperiences((current) =>
      current.map((item, itemIndex) =>
        itemIndex === index
          ? {
              ...item,
              currentlyHere,
              endDate: currentlyHere ? "" : item.endDate,
            }
          : item,
      ),
    );
  }

  function selectCompanyIdentity(index: number, organisationNumber: string) {
    setWorkExperiences((current) =>
      current.map((item, itemIndex) => {
        if (itemIndex !== index) {
          return item;
        }
        const selected = item.companyIdentityOptions.find(
          (option) => option.organisationNumber === organisationNumber,
        );
        return selected
          ? {
              ...item,
              workExpCompany: selected.organisationName,
              workExpCompanyOrgNr: selected.organisationNumber,
              city: selected.organisationCity || item.city,
              country: selected.organisationCity ? "Sweden" : item.country,
              companyIdentityOptions: [],
            }
          : item;
      }),
    );
  }

  function updateEducationField<K extends keyof EducationFields>(
    index: number,
    name: K,
    value: EducationFields[K],
  ) {
    setEducations((current) =>
      current.map((item, itemIndex) => {
        if (itemIndex !== index) {
          return item;
        }
        const next = { ...item, [name]: value };
        if (name === "endDate" && endDateHasPassed(String(value))) {
          next.currentlyStudying = false;
        }
        return next;
      }),
    );
  }

  function updateCurrentEducation(index: number, currentlyStudying: boolean) {
    setEducations((current) =>
      current.map((item, itemIndex) => {
        if (itemIndex !== index) {
          return item;
        }
        const nextCurrentlyStudying =
          currentlyStudying && currentlyStudyingAvailable(item);
        return {
          ...item,
          currentlyStudying: nextCurrentlyStudying,
          endDate: nextCurrentlyStudying ? "" : item.endDate,
        };
      }),
    );
  }

  function addEducation() {
    setEducations((current) => [...current, { ...blankCandidateEducation }]);
  }

  function removeEducation(index: number) {
    setEducations((current) => {
      if (current.length <= 1) {
        return current;
      }
      return current.filter((_, itemIndex) => itemIndex !== index);
    });
  }

  function addWorkExperience() {
    setWorkExperiences((current) => [
      ...current,
      { ...blankCandidateWorkExperience },
    ]);
  }

  function removeWorkExperience(index: number) {
    setWorkExperiences((current) => {
      if (current.length <= 1) {
        return current;
      }
      return current.filter((_, itemIndex) => itemIndex !== index);
    });
  }

  function updateCertificationField<K extends keyof CertificationFields>(
    index: number,
    name: K,
    value: CertificationFields[K],
  ) {
    setCertifications((current) =>
      current.map((item, itemIndex) =>
        itemIndex === index ? { ...item, [name]: value } : item,
      ),
    );
  }

  function updateCertificationDocument(index: number, file: File | null) {
    if (file) {
      setCertificationFeedback(null);
    }
    setCertificateFiles((current) =>
      current.map((item, itemIndex) => (itemIndex === index ? file : item)),
    );
    setCertifications((current) =>
      current.map((item, itemIndex) =>
        itemIndex === index
          ? {
              ...item,
              documentAttached: file !== null,
              documentFileName: file?.name ?? "",
              documentContentType: file?.type || "",
              documentSizeBytes: file?.size ?? null,
            }
          : item,
      ),
    );
  }

  function addCertification() {
    const hasActiveCertificationWithoutFile = certifications.some(
      (certification, index) =>
        !certification.documentAttached && !certificateFiles[index],
    );
    if (hasActiveCertificationWithoutFile) {
      setCertificationFeedback(
        "Färdigställ den aktiva instansen innan du lägger till en ny.",
      );
      return;
    }
    setCertificationFeedback(null);
    setCertifications((current) => [
      ...current,
      { ...blankCandidateCertification },
    ]);
    setCertificateFiles((current) => [...current, null]);
  }

  function removeCertification(index: number) {
    setCertificationFeedback(null);
    setCertifications((current) =>
      current.filter((_, itemIndex) => itemIndex !== index),
    );
    setCertificateFiles((current) =>
      current.filter((_, itemIndex) => itemIndex !== index),
    );
  }

  function hasSummarySourceData() {
    return hasCandidateApplicationSummarySourceData({
      cvProfile,
      candidateRoles,
      candidateSkills,
      workExperiences,
      educations,
      certifications,
      referenceData,
    });
  }

  function generateStructuredSummary() {
    setGeneratedSummary(
      buildCandidateApplicationGeneratedSummary({
        cvProfile,
        candidateRoles,
        candidateSkills,
        workExperiences,
        educations,
        certifications,
        referenceData,
      }),
    );
  }

  function applyCvPreview(preview: CandidateCvPreviewView) {
    setCvPreviewStatus(preview.cvExtraction.status);
    setCvPreviewError(preview.cvExtraction.error || null);
    setCvProfile((current) =>
      mergeCandidateCvProfile(current, preview.profileWorkingCopy),
    );
    setCandidateRoles((current) =>
      mergeCandidateRoles(current, preview.profileWorkingCopy),
    );
    setCandidateSkills((current) =>
      mergeCandidateSkills(current, preview.profileWorkingCopy),
    );
    setWorkExperiences((current) =>
      mergeWorkExperiences(
        current,
        preview.profileWorkingCopy.workExperiences,
      ),
    );
    setEducations((current) =>
      mergeEducations(current, preview.profileWorkingCopy.educations),
    );
    setCertifications((current) =>
      mergeCertifications(current, preview.profileWorkingCopy.certifications),
    );
    setCertificateFiles((current) =>
      mergeCertificationFiles(
        current,
        preview.profileWorkingCopy.certifications,
      ),
    );
    setForm((current) => ({
      ...current,
      contactEmail: mergeText(
        current.contactEmail,
        preview.profileWorkingCopy.contactEmail,
      ),
    }));
  }

  function buildCertificationSubmission() {
    const filteredEntries = certifications
      .map((certification, index) => ({
        certification,
        file: certificateFiles[index] ?? null,
      }))
      .filter(({ certification }) => hasCertificationData(certification));

    return {
      certificationWorkingCopys: filteredEntries.map(({ certification }) => ({
        name: certification.name.trim(),
        documentAttached: certification.documentAttached,
        documentFileName: certification.documentFileName.trim(),
        documentContentType: certification.documentContentType.trim(),
        documentSizeBytes: certification.documentSizeBytes,
      })),
      certificateUploads: filteredEntries.flatMap(({ certification, file }) =>
        certification.documentAttached && file ? [file] : [],
      ),
    };
  }

  function buildCandidateRoles(): CandidateRoleWorkingCopyInput[] {
    return candidateRoles
      .filter((role) => role.roleId > 0)
      .map((role) => ({
        roleId: role.roleId,
        roleTitle:
          referenceData?.roles.find((item) => item.id === role.roleId)?.title ??
          "",
        roleExperienceYears: Number(role.roleExperienceYears) || 0,
      }));
  }

  function buildCandidateSkills(): CandidateSkillWorkingCopyInput[] {
    return candidateSkills
      .filter((skill) => skill.skillId > 0 && skill.skillLevelId > 0)
      .map((skill) => {
        const refSkill = referenceData?.skills.find(
          (item) =>
            item.id === skill.skillId && item.category === skill.skillCategory,
        );
        return {
          skillId: skill.skillId,
          skillTitle: refSkill?.title ?? "",
          skillCategory: skill.skillCategory,
          skillLevelId: skill.skillLevelId,
          skillLevelName:
            referenceData?.skillLevels.find(
              (item) => item.id === skill.skillLevelId,
            )?.name ?? "",
        };
      });
  }

  function toProfileWorkingCopyInput(
    certificationWorkingCopys: CandidateCvProfileWorkingCopyInput["certifications"],
  ): CandidateCvProfileWorkingCopyInput {
    return {
      contactEmail: form.contactEmail.trim(),
      firstName: cvProfile.firstName.trim(),
      lastName: cvProfile.lastName.trim(),
      phoneNumber: cvProfile.phoneNumber.trim(),
      country: cvProfile.country.trim(),
      city: cvProfile.city.trim(),
      workStatus: "",
      languages: cvProfile.languages.trim(),
      roleTitle: "",
      profileSummary: cvProfile.profileSummary.trim(),
      yearsOfExperience: "",
      expectedSalary: cvProfile.expectedSalary.trim(),
      hourlyRate: cvProfile.hourlyRate.trim(),
      skills: "",
      candidateRoles: buildCandidateRoles(),
      candidateSkills: buildCandidateSkills(),
      workExperiences: workExperiences
        .filter(hasWorkExperienceData)
        .map((workExperience) => ({
          jobTitle: workExperience.jobTitle.trim(),
          workExpCompany: workExperience.workExpCompany.trim(),
          workExpCompanyOrgNr: workExperience.workExpCompanyOrgNr.trim(),
          companyIdentityOptions: [],
          city: workExperience.city.trim(),
          country: workExperience.country.trim(),
          startDate: workExperience.startDate,
          endDate: workExperience.endDate,
          currentlyHere: workExperience.currentlyHere,
        })),
      workMode: cvProfile.workMode,
      locationFlexibility: normalizeLocationFlexibility(
        cvProfile.locationFlexibility,
      ),
      preferredLocation: "",
      willingToRelocate: cvProfile.willingToRelocate,
      educations: educations.filter(hasEducationData).map((education) => ({
        institution: education.institution.trim(),
        fieldOfStudy: education.fieldOfStudy.trim(),
        startDate: education.startDate,
        endDate: education.endDate,
        currentlyStudying:
          education.currentlyStudying && currentlyStudyingAvailable(education),
      })),
      certifications: certificationWorkingCopys,
      gdprConsent: cvProfile.gdprConsent,
    };
  }

  function validateCandidateRegistration(
    profileWorkingCopy: CandidateCvProfileWorkingCopyInput,
  ): string | null {
    if (!form.contactEmail.trim()) {
      return "Kontakt-email krävs.";
    }
    if (!profileWorkingCopy.firstName || !profileWorkingCopy.lastName) {
      return "Förnamn och efternamn krävs.";
    }
    if (!profileWorkingCopy.workMode) {
      return "Arbetsläge krävs.";
    }
    if (profileWorkingCopy.candidateRoles.length === 0) {
      return "Minst en kandidatroll krävs.";
    }
    if (profileWorkingCopy.candidateSkills.length === 0) {
      return "Minst en kompetens krävs.";
    }
    return null;
  }

  async function updateCvMetadata(event: ChangeEvent<HTMLInputElement>) {
    const file = event.target.files?.[0] ?? null;
    setSelectedFile(file);
    setReceiptId(null);
    setError(null);
    setForm((current) => ({
      ...current,
      cvFileName: file?.name ?? "",
      cvContentType: file?.type || "application/octet-stream",
      cvSizeBytes: file?.size ?? null,
    }));

    if (!file) {
      setCvPreviewStatus(null);
      setCvPreviewError(null);
      return;
    }

    setPreviewingCv(true);
    setCvPreviewStatus(null);
    setCvPreviewError(null);
    try {
      await ensureSkillReferenceOptionsLoaded();
      const preview = await candidateProfilesApi.previewCandidateCv(file);
      applyCvPreview(preview);
    } catch (err) {
      const apiError = err as ApiError;
      setCvPreviewStatus(null);
      setCvPreviewError(
        apiError.message ?? "Kunde inte extrahera CV direkt efter upload.",
      );
    } finally {
      setPreviewingCv(false);
    }
  }

  async function submitForm(event: FormEvent<HTMLFormElement>) {
    event.preventDefault();
    setSubmitting(true);
    setError(null);
    setReceiptId(null);

    try {
      if (!selectedFile) {
        setError("CV-fil krävs.");
        return;
      }
      const { certificationWorkingCopys, certificateUploads } =
        buildCertificationSubmission();
      const profileWorkingCopy = toProfileWorkingCopyInput(
        certificationWorkingCopys,
      );
      const validationError = validateCandidateRegistration(profileWorkingCopy);
      if (validationError) {
        setError(validationError);
        return;
      }
      const response = await candidateProfilesApi.submitCandidateApplicationFile(
        form.contactEmail,
        selectedFile,
        profileWorkingCopy,
        certificateUploads,
        {
          ...generatedSummary,
          coreCompetenceOverview: stripCandidateCompetencyYearThresholds(
            generatedSummary.coreCompetenceOverview,
          ),
        },
      );
      setReceiptId(response.id);
      const extractedTextPreview =
        response.cvExtraction.extractedTextPreview.trim();
      if (extractedTextPreview) {
        setCvProfile((current) => ({
          ...current,
          profileSummary: current.profileSummary.trim()
            ? current.profileSummary
            : extractedTextPreview,
        }));
      }
    } catch (err) {
      const apiError = err as ApiError;
      setError(apiError.message ?? "Kunde inte skicka konsultprofilen.");
    } finally {
      setSubmitting(false);
    }
  }

  return {
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
    stripCompetencyYearThresholds: stripCandidateCompetencyYearThresholds,
  };
}
