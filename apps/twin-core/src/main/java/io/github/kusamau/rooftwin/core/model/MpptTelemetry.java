package io.github.kusamau.rooftwin.core.model;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import java.time.Instant;
import java.util.Optional;

/**
 * Telemetry of {@code dtmi:io:github:kusamau:rooftwin:MpptChargeController;1}.
 *
 * <p>Primitive fields are the ones Victron Instant Readout delivers. PV voltage, PV current and
 * internal temperature exist in the model but not in Instant Readout, so they are optional.
 *
 * @param twinId twin instance identifier
 * @param observedAt observation time
 * @param pvVoltageV PV input voltage, in volts (optional)
 * @param pvCurrentA PV input current, in amperes (optional)
 * @param pvPowerW PV input power, in watts
 * @param batteryVoltageV battery-side voltage, in volts (bus-voltage reading #1)
 * @param batteryCurrentA battery-side charge current, in amperes
 * @param chargeStateCode raw Victron charge-state code (see {@link #chargeState()})
 * @param chargerErrorCode raw Victron charger error code, 0 = no error
 * @param dailyEnergyKwh energy yield since midnight, in kWh
 * @param internalTempC controller temperature, in °C (optional)
 */
@JsonInclude(JsonInclude.Include.NON_NULL)
public record MpptTelemetry(
    String twinId,
    Instant observedAt,
    Double pvVoltageV,
    Double pvCurrentA,
    @JsonProperty(required = true) double pvPowerW,
    @JsonProperty(required = true) double batteryVoltageV,
    @JsonProperty(required = true) double batteryCurrentA,
    @JsonProperty(required = true) int chargeStateCode,
    @JsonProperty(required = true) int chargerErrorCode,
    @JsonProperty(required = true) double dailyEnergyKwh,
    Double internalTempC)
    implements Telemetry {

  /** Validates values. The raw charge-state code is kept even if this code base doesn't know it. */
  public MpptTelemetry {
    Checks.requireId("twinId", twinId);
    Checks.requireNonNull("observedAt", observedAt);
    Checks.requireNonNegativeIfPresent("pvVoltageV", pvVoltageV);
    Checks.requireNonNegativeIfPresent("pvCurrentA", pvCurrentA);
    Checks.requireNonNegative("pvPowerW", pvPowerW);
    Checks.requireNonNegative("batteryVoltageV", batteryVoltageV);
    Checks.requireFinite("batteryCurrentA", batteryCurrentA);
    Checks.requireNonNegative("dailyEnergyKwh", dailyEnergyKwh);
    Checks.requireFiniteIfPresent("internalTempC", internalTempC);
  }

  /**
   * The charge state as an enum, or empty for a code this version doesn't know.
   *
   * <p>Rejecting unknown codes at deserialization would drop a whole snapshot because of one field
   * after a firmware update. Keeping the raw code and interpreting it here is the more robust
   * split.
   */
  public Optional<ChargeState> chargeState() {
    return ChargeState.fromCode(chargeStateCode);
  }
}
