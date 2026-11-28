# Information model — Lesson 2

What the twin knows about the installation's *structure*: the DTDL v3 interfaces, the AAS
environment, the Java mirror, and the modelling decisions behind them.

```
Site ─hasSolarArray──────────► SolarArray ──feeds──► MpptChargeController ──chargesInto──┐
     ─hasMpptChargeController─►                                                         ▼
     ─hasSmartShunt──────────► SmartShunt ────────────────measures──────────────► BatteryBank
     ─hasBatteryBank─────────►                                                    ├ battery_a : Battery
     ─hasInverter────────────► Inverter ──────────────────drawsFrom─────────────► ├ battery_b : Battery
                                                                                  └ battery_c : Battery
```

## 1. Interfaces

| Interface | Properties | Telemetry | Commands / relationships |
|---|---|---|---|
| `Site;1` | location (GeoJSON point), timezone | – | hasSolarArray, hasMpptChargeController, hasSmartShunt, hasBatteryBank, hasInverter |
| `SolarArray;1` | serial, model, wpPeak, tiltDeg, azimuthDeg | panelVoltageV°, panelCurrentA°, powerW, irradianceWm2 (derived)°, moduleTempC (estimated)° | feeds → MPPT |
| `MpptChargeController;1` | model, serial, maxPvVoltageV, maxChargeCurrentA | pvVoltageV°, pvCurrentA°, pvPowerW, batteryVoltageV, batteryCurrentA, chargeStateCode, chargerErrorCode, dailyEnergyKwh, internalTempC° | resetDailyCounters; chargesInto → bank |
| `SmartShunt;1` | model, serial, shuntRatingA | voltageV, currentA, socPct, consumedAh, timeToGoMin°, alarmFlags | measures → bank |
| `BatteryBank;1` | topology, cabling, nominalVoltageV; Components battery_a/b/c | effectiveCapacityAhEstimated°, healthIndex° | – |
| `Battery;1` (Component only) | slot, model, chemistry, construction, nominalCapacityAh, installedOn, hopPosition | relativeHealthDelta°, estimatedEsrMilliOhm° | – |
| `Inverter;1` | model, ratedPowerW, acOutputVoltageV, **isOn (writable)** | – | drawsFrom → bank |

° optional: `null` in Java and absent in JSON until a source exists. Each element's DTDL
`description` says where its value comes from.

## 2. Decisions

