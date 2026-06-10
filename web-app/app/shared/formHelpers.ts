export function words(value: string): string[] {
  return value.trim().split(/\s+/).filter(Boolean);
}

export function wordCount(value: string): number {
  return words(value).length;
}

export function limitWords(value: string, maxWords: number): string {
  const parts = words(value);
  if (parts.length <= maxWords) {
    return value;
  }
  return parts.slice(0, maxWords).join(" ");
}

export function normalizeDateInput(value: string): string {
  const visibleSeparators = value.replace(/[^\d-]/g, "").slice(0, 10);
  if (
    /^\d{4}-$/.test(visibleSeparators) ||
    /^\d{4}-\d{2}-$/.test(visibleSeparators)
  ) {
    return visibleSeparators;
  }
  const digits = value.replace(/\D/g, "").slice(0, 8);
  if (digits.length <= 4) {
    return digits;
  }
  if (digits.length <= 6) {
    return `${digits.slice(0, 4)}-${digits.slice(4)}`;
  }
  return `${digits.slice(0, 4)}-${digits.slice(4, 6)}-${digits.slice(6)}`;
}
