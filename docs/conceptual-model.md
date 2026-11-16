# Conceptual model — Lesson 1

What is being twinned, how it maps onto a reference model, which quantities we can actually observe,
and what maturity this learning-grade twin aims for. This document is the anchor that every later
lesson refines; it changes when reality disagrees with it.

## 1. The physical entity

A small off-grid PV system on a residential roof:

| Element | Detail | Observable? |
|---|---|---|
| PV array | ~500 Wp, exact module data still to be measured | Indirectly: PV power via the MPPT |
| MPPT charge controller | Victron SmartSolar MPPT 100/30 (100 V PV max, 30 A charge) | Yes, BLE Instant Readout |
| Battery monitor | Victron SmartShunt on the bank negative | Yes, BLE Instant Readout |
| Battery bank | 3 lead-acid batteries in parallel, 12 V bus, cable-hop wiring:<br>(a) 130 Ah leisure deep-cycle, 2025 · (b) SLI starter, 2022 · (c) SLI starter, 2020 | Bank-level only (one shunt for all three) |
| Inverter | 300 W pure-sine, 12 V DC → 230 V AC, on its own DC breaker | No telemetry; on/off state entered by hand |
| Protection | Three 50 A DC breakers; no battery main fuse (logged, revisit at course closure) | No |
| Environment | Sun position, irradiance, ambient/cell temperature | Sun position computable; weather via API (Lesson 6) |

## 2. Five-dimension mapping (Tao et al.)

Tao's five-dimension digital twin model is **DT = (PE, VE, Ss, DD, CN)**: physical entity, virtual
entity, services, twin data, and the connections between them.

| Dimension | In rooftwin | Lands in |
|---|---|---|
| **PE** — Physical entity | The table above, plus its environment (sun, temperature). The cable-hop topology and the battery mix belong here: they change behaviour. | — |
| **VE** — Virtual entity | Four kinds of model, added gradually:<br>• *structural*: static specs (`twin-core` records), DTDL / AAS information model<br>• *physical*: single-diode PV, MPPT, three Thevenin-Shepherd batteries, inverter step-load<br>• *behavioural*: data-driven forecasts, anomaly models, state of health<br>• *rules*: alarm thresholds, depth-of-discharge policy | L1 specs → L2 DTDL/AAS → L5 physics → L6 ML → L7 alarms |
| **Ss** — Services | Live monitoring; bus-voltage cross-check; PV forecast; per-battery SoH; what-if scenarios; load-shift recommendations in shadow mode | L3–L8 |
| **DD** — Twin data | Live telemetry (MQTT); static specs; derived signals; history (InfluxDB); model outputs; the audit log of recommendations | L3–L8 |
| **CN** — Connections | PE→VE: BLE → bridge → MQTT. VE↔DD: Ditto + InfluxDB. Ss↔User: Grafana, 3D house, notifications. **VE→PE: none.** Actions go to a human or to simulated loads. | L3, L4, L7, L8 |

> A note on the planning doc: it listed the five blocks as "physical, virtual, data link, services,
> users". Tao's model has *twin data* and *connections* as separate dimensions and no "users"
> dimension. Users appear as the **user entity / user domain** in the ISO 23247 reference
> architecture (Lesson 2). Both views are useful; just don't mix the labels.

## 3. What we can actually measure

Both Victron devices broadcast encrypted **Instant Readout** BLE advertisements, decoded by the
`victron-ble` Python library. Field names below follow that library and will be confirmed against
real captures in Lesson 3.

### SmartSolar MPPT 100/30 (`SolarCharger`)

| Field | Unit | Note |
|---|---|---|
| `charge_state` | enum | OFF, BULK, ABSORPTION, FLOAT, … — drives the battery model's mode |
| `battery_voltage` | V | Independent voltage measurement #1 |
| `battery_charging_current` | A | Battery-side output current |
| `solar_power` | W | PV input power. **No PV voltage or current in Instant Readout**, which constrains PV model calibration in Lesson 5 |
| `yield_today` | Wh | Daily energy counter |
| `charger_error` | enum | Fault code |
| `external_device_load` | A | Load output; not used (the 100/30 has no load terminals) |

### SmartShunt (`BatteryMonitor`)

| Field | Unit | Note |
|---|---|---|
| `voltage` | V | Independent voltage measurement #2 |
| `current` | A | Net bank current, sign = charge (+) / discharge (−) |
| `soc` | % | Coulomb-counted state of charge (bank-level) |
| `consumed_ah` | Ah | Since last full charge |
| `remaining_mins` | min | Time-to-go estimate |
| `alarm` | flags | Low/high voltage, low SoC, … |
| `aux_mode` + aux value | — | Starter voltage, midpoint or temperature, depending on configuration. Temperature would be the most useful here (open question 3). |