| # | Decision | Why | Alternative rejected |
|---|---|---|---|
| D1 | Batteries are **Components** of `BatteryBank` in DTDL, but **separate shells** in AAS | In the runtime, a battery has no twin of its own: nothing measures it, and its state is inferred from the bank. In the exchange format, each battery is a physical asset with its own nameplate and its own end of life | One monolithic bank (hides the most interesting physics); three twins with relationships in DTDL (implies independent observability we don't have) |
| D2 | One **typed relationship per asset** on `Site` (`hasInverter`, ...) | A DTDL Relationship has at most one `target`. An untyped `contains` would accept anything | Single `contains` with no target |
| D3 | **Electrical topology as relationships** (`feeds`, `chargesInto`, `measures`, `drawsFrom`) | The graph can answer "what feeds the bank?" and "which sensor sees it?". Lessons 5 and 7 use this | Topology only in prose |
| D4 | `chemistry` and `construction` are **separate** | Deep-cycle and SLI are both flooded lead-acid; they differ in plate design, which is what ages an SLI battery used for cycling. One field mixed two concepts | `chemistry: deep-cycle \| SLI` (planning doc) |
| D5 | `hopPosition`, not `agePosition` | The field is the battery's position on the cable hop, not its age. Age is `installedOn` | Contract name |
| D6 | `Inverter.isOn` is a **writable property**, not a command | It records state that a human set by hand. A command would suggest the twin can switch the inverter, and it can't | `switchOn()` / `switchOff()` commands |
| D7 | **Declared ≠ observed**: telemetry the hardware can't provide today is still modelled, as optional | The model describes the asset, not today's data path. Lesson 5 fills some values | Leave them out and remodel later |
| D8 | **Declared ≠ permitted**: `resetDailyCounters` exists in the model and is never sent | Completeness of the model is separate from authority to act. Policy comes in Lesson 9 | Omit commands while in shadow mode |
| D9 | Raw `chargeStateCode` integer + Java `ChargeState` interpretation | A firmware update that adds a state shouldn't make the twin drop whole snapshots | Strict enum at deserialization |
| D10 | GeoJSON `[longitude, latitude]`, **coarse** location | RFC 7946 order. The DTDL spec's own example lists latitude first, which is a trap. Two decimals (~1 km) are enough for sun position, and a public repo should not pinpoint a house | Precise coordinates |
| D11 | `socPct`, `healthIndex` without a semantic type | QuantitativeTypes has no state-of-charge type; `Unitless` belongs to concentration and humidity types | Misusing `RelativeHumidity` to get `percent` |
| D12 | Repo-specific validator rule: **no dangling relationship targets** | DTDL allows a target that names no interface (by-reference semantics). In a closed model that's always a typo | Rely on the parser alone |
| D13 | Battery **slots follow the cable hop**: `battery_a` is nearest the bus take-off | One convention for slot letters, hop position and the per-battery cable resistance in Lesson 5. The System Assessment lettered them differently; `conceptual-model.md` §7.2 is the reference now | Letters by purchase order |
| D14 | No `SmartShunt.temperatureC` | The aux input has no temperature sensor. A field that can never be filled is noise; it comes back as a model version bump if a sensor is added | Keep it optional "just in case" |

## 3. One concept, three places

| Concept | DTDL (shape) | AAS (fact) | Java |
|---|---|---|---|
| Max charge current | `MpptChargeController.maxChargeCurrentA` (Property, Current, ampere) | `TechnicalData/…/MaxChargeCurrent = 30` → CD `…/cd/MaxChargeCurrent` (unit A) | `MpptSpec.maxChargeCurrentA()` |
| Max PV voltage | `maxPvVoltageV` | `MaxPvVoltage = 100` | `MpptSpec.maxPvVoltage()` |
| PV peak power | `SolarArray.wpPeak` | `PeakPower = 615` | `SolarArraySpec.wpPeak()` |
| Serial number | `MpptChargeController.serial` (string, no value) | `Nameplate/SerialNumber = HQ223242XYT`, semanticId `0112/2///61987#ABA951#009` | – |
| Battery chemistry | `Battery.chemistry` enum values | `Chemistry = LEAD_ACID_FLOODED` | `core.Chemistry` constants |
| Battery parts of the bank | Components `battery_a/b/c` | `BatteryBank/TechnicalData/…/Batteries`: list of references to shells | `BatteryTelemetry.COMPONENTS` |
| Rated inverter power | `Inverter.ratedPowerW` | `RatedPower = 300` | `InverterSpec.ratedPowerW()` |

Kept in agreement by: `DtdlConformanceTest` (DTDL ↔ Java), `AasSpecConsistencyTest` (AAS ↔ Java),
and, for DTDL ↔ AAS, the definition text of each concept description, which is by hand.

## 4. Telemetry on the wire

```json
{"$model": "dtmi:io:github:kusamau:rooftwin:SmartShunt;1",
 "twinId": "shunt-1", "observedAt": "2026-11-19T19:42:10Z",
 "voltageV": 12.21, "currentA": -27.4, "socPct": 81.5, "consumedAh": -24.6,
 "timeToGoMin": 236.0, "alarmFlags": 0}
```

- `$model` names the DTDL interface. Java dispatches on it (`Telemetry` is sealed).
- Required readings missing, or `null`, are rejected. Optional readings are omitted, never `null`.
- Unknown fields are rejected by `TwinJson.mapper()`. Lesson 3's bridge must produce exactly this
  shape, and the strictness will tell it when it doesn't.

## 5. Open items carried into the model

| Item | Where it shows | Close by |
|---|---|---|
| SmartShunt rating (500 A?) | AAS `ShuntRating` has no value | Read the shunt label or VictronConnect product info |
| PV module serial; manufacturer/series on the label | `SolarArray.serial` unset; AAS Nameplate | Read the module label ("HiKu6" is also a series name used by another manufacturer, so it's worth a second look) |

Closed in Lesson 2 from `conceptual-model.md` §7: battery order on the hop (a = Bosch S5 nearest,
b = SB 130 Ah, c = Bosch S4 44 Ah farthest), tilt 70° and azimuth 200°, and no battery temperature
sensor.
