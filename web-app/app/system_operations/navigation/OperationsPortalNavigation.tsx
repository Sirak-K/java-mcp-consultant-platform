import { NavLink, useLocation } from "react-router";
import { useLogout } from "~/auth/authSession";
import { useUnreadMatchNotificationCount } from "~/match_notifications/navigation/useUnreadMatchNotificationCount";

const navLink = ({ isActive }: { isActive: boolean }) =>
  isActive ? "text-header-link-active" : "text-header-link";

const navLinkWithBadge = ({ isActive }: { isActive: boolean }) =>
  `${navLink({ isActive })} relative inline-flex items-center`;

export function OperationsPortalNavigation() {
  const logout = useLogout();
  const location = useLocation();
  const matchNotificationOpen = location.pathname.startsWith(
    "/ops/match-notifications/previews",
  );
  const matchNotificationUnreadCount = useUnreadMatchNotificationCount({
    enabled: true,
    matchNotificationOpen,
  });

  return (
    <nav className="header header-ops flex flex-wrap items-center gap-x-4 gap-y-3 px-4 py-3 sm:px-6">
      <span className="text-header-title mr-2 font-semibold">Ops</span>
      <NavLink to="/ops/overview" className={navLink}>
        Översikt
      </NavLink>
      <NavLink to="/ops/diagnostics" className={navLink}>
        Diagnostik
      </NavLink>
      <NavLink to="/ops/triage" className={navLink}>
        Triage
      </NavLink>
      <NavLink
        to="/ops/match-notifications/previews"
        className={navLinkWithBadge}
      >
        Matchningsbrev
        {matchNotificationUnreadCount > 0 && (
          <span
            aria-label={`${matchNotificationUnreadCount} unread match notifications`}
            className="pointer-events-none absolute -right-4 -top-3 flex h-5 min-w-5 items-center justify-center rounded-full bg-[#22c55e] px-1.5 text-[11px] font-bold leading-none text-[#020617] shadow-[0_0_0_2px_#020617]"
          >
            {matchNotificationUnreadCount > 99
              ? "99+"
              : matchNotificationUnreadCount}
          </span>
        )}
      </NavLink>
      <NavLink to="/ops/matches-viewer" className={navLink}>
        Alla Matchningar
      </NavLink>
      <NavLink to="/ops/review/mission-proposals" className={navLink}>
        Uppdragsgranskning
      </NavLink>
      <NavLink to="/ops/registered-missions" className={navLink}>
        Alla Uppdrag
      </NavLink>
      <NavLink to="/ops/review/candidate-applications" className={navLink}>
        Kandidatansökningar
      </NavLink>
      <NavLink to="/ops/registered-candidate-profiles" className={navLink}>
        Kandidatprofiler
      </NavLink>
      <NavLink to="/ops/candidate-presentation-artifacts" className={navLink}>
        Kandidatpresentationer
      </NavLink>
      <div className="flex shrink-0 items-center gap-3 sm:ml-auto">
        <button
          type="button"
          onClick={() => {
            void logout();
          }}
          className="btn btn-header rounded px-3 py-1.5 text-sm font-medium"
        >
          Logga ut
        </button>
      </div>
    </nav>
  );
}
