import { useEffect, useState } from "react";
import { systemOperationsApi } from "~/system_operations/api/systemOperationsApi";
import type { TriageEntry } from "~/system_operations/types";
import type { ApiError } from "~/shared/api/apiErrors";
import { LoadingPlaceholder, ErrorPlaceholder } from "~/shared/components";

export default function OperationsTriagePage() {
  const [entries, setEntries] = useState<TriageEntry[] | null>(null);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState<string | null>(null);

  useEffect(() => {
    systemOperationsApi
      .triage()
      .then(setEntries)
      .catch((err: ApiError) =>
        setError(err.message ?? "Kunde inte ladda triage"),
      )
      .finally(() => setLoading(false));
  }, []);

  if (loading) return <LoadingPlaceholder />;
  if (error) return <ErrorPlaceholder message={error} />;

  return (
    <div className="p-6">
      <h1 className="text-title mb-6 text-2xl font-semibold">Triage</h1>

      <section>
        <h2 className="text-section mb-3 text-base font-medium">
          Aktiva Signaler
        </h2>
        <div className="table overflow-hidden rounded text-sm">
          <div className="table-header grid grid-cols-4 gap-4 px-4 py-2 text-xs font-medium uppercase">
            <span>Tid</span>
            <span>Type</span>
            <span>Kontext</span>
            <span>Meddelande</span>
          </div>
          {!entries || entries.length === 0 ? (
            <div className="text-soft px-4 py-6 text-center">
              Inga aktiva signaler.
            </div>
          ) : (
            entries.map((e, i) => (
              <div
                key={e.id ?? i}
                className="table-row grid grid-cols-4 items-start gap-4 px-4 py-3"
              >
                <span className="text-muted text-xs">{e.timestamp}</span>
                <span
                  className={`font-medium text-xs ${
                    e.type === "ERROR"
                      ? "text-danger"
                      : e.type === "WARN"
                        ? "text-warning"
                        : "text-muted"
                  }`}
                >
                  {e.type}
                </span>
                <span className="text-muted text-xs">{e.context ?? "—"}</span>
                <span className="text-title text-xs">{e.message}</span>
              </div>
            ))
          )}
        </div>
      </section>

      <section className="mt-6">
        <h2 className="text-section mb-3 text-base font-medium">
          Symptom → Orsak
        </h2>
        <div className="panel text-placeholder rounded p-4 text-sm">
          Manuell analys krävs — se aktiva signaler ovan.
        </div>
      </section>
    </div>
  );
}
