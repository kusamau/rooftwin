package io.github.kusamau.rooftwin.core.model;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertThrows;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.time.ZoneId;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

class SiteAndCommandJsonTest {

  private static final ObjectMapper JSON = TwinJson.mapper();

  private static String siteJson() throws IOException {
    try (InputStream in = SiteAndCommandJsonTest.class.getResourceAsStream("/site.json")) {
      return new String(in.readAllBytes(), StandardCharsets.UTF_8);
    }
  }

  @Test
  @DisplayName("Site round-trips through JSON")
  void siteRoundTrip() throws IOException {
    Site site = JSON.readValue(siteJson(), Site.class);
    assertEquals(ZoneId.of("Europe/London"), site.timezone());
    assertEquals("inverter-1", site.hasInverter());
    assertEquals(JSON.readTree(siteJson()), JSON.valueToTree(site));
  }

  @Test
  @DisplayName("GeoJSON coordinates are [longitude, latitude]")
  void geoJsonOrder() throws IOException {
    JsonNode json = JSON.valueToTree(new GeoPoint(-3.19, 55.95));
    assertEquals("Point", json.get("type").asText());
    assertEquals(-3.19, json.get("coordinates").get(0).asDouble(), 0.0);
    assertEquals(55.95, json.get("coordinates").get(1).asDouble(), 0.0);
    Site site = JSON.readValue(siteJson(), Site.class);
    assertEquals(-0.01, site.location().longitude(), 0.0);
    assertEquals(51.48, site.location().latitude(), 0.0);
  }

  @Test
  @DisplayName("an out-of-range latitude is rejected: the classic swapped-coordinates bug")
  void swappedCoordinatesAreCaughtWhenOutOfRange() {
    // A site at longitude 120 E, latitude 10 N written the "lat, lon" way: [10, 120].
    String swapped = "{\"type\":\"Point\",\"coordinates\":[10.0, 120.0]}";
    assertThrows(IOException.class, () -> JSON.readValue(swapped, GeoPoint.class));
  }

  @Test
  @DisplayName("commands round-trip and carry their DTDL name")
  void commandRoundTrip() throws IOException {
    Command reset = new MpptCommand.ResetDailyCounters();
    String json = JSON.writeValueAsString(reset);
    assertEquals(JSON.readTree("{\"command\":\"resetDailyCounters\"}"), JSON.readTree(json));
    Command back = JSON.readValue(json, Command.class);
    assertInstanceOf(MpptCommand.ResetDailyCounters.class, back);
    assertEquals(Dtmi.MPPT_CHARGE_CONTROLLER, back.interfaceDtmi());
    assertEquals("resetDailyCounters", back.name());
  }

  @Test
  @DisplayName("a command the model doesn't declare is rejected")
  void unknownCommandRejected() {
    assertThrows(
        IOException.class,
        () -> JSON.readValue("{\"command\":\"setMaxChargeCurrent\"}", Command.class));
  }
}
