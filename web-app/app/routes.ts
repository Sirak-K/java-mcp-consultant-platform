import { type RouteConfig, index, layout, route } from "@react-router/dev/routes";

export default [

  index("public_site/landing/LandingPage.tsx"),
  route("hitta-konsult", "missions/intake/MissionIntakePage.tsx"),
  route("hitta-uppdrag", "candidate_profiles/intake/CandidateApplicationIntakePage.tsx"),
  route("products", "public_site/products/ProductPlansPage.tsx"),

  // Auth
  layout("auth/layout/AuthLayout.tsx", [
    route("login", "auth/login/LoginPage.tsx"),
    route("unauthorized", "auth/unauthorized/UnauthorizedPage.tsx"),
  ]),

  // Ops portal
  layout("system_operations/layout/OperationsPortalLayout.tsx", [
    route("ops/overview", "system_operations/overview/OperationsOverviewPage.tsx"),
    route("ops/triage", "system_operations/triage/OperationsTriagePage.tsx"),
    route(
      "ops/match-notifications/previews",
      "match_notifications/preview/MatchNotificationPreviewPage.tsx",
    ),
    route("ops/review/mission-proposals", "missions/review/MissionProposalReviewPage.tsx"),
    route("ops/registered-missions", "missions/registered/RegisteredMissionsPage.tsx"),
    route("ops/matches-viewer", "matching/viewer/MatchViewerPage.tsx"),
    route("ops/candidate-presentation-artifacts", "candidate_presentation/artifacts/CandidatePresentationArtifactsPage.tsx"),
    route("ops/review/candidate-applications", "candidate_profiles/application_review/CandidateApplicationReviewPage.tsx"),
    route("ops/registered-candidate-profiles", "candidate_profiles/registered_profiles/RegisteredCandidateProfilesPage.tsx"),
    route("ops/diagnostics", "system_operations/diagnostics/RuntimeDiagnosticsPage.tsx"),
  ]),

] satisfies RouteConfig;
