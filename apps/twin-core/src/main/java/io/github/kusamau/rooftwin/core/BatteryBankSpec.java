package io.github.kusamau.rooftwin.core;

/**
 * Static specification of a battery bank on the DC bus.
 *
 * <p>In Lesson 1 this is the aggregate view that the SmartShunt sees. The real bank is three
 * heterogeneous lead-acid batteries in parallel. Lesson 2 models each one as a {@code Battery}
 * Component of the DTDL {@code BatteryBank} (own capacity, age, chemistry, construction, position
 * on the cable hop), and Lesson 5 gives each one its own Thevenin-Shepherd parameters.
 *
 * @param id stable identifier (e.g. {@code "battery-bank"})
 * @param chemistry cell chemistry; must be consistent with {@code nominalVoltage}
 * @param nominalVoltage nominal bank voltage, in volts (12.0 for six lead-acid cells)
 * @param capacityAh rated capacity, in ampere-hours (for lead-acid usually the 20-hour rate, C20)
 * @param cyclesEstimated estimated equivalent full cycles already consumed (0 for a new bank)
 */
public record BatteryBankSpec(
    String id, Chemistry chemistry, double nominalVoltage, double capacityAh, int cyclesEstimated) {

  private static final double CELL_COUNT_TOLERANCE = 1e-6;

  /** Validates values and the voltage/chemistry consistency. */
  public BatteryBankSpec {
    Specs.requireId("id", id);
    if (chemistry == null) {
      throw new IllegalArgumentException("chemistry must not be null");
    }
    Specs.requirePositive("nominalVoltage", nominalVoltage);
    Specs.requirePositive("capacityAh", capacityAh);
    Specs.requireNonNegative("cyclesEstimated", cyclesEstimated);
    double cells = nominalVoltage / chemistry.nominalCellVoltage();
    if (Math.abs(cells - Math.rint(cells)) > CELL_COUNT_TOLERANCE) {
      throw new IllegalArgumentException(
          "nominalVoltage "
              + nominalVoltage
              + " V is not a whole number of "
              + chemistry
              + " cells ("
              + chemistry.nominalCellVoltage()
              + " V each)");
    }
  }

  /** Number of cells in series implied by chemistry and nominal voltage. */
  public int cellsInSeries() {
    return (int) Math.rint(nominalVoltage / chemistry.nominalCellVoltage());
  }

  /** Nominal energy content, {@code V * Ah}, in watt-hours. */
  public double nominalEnergyWh() {
    return nominalVoltage * capacityAh;
  }

  /**
   * Energy you can take out without going below the chosen depth of discharge.
   *
   * <p>For lead-acid, 0.5 is the usual compromise between usable energy and cycle life; that is the
   * figure the prescriptive lesson will respect.
   *
   * @param maxDepthOfDischarge fraction of capacity allowed to be used, in {@code (0, 1]}
   * @return usable energy in watt-hours
   */
  public double usableEnergyWh(double maxDepthOfDischarge) {
    if (!(maxDepthOfDischarge > 0.0 && maxDepthOfDischarge <= 1.0)) {
      throw new IllegalArgumentException(
          "maxDepthOfDischarge must be in (0, 1], was " + maxDepthOfDischarge);
    }
    return nominalEnergyWh() * maxDepthOfDischarge;
  }
}
