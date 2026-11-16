package io.github.kusamau.rooftwin.core;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

class BatteryBankSpecTest {

  private static final double EPS = 1e-9;

  /** Battery (a) of the real bank: 12 V 130 Ah leisure deep-cycle, bought January 2025. */
  static BatteryBankSpec deepCycle130() {
    return new BatteryBankSpec("battery-a", Chemistry.LEAD_ACID_FLOODED, 12.0, 130.0, 0);
  }

  @Test
  @DisplayName("a 12 V lead-acid battery is six cells")
  void leadAcidCellCount() {
    assertEquals(6, deepCycle130().cellsInSeries());
  }

  @Test
  @DisplayName("nominal and usable energy follow V x Ah and the chosen depth of discharge")
  void energy() {
    BatteryBankSpec battery = deepCycle130();

    assertEquals(1560.0, battery.nominalEnergyWh(), EPS);
    assertEquals(780.0, battery.usableEnergyWh(0.5), EPS);
    assertEquals(1560.0, battery.usableEnergyWh(1.0), EPS);
  }

  @Test
  @DisplayName("rejects a depth of discharge outside (0, 1]")
  void rejectsInvalidDepthOfDischarge() {
    BatteryBankSpec battery = deepCycle130();

    assertThrows(IllegalArgumentException.class, () -> battery.usableEnergyWh(0.0));
    assertThrows(IllegalArgumentException.class, () -> battery.usableEnergyWh(1.5));
    assertThrows(IllegalArgumentException.class, () -> battery.usableEnergyWh(Double.NaN));
  }

  @Test
  @DisplayName("a '12 V' LiFePO4 bank is rejected: four cells make 12.8 V")
  void voltageMustMatchChemistry() {
    assertThrows(
        IllegalArgumentException.class,
        () -> new BatteryBankSpec("lfp", Chemistry.LIFEPO4, 12.0, 100.0, 0));

    BatteryBankSpec lfp = new BatteryBankSpec("lfp", Chemistry.LIFEPO4, 12.8, 100.0, 0);
    assertEquals(4, lfp.cellsInSeries());
  }

  @Test
  @DisplayName("rejects null chemistry, blank id, negative cycles and non-finite capacity")
  void rejectsInvalidValues() {
    assertThrows(
        IllegalArgumentException.class, () -> new BatteryBankSpec("b", null, 12.0, 130.0, 0));
    assertThrows(
        IllegalArgumentException.class,
        () -> new BatteryBankSpec("", Chemistry.LEAD_ACID_FLOODED, 12.0, 130.0, 0));
    assertThrows(
        IllegalArgumentException.class,
        () -> new BatteryBankSpec("b", Chemistry.LEAD_ACID_FLOODED, 12.0, 130.0, -1));
    assertThrows(
        IllegalArgumentException.class,
        () ->
            new BatteryBankSpec(
                "b", Chemistry.LEAD_ACID_FLOODED, 12.0, Double.POSITIVE_INFINITY, 0));
  }

  @Test
  @DisplayName("chemistry knows which variants are lead-acid")
  void chemistryFamilies() {
    assertTrue(Chemistry.LEAD_ACID_FLOODED.isLeadAcid());
    assertTrue(Chemistry.LEAD_ACID_AGM.isLeadAcid());
    assertFalse(Chemistry.LIFEPO4.isLeadAcid());
    assertEquals(2.0, Chemistry.LEAD_ACID_GEL.nominalCellVoltage(), EPS);
  }
}
