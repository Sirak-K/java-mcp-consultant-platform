import { Link } from "react-router";

export function PublicSiteHeader() {
  return (
    <header className="header">
      <div className="flex w-full items-center justify-between gap-8 px-6 py-6">
        <Link to="/" className="public-site-logo">
          Consultant Platform
        </Link>

        <nav className="flex flex-wrap items-center justify-end gap-12 text-sm font-semibold">
          <Link to="/hitta-konsult" className="text-header-link">
            Hitta konsult
          </Link>
          <Link to="/hitta-uppdrag" className="text-header-link">
            Hitta uppdrag
          </Link>
          <Link to="/products" className="text-header-link">
            Produkter
          </Link>
          <Link to="/login" className="btn btn-main rounded px-4 py-2 text-sm font-medium">
            Logga in
          </Link>
        </nav>
      </div>
    </header>
  );
}
