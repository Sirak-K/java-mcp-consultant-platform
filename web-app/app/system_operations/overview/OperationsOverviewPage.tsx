import { useEffect, useState } from "react";
import { systemOperationsApi } from "~/system_operations/api/systemOperationsApi";
import type { HealthStatus, OpsOverview } from "~/system_operations/types";
import type { ApiError } from "~/shared/api/apiErrors";
import { LoadingPlaceholder, ErrorPlaceholder } from "~/shared/components";

export default function OperationsOverviewPage() {
  const [health, setHealth] = useState<HealthStatus | null>(null);
  const [overview, setOverview] = useState<OpsOverview | null>(null);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState<string | null>(null);

  useEffect(() => {
    Promise.all([
      systemOperationsApi.health(),
      systemOperationsApi.overview(),
    ])
      .then(([h, o]) => {
        setHealth(h);
        setOverview(o);
      })
      .catch((err: ApiError) =>
        setError(err.message ?? "Kunde inte ladda overview"),
      )
      .finally(() => setLoading(false));
  }, []);

  if (loading) return <LoadingPlaceholder />;
  if (error) return <ErrorPlaceholder message={error} />;

  const healthColor =
    health?.status === "UP"
      ? "text-success"
      : health?.status === "DOWN"
        ? "text-danger"
        : "text-muted";

  return (
    <div className="mx-auto w-full max-w-6xl p-6">
      <h1 className="text-title mb-6 text-2xl font-semibold">Översikt</h1>

      <section>
        <h2 className="text-section mb-3 text-base font-medium">Status</h2>
        <div className="panel space-y-2 rounded p-4 text-sm">
          <div>
            <span className="text-muted">Status: </span>
            <span className={`font-medium ${healthColor}`}>
              {health?.status ?? "Okänd"}
            </span>
          </div>
          {health?.components &&
            Object.entries(health.components).map(([key, val]) => (
              <div key={key}>
                <span className="text-muted">{key}: </span>
                <span
                  className={
                    val.status === "UP" ? "text-success" : "text-danger"
                  }
                >
                  {val.status}
                </span>
              </div>
            ))}
        </div>
      </section>

      <section className="mt-6">
        <h2 className="text-section mb-3 text-base font-medium">
          Kommunikation
        </h2>
        <div className="panel space-y-1 rounded p-4 text-sm">
          <div>
            <span className="text-muted">Aktiva sessioner: </span>
            <span className="text-title">
              {overview?.activeSessions ?? "—"}
            </span>
          </div>
          {overview?.lastSessionAt && (
            <div>
              <span className="text-muted">Senaste session: </span>
              <span className="text-title">{overview.lastSessionAt}</span>
            </div>
          )}
        </div>
      </section>

      <section className="mt-6">
        <h2 className="text-section mb-3 text-base font-medium">Data</h2>
        <div className="panel grid grid-cols-2 gap-3 rounded p-4 text-sm">
          <div>
            <span className="text-muted">Antal Kandidater: </span>
            <span className="text-title font-medium">
              {overview?.candidateCount ?? "—"}
            </span>
          </div>
          <div>
            <span className="text-muted">Antal Uppdrag: </span>
            <span className="text-title font-medium">
              {overview?.totalMissions ?? "—"}
            </span>
          </div>
        </div>
      </section>
    </div>
  );
}