### Derived signals (computed by the twin, not measured)

| Signal | Formula | Why |
|---|---|---|
| `bus_voltage_delta` | `mppt.battery_voltage − shunt.voltage` | Cheap integrity check: a drift points at a corroded lug or loose terminal |
| `load_current` | `mppt.battery_charging_current − shunt.current` | All DC consumers (mostly the inverter) without a sensor of their own |
| `load_power` | `load_current × shunt.voltage` | Same, in watts |
| `mppt_efficiency_proxy` | `(mppt.battery_voltage × mppt.battery_charging_current) / solar_power` | Should be ~0.95–0.98; trend matters more than value |
| `clipping` | `mppt.battery_charging_current ≥ ~29.5 A` | At 14.4 V the 100/30 caps at ~432 W. A flat top on a clear day is expected, not a fault |

### What we **cannot** observe (and will infer)

- Per-battery current split and per-battery state of charge: one shunt measures all three batteries.
  Inferred from voltage sag on inverter switch-on, charge-tail shape and long-term trends
  (Lessons 5–6).
- Inverter state: entered by hand (`isOn`) until something better exists.
- PV string voltage: not in Instant Readout (see above).

## 4. Static specification (Lesson 1 code)

`apps/twin-core` holds three Java records: `SolarArraySpec`, `BatteryBankSpec` and `MpptSpec`. They
reject physically impossible values: Vmp ≥ Voc, a nameplate power that Vmp × Imp cannot explain, or a
"12 V" LiFePO4 bank (four cells give 12.8 V). Two derived checks already encode domain knowledge:

- `MpptSpec.acceptsArray(...)` — cold mornings raise Voc. The PV voltage limit must hold at the
  coldest expected cell temperature, not at STC.
- `MpptSpec.maxOutputPowerW(...)` — at 14.4 V the 100/30 delivers at most 432 W. With ~500 Wp of
  panels, clipping on clear summer days is a design property, not an anomaly.

The PV values in the tests are **illustrative** (two 250 W, 60-cell modules in parallel). Replace
them with real datasheet values once measured.

## 5. Model, shadow or twin? (Kritzinger et al.)

| Data flow | Physical → digital | Digital → physical | Term |
|---|---|---|---|
| Both manual | manual | manual | Digital **model** |
| One automatic | automatic | manual | Digital **shadow** |
| Both automatic | automatic | automatic | Digital **twin** |

Honest classification of rooftwin over the course:

- **Lessons 3–7: digital shadow.** Telemetry flows in automatically; nothing flows back.
- **Lesson 8: still a shadow for the real hardware, on purpose.** Recommendations reach a human
  (shadow mode) and only *simulated* loads are actuated automatically. The real Victron devices are
  never controlled. For the simulated loads the loop is closed, so that part is a twin in
  Kritzinger's strict sense.

The gap is a safety choice, not a technical limitation. Lesson 8 explains why shadow mode comes
before actuation, and the same reasoning applies to spacecraft and industrial control.

## 6. Maturity target

The most widely referenced leveling scheme is AWS's four-level **Digital Twin Leveling Index**:

| Level | Name | Meaning | rooftwin |
|---|---|---|---|
| L1 | Descriptive | Structure, components, relationships | Lesson 1–2 (specs, DTDL, AAS) |
| L2 | Informative | Live state from sensors, alarms | Lesson 3–4, 7 |
| L3 | Predictive | Pre-trained/static models predict behaviour | Lesson 5–6 (physics + forecasts) |
| L4 | Living | Model parameters keep updating from real data | Lesson 5–6 (calibration, SoH re-estimation) |
| (L5) | Autonomous | Acts on the physical system without a human | **Out of scope**, deliberately |

Some frameworks add a fifth, autonomous level by analogy with self-driving levels. rooftwin targets
**L4 Living**, with prescriptive recommendations in shadow mode on top.

For comparison, the VictronConnect app this system uses today sits at **L2**: live state, history,
alarms. Victron's cloud portal VRM (which needs a GX device this system doesn't have) adds a solar
production forecast, which reaches into L3 for that one signal.

## 7. Open items carried forward

1. PV module datasheet values (Voc, Isc, Vmp, Imp, module count, tilt, azimuth).
2. Order of the batteries on the cable hop: which is nearest the take-off point.
3. Typical inverter usage pattern.
4. Whether to configure the SmartShunt aux input as a battery temperature sensor.
5. Battery-bank main fuse (safety item, revisit at course closure).
