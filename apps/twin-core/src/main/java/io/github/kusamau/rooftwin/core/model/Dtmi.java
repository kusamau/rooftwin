package io.github.kusamau.rooftwin.core.model;

/**
 * Digital Twin Model Identifiers of the DTDL v3 interfaces in {@code models/dtdl/}.
 *
 * <p>These strings are the join key between the DTDL files and the Java types: they are the Jackson
 * type ids of {@link Telemetry} messages, and {@code DtdlConformanceTest} checks that every one of
 * them names an interface that really exists in the model files.
 */
public final class Dtmi {

  /** Common prefix of every rooftwin DTMI. */
  public static final String PREFIX = "dtmi:io:github:kusamau:rooftwin:";

  /** {@code models/dtdl/Site.json}. */
  public static final String SITE = PREFIX + "Site;1";

  /** {@code models/dtdl/SolarArray.json}. */
  public static final String SOLAR_ARRAY = PREFIX + "SolarArray;1";

  /** {@code models/dtdl/MpptChargeController.json}. */
  public static final String MPPT_CHARGE_CONTROLLER = PREFIX + "MpptChargeController;1";

  /** {@code models/dtdl/SmartShunt.json}. */
  public static final String SMART_SHUNT = PREFIX + "SmartShunt;1";

  /** {@code models/dtdl/BatteryBank.json}. */
  public static final String BATTERY_BANK = PREFIX + "BatteryBank;1";

  /** {@code models/dtdl/Battery.json} (used only as a Component of the battery bank). */
  public static final String BATTERY = PREFIX + "Battery;1";

  /** {@code models/dtdl/Inverter.json}. */
  public static final String INVERTER = PREFIX + "Inverter;1";

  private Dtmi() {}
}
