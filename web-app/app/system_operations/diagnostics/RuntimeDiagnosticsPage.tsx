import { useEffect, useState } from "react";
import { systemOperationsApi } from "~/system_operations/api/systemOperationsApi";
import type { BindingEntry, SessionEntry } from "~/system_operations/types";
import type { ApiError } from "~/shared/api/apiErrors";
import { LoadingPlaceholder, ErrorPlaceholder } from "~/shared/components";

export default function RuntimeDiagnosticsPage() {
  const [sessions, setSessions] = useState<SessionEntry[] | null>(null);
  const [bindings, setBindings] = useState<BindingEntry[] | null>(null);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState<string | null>(null);

  useEffect(() => {
    Promise.all([
      systemOperationsApi.diagnosticSessions(),
      systemOperationsApi.diagnosticBindings(),
    ])
      .then(([s, b]) => {
        setSessions(s);
        setBindings(b);
      })
      .catch((err: ApiError) =>
        setError(err.message ?? "Kunde inte ladda diagnostics"),
      )
      .finally(() => setLoading(false));
  }, []);

  if (loading) return <LoadingPlaceholder />;
  if (error) return <ErrorPlaceholder message={error} />;

  return (
    <div className="p-6">
      <h1 className="text-title mb-6 text-2xl font-semibold">Diagnostik</h1>

      {/* Sessions */}
      <section>
        <h2 className="text-section mb-3 text-base font-medium">
          Aktiva Sessioner
        </h2>
        <div className="table overflow-hidden rounded text-sm">
          <div className="table-header grid grid-cols-4 gap-4 px-4 py-2 text-xs font-medium uppercase">
            <span>Session ID</span>
            <span>Tenant Typ</span>
            <span>Företag ID</span>
            <span>Skapad</span>
          </div>
          {!sessions || sessions.length === 0 ? (
            <div className="text-soft px-4 py-6 text-center">
              Inga aktiva sessioner.
            </div>
          ) : (
            sessions.map((s) => (
              <div
                key={s.id}
                className="table-row grid grid-cols-4 items-center gap-4 px-4 py-3"
              >
                <span className="text-label font-mono text-xs">{s.id}</span>
                <span className="text-muted">{s.tenantType}</span>
                <span className="text-muted">{s.dbCompanyId}</span>
                <span className="text-muted text-xs">{s.createdAt ?? "—"}</span>
              </div>
            ))
          )}
        </div>
      </section>

      {/* Bindings */}
      <section className="mt-6">
        <h2 className="text-section mb-3 text-base font-medium">
          Tenant-kopplingar
        </h2>
        <div className="table overflow-hidden rounded text-sm">
          <div className="table-header grid grid-cols-3 gap-4 px-4 py-2 text-xs font-medium uppercase">
            <span>Kund ID</span>
            <span>Tenant Typ</span>
            <span>Plattform System</span>
          </div>
          {!bindings || bindings.length === 0 ? (
            <div className="text-soft px-4 py-6 text-center">
              Inga bindings registrerade.
            </div>
          ) : (
            bindings.map((b, i) => (
              <div
                key={i}
                className="table-row grid grid-cols-3 items-center gap-4 px-4 py-3"
              >
                <span className="text-title font-medium">{b.dbCompanyId}</span>
                <span className="text-muted">{b.tenantType}</span>
                <span
                  className={
                    b.isPlatformSystem
                      ? "text-success font-medium"
                      : "text-placeholder"
                  }
                >
                  {b.isPlatformSystem ? "Ja" : "Nej"}
                </span>
              </div>
            ))
          )}
        </div>
      </section>
    </div>
  );
}
