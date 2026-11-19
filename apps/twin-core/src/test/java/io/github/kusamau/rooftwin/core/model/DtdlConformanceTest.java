package io.github.kusamau.rooftwin.core.model;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.fasterxml.jackson.annotation.JsonSubTypes;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.github.kusamau.rooftwin.core.Chemistry;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.lang.reflect.Field;
import java.lang.reflect.Modifier;
import java.lang.reflect.RecordComponent;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

/**
 * The DTDL files in {@code models/dtdl/} are the source of truth; these tests fail the build when
 * the Java mirror drifts from them. They read the JSON directly: no DTDL parser on the classpath.
 * (Validity of the DTDL itself is the dtdl-validator container's job.)
 */
class DtdlConformanceTest {

  private static final Set<String> ENVELOPE = Set.of("twinId", "observedAt", "component");
  private static final Map<String, JsonNode> INTERFACES = loadInterfaces();

  private static Path modelsDir() {
    // Maven runs tests from the module directory, IDEs often from the repo root: walk up.
    for (Path p = Path.of("").toAbsolutePath(); p != null; p = p.getParent()) {
      Path candidate = p.resolve("models/dtdl");
      if (Files.isDirectory(candidate)) {
        return candidate;
      }
    }
    throw new IllegalStateException("models/dtdl not found above " + Path.of("").toAbsolutePath());
  }

  private static Map<String, JsonNode> loadInterfaces() {
    ObjectMapper json = new ObjectMapper();
    Map<String, JsonNode> byId = new HashMap<>();
    try (Stream<Path> files = Files.list(modelsDir())) {
      for (Path f : files.filter(p -> p.toString().endsWith(".json")).toList()) {
        JsonNode node = json.readTree(f.toFile());
        byId.put(node.get("@id").asText(), node);
      }
    } catch (IOException e) {
      throw new UncheckedIOException(e);
    }
    return byId;
  }

  /** Names of the contents of one DTDL kind ("Telemetry", "Property", ...) in an interface. */
  private static Set<String> contents(String dtmi, String kind) {
    JsonNode iface = INTERFACES.get(dtmi);
    assertTrue(iface != null, "no DTDL interface with @id " + dtmi);
    Set<String> names = new TreeSet<>();
    for (JsonNode c : iface.get("contents")) {
      // "@type" is "Telemetry" or, with a semantic type, ["Telemetry", "Voltage"].
      JsonNode type = c.get("@type");
      Set<String> types = type.isArray() ? toStrings(type) : Set.of(type.asText());
      if (types.contains(kind)) {
        names.add(c.get("name").asText());
      }
    }
    return names;
  }

  private static Set<String> toStrings(JsonNode array) {
    Set<String> out = new TreeSet<>();
    array.forEach(n -> out.add(n.asText()));
    return out;
  }

  private static Set<String> recordComponents(Class<?> type, Set<String> excluding) {
    return Arrays.stream(type.getRecordComponents())
        .map(RecordComponent::getName)
        .filter(n -> !excluding.contains(n))
        .collect(Collectors.toCollection(TreeSet::new));
  }

  @Test
  @DisplayName("every Dtmi constant names an interface that exists in models/dtdl")
  void dtmiConstantsExist() throws IllegalAccessException {
    for (Field f : Dtmi.class.getFields()) {
      if (Modifier.isStatic(f.getModifiers()) && !f.getName().equals("PREFIX")) {
        String dtmi = (String) f.get(null);
        assertTrue(INTERFACES.containsKey(dtmi), f.getName() + " -> " + dtmi + " not in models");
      }
    }
    assertEquals(Dtmi.class.getFields().length - 1, INTERFACES.size(), "one constant per model");
  }

  @Test
  @DisplayName("each Telemetry record has exactly the Telemetry names of its DTDL interface")
  void telemetryRecordsMatchDtdl() {
    JsonSubTypes subTypes = Telemetry.class.getAnnotation(JsonSubTypes.class);
    assertEquals(
        Telemetry.class.getPermittedSubclasses().length,
        subTypes.value().length,
        "every permitted subclass needs a @JsonSubTypes entry");
    for (JsonSubTypes.Type t : subTypes.value()) {
      assertEquals(
          contents(t.name(), "Telemetry"),
          recordComponents(t.value(), ENVELOPE),
          t.value().getSimpleName() + " vs " + t.name());
    }
  }

  @Test
  @DisplayName("each DTDL Command has a Java type, and vice versa")
  void commandsMatchDtdl() {
    Set<String> java =
        Arrays.stream(MpptCommand.class.getPermittedSubclasses())
            .map(DtdlConformanceTest::commandName)
            .collect(Collectors.toCollection(TreeSet::new));
    assertEquals(contents(Dtmi.MPPT_CHARGE_CONTROLLER, "Command"), java);
    for (String dtmi : INTERFACES.keySet()) {
      if (!dtmi.equals(Dtmi.MPPT_CHARGE_CONTROLLER)) {
        assertEquals(
            Set.of(), contents(dtmi, "Command"), dtmi + " declares a command with no Java type");
      }
    }
  }

  private static String commandName(Class<?> type) {
    try {
      return ((Command) type.getDeclaredConstructor().newInstance()).name();
    } catch (ReflectiveOperationException e) {
      throw new IllegalStateException("command records with parameters need a fixture: " + type, e);
    }
  }

  @Test
  @DisplayName("Site mirrors the DTDL Site properties and relationships")
  void siteMatchesDtdl() {
    Set<String> dtdl = new TreeSet<>(contents(Dtmi.SITE, "Property"));
    dtdl.addAll(contents(Dtmi.SITE, "Relationship"));
    assertEquals(dtdl, recordComponents(Site.class, Set.of("id")));
  }

  @Test
  @DisplayName("BatteryTelemetry.COMPONENTS are the Components of BatteryBank")
  void batteryComponentsMatchDtdl() {
    assertEquals(
        contents(Dtmi.BATTERY_BANK, "Component"), new TreeSet<>(BatteryTelemetry.COMPONENTS));
  }

  @Test
  @DisplayName("Battery.chemistry enum values are exactly the core Chemistry constants")
  void batteryChemistryMatchesJavaEnum() {
    Set<String> dtdl = new TreeSet<>();
    for (JsonNode c : INTERFACES.get(Dtmi.BATTERY).get("contents")) {
      if (c.get("name").asText().equals("chemistry")) {
        c.get("schema").get("enumValues").forEach(v -> dtdl.add(v.get("enumValue").asText()));
      }
    }
    Set<String> java =
        Arrays.stream(Chemistry.values())
            .map(Enum::name)
            .collect(Collectors.toCollection(TreeSet::new));
    assertEquals(java, dtdl);
  }

  @Test
  @DisplayName("ChargeState mirrors the chargeStateCode Enum, name for name and code for code")
  void chargeStateMatchesDtdl() {
    JsonNode enumValues = null;
    for (JsonNode c : INTERFACES.get(Dtmi.MPPT_CHARGE_CONTROLLER).get("contents")) {
      if (c.get("name").asText().equals("chargeStateCode")) {
        enumValues = c.get("schema").get("enumValues");
      }
    }
    Map<String, Integer> dtdl = new HashMap<>();
    enumValues.forEach(v -> dtdl.put(v.get("name").asText(), v.get("enumValue").asInt()));
    Map<String, Integer> java =
        Arrays.stream(ChargeState.values())
            .collect(Collectors.toMap(ChargeState::dtdlName, ChargeState::code));
    assertEquals(dtdl, java);
  }
}
