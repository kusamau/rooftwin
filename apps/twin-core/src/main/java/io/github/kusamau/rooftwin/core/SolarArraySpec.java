package io.github.kusamau.rooftwin.core;

/**
 * Static (nameplate) specification of the PV array as seen by the MPPT, at Standard Test Conditions
 * (STC: 1000 W/m², 25 °C cell temperature, AM1.5).
 *
 * <p>Values describe the <em>array</em>, not a single module: for modules in parallel, currents
 * add; for modules in series, voltages add. {@code cellsInSeries} is the number of cells in series
 * along one string, which is what sets the voltage and is needed by the single-diode model in
 * Lesson 5.
 *
 * @param id stable identifier used across twin, topics and models (e.g. {@code "pv-array-1"})
 * @param wpPeak rated peak power at STC, in watts
 * @param vmp voltage at maximum power point, in volts
 * @param imp current at maximum power point, in amperes
 * @param voc open-circuit voltage, in volts
 * @param isc short-circuit current, in amperes
 * @param cellsInSeries number of PV cells in series along one string
 */
public record SolarArraySpec(
    String id, double wpPeak, double vmp, double imp, double voc, double isc, int cellsInSeries) {

  /**
   * Allowed mismatch between declared {@code wpPeak} and {@code vmp * imp} (datasheet rounding).
   */
  static final double NAMEPLATE_TOLERANCE = 0.10;

  /** STC cell temperature, in °C. */
  public static final double STC_CELL_TEMPERATURE_C = 25.0;

  /** Validates physical plausibility; a twin that accepts impossible specs lies quietly. */
  public SolarArraySpec {
    Specs.requireId("id", id);
    Specs.requirePositive("wpPeak", wpPeak);
    Specs.requirePositive("vmp", vmp);
    Specs.requirePositive("imp", imp);
    Specs.requirePositive("voc", voc);
    Specs.requirePositive("isc", isc);
    Specs.requirePositive("cellsInSeries", cellsInSeries);
    if (vmp >= voc) {
      throw new IllegalArgumentException("vmp (" + vmp + ") must be lower than voc (" + voc + ")");
    }
    if (imp >= isc) {
      throw new IllegalArgumentException("imp (" + imp + ") must be lower than isc (" + isc + ")");
    }
    double ivPower = vmp * imp;
    if (Math.abs(ivPower - wpPeak) / wpPeak > NAMEPLATE_TOLERANCE) {
      throw new IllegalArgumentException(
          "vmp * imp (" + ivPower + " W) is inconsistent with wpPeak (" + wpPeak + " W)");
    }
  }

  /**
   * Fill factor, {@code (Vmp * Imp) / (Voc * Isc)}. Typical crystalline silicon modules sit around
   * 0.75-0.82; a much lower value usually means a data-entry error.
   */
  public double fillFactor() {
    return (vmp * imp) / (voc * isc);
  }

  /** Open-circuit voltage per cell, in volts (around 0.6-0.7 V for crystalline silicon). */
  public double vocPerCell() {
    return voc / cellsInSeries;
  }

  /**
   * Open-circuit voltage at a given cell temperature, using the linear temperature coefficient from
   * the datasheet.
   *
   * <p>Cold mornings raise Voc. This is the number that must stay below the MPPT's maximum PV
   * voltage: see {@link MpptSpec#acceptsArray(SolarArraySpec, double, double)}.
   *
   * @param cellTemperatureC cell temperature in °C
   * @param vocCoefficientPerC relative Voc temperature coefficient per °C, as a fraction (for
   *     example {@code -0.0029} for -0.29 %/°C)
   * @return the temperature-corrected open-circuit voltage, in volts
   */
  public double vocAt(double cellTemperatureC, double vocCoefficientPerC) {
    return voc * (1.0 + vocCoefficientPerC * (cellTemperatureC - STC_CELL_TEMPERATURE_C));
  }
}
