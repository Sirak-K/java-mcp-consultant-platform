import type { MatchNotificationSendView } from "../types";
import { sendResultRows } from "./matchNotificationPreviewFormatting";

export function MatchNotificationSendResultPanel({
  result,
}: {
  result: MatchNotificationSendView;
}) {
  return (
    <div className="panel mb-6 rounded p-4 text-sm">
      <p className="text-success font-semibold">{result.status}</p>
      <dl className="mt-2 grid gap-2 sm:grid-cols-2 lg:grid-cols-4">
        {sendResultRows(result).map(([label, value]) => (
          <div key={`${label}:${value}`} className="min-w-0">
            <dt className="text-muted text-xs uppercase">{label}</dt>
            <dd className="text-soft break-words">{value}</dd>
          </div>
        ))}
      </dl>
    </div>
  );
}
