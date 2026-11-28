# models/aas/

`Solar.json` is the Asset Administration Shell environment (metamodel V3; validated against both
V3.0 and V3.1) for every asset of the installation: 9 shells (8 assets + 1 type), 17 submodels,
19 concept descriptions.

**Do not edit `Solar.json` by hand.** Edit the `ASSETS` table in `generate_solar_aas.py`, then:

```bash
python3 models/aas/generate_solar_aas.py           # regenerate
docker build -t aas-validator docker/aas-validator
docker run --rm -v "$PWD/models/aas:/w:ro" aas-validator /w/Solar.json
```

CI fails if `Solar.json` is stale (`--check`), invalid, or disagrees with the Java specs in
`twin-core` (`AasSpecConsistencyTest`).

| Submodel | Template | Status |
|---|---|---|
| `Nameplate` | IDTA 02006-3-0 Digital Nameplate | *aligned*: template idShorts and semantic IDs, but not all mandatory elements (no `AddressInformation`, no `OrderCodeOfManufacturer`) |
| `TechnicalData` | IDTA 02003 Technical Data 2.0 | *aligned*: template structure; property semantic IDs are our own concept descriptions until ECLASS / IEC CDD IDs are chosen |

Properties with no `value` are deliberate: the fact is known to exist and has not been read yet
(SmartShunt rating, battery C capacity, positions on the cable hop).

Open the file in the Eclipse AASX Package Explorer or BaSyx Web UI to browse it.
