#!/usr/bin/env python3
"""Generate models/aas/Solar.json, the AAS (metamodel V3) environment for rooftwin.

Standard library only. Edit the ASSETS table below, run the script, commit both files:

    python3 models/aas/generate_solar_aas.py          # writes models/aas/Solar.json
    python3 models/aas/generate_solar_aas.py --check  # exits 1 if Solar.json is stale

Design choices (see docs/architecture-choice.md):
- One AssetAdministrationShell per physical asset, plus one *type* shell for the MPPT model, so the
  type/instance split is visible (instance.derivedFrom -> type).
- Two submodels per asset:
  * "Nameplate": aligned with IDTA 02006-3-0 Digital Nameplate. idShorts and semanticIds are
    copied from the published template. Only the elements we can fill are present, so the
    submodel is template-*aligned*, not template-*conformant* (AddressInformation and
    OrderCodeOfManufacturer are mandatory in the template and missing here).
  * "TechnicalData": aligned with IDTA 02003 Technical Data 2.0. Our properties have no verified
    ECLASS/IEC CDD identifiers yet, so their semanticIds point at our own ConceptDescriptions
    (IEC 61360 data specification, with units). Owning the IRIs is a stopgap, not a goal.
- A property whose value is unknown is still present, with no value. "We know this exists and we
  have not measured it" is information too (today: the SmartShunt rating).
"""

from __future__ import annotations

import json
import sys
from pathlib import Path

OUT = Path(__file__).with_name("Solar.json")
BASE = "https://github.com/kusamau/rooftwin/ids"

# --- Semantic IDs copied from the IDTA templates (do not retype them from memory) --------------
NAMEPLATE_SM = "https://admin-shell.io/idta/nameplate/3/0/Nameplate"  # IDTA 02006-3-0
NP = {
    "ManufacturerName": "0112/2///61987#ABA565#009",
    "ManufacturerProductDesignation": "0112/2///61987#ABA567#009",
    "ManufacturerProductFamily": "0112/2///61987#ABP464#002",
    "ManufacturerProductType": "0112/2///61987#ABA300#008",
    "SerialNumber": "0112/2///61987#ABA951#009",
    "YearOfConstruction": "0112/2///61987#ABP000#002",
}
TECHDATA_SM = "0173-1#01-AHX837#002"  # IDTA 02003 Technical Data 2.0
TD_GENERAL = "0173-1#02-ABK161#002/0173-1#01-AHX838#002"
TD_GENERAL_MANUFACTURER = "0173-1#02-AAO677#004"
TD_GENERAL_DESIGNATION = "0173-1#02-AAW338#003"
TD_AREAS = "0173-1#02-ABK163#002"
TD_AREA = "0173-1#02-ABL358#002/0173-1#01-AHX773#002"
IEC61360 = "https://admin-shell.io/DataSpecificationTemplates/DataSpecificationIec61360/3/0"

