import { useEffect, useState } from "react";
import { ErrorPlaceholder, LoadingPlaceholder } from "~/shared/components";
import { missionsApi, missionsApiPaths } from "~/missions/api/missionsApi";
import type { RegisteredMissionView } from "~/missions/types";
import type { ApiError } from "~/shared/api/apiErrors";
import { RegisteredMissionArticle } from "~/missions/registered/RegisteredMissionArticle";

const contract = `GET ${missionsApiPaths.registeredMissions} -> RegisteredMissionView[]`;

export default function RegisteredMissionsPage() {
  const [missions, setMissions] = useState<
    RegisteredMissionView[] | null
  >(null);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState<string | null>(null);

  useEffect(() => {
    let mounted = true;
    missionsApi
      .listRegisteredMissions()
      .then((result) => {
        if (mounted) {
          setMissions(result);
          setError(null);
        }
      })
      .catch((err: ApiError) => {
        if (mounted) {
          setError(
            err.message ?? "Kunde inte ladda registrerade missions.",
          );
        }
      })
      .finally(() => {
        if (mounted) {
          setLoading(false);
        }
      });

    return () => {
      mounted = false;
    };
  }, []);

  if (loading) {
    return <LoadingPlaceholder />;
  }

  return (
    <div className="mx-auto grid w-full max-w-6xl gap-6 p-6">
      <header className="max-w-4xl">
        <h1 className="text-title mt-2 text-2xl font-semibold">Alla Uppdrag</h1>
        <p className="text-muted mt-2 text-sm leading-6">Godk?nda uppdrag.</p>
      </header>

      {error && (
        <div>
          <ErrorPlaceholder message={error} />
          <div className="panel mt-4 rounded p-4 text-sm">
            <h2 className="text-section mb-2 font-medium">
              F?rv?ntat API-kontrakt
            </h2>
            <p className="text-muted">{contract}</p>
          </div>
        </div>
      )}

      {!error && (!missions || missions.length === 0) && (
        <div className="panel rounded p-6 text-sm">
          <p className="text-soft">
            Inga registrerade missions finns att visa.
          </p>
        </div>
      )}

      {!error && missions && missions.length > 0 && (
        <div className="grid gap-6">
          {missions.map((mission) => (
            <RegisteredMissionArticle
              key={mission.id}
              mission={mission}
            />
          ))}
        </div>
      )}
    </div>
  );
}
