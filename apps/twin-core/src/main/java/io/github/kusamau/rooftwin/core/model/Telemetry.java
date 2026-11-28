package io.github.kusamau.rooftwin.core.model;

import com.fasterxml.jackson.annotation.JsonSubTypes;
import com.fasterxml.jackson.annotation.JsonTypeInfo;
import java.time.Instant;

/**
 * One telemetry snapshot from one twin, mirroring the Telemetry contents of a DTDL interface.
 *
 * <p>The JSON form carries the interface's DTMI in a {@code "$model"} field, so a message says by
 * itself which model it conforms to:
 *
 * <pre>{@code
 * {"$model": "dtmi:io:github:kusamau:rooftwin:SmartShunt;1",
 *  "twinId": "shunt-1", "observedAt": "2026-11-19T12:00:00Z", "voltageV": 12.71, ...}
 * }</pre>
 *
 * <p>Conventions shared by all implementations:
 *
 * <ul>
 *   <li>Component names match the DTDL Telemetry names exactly, units included in the name. {@code
 *       DtdlConformanceTest} fails the build if they drift apart.
 *   <li>A primitive field is always present in live mode. A boxed {@code Double} is optional:
 *       {@code null} means "not observed", and the field is left out of the JSON, never written as
 *       {@code null}. Zero is a measurement; absence is not.
 *   <li>{@code twinId} and {@code observedAt} are the envelope, not DTDL contents.
 * </ul>
 *
 * <p>The interface is sealed: adding a device means adding a DTDL interface, a record and a
 * {@code @JsonSubTypes} entry, and the compiler points at every {@code switch} that must handle it.
 */
@JsonTypeInfo(use = JsonTypeInfo.Id.NAME, include = JsonTypeInfo.As.PROPERTY, property = "$model")
@JsonSubTypes({
  @JsonSubTypes.Type(value = SolarArrayTelemetry.class, name = Dtmi.SOLAR_ARRAY),
  @JsonSubTypes.Type(value = MpptTelemetry.class, name = Dtmi.MPPT_CHARGE_CONTROLLER),
  @JsonSubTypes.Type(value = SmartShuntTelemetry.class, name = Dtmi.SMART_SHUNT),
  @JsonSubTypes.Type(value = BatteryBankTelemetry.class, name = Dtmi.BATTERY_BANK),
  @JsonSubTypes.Type(value = BatteryTelemetry.class, name = Dtmi.BATTERY)
})
public sealed interface Telemetry
    permits SolarArrayTelemetry,
        MpptTelemetry,
        SmartShuntTelemetry,
        BatteryBankTelemetry,
        BatteryTelemetry {

  /** Identifier of the twin instance that produced the snapshot (e.g. {@code "mppt-1"}). */
  String twinId();

  /** When the physical device observed the values (not when the twin received them). */
  Instant observedAt();
}
