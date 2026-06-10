import { Link } from "react-router";
import { PublicSiteHeader } from "../navigation/PublicSiteHeader";

export default function LandingPage() {
  return (
    <div className="flex min-h-screen flex-col overflow-x-hidden md:h-screen md:overflow-hidden">
      <PublicSiteHeader />

      <main className="relative mx-auto flex w-full max-w-6xl flex-1 px-6 py-8 md:py-10">
        <div className="pointer-events-none absolute inset-x-6 top-8 h-64 rounded-full bg-cyan-400/10 blur-3xl" />
        <div className="relative flex w-full flex-col justify-start gap-7 pt-10 sm:pt-16 lg:pt-20">
          <div className="max-w-3xl">
            <p className="text-label text-xs font-semibold uppercase tracking-[0.24em]">
              Expertis som gör skillnad
            </p>
            <h1 className="text-title mt-4 text-4xl font-semibold tracking-tight sm:text-6xl">
              Framtidens kompetensmatchning är här.
            </h1>
            <p className="text-muted mt-5 max-w-xl text-base leading-7">
              Konsultmarknad utan friktion.
            </p>
          </div>

          <div className="grid gap-5 md:grid-cols-2">
            <Link
              to="/hitta-konsult"
              className="landing-page-action-card card-hover"
            >
              <p className="text-label text-xs font-semibold uppercase tracking-[0.22em]">
                För kundföretag
              </p>
              <h2 className="text-title text-2xl font-semibold">
                Hitta Konsult
              </h2>

              <span className="text-link inline-flex text-sm font-semibold">
                Beskriv uppdraget
              </span>
            </Link>

            <Link
              to="/hitta-uppdrag"
              className="landing-page-action-card card-hover"
            >
              <p className="text-label text-xs font-semibold uppercase tracking-[0.22em]">
                För konsulter
              </p>
              <h2 className="text-title text-2xl font-semibold">
                Hitta Uppdrag
              </h2>

              <span className="text-link inline-flex text-sm font-semibold">
                Anmäl konsultprofil
              </span>
            </Link>
          </div>
        </div>
      </main>
    </div>
  );
}
