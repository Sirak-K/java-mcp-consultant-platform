package mcp.server.domain.shared_kernel.validation;

import java.time.LocalDate;
import java.time.format.DateTimeParseException;

/**
 * Small validation helpers for cross-domain application input.
 */
public final class ApplicationInputValidation {

    private ApplicationInputValidation() {
    }

    public static void requireWorkMode(String workMode) {
        requireText(workMode, "workMode is required");
        String normalized = workMode.trim().toUpperCase();
        if (!"ON_PREMISE".equals(normalized)
                && !"REMOTE".equals(normalized)
                && !"HYBRID".equals(normalized)) {
            throw reject("workMode must be ON_PREMISE, REMOTE or HYBRID");
        }
    }

    public static void requireEmail(String value, String message) {
        requireText(value, message);
        if (!value.contains("@")) {
            throw reject(message);
        }
    }

    public static void requireText(String value, String message) {
        if (value == null || value.isBlank()) {
            throw reject(message);
        }
    }

    public static LocalDate parseDate(String value, String fieldName) {
        try {
            return LocalDate.parse(value);
        } catch (DateTimeParseException exception) {
            throw reject(fieldName + " must use ISO date format yyyy-MM-dd");
        }
    }

    public static LocalDate parseOptionalDate(String value, String fieldName) {
        String normalized = safeText(value).trim();
        if (normalized.isBlank()) {
            return null;
        }
        return parseDate(normalized, fieldName);
    }

    public static Short toShort(int value, String fieldName) {
        if (value > Short.MAX_VALUE) {
            throw reject(fieldName + " is too large");
        }
        return (short) value;
    }

    public static String safeText(String value) {
        return value == null ? "" : value;
    }

    public static InvalidApplicationInputException reject(String message) {
        return new InvalidApplicationInputException(message);
    }
}
