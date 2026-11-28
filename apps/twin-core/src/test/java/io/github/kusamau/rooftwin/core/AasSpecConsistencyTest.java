package io.github.kusamau.rooftwin.core;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.Map;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

/**
 * The same nameplate facts now live in three places: the Lesson 1 spec records, the DTDL property
 * values a twin will carry, and {@code models/aas/Solar.json}. This test keeps the Java specs and
 * the AAS file in agreement, so a datasheet correction can't land in one and miss the other.
 */
class AasSpecConsistencyTest {

  private static final double EPS = 1e-9;

  private static JsonNode solarAas() throws IOException {
    for (Path p = Path.of("").toAbsolutePath(); p != null; p = p.getParent()) {
      Path candidate = p.resolve("models/aas/Solar.json");
      if (Files.isRegularFile(candidate)) {
        return new ObjectMapper().readTree(candidate.toFile());
      }
    }
    throw new IllegalStateException("models/aas/Solar.json not found");
  }

  /** idShort -> value of every Property in the TechnicalData submodel of one asset. */
  private static Map<String, String> technicalData(String assetKey) throws IOException {
    String id = "https://github.com/kusamau/rooftwin/ids/sm/" + assetKey + "/technicaldata";
    Map<String, String> values = new HashMap<>();
    for (JsonNode sm : solarAas().get("submodels")) {
      if (sm.get("id").asText().equals(id)) {
        collect(sm.get("submodelElements"), values);
        return values;
      }
    }
    throw new IllegalStateException("no submodel " + id);
  }

  private static void collect(JsonNode elements, Map<String, String> out) {
    for (JsonNode e : elements) {
      if ("Property".equals(e.path("modelType").asText()) && e.has("value")) {
        out.put(e.get("idShort").asText(), e.get("value").asText());
      }
      if (e.get("value") != null && e.get("value").isArray()) {
        collect(e.get("value"), out);
      }
    }
  }

  @Test
  @DisplayName("MPPT ratings in the AAS equal MpptSpec.SMART_SOLAR_100_30")
  void mpptMatches() throws IOException {
    for (String key : new String[] {"mppt", "mppt-type"}) {
      Map<String, String> td = technicalData(key);
      MpptSpec spec = MpptSpec.SMART_SOLAR_100_30;
      assertEquals(spec.maxPvVoltage(), Double.parseDouble(td.get("MaxPvVoltage")), EPS, key);
      assertEquals(
          spec.maxChargeCurrentA(), Double.parseDouble(td.get("MaxChargeCurrent")), EPS, key);
    }
  }

  @Test
  @DisplayName("PV module data in the AAS equal the measured-array fixture")
  void pvArrayMatches() throws IOException {
    Map<String, String> td = technicalData("pv");
    SolarArraySpec real = RealArrayTest.real();
    assertEquals(real.wpPeak(), Double.parseDouble(td.get("PeakPower")), EPS);
    assertEquals(real.vmp(), Double.parseDouble(td.get("VoltageAtMaxPower")), EPS);
    assertEquals(real.imp(), Double.parseDouble(td.get("CurrentAtMaxPower")), EPS);
    assertEquals(real.voc(), Double.parseDouble(td.get("OpenCircuitVoltage")), EPS);
    assertEquals(real.isc(), Double.parseDouble(td.get("ShortCircuitCurrent")), EPS);
    assertEquals(real.cellsInSeries(), Integer.parseInt(td.get("CellsInSeries")));
  }

  @Test
  @DisplayName("inverter rating in the AAS equals InverterSpec.PURE_SINE_300_W_12_V")
  void inverterMatches() throws IOException {
    Map<String, String> td = technicalData("inverter");
    InverterSpec spec = InverterSpec.PURE_SINE_300_W_12_V;
    assertEquals(spec.ratedPowerW(), Double.parseDouble(td.get("RatedPower")), EPS);
    assertEquals(spec.nominalDcVoltage(), Double.parseDouble(td.get("NominalVoltage")), EPS);
  }

  @Test
  @DisplayName("battery chemistries in the AAS are valid Chemistry constants")
  void batteryChemistryNamesAreJavaEnumNames() throws IOException {
    for (String key : new String[] {"battery-a", "battery-b", "battery-c"}) {
      String chemistry = technicalData(key).get("Chemistry");
      Chemistry parsed = Chemistry.valueOf(chemistry);
      assertTrue(parsed.isLeadAcid(), key);
    }
  }
}
