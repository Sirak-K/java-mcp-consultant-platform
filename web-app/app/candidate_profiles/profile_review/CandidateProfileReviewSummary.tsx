import type { CandidateProfileReviewItem } from "~/candidate_profiles/types";
import { formatCompetencyLevelLabel } from "~/reference_data/competencyLevels";
import { roleExperienceTierLabel as roleExperienceLabel } from "~/reference_data/roleExperience";
import { formatWorkMode, formatWorkModeText } from "~/reference_data/workMode";

const invariantHeadingClass =
  "text-label text-sm font-semibold uppercase tracking-[0.14em]";

function splitListValue(value: string): string[] {
  return value
    .split(/[\n,;|]+/)
    .map((part) => part.trim())
    .filter(Boolean);
}

function roleRows(item: CandidateProfileReviewItem) {
  return (item.specification.profileWorkingCopy.candidateRoles ?? [])
    .filter((role) => role.roleTitle || role.roleId)
    .map((role) => ({
      role: role.roleTitle || `Roll ${role.roleId}`,
      years: Number(role.roleExperienceYears) || 0,
    }));
}

function skillRows(item: CandidateProfileReviewItem) {
  return (item.specification.profileWorkingCopy.candidateSkills ?? [])
    .filter((skill) => skill.skillTitle || skill.skillId)
    .map((skill) => {
      const formattedLevel = formatCompetencyLevelLabel(skill.skillLevelName);
      const levelMatch = formattedLevel.match(/^(.+?)\s+\((.+)\)$/);
      return {
        skill: skill.skillTitle || `Kompetens ${skill.skillId}`,
        level: (levelMatch?.[1] ?? formattedLevel) || "Ej angivet",
        levelYears: levelMatch?.[2] ?? "Ej angivet",
      };
    });
}

function summaryTextExists(value: string | null | undefined): boolean {
  return Boolean(value?.trim());
}

function cleanGeneratedSummaryText(value: string): string {
  return value
    .trim()
    .replace(/\bRoles:\s*/gi, "Roller: ")
    .replace(/\bSkills:\s*/gi, "Kompetenser: ")
    .replace(/\bFlexibility:\s*/gi, "Flexibilitet: ")
    .replace(/\bWork mode:\s*/gi, "Arbetsläge: ")
    .replace(/\b(ON_PREMISE|REMOTE|HYBRID|On premise|Remote)\b/gi, (match) =>
      formatWorkModeText(match),
    )
    .replace(/\s+(Flexibilitet|Arbetsläge):/g, ". $1:")
    .replace(/\.\s*\./g, ".");
}

function summarySentences(value: string): string[] {
  return cleanGeneratedSummaryText(value)
    .split(". ")
    .map((part, index, parts) => {
      const trimmed = part.trim();
      if (!trimmed) {
        return "";
      }
      return index < parts.length - 1 && !trimmed.endsWith(".")
        ? `${trimmed}.`
        : trimmed;
    })
    .filter(Boolean);
}

function CandidateSkillReviewTable({
  rows,
}: {
  rows: ReturnType<typeof skillRows>;
}) {
  return (
    <dd className="mt-2 max-w-full overflow-x-auto">
      <div className="inline-block min-w-[520px] max-w-full overflow-hidden rounded border border-cyan-400/20 align-top">
        <table className="w-auto min-w-[520px] table-auto border-collapse text-left text-sm">
          <thead className="text-label bg-cyan-400/5">
            <tr>
              <th className="w-12 px-3 py-2 text-center font-semibold">#</th>
              <th className="w-52 px-3 py-2 font-semibold">Kompetens</th>
              <th className="w-36 px-3 py-2 text-center font-semibold">Nivå</th>
              <th className="w-36 px-3 py-2 text-center font-semibold">
                Nivå (år)
              </th>
            </tr>
          </thead>
          <tbody className="divide-y divide-cyan-400/10">
            {rows.map((skill, index) => (
              <tr
                key={`${skill.skill}-${skill.level}-${skill.levelYears}-${index}`}
                className="transition-colors hover:bg-cyan-400/5"
              >
                <td className="px-3 py-2 text-center text-soft tabular-nums">
                  {index + 1}
                </td>
                <td className="px-3 py-2 font-medium text-title">
                  {skill.skill}
                </td>
                <td className="px-3 py-2 text-center">
                  <span className="inline-flex max-w-full justify-center rounded-full border border-cyan-400/20 bg-slate-950/40 px-2.5 py-1 text-xs font-semibold text-cyan-100">
                    {skill.level}
                  </span>
                </td>
                <td className="px-3 py-2 text-center text-soft">
                  {skill.levelYears}
                </td>
              </tr>
            ))}
          </tbody>
        </table>
      </div>
    </dd>
  );
}

