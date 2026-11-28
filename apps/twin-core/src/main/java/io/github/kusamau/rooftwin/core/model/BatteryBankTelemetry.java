package io.github.kusamau.rooftwin.core.model;

import com.fasterxml.jackson.annotation.JsonInclude;
import java.time.Instant;

/**
 * Aggregate telemetry of {@code dtmi:io:github:kusamau:rooftwin:BatteryBank;1}. Both values are
 * estimates produced by the twin (Lesson 6), not measurements, so both are optional.
 *
 * @param twinId twin instance identifier
 * @param observedAt time the estimate refers to
 * @param effectiveCapacityAhEstimated estimated usable capacity of the whole bank, in Ah
 * @param healthIndex 0 = end of life, 1 = as new
 */
@JsonInclude(JsonInclude.Include.NON_NULL)
public record BatteryBankTelemetry(
    String twinId, Instant observedAt, Double effectiveCapacityAhEstimated, Double healthIndex)
    implements Telemetry {

  /** Validates values. */
  public BatteryBankTelemetry {
    Checks.requireId("twinId", twinId);
    Checks.requireNonNull("observedAt", observedAt);
    Checks.requireNonNegativeIfPresent(
        "effectiveCapacityAhEstimated", effectiveCapacityAhEstimated);
    Checks.requireRangeIfPresent("healthIndex", healthIndex, 0.0, 1.0);
  }
}
