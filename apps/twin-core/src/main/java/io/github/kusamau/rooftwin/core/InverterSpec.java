package io.github.kusamau.rooftwin.core;

/**
 * Static specification of a DC-to-AC inverter.
 *
 * @param model human-readable model name
 * @param ratedPowerW continuous rated AC output power, in watts
 * @param peakPowerW peak AC output power, in watts
 * @param idleDrawW DC power drawn while the inverter is on with no AC load, in watts
 * @param nominalDcVoltage nominal DC input voltage, in volts
 * @param efficiency conversion efficiency as a fraction in {@code (0, 1]}
 */
public record InverterSpec(
    String model,
    double ratedPowerW,
    double peakPowerW,
    double idleDrawW,
    double nominalDcVoltage,
    double efficiency) {

  /**
   * The installed 300 W, 12 V pure-sine inverter. Idle draw (5 W) and efficiency (90%) are
   * placeholders until its datasheet values are available; peak power is conservatively set equal
   * to the known continuous rating because no surge rating is known.
   */
  public static final InverterSpec PURE_SINE_300_W_12_V =
      new InverterSpec("Generic 300 W 12 V pure-sine inverter", 300.0, 300.0, 5.0, 12.0, 0.90);

  /** Validates values. */
  public InverterSpec {
    Specs.requireId("model", model);
    Specs.requirePositive("ratedPowerW", ratedPowerW);
    Specs.requirePositive("peakPowerW", peakPowerW);
    Specs.requirePositive("idleDrawW", idleDrawW);
    Specs.requirePositive("nominalDcVoltage", nominalDcVoltage);
    Specs.requirePositive("efficiency", efficiency);
    if (peakPowerW < ratedPowerW) {
      throw new IllegalArgumentException(
          "peakPowerW (" + peakPowerW + ") must be >= ratedPowerW (" + ratedPowerW + ")");
    }
    if (efficiency > 1.0) {
      throw new IllegalArgumentException("efficiency must be <= 1, was " + efficiency);
    }
  }

  /**
   * DC current required for an AC load at the actual DC bus voltage.
   *
   * @param acLoadW AC load power, in watts
   * @param busVoltage actual DC bus voltage, in volts
   * @return required DC current, in amperes
   */
  public double dcCurrentAt(double acLoadW, double busVoltage) {
    if (!Double.isFinite(acLoadW) || acLoadW < 0.0) {
      throw new IllegalArgumentException("acLoadW must be a finite value >= 0, was " + acLoadW);
    }
    Specs.requirePositive("busVoltage", busVoltage);
    return (acLoadW / efficiency + idleDrawW) / busVoltage;
  }
}
