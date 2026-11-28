package io.github.kusamau.rooftwin.core.model;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.io.IOException;
import java.io.InputStream;
import java.io.UncheckedIOException;
import java.time.Instant;
import java.util.Optional;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

class TelemetryJsonTest {

  private static final ObjectMapper JSON = TwinJson.mapper();

  private static final String[] PAYLOADS = {
    "mppt-absorption-clipping.json",
    "mppt-unknown-state.json",
    "shunt-inverter-on.json",
    "shunt-charging.json",
    "solar-array.json",
    "battery-bank-no-estimates.json",
    "battery-c.json",
  };

  static String payload(String name) {
    try (InputStream in = TelemetryJsonTest.class.getResourceAsStream("/payloads/" + name)) {
      if (in == null) {
        throw new IllegalStateException("missing test resource /payloads/" + name);
      }
      return new String(in.readAllBytes(), java.nio.charset.StandardCharsets.UTF_8);
    } catch (IOException e) {
      throw new UncheckedIOException(e);
    }
  }

  @Test
  @DisplayName("every reference payload survives JSON -> record -> JSON unchanged")
  void roundTripsEveryPayload() throws IOException {
    for (String name : PAYLOADS) {
      String original = payload(name);
      Telemetry parsed = JSON.readValue(original, Telemetry.class);
      String written = JSON.writeValueAsString(parsed);
      assertEquals(JSON.readTree(original), JSON.readTree(written), name);
      assertEquals(parsed, JSON.readValue(written, Telemetry.class), name);
    }
  }

  @Test
  @DisplayName("$model selects the record type")
  void dispatchesOnModel() throws IOException {
    assertInstanceOf(
        MpptTelemetry.class,
        JSON.readValue(payload("mppt-absorption-clipping.json"), Telemetry.class));
    assertInstanceOf(
        SmartShuntTelemetry.class,
        JSON.readValue(payload("shunt-inverter-on.json"), Telemetry.class));
    assertInstanceOf(
        BatteryTelemetry.class, JSON.readValue(payload("battery-c.json"), Telemetry.class));
  }

  @Test
  @DisplayName("parsed values equal the hand-built record")
  void parsesToExpectedRecord() throws IOException {
    MpptTelemetry expected =
        new MpptTelemetry(
            "mppt-1",
            Instant.parse("2026-06-21T11:30:00Z"),
            null,
            null,
            447.0,
            14.4,
            29.8,
            4,
            0,
            1.62,
            null);
    assertEquals(
        expected, JSON.readValue(payload("mppt-absorption-clipping.json"), Telemetry.class));
  }

  @Test
  @DisplayName("an absent optional reading stays absent: no \"null\" in the JSON")
  void absentStaysAbsent() throws IOException {
    Telemetry bank = JSON.readValue(payload("battery-bank-no-estimates.json"), Telemetry.class);
    JsonNode written = JSON.valueToTree(bank);
    assertFalse(written.has("healthIndex"));
    assertFalse(written.has("effectiveCapacityAhEstimated"));
    assertNull(((BatteryBankTelemetry) bank).healthIndex());
  }

  @Test
  @DisplayName("a charge-state code we don't know keeps the snapshot and maps to empty")
  void unknownChargeStateIsKept() throws IOException {
    MpptTelemetry t =
        (MpptTelemetry) JSON.readValue(payload("mppt-unknown-state.json"), Telemetry.class);
    assertEquals(99, t.chargeStateCode());
    assertEquals(Optional.empty(), t.chargeState());
    MpptTelemetry clipping =
        (MpptTelemetry) JSON.readValue(payload("mppt-absorption-clipping.json"), Telemetry.class);
    assertEquals(Optional.of(ChargeState.ABSORPTION), clipping.chargeState());
  }

  @Test
  @DisplayName("sign convention: negative shunt current means discharging")
  void shuntSignConvention() throws IOException {
    SmartShuntTelemetry on =
        (SmartShuntTelemetry) JSON.readValue(payload("shunt-inverter-on.json"), Telemetry.class);
    SmartShuntTelemetry sun =
        (SmartShuntTelemetry) JSON.readValue(payload("shunt-charging.json"), Telemetry.class);
    assertFalse(on.isCharging());
    assertTrue(sun.isCharging());
  }

  @Test
  @DisplayName("a field the model doesn't declare is rejected (drift is loud)")
  void rejectsUnknownField() {
    String drifted = payload("shunt-charging.json").replace("\"alarmFlags\"", "\"alarmFlagz\"");
    assertThrows(IOException.class, () -> JSON.readValue(drifted, Telemetry.class));
  }

  @Test
  @DisplayName("a missing or unknown $model is rejected")
  void rejectsMissingOrUnknownModel() {
    String noModel = payload("shunt-charging.json").replaceFirst("\"\\$model\": \"[^\"]+\",", "");
    String wrongModel = payload("shunt-charging.json").replace("SmartShunt;1", "SmartShunt;2");
    assertThrows(IOException.class, () -> JSON.readValue(noModel, Telemetry.class));
    assertThrows(IOException.class, () -> JSON.readValue(wrongModel, Telemetry.class));
  }

  @Test
  @DisplayName("null for a required reading is rejected instead of becoming 0")
  void rejectsNullPrimitive() {
    String nullVoltage = payload("shunt-charging.json").replace("13.38", "null");
    assertThrows(IOException.class, () -> JSON.readValue(nullVoltage, Telemetry.class));
  }

  @Test
  @DisplayName("a missing required reading is rejected: no PV power must not mean 0 W")
  void rejectsMissingRequiredReading() {
    String noPower = payload("mppt-absorption-clipping.json").replace("\"pvPowerW\": 447.0,", "");
    assertThrows(IOException.class, () -> JSON.readValue(noPower, Telemetry.class));
  }

  @Test
  @DisplayName("physically impossible values are rejected by the record, also from JSON")
  void rejectsImpossibleValues() {
    String soc = payload("shunt-charging.json").replace("92.0", "120.0");
    assertThrows(IOException.class, () -> JSON.readValue(soc, Telemetry.class));
    assertThrows(
        IllegalArgumentException.class,
        () ->
            new BatteryTelemetry(
                "battery-bank", "battery_d", Instant.EPOCH, null, null)); // no battery_d in DTDL
    assertThrows(
        IllegalArgumentException.class,
        () -> new BatteryBankTelemetry("battery-bank", Instant.EPOCH, null, 1.5));
  }
}
