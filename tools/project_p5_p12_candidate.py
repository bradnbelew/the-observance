#!/usr/bin/env python3
"""Deterministically project the canonical authored campaign into each runtime package."""
from __future__ import annotations

import hashlib
import json
from pathlib import Path


ROOT = Path(__file__).resolve().parents[1]
SOURCE = ROOT / "campaign/p5-p12"
TARGETS = [
    ROOT / "plugin/src/main/resources/campaign/p5-p12.json",
    ROOT / "dashboard/content/campaign/p5-p12.json",
    ROOT / "discord/src/v5/generated/p5-p12.json",
]
MANIFEST = SOURCE / "projection-manifest.json"
BINDING_SOURCE = SOURCE / "minecraft-bindings.json"
BINDING_TARGET = ROOT / "plugin/src/main/resources/campaign/p5-p12-minecraft-bindings.json"
EXPERIENCE_SOURCE = ROOT / "campaign/arg-experience-redesign.json"
CHOREOGRAPHY_SOURCE = ROOT / "campaign/arg-state-choreography.json"
INPUT_CONTRACT_SOURCE = SOURCE / "input-contracts.json"


def canonical_bytes(value: object) -> bytes:
    return (json.dumps(value, indent=2, ensure_ascii=False, sort_keys=True) + "\n").encode("utf-8")


def main() -> None:
    index = json.loads((SOURCE / "campaign.json").read_text(encoding="utf-8"))
    experience_root = json.loads(EXPERIENCE_SOURCE.read_text(encoding="utf-8"))
    experience_by_phase = {case["phase"]: case for case in experience_root["cases"]}
    choreography = json.loads(CHOREOGRAPHY_SOURCE.read_text(encoding="utf-8"))
    input_contract_root = json.loads(INPUT_CONTRACT_SOURCE.read_text(encoding="utf-8"))
    input_contracts = {contract["id"]: contract for contract in input_contract_root["contracts"]}
    used_input_contracts = set()
    cases = []
    for row in index["phases"]:
        if row["status"] != "authored_content_scaffolding":
            raise RuntimeError(f"refusing incomplete projection: {row['id']}={row['status']}")
        case = json.loads((SOURCE / row["file"]).read_text(encoding="utf-8"))
        experience = experience_by_phase.get(row["id"])
        if experience is None or experience["case_id"] != row["experience_case_id"]:
            raise RuntimeError(f"missing/mismatched ARG experience redesign: {row['id']}")
        case["arg_experience"] = experience
        conclusion_rows = list(case.get("conclusions", []))
        if case.get("group_conclusion"):
            conclusion_rows.append(case["group_conclusion"])
        for conclusion in conclusion_rows:
            contract = input_contracts.get(conclusion["id"])
            if contract is None:
                raise RuntimeError(f"missing input contract: {conclusion['id']}")
            conclusion["input_contract"] = contract
            used_input_contracts.add(conclusion["id"])
        cases.append(case)
    if used_input_contracts != set(input_contracts):
        raise RuntimeError("unused or missing P5-P12 input contracts")
    projection = {
        "schema_version": "1.0.0-runtime-campaign-projection",
        "source_schema_version": index["schema_version"],
        "closed_mechanism_taxonomy": False,
        "observation_receipts_gate_answers": False,
        "experiential_status": index["experiential_status"],
        "brad_approval": None,
        "arg_state_choreography": choreography,
        "phases": [case["phase"] for case in cases],
        "cases": cases,
    }
    payload = canonical_bytes(projection)
    digest = hashlib.sha256(payload).hexdigest()
    for target in TARGETS:
        target.parent.mkdir(parents=True, exist_ok=True)
        target.write_bytes(payload)
    binding_payload = BINDING_SOURCE.read_bytes()
    BINDING_TARGET.parent.mkdir(parents=True, exist_ok=True)
    BINDING_TARGET.write_bytes(binding_payload)
    manifest = {
        "schema_version": "1.0.0-campaign-projection-manifest",
        "source_files": [
            "campaign/p5-p12/campaign.json",
            "campaign/arg-experience-redesign.json",
            "campaign/arg-state-choreography.json",
            "campaign/p5-p12/input-contracts.json",
        ] + ["campaign/p5-p12/" + row["file"] for row in index["phases"]],
        "projection_sha256": digest,
        "projection_bytes": len(payload),
        "targets": [str(path.relative_to(ROOT)).replace("\\", "/") for path in TARGETS],
        "phase_count": len(cases),
        "experiential_status": index["experiential_status"],
        "brad_approval": None,
        "production_deployed": False,
        "minecraft_binding_source": str(BINDING_SOURCE.relative_to(ROOT)).replace("\\", "/"),
        "minecraft_binding_target": str(BINDING_TARGET.relative_to(ROOT)).replace("\\", "/"),
        "minecraft_binding_sha256": hashlib.sha256(binding_payload).hexdigest(),
    }
    MANIFEST.write_bytes(canonical_bytes(manifest))
    print(f"P5-P12 projection written: phases={len(cases)} bytes={len(payload)} sha256={digest}")


if __name__ == "__main__":
    main()
