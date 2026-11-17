package io.github.kusamau.rooftwin.core;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

class SolarArraySpecTest {

  private static final double EPS = 1e-9;

  /**
   * Illustrative array: two 250 W, 60-cell modules in parallel. Replace with the real datasheet
   * values once they are measured (System Assessment, open question 1).
   */
  static SolarArraySpec illustrativeArray() {
    return new SolarArraySpec("pv-array-1", 500.0, 30.5, 16.4, 37.8, 17.4, 60);
  }

  @Test
  @DisplayName("a plausible array is accepted and exposes its components")
  void acceptsPlausibleArray() {
    SolarArraySpec array = illustrativeArray();

    assertEquals("pv-array-1", array.id());
    assertEquals(500.0, array.wpPeak(), EPS);
    assertEquals(60, array.cellsInSeries());
  }

  @Test
  @DisplayName("records have value semantics: equal components, equal specs")
  void valueSemantics() {
    assertEquals(illustrativeArray(), illustrativeArray());
    assertEquals(illustrativeArray().hashCode(), illustrativeArray().hashCode());
    assertNotEquals(
        illustrativeArray(), new SolarArraySpec("pv-array-2", 500.0, 30.5, 16.4, 37.8, 17.4, 60));
  }

  @Test
  @DisplayName("fill factor and Voc per cell are in the crystalline-silicon range")
  void derivedQuantities() {
    SolarArraySpec array = illustrativeArray();

    assertEquals((30.5 * 16.4) / (37.8 * 17.4), array.fillFactor(), EPS);
    assertEquals(0.63, array.vocPerCell(), EPS);
  }

  @Test
  @DisplayName("Voc rises on a cold morning and is unchanged at STC")
  void vocTemperatureCorrection() {
    SolarArraySpec array = illustrativeArray();

    assertEquals(37.8, array.vocAt(25.0, -0.0031), EPS);
    assertEquals(37.8 * (1.0 + 0.0031 * 35.0), array.vocAt(-10.0, -0.0031), EPS);
  }

  @Test
  @DisplayName("rejects Vmp >= Voc")
  void rejectsVmpAboveVoc() {
    assertThrows(
        IllegalArgumentException.class,
        () -> new SolarArraySpec("pv", 500.0, 38.0, 13.2, 37.8, 17.4, 60));
  }

  @Test
  @DisplayName("rejects Imp >= Isc")
  void rejectsImpAboveIsc() {
    assertThrows(
        IllegalArgumentException.class,
        () -> new SolarArraySpec("pv", 500.0, 28.5, 17.5, 37.8, 17.4, 60));
  }

  @Test
  @DisplayName("rejects a nameplate power that Vmp x Imp cannot explain")
  void rejectsInconsistentNameplate() {
    assertThrows(
        IllegalArgumentException.class,
        () -> new SolarArraySpec("pv", 800.0, 30.5, 16.4, 37.8, 17.4, 60));
  }

  @Test
  @DisplayName("rejects blank ids, non-positive and non-finite values")
  void rejectsInvalidValues() {
    assertThrows(
        IllegalArgumentException.class,
        () -> new SolarArraySpec(" ", 500.0, 30.5, 16.4, 37.8, 17.4, 60));
    assertThrows(
        IllegalArgumentException.class,
        () -> new SolarArraySpec("pv", 500.0, 30.5, 16.4, Double.NaN, 17.4, 60));
    assertThrows(
        IllegalArgumentException.class,
        () -> new SolarArraySpec("pv", 500.0, 30.5, 16.4, 37.8, 17.4, 0));
    assertThrows(
        IllegalArgumentException.class,
        () -> new SolarArraySpec("pv", -500.0, 30.5, 16.4, 37.8, 17.4, 60));
  }
}
