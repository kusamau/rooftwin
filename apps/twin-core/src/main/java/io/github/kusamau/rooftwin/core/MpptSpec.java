package io.github.kusamau.rooftwin.core;

/**
 * Static specification of an MPPT solar charge controller.
 *
 * @param model human-readable model name
 * @param maxPvVoltage absolute maximum PV open-circuit voltage, in volts (exceeding it damages the
 *     controller)
 * @param maxChargeCurrentA maximum battery-side charge current, in amperes
 */
public record MpptSpec(String model, double maxPvVoltage, double maxChargeCurrentA) {

  /** The controller on the roof: Victron SmartSolar MPPT 100/30 (100 V PV, 30 A charge). */
  public static final MpptSpec SMART_SOLAR_100_30 =
      new MpptSpec("Victron SmartSolar MPPT 100/30", 100.0, 30.0);

  /** Validates values. */
  public MpptSpec {
    Specs.requireId("model", model);
    Specs.requirePositive("maxPvVoltage", maxPvVoltage);
    Specs.requirePositive("maxChargeCurrentA", maxChargeCurrentA);
  }

  /**
   * Maximum power the controller can push into the battery at a given battery voltage. Anything the
   * array could produce above this is clipped.
   *
   * <p>For the 100/30 at a 14.4 V absorption voltage that is 432 W, less than a 500 Wp array's
   * nameplate. On the best days the twin should expect a flat top at ~30 A, not a fault.
   *
   * @param batteryVoltage battery terminal voltage, in volts
   * @return maximum output power, in watts
   */
  public double maxOutputPowerW(double batteryVoltage) {
    Specs.requirePositive("batteryVoltage", batteryVoltage);
    return maxChargeCurrentA * batteryVoltage;
  }

  /**
   * Whether an array is electrically safe for this controller at the coldest expected cell
   * temperature (cold raises Voc).
   *
   * @param array the PV array
   * @param minCellTemperatureC coldest expected cell temperature, in °C
   * @param vocCoefficientPerC relative Voc temperature coefficient per °C, as a fraction
   * @return {@code true} if the worst-case Voc stays strictly below {@link #maxPvVoltage()}
   */
  public boolean acceptsArray(
      SolarArraySpec array, double minCellTemperatureC, double vocCoefficientPerC) {
    return array.vocAt(minCellTemperatureC, vocCoefficientPerC) < maxPvVoltage;
  }
}
