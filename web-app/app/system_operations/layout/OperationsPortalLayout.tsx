import { Navigate, Outlet } from "react-router";
import { useAuthContext } from "~/auth/AuthContext";
import { OperationsPortalNavigation } from "~/system_operations/navigation/OperationsPortalNavigation";

export default function OperationsPortalLayout() {
  const { user, loading } = useAuthContext();

  if (loading) return null;
  if (!user) return <Navigate to="/login" replace />;
  if (!user.isPlatformSystem) return <Navigate to="/unauthorized" replace />;

  return (
    <div className="min-h-screen flex flex-col">
      <OperationsPortalNavigation />
      <main className="page-ops flex-1 overflow-y-auto p-3 sm:p-4 lg:p-6">
        <Outlet />
      </main>
    </div>
  );
}
