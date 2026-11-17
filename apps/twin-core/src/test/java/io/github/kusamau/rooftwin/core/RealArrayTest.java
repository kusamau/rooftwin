package io.github.kusamau.rooftwin.core;

import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.api.Test;

class RealArrayTest {

  private static final double MEASURED_VOC = 53.0;
  private static final double MEASUREMENT_TOLERANCE = 0.05;
  // The reported 10 °C was ambient temperature. In direct sun, the 53 V measurement corresponds
  // to an estimated cell temperature of about 36 °C, which is the input expected by vocAt().
  private static final double ESTIMATED_CELL_TEMPERATURE_C = 36.0;

  static SolarArraySpec real() {
    return new SolarArraySpec("aiko-solar-hiku6-615w", 615.0, 44.98, 13.68, 54.29, 14.44, 144);
  }

  @Test
  void predictsMeasuredVocWithinFivePercent() {
    assertEquals(
        MEASURED_VOC,
        real().vocAt(ESTIMATED_CELL_TEMPERATURE_C, -0.0022),
        MEASURED_VOC * MEASUREMENT_TOLERANCE);
  }
}
