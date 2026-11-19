#!/usr/bin/env python3
"""Validate AAS JSON environments against the AAS metamodel.

    python3 validate_aas.py <file.json> ...

Checks, in order:
1. JSON de-serialization into the metamodel (aas-core3.0 / V3.0 and aas-core3.1 / V3.1):
   wrong modelType, missing required fields, bad enum values.
2. Metamodel constraints (aas-core verification): idShort syntax, value vs valueType,
   SubmodelElementList element types, data-specification rules, ...
3. Referential integrity, which the metamodel does not check: every ModelReference must
   point at a Submodel, AssetAdministrationShell or ConceptDescription defined in the file,
   and every idShort in a submodel's top level must be unique.

Exit codes: 0 valid, 1 invalid, 2 usage error.
"""

from __future__ import annotations

import json
import sys

import aas_core3.jsonization as j30
import aas_core3.verification as v30
import aas_core3_1.jsonization as j31
import aas_core3_1.verification as v31


def metamodel_errors(jsonable: dict) -> list[str]:
    errors = []
    for label, jz, vf in (("V3.0", j30, v30), ("V3.1", j31, v31)):
        try:
            env = jz.environment_from_jsonable(jsonable)
        except jz.DeserializationException as e:
            errors.append(f"[{label}] deserialization: {e.path}: {e.cause}")
            continue
        errors += [f"[{label}] {err.path}: {err.cause}" for err in vf.verify(env)]
    return errors


def model_refs(node, path="$"):
    if isinstance(node, dict):
        if node.get("type") == "ModelReference" and node.get("keys"):
            yield path, node["keys"][0]["type"], node["keys"][0]["value"]
        for k, v in node.items():
            yield from model_refs(v, f"{path}.{k}")
    elif isinstance(node, list):
        for i, v in enumerate(node):
            yield from model_refs(v, f"{path}[{i}]")


def integrity_errors(jsonable: dict) -> list[str]:
    defined = {
        "AssetAdministrationShell": {s["id"] for s in jsonable.get("assetAdministrationShells", [])},
        "Submodel": {s["id"] for s in jsonable.get("submodels", [])},
        "ConceptDescription": {c["id"] for c in jsonable.get("conceptDescriptions", [])},
    }
    errors = []
    for path, kind, value in model_refs(jsonable):
        if kind in defined and value not in defined[kind]:
            errors.append(f"{path}: dangling {kind} reference -> {value}")
    for kind, ids in defined.items():
        items = {"AssetAdministrationShell": "assetAdministrationShells", "Submodel": "submodels",
                 "ConceptDescription": "conceptDescriptions"}[kind]
        all_ids = [x["id"] for x in jsonable.get(items, [])]
        if len(all_ids) != len(ids):
            errors.append(f"duplicate {kind} ids")
    return errors


def main(argv: list[str]) -> int:
    if not argv:
        print(__doc__, file=sys.stderr)
        return 2
    failed = False
    for f in argv:
        with open(f, encoding="utf-8") as fh:
            jsonable = json.load(fh)
        errors = metamodel_errors(jsonable) + integrity_errors(jsonable)
        if errors:
            failed = True
            print(f"✗ {f}")
            for e in errors:
                print(f"  {e}")
        else:
            n = {k: len(jsonable.get(k, [])) for k in ("assetAdministrationShells", "submodels", "conceptDescriptions")}
            print(f"✓ {f}: {n['assetAdministrationShells']} shells, {n['submodels']} submodels, "
                  f"{n['conceptDescriptions']} concept descriptions (V3.0 and V3.1)")
    return 1 if failed else 0


if __name__ == "__main__":
    sys.exit(main(sys.argv[1:]))
