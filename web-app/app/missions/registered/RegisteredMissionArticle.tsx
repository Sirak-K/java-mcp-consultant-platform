import type { ReactNode } from "react";
import type { RegisteredMissionView } from "~/missions/types";
import {
  displayCustomerName,
  displayText,
  displayWorkMode,
  formatPeriod,
  presentationText,
  registeredMissionPlaceholder,
  requiredSkillsText,
  roleExperienceText,
  slotTitle,
} from "~/missions/registered/registeredMissionFormatting";

function Section({
  title,
  children,
}: {
  title: string;
  children: ReactNode;
}) {
  return (
    <section className="grid gap-3 border-t border-cyan-400/20 pt-6">
      <h2 className="text-section text-xl font-semibold">{title}</h2>
      <div className="text-muted text-sm leading-6">{children}</div>
    </section>
  );
}

function RequirementSection({
  title,
  children,
}: {
  title: string;
  children: ReactNode;
}) {
  return (
    <section className="panel-soft rounded p-4">
      <h3 className="text-label text-sm font-semibold">{title}</h3>
      <div className="text-muted mt-3 text-sm leading-6">{children}</div>
    </section>
  );
}

function PlaceholderText() {
  return <p className="text-soft italic">{registeredMissionPlaceholder}</p>;
}

export function RegisteredMissionArticle({
  mission,
}: {
  mission: RegisteredMissionView;
}) {
  const slots = mission.specification.missionSlots ?? [];
  const customerName = displayCustomerName(mission);

  return (
    <article className="panel rounded-2xl p-5 md:p-7">
      <header className="grid gap-5 md:grid-cols-[1fr_auto] md:items-start">
        <div>
          <h1 className="text-title mt-2 text-3xl font-semibold leading-tight">
            {displayText(mission.specification.missionTitle)}
          </h1>
          <p className="text-soft mt-2 text-sm">{customerName}</p>
        </div>
        <div className="panel-soft grid min-w-52 gap-2 rounded p-4 text-sm">
          <span className="chip w-fit rounded-full px-3 py-1 text-xs font-medium">
            {mission.status ?? "REGISTERED"}
          </span>
          <span className="text-muted">{formatPeriod(mission)}</span>
          <span className="text-muted">
            {displayWorkMode(mission.specification.workMode)}
          </span>
        </div>
      </header>

      <div className="mt-7 grid gap-7">
        <Section title="Översikt">
          <div className="grid gap-3 md:grid-cols-4">
            <div>
              <p className="text-label text-xs font-semibold uppercase tracking-wide">
                Titel
              </p>
              <p>{displayText(mission.specification.missionTitle)}</p>
            </div>
            <div>
              <p className="text-label text-xs font-semibold uppercase tracking-wide">
                Period
              </p>
              <p>{formatPeriod(mission)}</p>
            </div>
            <div>
              <p className="text-label text-xs font-semibold uppercase tracking-wide">
                Kontakt
              </p>
              <p>{displayText(mission.specification.customerEmail)}</p>
            </div>
            <div>
              <p className="text-label text-xs font-semibold uppercase tracking-wide">
                Arbetsläge
              </p>
              <p>{displayWorkMode(mission.specification.workMode)}</p>
            </div>
          </div>
        </Section>

        <Section title="Detaljer">
          <div className="grid gap-4">
            <RequirementSection title="Uppdragskrav - Roller">
              {slots.length === 0 ? (
                <PlaceholderText />
              ) : (
                <ul className="grid gap-2">
                  {slots.map((slot) => (
                    <li key={slot.slotNumber}>
                      <span className="text-title">{slotTitle(slot)}</span>
                      {" - "}
                      {roleExperienceText(slot)}
                    </li>
                  ))}
                </ul>
              )}
            </RequirementSection>

            <RequirementSection title="Uppdragskrav - Skills & Skill Levels">
              {slots.length === 0 ? (
                <PlaceholderText />
              ) : (
                <div className="grid gap-3">
                  {slots.map((slot) => (
                    <div key={slot.slotNumber}>
                      <p className="text-title font-medium">
                        {slotTitle(slot)}
                      </p>
                      <p>{requiredSkillsText(slot)}</p>
                    </div>
                  ))}
                </div>
              )}
            </RequirementSection>

            <RequirementSection title="Uppdragskrav - IT-relaterade Erfarenheter">
              <PlaceholderText />
            </RequirementSection>

            <RequirementSection title="Uppdragskrav - Utbildning ">
              <PlaceholderText />
            </RequirementSection>

            <RequirementSection title="Uppdragskrav - Språk">
              <PlaceholderText />
            </RequirementSection>

            <RequirementSection title="Uppdragskrav - Övriga Krav">
              <PlaceholderText />
            </RequirementSection>

            <RequirementSection title="Uppdragskrav - Arbetsläge">
              <p>{displayWorkMode(mission.specification.workMode)}</p>
            </RequirementSection>
          </div>
        </Section>

        <Section title="En dag på jobbet">
          <p>{presentationText(mission, "oneDayAtWork")}</p>
        </Section>

        <Section title="Vårt tekniska landskap">
          <p>{presentationText(mission, "technicalLandscape")}</p>
        </Section>

        <Section title="Vem söker vi?">
          <p>{presentationText(mission, "whoWeAreLookingFor")}</p>
        </Section>

        <Section title="Vad vi erbjuder">
          <p>{presentationText(mission, "whatWeOffer")}</p>
        </Section>

        <Section title={`Om ${customerName}`}>
          <p>{presentationText(mission, "aboutCustomer")}</p>
        </Section>

        <Section title="Rekryteringsprocess">
          <p>{presentationText(mission, "recruitmentProcess")}</p>
        </Section>
      </div>
    </article>
  );
}