# --- Our own concepts: idShort -> (preferred name, definition, unit, IEC 61360 data type,
#     AAS value type, DTDL element name or None)
CONCEPTS = {
    "PeakPower": ("Peak power", "Rated DC output power at Standard Test Conditions (1000 W/m², 25 °C cell, AM1.5).", "W", "REAL_MEASURE", "xs:double", "wpPeak"),
    "VoltageAtMaxPower": ("Voltage at maximum power", "Module voltage at the maximum power point, at STC.", "V", "REAL_MEASURE", "xs:double", None),
    "CurrentAtMaxPower": ("Current at maximum power", "Module current at the maximum power point, at STC.", "A", "REAL_MEASURE", "xs:double", None),
    "OpenCircuitVoltage": ("Open-circuit voltage", "Module voltage with no load connected, at STC.", "V", "REAL_MEASURE", "xs:double", None),
    "ShortCircuitCurrent": ("Short-circuit current", "Module current with the terminals shorted, at STC.", "A", "REAL_MEASURE", "xs:double", None),
    "CellsInSeries": ("Cells in series", "Number of PV cells connected in series along one string.", None, "INTEGER_COUNT", "xs:int", None),
    "MaxPvVoltage": ("Maximum PV voltage", "Absolute maximum PV open-circuit voltage the charge controller tolerates.", "V", "REAL_MEASURE", "xs:double", "maxPvVoltageV"),
    "MaxChargeCurrent": ("Maximum charge current", "Maximum battery-side charge current of the controller.", "A", "REAL_MEASURE", "xs:double", "maxChargeCurrentA"),
    "ShuntRating": ("Shunt rating", "Current at which the measurement shunt produces its rated voltage drop.", "A", "REAL_MEASURE", "xs:double", "shuntRatingA"),
    "NominalVoltage": ("Nominal DC voltage", "Nominal voltage of the DC system or battery.", "V", "REAL_MEASURE", "xs:double", "nominalVoltageV"),
    "NominalCapacity": ("Nominal capacity", "Rated capacity at the 20-hour discharge rate (C20).", "A·h", "REAL_MEASURE", "xs:double", "nominalCapacityAh"),
    "Chemistry": ("Battery chemistry", "Electrochemistry of the battery cells, e.g. LEAD_ACID_FLOODED.", None, "STRING", "xs:string", "chemistry"),
    "Construction": ("Battery construction", "What the battery was built for: deep-cycle (thick plates) or SLI starter (thin plates).", None, "STRING", "xs:string", "construction"),
    "InstalledOn": ("Installation month", "Year and month in which the battery was installed in this bank.", None, "STRING", "xs:gYearMonth", "installedOn"),
    "HopPosition": ("Position on the cable hop", "Position of the battery along the post-to-post cabling, counted from the bus take-off.", None, "STRING", "xs:string", "hopPosition"),
    "Topology": ("Bank topology", "How the batteries of the bank are connected: parallel or series.", None, "STRING", "xs:string", "topology"),
    "Cabling": ("Bank cabling", "How the parallel connections are made: cable-hop, busbar or diagonal.", None, "STRING", "xs:string", "cabling"),
    "Tilt": ("Tilt", "Angle between the module plane and horizontal: 0 = flat, 90 = vertical.", "°", "REAL_MEASURE", "xs:double", "tiltDeg"),
    "Azimuth": ("Azimuth", "Direction the module faces, as a compass bearing clockwise from north: 180 = due south.", "°", "REAL_MEASURE", "xs:double", "azimuthDeg"),
    "RatedPower": ("Rated power", "Continuous AC output power the inverter is rated for.", "W", "REAL_MEASURE", "xs:double", "ratedPowerW"),
    "AcOutputVoltage": ("AC output voltage", "Nominal RMS voltage of the inverter's AC output.", "V", "REAL_MEASURE", "xs:double", "acOutputVoltageV"),
}