function GeneratedSummaryItem({
  label,
  value,
}: {
  label: string;
  value: string;
}) {
  return (
    <div className="grid gap-1 border-l border-cyan-400/25 py-1 pl-3 sm:grid-cols-[10rem_1fr] sm:gap-4">
      <span className="text-label text-xs font-semibold uppercase tracking-[0.12em]">
        {label}
      </span>
      <span className="text-muted grid gap-1 leading-6">
        {summarySentences(value).map((sentence, index) => (
          <GeneratedSummarySentence
            key={`${label}-${index}`}
            sentence={sentence}
          />
        ))}
      </span>
    </div>
  );
}

function GeneratedSummarySentence({ sentence }: { sentence: string }) {
  const chipPrefix = "Kompetenser:";
  if (sentence.startsWith(chipPrefix)) {
    const skills = sentence
      .slice(chipPrefix.length)
      .replace(/\.$/, "")
      .split(",")
      .map((skill) => skill.trim())
      .filter(Boolean);

    return (
      <span className="grid gap-2">
        <span>{chipPrefix}</span>
        <span className="flex flex-wrap gap-1.5">
          {skills.map((skill) => (
            <span
              key={skill}
              className="rounded-full bg-slate-950/45 px-2.5 py-1 text-xs text-cyan-100"
            >
              {skill}
            </span>
          ))}
        </span>
      </span>
    );
  }

  return <span>{sentence}</span>;
}

