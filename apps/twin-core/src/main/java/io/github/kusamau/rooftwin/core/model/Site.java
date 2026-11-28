package io.github.kusamau.rooftwin.core.model;

import java.time.ZoneId;

/**
 * The installation, mirroring {@code dtmi:io:github:kusamau:rooftwin:Site;1}: two properties and
 * one Relationship per contained asset. Relationship components hold the <em>target twin id</em>
 * and keep the DTDL relationship name, so {@code site.hasInverter()} returns e.g. {@code
 * "inverter-1"}.
 *
 * @param id this site's twin identifier
 * @param location coarse location of the site (two decimals is enough for sun position)
 * @param timezone IANA time zone of the site
 * @param hasSolarArray twin id of the PV array
 * @param hasMpptChargeController twin id of the MPPT
 * @param hasSmartShunt twin id of the battery monitor
 * @param hasBatteryBank twin id of the battery bank
 * @param hasInverter twin id of the inverter
 */
public record Site(
    String id,
    GeoPoint location,
    ZoneId timezone,
    String hasSolarArray,
    String hasMpptChargeController,
    String hasSmartShunt,
    String hasBatteryBank,
    String hasInverter) {

  /** Validates that every field is present. */
  public Site {
    Checks.requireId("id", id);
    Checks.requireNonNull("location", location);
    Checks.requireNonNull("timezone", timezone);
    Checks.requireId("hasSolarArray", hasSolarArray);
    Checks.requireId("hasMpptChargeController", hasMpptChargeController);
    Checks.requireId("hasSmartShunt", hasSmartShunt);
    Checks.requireId("hasBatteryBank", hasBatteryBank);
    Checks.requireId("hasInverter", hasInverter);
  }
}