# --- The assets ---------------------------------------------------------------------------------
# nameplate: dict or None (a site-built bank has no nameplate); tech: list of (idShort, value|None)
ASSETS = [
    {
        "key": "mppt-type",
        "idShort": "SmartSolarMppt100_30_Type",
        "kind": "Type",
        "globalAssetId": f"{BASE}/asset/type/victron/smartsolar-mppt-100-30",
        "nameplate": {
            "ManufacturerName": "Victron Energy",
            "ManufacturerProductDesignation": "SmartSolar MPPT 100/30",
            "ManufacturerProductFamily": "SmartSolar MPPT",
        },
        "tech": [("MaxPvVoltage", "100"), ("MaxChargeCurrent", "30"), ("NominalVoltage", "12")],
    },
    {
        "key": "mppt",
        "idShort": "MpptChargeController",
        "kind": "Instance",
        "derivedFrom": "mppt-type",
        "globalAssetId": f"{BASE}/asset/victron/HQ223242XYT",
        "nameplate": {
            "ManufacturerName": "Victron Energy",
            "ManufacturerProductDesignation": "SmartSolar MPPT 100/30",
            "SerialNumber": "HQ223242XYT",
        },
        "tech": [("MaxPvVoltage", "100"), ("MaxChargeCurrent", "30"), ("NominalVoltage", "12")],
    },
    {
        "key": "shunt",
        "idShort": "SmartShunt",
        "kind": "Instance",
        "globalAssetId": f"{BASE}/asset/victron/HQ2435GMKJX",
        "nameplate": {
            "ManufacturerName": "Victron Energy",
            "ManufacturerProductDesignation": "SmartShunt",
            "SerialNumber": "HQ2435GMKJX",
        },
        # Rating not yet read from the device label (open item): 500 A is the most common variant.
        "tech": [("ShuntRating", None)],
    },
    {
        "key": "pv",
        "idShort": "SolarArray",
        "kind": "Instance",
        "globalAssetId": f"{BASE}/asset/pv-array-1",
        "nameplate": {
            "ManufacturerName": "Aiko Solar",
            "ManufacturerProductDesignation": "Aiko Solar HiKu6 615W",
        },
        "tech": [
            ("PeakPower", "615"),
            ("VoltageAtMaxPower", "44.98"),
            ("CurrentAtMaxPower", "13.68"),
            ("OpenCircuitVoltage", "54.29"),
            ("ShortCircuitCurrent", "14.44"),
            ("CellsInSeries", "144"),
            ("Tilt", "70"),
            ("Azimuth", "200"),
        ],
    },
    # Slots follow the cable hop: a is nearest the bus take-off (docs/conceptual-model.md §7.2).
    {
        "key": "battery-a",
        "idShort": "Battery_A",
        "kind": "Instance",
        "globalAssetId": f"{BASE}/asset/battery/a",
        "nameplate": {
            "ManufacturerName": "Bosch",
            "ManufacturerProductDesignation": "S5 010",
            "ManufacturerProductFamily": "S5",
        },
        "tech": [
            ("NominalVoltage", "12"),
            ("NominalCapacity", "85"),
            ("Chemistry", "LEAD_ACID_FLOODED"),
            ("Construction", "SLI"),
            ("InstalledOn", "2022-01"),
            ("HopPosition", "closest-to-bus"),
        ],
    },
    {
        "key": "battery-b",
        "idShort": "Battery_B",
        "kind": "Instance",
        "globalAssetId": f"{BASE}/asset/battery/b",
        "nameplate": {
            "ManufacturerName": "SB",
            "ManufacturerProductDesignation": "HL500 Leisure Deep Cycle 12 V 130 Ah",
        },
        "tech": [
            ("NominalVoltage", "12"),
            ("NominalCapacity", "130"),
            ("Chemistry", "LEAD_ACID_FLOODED"),
            ("Construction", "deep-cycle"),
            ("InstalledOn", "2025-01"),
            ("HopPosition", "middle"),
        ],
    },
    {
        "key": "battery-c",
        "idShort": "Battery_C",
        "kind": "Instance",
        "globalAssetId": f"{BASE}/asset/battery/c",
        "nameplate": {
            "ManufacturerName": "Bosch",
            "ManufacturerProductDesignation": "S4 (UK type 063)",
            "ManufacturerProductFamily": "S4",
        },
        "tech": [
            ("NominalVoltage", "12"),
            ("NominalCapacity", "44"),
            ("Chemistry", "LEAD_ACID_FLOODED"),
            ("Construction", "SLI"),
            ("InstalledOn", "2020-07"),
            ("HopPosition", "farthest-from-bus"),
        ],
    },
    {
        "key": "bank",
        "idShort": "BatteryBank",
        "kind": "Instance",
        "globalAssetId": f"{BASE}/asset/battery-bank",
        "nameplate": None,
        "tech": [("NominalVoltage", "12"), ("Topology", "parallel"), ("Cabling", "cable-hop")],
        "parts": ["battery-a", "battery-b", "battery-c"],
    },
    {
        "key": "inverter",
        "idShort": "Inverter",
        "kind": "Instance",
        "globalAssetId": f"{BASE}/asset/inverter-1",
        "nameplate": {
            "ManufacturerName": "BESTEK",
            "ManufacturerProductDesignation": "300 W pure-sine inverter",
        },
        "tech": [("RatedPower", "300"), ("NominalVoltage", "12"), ("AcOutputVoltage", "230")],
    },
]


# --- Builders -----------------------------------------------------------------------------------
def ext_ref(value: str) -> dict:
    return {"type": "ExternalReference", "keys": [{"type": "GlobalReference", "value": value}]}


def model_ref(kind: str, value: str) -> dict:
    return {"type": "ModelReference", "keys": [{"type": kind, "value": value}]}


def mlp(id_short: str, text: str, semantic_id: str) -> dict:
    return {
        "modelType": "MultiLanguageProperty",
        "idShort": id_short,
        "semanticId": ext_ref(semantic_id),
        "value": [{"language": "en", "text": text}],
    }


def prop(id_short: str, value: str | None, value_type: str, semantic: dict) -> dict:
    p = {"modelType": "Property", "idShort": id_short, "semanticId": semantic, "valueType": value_type}
    if value is not None:
        p["value"] = value
    return p


def aas_id(key: str) -> str:
    return f"{BASE}/aas/{key}"


def sm_id(key: str, name: str) -> str:
    return f"{BASE}/sm/{key}/{name.lower()}"


def cd_id(id_short: str) -> str:
    return f"{BASE}/cd/{id_short}"


def nameplate(asset: dict) -> dict:
    elements = []
    for k, v in asset["nameplate"].items():
        if k in ("ManufacturerName", "ManufacturerProductDesignation", "ManufacturerProductFamily"):
            elements.append(mlp(k, v, NP[k]))
        else:
            elements.append(prop(k, v, "xs:string", ext_ref(NP[k])))
    return {
        "modelType": "Submodel",
        "id": sm_id(asset["key"], "Nameplate"),
        "idShort": "Nameplate",
        "kind": "Instance",
        "semanticId": ext_ref(NAMEPLATE_SM),
        "submodelElements": elements,
    }


