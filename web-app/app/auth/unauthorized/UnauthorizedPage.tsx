import { Link } from "react-router";

export default function UnauthorizedPage() {
  return (
    <div className="panel w-full rounded p-8 text-center">
      <h1 className="text-title mb-2 text-2xl font-semibold">
        401 - Saknar behörighet
      </h1>
      <p className="text-muted mb-6 text-sm">Du saknar behörighet till denna yta.</p>
      <Link to="/login" className="link-color text-sm">
        Tillbaka till inloggning
      </Link>
    </div>
  );
}
