package io.github.kusamau.rooftwin.core.model;

import com.fasterxml.jackson.annotation.JsonInclude;
import java.time.Instant;
import java.util.Set;

/**
 * Telemetry of one {@code dtmi:io:github:kusamau:rooftwin:Battery;1} Component inside the battery
 * bank.
 *
 * <p>A DTDL Component has no twin of its own: it is addressed as <em>bank twin + component
 * name</em>. That's why this record carries {@code component} next to {@code twinId}, where {@code
 * twinId} is the bank's.
 *
 * @param twinId the battery bank's twin identifier
 * @param component DTDL component name: {@code battery_a}, {@code battery_b} or {@code battery_c}
 * @param observedAt time the estimate refers to
 * @param relativeHealthDelta health relative to the bank average; negative = weaker (optional)
 * @param estimatedEsrMilliOhm estimated internal resistance, in mΩ (optional)
 */
@JsonInclude(JsonInclude.Include.NON_NULL)
public record BatteryTelemetry(
    String twinId,
    String component,
    Instant observedAt,
    Double relativeHealthDelta,
    Double estimatedEsrMilliOhm)
    implements Telemetry {

  /** The Component names declared in {@code BatteryBank.json}. */
  public static final Set<String> COMPONENTS = Set.of("battery_a", "battery_b", "battery_c");

  /** Validates values. */
  public BatteryTelemetry {
    Checks.requireId("twinId", twinId);
    if (!COMPONENTS.contains(component)) {
      throw new IllegalArgumentException(
          "component must be one of " + COMPONENTS + ", was " + component);
    }
    Checks.requireNonNull("observedAt", observedAt);
    Checks.requireFiniteIfPresent("relativeHealthDelta", relativeHealthDelta);
    Checks.requireNonNegativeIfPresent("estimatedEsrMilliOhm", estimatedEsrMilliOhm);
  }
}
