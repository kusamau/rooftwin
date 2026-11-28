# Architecture choice — information models and twin runtime (Lesson 2)

Status: **accepted**, 2026-11. Revisit at Lesson 4 (Ditto integration) and Lesson 9 (security).

## Context

The twin needs three things from an information model, and they're not the same thing:

1. **A runtime shape.** Which telemetry each device emits, with units. Which properties are
   writable, which commands exist, how the devices relate. Telemetry is validated against it, the
   Java records mirror it, and the Lesson 4 runtime enforces it.
2. **Asset facts to exchange.** Manufacturer, serial number, ratings, type vs instance. These are
   what you would hand to an installer, an insurer or a future Digital Product Passport, in a form
   *they* already understand.
3. **A runtime** that holds twin state, separates reported from desired values, applies access
   policies and offers an API. Lesson 4 builds it.

Candidates considered:

| | DTDL v3 | AAS V3 (IDTA) | W3C WoT TD / TM 1.1 | Eclipse Ditto Thing |
|---|---|---|---|---|
| Primary purpose | Model a twin's interface: telemetry, properties, commands, relationships | Describe an asset and its data for exchange along the value chain | Describe how to interact with a Thing over a protocol | Runtime representation of a twin |
| Typed telemetry with units | Yes (QuantitativeTypes extension) | Via data elements and concept descriptions, but static-first | Yes (data schemas, units via ontologies) | Only through a linked WoT model |
| Commands | Yes | Operations (rarely used outside AAS servers) | Actions | Messages |
| Graph of twins | Relationships, Components | References between shells | Links | Thing ids in attributes (by convention) |
| Type vs instance | No (one interface) | Yes (`assetKind`, `derivedFrom`) | TM (model) vs TD (instance) | Thing definition vs Thing |
| Shared semantics | DTMI (own namespace); semantic types | Global semantic IDs (ECLASS, IEC CDD), IDTA submodel templates | JSON-LD contexts (SAREF, ...) | Via WoT |
| Protocol bindings | No | No (API spec is separate) | Yes: core strength | Connectivity service |
| Reference validator | Microsoft DTDLParser | aas-core SDKs, BaSyx | node-wot, TD playground | Ditto itself |
| Status, Nov 2026 | v3 in Azure Digital Twins; v4 in Azure IoT Operations | Metamodel 3.1 (3.2 published Jul 2026) | TD 1.1 Recommendation (2023); TD 2.0 Working Draft | 3.8.x / 3.9 line, WoT TD 1.1 based |

## Decision

**DTDL v3 is the primary model** of the twin's runtime shape (`models/dtdl/`).
- It models exactly what this twin has: telemetry streams, a few properties, one command and a
  device graph, including a bank that is three batteries (Components).
- It has a strict reference parser that runs in a container (`docker/dtdl-validator`).
- It maps one-to-one onto Java records (`twin-core/.../model`), and `DtdlConformanceTest` keeps the
  two in step.

**AAS V3 is the interchange format** for asset facts (`models/aas/Solar.json`).
- It carries what DTDL deliberately leaves out: values, serial numbers, manufacturers, type vs
  instance, and semantic IDs that other parties share (IEC CDD in the Digital Nameplate).
- It is what a supply chain partner expects. A DTDL interface tells them nothing about *this*
  controller with serial HQ223242XYT.

**Eclipse Ditto is the runtime** (Lesson 4).
- Open source, runs in Docker Compose, policy-based access control, reported and desired state, a
  search API.
- **Known cost:** Ditto's native type system is the W3C WoT Thing Model, not DTDL. Lesson 4
  therefore *generates* Ditto Thing skeletons and WoT Thing Models from the DTDL files instead of
  maintaining a third hand-written model. If that generator turns out to be the bulk of Lesson 4's
  work, that's the signal to revisit this decision (see "Revisit when").

**WoT TD is not adopted as a model now.** It becomes relevant at the edge: the BLE bridge could
publish a TD that describes its MQTT topics (Lesson 3 exercise), and Ditto can serve TDs for its
twins.

## Broker-based or platform-based?

- **Broker-based twin:** the twin *is* the topic tree. Devices publish to MQTT, retained messages
  hold the last known state, and services subscribe. It is cheap, fast and easy to replay. But no
  component owns the twin. There is no model enforcement, per-twin access policy, desired state,
  query or history unless every service re-implements them.
- **Platform-based twin:** a twin platform (Ditto, Azure Digital Twins, ...) owns twin state,
  model, policy and API. Devices still need a transport, and the platform becomes a dependency
  that must be operated.

**rooftwin is hybrid, with the broker below the platform.** Mosquitto stays the backbone: the BLE
bridge, replay simulator and data recorder only speak MQTT, which keeps Lesson 3 independent of
Lesson 4 and makes replay trivial. Ditto sits on top as the system of record for twin state, with
its connectivity service subscribed to the broker. Services that need *state* ask Ditto; services
that need *streams* (recorder, anomaly detector) subscribe to MQTT directly.

## Mapping onto ISO 23247-2

| ISO 23247-2 | rooftwin |
|---|---|
| Observable manufacturing element domain (OMEs) | PV module, MPPT, SmartShunt, batteries A/B/C, inverter, and the environment (sun, temperature) |
| Device communication entity: data collection | BLE Instant Readout → `victron-ble` bridge → MQTT (Lesson 3) |
| Device communication entity: device control | **None, on purpose.** Commands are declared in DTDL but never sent to the hardware (shadow mode) |
| Digital twin entity: operation & management | Ditto + the model registry (`models/`) (Lesson 4) |
| Digital twin entity: application & service | `twin-physics`, `twin-analytics`, optimizer (Lessons 5–8) |
| Digital twin entity: resource access & interchange | `twin-api` REST, AAS export (`Solar.json`) |
| User entity | Grafana, 3D house, notifications, and the human who switches the inverter (Lesson 7–8) |
| Cross-system entity | Data assurance: validators, conformance tests, data quality checks. Security support: mTLS, OIDC, OPA (Lesson 9). Data translation: DTDL ↔ AAS ↔ WoT generators |

## Consequences

- **Three representations of overlapping facts** (DTDL, AAS, Java specs). Drift is the main risk.
  It is contained by `DtdlConformanceTest` (Java ↔ DTDL), `AasSpecConsistencyTest` (AAS ↔ Java
  specs) and `generate_solar_aas.py --check`. The DTDL ↔ AAS concept mapping is still by hand (the
  concept descriptions name the DTDL element in their definition).
- **Two validators, both containerized**, run in CI on every PR. A model change that does not
  parse cannot merge.
- **Typed interfaces and semantic IDs are what make policy possible later.** An OPA rule in
  Lesson 9 can say "only role *operator* may invoke `MpptChargeController.resetDailyCounters`" or
  "deny any write to a property whose semantic ID is a rating" only because those names are
  declared, typed and stable.
- DTMIs are `;1` until Lesson 4 uploads them to a runtime. After that a breaking change means a
  new version, never an in-place edit.

## Revisit when

- The DTDL → Ditto/WoT generator in Lesson 4 is harder than the twin itself. Consider WoT TM as
  primary, with DTDL dropped or generated.
- A partner or tool needs full IDTA template conformance (add `AddressInformation`, choose ECLASS
  IRDIs for the technical properties).
- DTDL v4 features (nullable command payloads, `scaledDecimal`) become necessary, or AAS 3.2
  tooling becomes the norm.
