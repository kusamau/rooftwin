package io.github.kusamau.rooftwin.core.model;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import java.time.Instant;

/**
 * Telemetry of {@code dtmi:io:github:kusamau:rooftwin:SmartShunt;1}: the whole bank, as seen from
 * its shared negative.
 *
 * @param twinId twin instance identifier
 * @param observedAt observation time
 * @param voltageV bus voltage, in volts (bus-voltage reading #2)
 * @param currentA bank current, in amperes; positive = charging, negative = discharging
 * @param socPct coulomb-counted state of charge, in percent [0, 100]
 * @param consumedAh charge consumed since the last full charge, in Ah (zero or negative)
 * @param timeToGoMin time-to-go estimate, in minutes (optional: absent while charging)
 * @param alarmFlags Victron alarm bit field, 0 = no alarm
 */
@JsonInclude(JsonInclude.Include.NON_NULL)
public record SmartShuntTelemetry(
    String twinId,
    Instant observedAt,
    @JsonProperty(required = true) double voltageV,
    @JsonProperty(required = true) double currentA,
    @JsonProperty(required = true) double socPct,
    @JsonProperty(required = true) double consumedAh,
    Double timeToGoMin,
    @JsonProperty(required = true) int alarmFlags)
    implements Telemetry {

  /** Validates values. */
  public SmartShuntTelemetry {
    Checks.requireId("twinId", twinId);
    Checks.requireNonNull("observedAt", observedAt);
    Checks.requireNonNegative("voltageV", voltageV);
    Checks.requireFinite("currentA", currentA);
    Checks.requireRange("socPct", socPct, 0.0, 100.0);
    Checks.requireFinite("consumedAh", consumedAh);
    Checks.requireNonNegativeIfPresent("timeToGoMin", timeToGoMin);
  }

  /**
   * True when the bank is taking charge (current into the batteries).
   *
   * <p>{@code @JsonIgnore}: Jackson treats any {@code isX()} as a property, so without it the JSON
   * would gain a {@code "charging"} field that the strict mapper then rejects on the way back in.
   */
  @JsonIgnore
  public boolean isCharging() {
    return currentA > 0.0;
  }
}
