package io.github.kusamau.rooftwin.core;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

class MpptSpecTest {

  private static final double EPS = 1e-9;
  private static final double VOC_COEFF = -0.0031;

  @Test
  @DisplayName("the SmartSolar 100/30 constant carries the nameplate limits")
  void smartSolarConstant() {
    MpptSpec mppt = MpptSpec.SMART_SOLAR_100_30;

    assertEquals(100.0, mppt.maxPvVoltage(), EPS);
    assertEquals(30.0, mppt.maxChargeCurrentA(), EPS);
  }

  @Test
  @DisplayName("at 14.4 V absorption the 100/30 tops out at 432 W: a 500 Wp array gets clipped")
  void clipping() {
    double maxOut = MpptSpec.SMART_SOLAR_100_30.maxOutputPowerW(14.4);

    assertEquals(432.0, maxOut, EPS);
    assertTrue(maxOut < SolarArraySpecTest.illustrativeArray().wpPeak());
  }

  @Test
  @DisplayName("the illustrative parallel array is safe even at -10 °C")
  void acceptsParallelArray() {
    assertTrue(
        MpptSpec.SMART_SOLAR_100_30.acceptsArray(
            SolarArraySpecTest.illustrativeArray(), -10.0, VOC_COEFF));
  }

  @Test
  @DisplayName("a series string that is fine at STC can exceed 100 V on a frosty morning")
  void rejectsSeriesStringWhenCold() {
    SolarArraySpec series = new SolarArraySpec("pv-series", 576.0, 72.0, 8.0, 90.0, 8.6, 144);
    MpptSpec mppt = MpptSpec.SMART_SOLAR_100_30;

    assertTrue(mppt.acceptsArray(series, 25.0, VOC_COEFF));
    assertTrue(mppt.acceptsArray(series, -10.0, VOC_COEFF));
    assertFalse(mppt.acceptsArray(series, -15.0, VOC_COEFF));
  }

  @Test
  @DisplayName("rejects blank model and non-positive limits")
  void rejectsInvalidValues() {
    assertThrows(IllegalArgumentException.class, () -> new MpptSpec(" ", 100.0, 30.0));
    assertThrows(IllegalArgumentException.class, () -> new MpptSpec("m", 0.0, 30.0));
    assertThrows(IllegalArgumentException.class, () -> new MpptSpec("m", 100.0, -1.0));
    assertThrows(
        IllegalArgumentException.class, () -> MpptSpec.SMART_SOLAR_100_30.maxOutputPowerW(0.0));
  }
}
