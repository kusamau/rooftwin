# docker/

Dockerfiles for the containers that are not Java applications (Java modules are built with Jib).

| Directory | Lesson | Purpose |
|---|---|---|
| `bridge/` | 3 | Python `victron-ble` → MQTT bridge for the SmartSolar MPPT and the SmartShunt |
| `replay-simulator/` | 3 | Replays recorded Parquet telemetry into MQTT at 1× / 10× / 100× |
| `dtdl-validator/` | 2 | Validates `models/dtdl/` with Microsoft DTDLParser (WebAssembly build on Node) |
| `aas-validator/` | 2 | Validates `models/aas/Solar.json` against AAS metamodel V3.0 and V3.1 |

