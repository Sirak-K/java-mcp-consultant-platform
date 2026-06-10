import { PublicSiteHeader } from "../navigation/PublicSiteHeader";

const plans = [
  {
    name: "Startplan",
    features: [
      "Kundföretagskonto",
      "5 aktiva uppdrag",
      "2 uppdragsplatser per uppdrag",
    ],
  },
  {
    name: "Mellanplan",
    features: [
      "Kundföretagskonto",
      "20 aktiva uppdrag",
      "8 uppdragsplatser per uppdrag",
    ],
  },
  {
    name: "Stor plan",
    features: [
      "Kundföretagskonto",
      "Obegränsat antal uppdrag",
      "Obegränsat antal uppdragsplatser",
    ],
  },
];

export default function ProductPlansPage() {
  return (
    <div className="min-h-screen overflow-hidden">
      <PublicSiteHeader />

      <main className="relative mx-auto min-h-[calc(100vh-89px)] w-full max-w-6xl px-6 py-12">
        <div className="pointer-events-none absolute inset-x-6 top-10 h-72 rounded-full bg-cyan-400/10 blur-3xl" />
        <section className="relative">
          <p className="text-label text-center text-xs font-semibold uppercase tracking-[0.24em]">
            Produkter
          </p>
          <h1 className="text-title mt-4 text-center text-3xl font-semibold tracking-tight sm:text-4xl">
            Abonnemangsplaner
          </h1>

          <div className="mt-8 grid gap-5 md:grid-cols-3">
            {plans.map((plan) => (
              <article
                key={plan.name}
                className="panel flex min-h-72 flex-col rounded-lg bg-cyan-200/80 p-6 text-slate-950 shadow-none"
              >
                <h2 className="text-xl font-semibold underline underline-offset-4">
                  {plan.name}
                </h2>
                <ul className="mt-6 space-y-4 text-sm font-semibold leading-6">
                  {plan.features.map((feature) => (
                    <li key={feature}>- {feature}</li>
                  ))}
                </ul>
              </article>
            ))}
          </div>
        </section>
      </main>
    </div>
  );
}
