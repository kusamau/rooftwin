package io.github.kusamau.rooftwin.core;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

class InverterSpecTest {

  private static final double EPS = 1e-9;

  @Test
  @DisplayName("the 300 W pure-sine constant carries the known nameplate values")
  void pureSineConstant() {
    InverterSpec inverter = InverterSpec.PURE_SINE_300_W_12_V;

    assertEquals(300.0, inverter.ratedPowerW(), EPS);
    assertEquals(300.0, inverter.peakPowerW(), EPS);
    assertEquals(12.0, inverter.nominalDcVoltage(), EPS);
  }

  @Test
  @DisplayName("DC current includes conversion loss and idle draw")
  void dcCurrent() {
    InverterSpec inverter = new InverterSpec("test", 300.0, 600.0, 6.0, 12.0, 0.90);

    assertEquals((150.0 / 0.90 + 6.0) / 12.5, inverter.dcCurrentAt(150.0, 12.5), EPS);
    assertEquals(6.0 / 12.5, inverter.dcCurrentAt(0.0, 12.5), EPS);
  }

  @Test
  @DisplayName("rejects blank model, non-positive and non-finite values")
  void rejectsInvalidValues() {
    assertThrows(
        IllegalArgumentException.class, () -> new InverterSpec(" ", 300.0, 600.0, 5.0, 12.0, 0.90));
    assertThrows(
        IllegalArgumentException.class,
        () -> new InverterSpec("inverter", 0.0, 600.0, 5.0, 12.0, 0.90));
    assertThrows(
        IllegalArgumentException.class,
        () -> new InverterSpec("inverter", 300.0, Double.NaN, 5.0, 12.0, 0.90));
    assertThrows(
        IllegalArgumentException.class,
        () -> new InverterSpec("inverter", 300.0, 600.0, -1.0, 12.0, 0.90));
    assertThrows(
        IllegalArgumentException.class,
        () -> new InverterSpec("inverter", 300.0, 600.0, 5.0, Double.POSITIVE_INFINITY, 0.90));
    assertThrows(
        IllegalArgumentException.class,
        () -> new InverterSpec("inverter", 300.0, 600.0, 5.0, 12.0, 0.0));
  }

  @Test
  @DisplayName("rejects peak power below rated power and efficiency above one")
  void rejectsInconsistentValues() {
    assertThrows(
        IllegalArgumentException.class,
        () -> new InverterSpec("inverter", 300.0, 299.0, 5.0, 12.0, 0.90));
    assertThrows(
        IllegalArgumentException.class,
        () -> new InverterSpec("inverter", 300.0, 600.0, 5.0, 12.0, 1.01));
  }

  @Test
  @DisplayName("rejects invalid load and bus voltage")
  void rejectsInvalidOperatingPoint() {
    assertThrows(
        IllegalArgumentException.class,
        () -> InverterSpec.PURE_SINE_300_W_12_V.dcCurrentAt(-1.0, 12.0));
    assertThrows(
        IllegalArgumentException.class,
        () -> InverterSpec.PURE_SINE_300_W_12_V.dcCurrentAt(100.0, 0.0));
  }
}