export function CandidateProfileReviewSummary({
  item,
  isRegisteredProfiles,
}: {
  item: CandidateProfileReviewItem;
  isRegisteredProfiles: boolean;
}) {
  const profileRoleRows = roleRows(item);
  const profileSkillRows = skillRows(item);

  return (
    <dl className="grid gap-x-8 gap-y-4 md:grid-cols-2">
      <div>
        <dt className={invariantHeadingClass}>Kontakt</dt>
        <dd>{item.specification.contactEmail}</dd>
      </div>
      {!isRegisteredProfiles && (
        <>
          <div>
            <dt className={invariantHeadingClass}>CV-fil</dt>
            <dd>{item.specification.cvFileName}</dd>
          </div>
          <div>
            <dt className={invariantHeadingClass}>Telefon</dt>
            <dd>
              {item.specification.profileWorkingCopy.phoneNumber ||
                "Ej angivet"}
            </dd>
          </div>
          <div>
            <dt className={invariantHeadingClass}>Storlek</dt>
            <dd>{item.specification.cvSizeBytes ?? 0} bytes</dd>
          </div>
        </>
      )}
      <div>
        <dt className={invariantHeadingClass}>Namn</dt>
        <dd>
          {item.specification.profileWorkingCopy.firstName}{" "}
          {item.specification.profileWorkingCopy.lastName}
        </dd>
      </div>
      {isRegisteredProfiles ? (
        <div>
          <dt className={invariantHeadingClass}>Telefon</dt>
          <dd>
            {item.specification.profileWorkingCopy.phoneNumber || "Ej angivet"}
          </dd>
        </div>
      ) : (
        <div>
          <dt className={invariantHeadingClass}>MIME-typ</dt>
          <dd>{item.specification.cvContentType}</dd>
        </div>
      )}
      <div>
        <dt className={invariantHeadingClass}>Arbetsläge</dt>
        <dd>
          {item.specification.profileWorkingCopy.workMode
            ? formatWorkMode(item.specification.profileWorkingCopy.workMode)
            : "Ej angivet"}
        </dd>
      </div>
      <div className="md:col-span-2">
        <dt className={invariantHeadingClass}>Språk</dt>
        <dd>
          {splitListValue(
            item.specification.profileWorkingCopy.languages || "",
          ).length === 0 ? (
            "Ej angivet"
          ) : (
            <ul className="grid gap-1">
              {splitListValue(
                item.specification.profileWorkingCopy.languages || "",
              ).map((language, index) => (
                <li key={`${language}-${index}`}>{language}</li>
              ))}
            </ul>
          )}
        </dd>
      </div>
      {isRegisteredProfiles && (
        <div className="md:col-span-2">
          <dt className={invariantHeadingClass}>Roller</dt>
          {profileRoleRows.length === 0 ? (
            <dd>Inga roller angivna.</dd>
          ) : (
            <dd className="max-w-full overflow-x-auto">
              <table className="mt-2 w-auto table-auto border-collapse text-left text-sm">
                <thead className="text-label border-b border-cyan-400/25">
                  <tr>
                    <th className="w-12 whitespace-nowrap px-3 py-2 text-center">
                      #
                    </th>
                    <th className="whitespace-nowrap py-2 pr-6">Roll</th>
                    <th className="w-40 whitespace-nowrap px-3 py-2 text-center">
                      Erfarenhet (år)
                    </th>
                    <th className="w-44 whitespace-nowrap px-3 py-2 text-center">
                      Erfarenhet
                    </th>
                  </tr>
                </thead>
                <tbody>
                  {profileRoleRows.map((role, index) => (
                    <tr
                      key={`${role.role}-${index}`}
                      className="border-b border-cyan-400/10 last:border-b-0"
                    >
                      <td className="whitespace-nowrap px-3 py-2 text-center">
                        {index + 1}
                      </td>
                      <td className="whitespace-nowrap py-2 pr-6">
                        {role.role}
                      </td>
                      <td className="whitespace-nowrap px-3 py-2 text-center tabular-nums">
                        {role.years}
                      </td>
                      <td className="whitespace-nowrap px-3 py-2 text-center">
                        {roleExperienceLabel(role.years)}
                      </td>
                    </tr>
                  ))}
                </tbody>
              </table>
            </dd>
          )}
        </div>
      )}
      {!isRegisteredProfiles && (
        <div className="md:col-span-2">
          <dt className={invariantHeadingClass}>Roller</dt>
          {(item.specification.profileWorkingCopy.candidateRoles ?? [])
            .length === 0 ? (
            <dd>Inga strukturerade roller angivna.</dd>
          ) : (
            <dd className="grid gap-1">
              {(item.specification.profileWorkingCopy.candidateRoles ?? []).map(
                (role, index) => (
                  <span key={`${role.roleId}-${index}`}>
                    {index + 1}. {role.roleTitle || `Roll ${role.roleId}`} |{" "}
                    {role.roleExperienceYears} år
                  </span>
                ),
              )}
            </dd>
          )}
        </div>
      )}
      <div className="md:col-span-2">
        <dt className={invariantHeadingClass}>Kompetenser</dt>
        {profileSkillRows.length === 0 ? (
          <dd>Inga strukturerade kompetenser angivna.</dd>
        ) : (
          <CandidateSkillReviewTable rows={profileSkillRows} />
        )}
      </div>
      <div className="md:col-span-2 pt-5">
        <dt className={invariantHeadingClass}>Arbetslivserfarenhet</dt>
        {item.specification.profileWorkingCopy.workExperiences.length === 0 ? (
          <dd>Inga poster angivna.</dd>
        ) : (
          <dd className="mt-3 grid max-w-2xl gap-4">
            {item.specification.profileWorkingCopy.workExperiences.map(
              (workExperience, index) => (
                <div
                  key={`${workExperience.jobTitle}-${workExperience.workExpCompany}-${workExperience.workExpCompanyOrgNr}-${index}`}
                  className="border-b border-cyan-400/15 pb-4 last:border-b-0 last:pb-0"
                >
                  <p>
                    {index + 1}. {workExperience.jobTitle || "Utan titel"} |{" "}
                    {workExperience.workExpCompany || "Utan bolag"}
                    {workExperience.workExpCompanyOrgNr
                      ? ` | Org Nr: ${workExperience.workExpCompanyOrgNr}`
                      : ""}
                  </p>
                  <p className="text-soft">
                    {[workExperience.city, workExperience.country]
                      .filter(Boolean)
                      .join(", ") || "Ingen plats"}
                    {" | "}
                    {workExperience.startDate || "?"} -{" "}
                    {workExperience.currentlyHere
                      ? "Pågående"
                      : workExperience.endDate || "?"}
                  </p>
                </div>
              ),
            )}
          </dd>
        )}
      </div>
      <div className="md:col-span-2 pt-5">
        <dt className={invariantHeadingClass}>Utbildning</dt>
        {item.specification.profileWorkingCopy.educations.length === 0 ? (
          <dd>Inga utbildningar angivna.</dd>
        ) : (
          <dd className="mt-3 grid max-w-2xl gap-4">
            {item.specification.profileWorkingCopy.educations.map(
              (education, index) => (
                <div
                  key={`${education.institution}-${education.fieldOfStudy}-${index}`}
                  className="border-b border-cyan-400/15 pb-4 last:border-b-0 last:pb-0"
                >
                  <p>
                    {index + 1}. {education.institution || "Utan institution"} |{" "}
                    {education.fieldOfStudy || "Inget utbildningsområde"}
                  </p>
                  <p className="text-soft">
                    {education.startDate || "?"} -{" "}
                    {education.currentlyStudying
                      ? "Pågående"
                      : education.endDate || "?"}
                  </p>
                </div>
              ),
            )}
          </dd>
        )}
      </div>
      <div className="md:col-span-2 pt-5">
        <dt className={invariantHeadingClass}>Certifikat</dt>
        {item.specification.profileWorkingCopy.certifications.length === 0 ? (
          <dd>Inga certifikat.</dd>
        ) : (
          <dd className="mt-3 grid max-w-2xl gap-3">
            {item.specification.profileWorkingCopy.certifications.map(
              (certification, index) => (
                <div
                  key={`${certification.name}-${certification.documentFileName}-${index}`}
                >
                  <p>
                    {index + 1}. {certification.name || "Utan namn"}
                  </p>
                  {certification.documentAttached ? (
                    <p className="text-soft">
                      Dokument:{" "}
                      {certification.documentFileName || "Okänt filnamn"}
                      {" | "}
                      {certification.documentContentType ||
                        "application/octet-stream"}
                      {" | "}
                      {certification.documentSizeBytes ?? 0} bytes
                    </p>
                  ) : (
                    <p className="text-soft">Inget dokument uppladdat.</p>
                  )}
                </div>
              ),
            )}
          </dd>
        )}
      </div>
      {!isRegisteredProfiles && (
        <div className="md:col-span-2 pt-5">
          <dt className={invariantHeadingClass}>Extraherat CV</dt>
          <dd className="mt-3 grid gap-2">
            <span>Status: {item.cvExtraction.status}</span>
            {item.cvExtraction.extractedAt && (
              <span className="text-soft">
                Extraherat: {item.cvExtraction.extractedAt}
              </span>
            )}
            {item.cvExtraction.error && (
              <span className="text-soft">{item.cvExtraction.error}</span>
            )}
            {item.cvExtraction.extractedTextPreview && (
              <pre className="panel-soft mt-2 max-h-48 overflow-auto rounded p-3 text-xs whitespace-pre-wrap">
                {item.cvExtraction.extractedTextPreview}
              </pre>
            )}
          </dd>
        </div>
      )}
      <div className="md:col-span-2 pt-6">
        <dt className={invariantHeadingClass}>
          Genererad profilsammanfattning
        </dt>
        <dd className="mt-2 grid max-w-5xl gap-2">
          {item.generatedSummary.status === "GENERATED" ||
          summaryTextExists(
            item.generatedSummary.coreCompetenceOverview,
          ) ||
          summaryTextExists(item.generatedSummary.location) ||
          summaryTextExists(item.generatedSummary.otherDetails) ? (
            <>
              {summaryTextExists(
                item.generatedSummary.coreCompetenceOverview,
              ) && (
                <GeneratedSummaryItem
                  label="Kompetens"
                  value={item.generatedSummary.coreCompetenceOverview ?? ""}
                />
              )}
              {summaryTextExists(
                item.generatedSummary.location,
              ) && (
                <GeneratedSummaryItem
                  label="Plats"
                  value={item.generatedSummary.location ?? ""}
                />
              )}
              {summaryTextExists(
                item.generatedSummary.otherDetails,
              ) && (
                <GeneratedSummaryItem
                  label="Övrigt"
                  value={item.generatedSummary.otherDetails ?? ""}
                />
              )}
              {item.generatedSummary.generatedAt && (
                <span className="text-soft pt-1 text-xs">
                  Genererad: {item.generatedSummary.generatedAt}
                </span>
              )}
            </>
          ) : (
            <span className="text-soft">
              Ingen genererad profilsammanfattning finns ännu.
            </span>
          )}
        </dd>
      </div>
    </dl>
  );
}
