package io.github.kusamau.rooftwin.core.model;

import java.util.Arrays;
import java.util.Optional;

/**
 * Victron charge-state codes, as declared in the DTDL Enum of {@code
 * MpptChargeController.chargeStateCode}. The DTDL enum names match {@link #dtdlName()}.
 */
public enum ChargeState {
  /** Not charging. */
  OFF(0, "off"),
  /** Charger fault. */
  FAULT(2, "fault"),
  /** Constant current: the battery takes everything the PV can give. */
  BULK(3, "bulk"),
  /** Constant voltage at the absorption set-point. */
  ABSORPTION(4, "absorption"),
  /** Maintenance voltage after absorption. */
  FLOAT(5, "float"),
  /** Reduced voltage for long idle periods. */
  STORAGE(6, "storage"),
  /** Manual equalization. */
  EQUALIZE(7, "equalize"),
  /** Controller starting up. */
  STARTING_UP(245, "startingUp"),
  /** Automatic equalization. */
  AUTO_EQUALIZE(247, "autoEqualize"),
  /** Controlled by an external device. */
  EXTERNAL_CONTROL(252, "externalControl");

  private final int code;
  private final String dtdlName;

  ChargeState(int code, String dtdlName) {
    this.code = code;
    this.dtdlName = dtdlName;
  }

  /** Numeric code, as transmitted by the device and declared as DTDL {@code enumValue}. */
  public int code() {
    return code;
  }

  /** Name of the corresponding DTDL {@code EnumValue}. */
  public String dtdlName() {
    return dtdlName;
  }

  /** The state for a code, or empty for a code this version doesn't know. */
  public static Optional<ChargeState> fromCode(int code) {
    return Arrays.stream(values()).filter(s -> s.code == code).findFirst();
  }
}