def technical_data(asset: dict) -> dict:
    general = []
    if asset["nameplate"]:
        np = asset["nameplate"]
        general = [
            prop("ManufacturerName", np["ManufacturerName"], "xs:string", ext_ref(TD_GENERAL_MANUFACTURER)),
            mlp("ManufacturerProductDesignation", np["ManufacturerProductDesignation"], TD_GENERAL_DESIGNATION),
        ]
    props = []
    for id_short, value in asset["tech"]:
        value_type = CONCEPTS[id_short][4]
        props.append(prop(id_short, value, value_type, model_ref("ConceptDescription", cd_id(id_short))))
    if "parts" in asset:
        # The AAS way to say "the bank is made of these three assets": references to other shells.
        # (DTDL says the same with Components, which are by value; AAS references are by reference.)
        props.append(
            {
                "modelType": "SubmodelElementList",
                "idShort": "Batteries",
                "orderRelevant": True,
                "typeValueListElement": "ReferenceElement",
                "value": [
                    {"modelType": "ReferenceElement", "value": model_ref("AssetAdministrationShell", aas_id(p))}
                    for p in asset["parts"]
                ],
            }
        )
    elements = []
    if general:
        elements.append(
            {
                "modelType": "SubmodelElementCollection",
                "idShort": "GeneralInformation",
                "semanticId": ext_ref(TD_GENERAL),
                "value": general,
            }
        )
    elements.append(
        {
            "modelType": "SubmodelElementList",
            "idShort": "TechnicalPropertyAreas",
            "semanticId": ext_ref(TD_AREAS),
            "typeValueListElement": "SubmodelElementCollection",
            "semanticIdListElement": ext_ref(TD_AREA),
            "value": [{"modelType": "SubmodelElementCollection", "semanticId": ext_ref(TD_AREA), "value": props}],
        }
    )
    return {
        "modelType": "Submodel",
        "id": sm_id(asset["key"], "TechnicalData"),
        "idShort": "TechnicalData",
        # A *type* asset is still described by Instance submodels; ModellingKind.Template is for
        # submodel templates (like the IDTA ones), not for asset types.
        "kind": "Instance",
        "semanticId": ext_ref(TECHDATA_SM),
        "submodelElements": elements,
    }


def shell(asset: dict, submodels: list[dict]) -> dict:
    s = {
        "modelType": "AssetAdministrationShell",
        "id": aas_id(asset["key"]),
        "idShort": asset["idShort"],
        "assetInformation": {"assetKind": asset["kind"], "globalAssetId": asset["globalAssetId"]},
        "submodels": [model_ref("Submodel", sm["id"]) for sm in submodels],
    }
    if "derivedFrom" in asset:
        s["derivedFrom"] = model_ref("AssetAdministrationShell", aas_id(asset["derivedFrom"]))
    return s


def concept(id_short: str) -> dict:
    name, definition, unit, data_type, _, dtdl = CONCEPTS[id_short]
    if dtdl:
        definition += f" Same concept as the DTDL element '{dtdl}' in models/dtdl."
    content = {
        "modelType": "DataSpecificationIec61360",
        "preferredName": [{"language": "en", "text": name}],
        "definition": [{"language": "en", "text": definition}],
        "dataType": data_type,
    }
    if unit:
        content["unit"] = unit
    return {
        "modelType": "ConceptDescription",
        "id": cd_id(id_short),
        "idShort": id_short,
        "embeddedDataSpecifications": [{"dataSpecification": ext_ref(IEC61360), "dataSpecificationContent": content}],
    }


def build() -> dict:
    shells, submodels = [], []
    for asset in ASSETS:
        sms = ([nameplate(asset)] if asset["nameplate"] else []) + [technical_data(asset)]
        submodels += sms
        shells.append(shell(asset, sms))
    used = sorted({k for a in ASSETS for k, _ in a["tech"]})
    return {
        "assetAdministrationShells": shells,
        "submodels": submodels,
        "conceptDescriptions": [concept(k) for k in used],
    }


def main() -> int:
    text = json.dumps(build(), indent=2, ensure_ascii=False) + "\n"
    if "--check" in sys.argv:
        if not OUT.exists() or OUT.read_text(encoding="utf-8") != text:
            print(f"{OUT} is stale: run python3 {Path(__file__).name}", file=sys.stderr)
            return 1
        print(f"{OUT.name} is up to date")
        return 0
    OUT.write_text(text, encoding="utf-8")
    print(f"wrote {OUT} ({len(ASSETS)} shells)")
    return 0


if __name__ == "__main__":
    sys.exit(main())
