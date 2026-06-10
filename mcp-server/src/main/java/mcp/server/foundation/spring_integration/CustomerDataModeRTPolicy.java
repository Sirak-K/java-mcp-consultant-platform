package mcp.server.foundation.spring_integration;

import java.util.Arrays;
import java.util.List;
import java.util.Locale;
import java.util.Objects;

public final class CustomerDataModeRTPolicy {

  public static final String POSTGRES_MODE = "postgresql";
  public static final String IN_MEMORY_MODE = "in-memory";

  public static final String POSTGRES_FLYWAY_LOCATION = "classpath:db/migration/postgresql";

  private static final List<String> SUPPORTED_MODES = List.of(
      POSTGRES_MODE,
      IN_MEMORY_MODE);

  private final String activeMode;
  private final String activeFlywayLocations;
  private final String activeSchemaValidationMode;

  public CustomerDataModeRTPolicy(
      String activeMode,
      String activeFlywayLocations,
      String activeSchemaValidationMode) {

    this.activeMode = normalizeText(activeMode, "activeMode");
    validateSupportedMode(this.activeMode);

    if (isFlywayRequired(this.activeMode)) {
      this.activeFlywayLocations = normalizeText(activeFlywayLocations, "activeFlywayLocations");
      validateRequiredFlywayLocation(this.activeMode, this.activeFlywayLocations);
    } else {
      this.activeFlywayLocations = "";
    }

    this.activeSchemaValidationMode = normalizeText(activeSchemaValidationMode, "activeSchemaValidationMode");
  }

  public String CustomerDataModeRTPolicyActiveMode() {
    return activeMode;
  }

  public String CustomerDataModeRTPolicyActiveFlywayLocations() {
    return activeFlywayLocations;
  }

  public String CustomerDataModeRTPolicyActiveSchemaValidationMode() {
    return activeSchemaValidationMode;
  }

  public List<String> CustomerDataModeRTPolicySupportedModes() {
    return SUPPORTED_MODES;
  }

  public boolean CustomerDataModeRTPolicyIsFlywayRequired() {
    return isFlywayRequired(activeMode);
  }

  public boolean CustomerDataModeRTPolicyRequiresJdbcDataSource() {
    return !IN_MEMORY_MODE.equals(activeMode);
  }

  public String CustomerDataModeRTPolicyRequiredFlywayLocationForActiveMode() {
    return requiredFlywayLocation(activeMode);
  }

  private static boolean isFlywayRequired(String normalizedMode) {
    return !IN_MEMORY_MODE.equals(normalizedMode);
  }

  private static void validateSupportedMode(String normalizedMode) {
    if (!SUPPORTED_MODES.contains(normalizedMode)) {
      throw new IllegalStateException(
          "Unsupported mcp.customer-data.mode="
              + normalizedMode
              + ". Supported modes: "
              + SUPPORTED_MODES);
    }
  }

  private static void validateRequiredFlywayLocation(
      String normalizedMode,
      String normalizedFlywayLocations) {

    String requiredFlywayLocation = requiredFlywayLocation(normalizedMode);
    boolean requiredLocationPresent = Arrays.stream(normalizedFlywayLocations.split(","))
        .map(String::trim)
        .filter(value -> !value.isBlank())
        .anyMatch(requiredFlywayLocation::equals);

    if (!requiredLocationPresent) {
      throw new IllegalStateException(
          "Active customer-data mode "
              + normalizedMode
              + " requires spring.flyway.locations to include "
              + requiredFlywayLocation
              + ".");
    }
  }

  private static String requiredFlywayLocation(String normalizedMode) {
    return switch (normalizedMode) {
      case POSTGRES_MODE -> POSTGRES_FLYWAY_LOCATION;
      case IN_MEMORY_MODE -> null;
      default -> throw new IllegalStateException("Unsupported customer-data mode: " + normalizedMode);
    };
  }

  private static String normalizeText(String rawValue, String fieldName) {
    Objects.requireNonNull(rawValue, fieldName);

    String normalized = rawValue.trim().toLowerCase(Locale.ROOT);
    if (normalized.isBlank()) {
      throw new IllegalArgumentException(fieldName + " must not be blank");
    }
    return normalized;
  }
}
