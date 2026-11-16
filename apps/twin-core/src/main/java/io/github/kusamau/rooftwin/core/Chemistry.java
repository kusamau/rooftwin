package io.github.kusamau.rooftwin.core;

/**
 * Battery chemistries the twin knows about.
 *
 * <p>The nominal cell voltage is what lets {@link BatteryBankSpec} check that a declared bank
 * voltage is physically plausible (a "12 V" lead-acid battery is six 2.0 V cells; a "12 V" LiFePO4
 * battery is really four 3.2 V cells, i.e. 12.8 V).
 */
public enum Chemistry {
  /** Flooded (wet) lead-acid: SLI starter batteries and most leisure batteries. */
  LEAD_ACID_FLOODED(2.0),
  /** Enhanced flooded battery (start-stop grade). */
  LEAD_ACID_EFB(2.0),
  /** Absorbent glass mat, valve-regulated lead-acid. */
  LEAD_ACID_AGM(2.0),
  /** Gel, valve-regulated lead-acid. */
  LEAD_ACID_GEL(2.0),
  /** Lithium iron phosphate. */
  LIFEPO4(3.2);

  private final double nominalCellVoltage;

  Chemistry(double nominalCellVoltage) {
    this.nominalCellVoltage = nominalCellVoltage;
  }

  /** Nominal voltage of a single cell, in volts. */
  public double nominalCellVoltage() {
    return nominalCellVoltage;
  }

  /** True for every lead-acid variant (flooded, EFB, AGM, gel). */
  public boolean isLeadAcid() {
    return this != LIFEPO4;
  }
}
