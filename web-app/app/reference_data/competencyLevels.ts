export function formatCompetencyLevelLabel(value: string | null | undefined): string {
  switch (value?.trim().toUpperCase()) {
    case "SENIOR":
      return "SENIOR (Mer än 5 år)";
    case "INTERMEDIATE":
      return "INTERMEDIATE (Mer än 3 år)";
    case "JUNIOR":
      return "JUNIOR (Mer än 0 år)";
    default:
      return value?.trim() || "";
  }
}
