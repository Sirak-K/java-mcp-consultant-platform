export const ROLE_EXPERIENCE_OPTIONS = [
  { label: "JUNIOR (Mer än 0 år)", years: 0 },
  { label: "INTERMEDIATE (Mer än 3 år)", years: 3 },
  { label: "SENIOR (Mer än 5 år)", years: 5 },
] as const;

const ROLE_EXPERIENCE_YEARS = new Set<number>(
  ROLE_EXPERIENCE_OPTIONS.map((option) => option.years),
);

export type RoleExperienceTier = "JUNIOR" | "INTERMEDIATE" | "SENIOR";

export function normalizeRoleExperienceYears(value: number): number {
  if (ROLE_EXPERIENCE_YEARS.has(value)) {
    return value;
  }
  if (value >= 5) {
    return 5;
  }
  if (value >= 3) {
    return 3;
  }
  return ROLE_EXPERIENCE_OPTIONS[0].years;
}

export function roleExperienceTierLabel(years: number): RoleExperienceTier {
  if (years >= 5) {
    return "SENIOR";
  }
  if (years >= 3) {
    return "INTERMEDIATE";
  }
  return "JUNIOR";
}

export function roleExperienceTitleLabel(years: number): string {
  const tier = roleExperienceTierLabel(years);
  return tier.charAt(0) + tier.slice(1).toLocaleLowerCase();
}

export function optionalRoleExperienceTierLabel(
  years: number | null | undefined,
  fallback = "N/A",
): string {
  if (years === null || years === undefined) {
    return fallback;
  }
  return roleExperienceTierLabel(years);
}
