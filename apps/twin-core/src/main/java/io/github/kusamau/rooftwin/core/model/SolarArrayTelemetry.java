package io.github.kusamau.rooftwin.core.model;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import java.time.Instant;

/**
 * Telemetry of {@code dtmi:io:github:kusamau:rooftwin:SolarArray;1}.
 *
 * <p>No sensor sits on the array. {@code powerW} is copied from the MPPT; everything else is
 * optional and, until Lesson 5, absent.
 *
 * @param twinId twin instance identifier
 * @param observedAt observation time
 * @param panelVoltageV array voltage, in volts (optional)
 * @param panelCurrentA array current, in amperes (optional)
 * @param powerW array output power, in watts
 * @param irradianceWm2 plane-of-array irradiance derived by the PV model, in W/m² (optional)
 * @param moduleTempC estimated module temperature, in °C (optional)
 */
@JsonInclude(JsonInclude.Include.NON_NULL)
public record SolarArrayTelemetry(
    String twinId,
    Instant observedAt,
    Double panelVoltageV,
    Double panelCurrentA,
    @JsonProperty(required = true) double powerW,
    Double irradianceWm2,
    Double moduleTempC)
    implements Telemetry {

  /** Validates values. */
  public SolarArrayTelemetry {
    Checks.requireId("twinId", twinId);
    Checks.requireNonNull("observedAt", observedAt);
    Checks.requireNonNegativeIfPresent("panelVoltageV", panelVoltageV);
    Checks.requireNonNegativeIfPresent("panelCurrentA", panelCurrentA);
    Checks.requireNonNegative("powerW", powerW);
    Checks.requireNonNegativeIfPresent("irradianceWm2", irradianceWm2);
    Checks.requireFiniteIfPresent("moduleTempC", moduleTempC);
  }
}
