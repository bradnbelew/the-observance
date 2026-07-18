#!/usr/bin/env python3
"""Validate the open-ended P5-P12 authored campaign pack without scoring creativity."""
from __future__ import annotations

import json
from pathlib import Path
from typing import Any


ROOT = Path(__file__).resolve().parents[1]
PACK = ROOT / "campaign/p5-p12"
INDEX = PACK / "campaign.json"


def require(condition: bool, message: str) -> None:
    if not condition:
        raise AssertionError(message)


def walk_strings(value: Any):
    if isinstance(value, str):
        yield value
    elif isinstance(value, list):
        for item in value:
            yield from walk_strings(item)
    elif isinstance(value, dict):
        for key, item in value.items():
            yield key
            yield from walk_strings(item)


def validate_evidence(case: dict[str, Any], evidence: list[dict[str, Any]]) -> None:
    ids: set[str] = set()
    surfaces: set[str] = set()
    roles: set[str] = set()
    for item in evidence:
        evidence_id = item["id"]
        require(evidence_id not in ids, f"{case['phase']}: duplicate evidence id {evidence_id}")
        ids.add(evidence_id)
        require(item.get("medium") and item.get("surface") and item.get("provenance"),
                f"{case['phase']}: evidence lacks medium/surface/provenance: {evidence_id}")
        require(len(item.get("content", "")) >= 60,
                f"{case['phase']}: evidence is a label rather than authored content: {evidence_id}")
        require(item.get("roles") and isinstance(item["roles"], list),
                f"{case['phase']}: evidence has no dramatic role: {evidence_id}")
        surfaces.add(item["surface"])
        roles.update(item["roles"])
        require(item.get("observation_receipt_non_gating", True) is True,
                f"{case['phase']}: observation receipt gates evidence: {evidence_id}")
    require(len(surfaces) >= 3, f"{case['phase']}: authored evidence does not cross enough surfaces")
    require({"contradiction", "provenance"}.issubset(roles),
            f"{case['phase']}: contradiction/provenance work is missing")


def validate_case(case: dict[str, Any]) -> None:
    phase = case["phase"]
    for field in (
        "player_facing_question", "plausible_initial_belief", "human_stake", "earned_belief",
        "dramaturgical_rhythm", "required_inference", "callback", "consequence", "spaces",
        "meaningful_wrong_theories", "hints", "runtime", "novelty_comparison", "cold_theory_paths",
    ):
        require(case.get(field), f"{phase}: required dramaturgy field missing: {field}")
    runtime = case["runtime"]
    require(runtime.get("local_primary") is True and runtime.get("any_subset") is True,
            f"{phase}: local-primary/any-subset weakened")
    require(runtime.get("answer_requires_observation_receipts") is False,
            f"{phase}: answer acceptance incorrectly depends on observation receipts")
    require(runtime.get("replay") and runtime.get("restart") and runtime.get("outage"),
            f"{phase}: replay/restart/outage contract incomplete")
    require(all({"theory", "response"}.issubset(theory) for theory in case["meaningful_wrong_theories"]),
            f"{phase}: meaningful wrong theory lacks authored acknowledgement")
    require(set(case["hints"]) == {"H0", "H1", "H2", "H3"}, f"{phase}: H0-H3 drift")
    require(all(space.get("job") and space.get("composition") and space.get("navigation")
                for space in case["spaces"]), f"{phase}: space lacks function/composition/navigation")

    if phase == "P6":
        dossiers = case.get("dossiers", [])
        require([d["person"] for d in dossiers] == ["Vaun", "Mara", "Sella", "Orin", "Brann", "Iss"],
                "P6: six distinct Keeper dossier identities/order drift")
        require(len({d["operation"] for d in dossiers}) == 6,
                "P6: professional operations collapse to a repeated station")
        for dossier in dossiers:
            validate_evidence(case, dossier["evidence"])
        conclusions = [case["group_conclusion"]]
    else:
        validate_evidence(case, case["evidence"])
        conclusions = case.get("conclusions", [])

    require(conclusions, f"{phase}: no content-dependent conclusion contract")
    for conclusion in conclusions:
        require(conclusion.get("required_concepts") and conclusion.get("accepted_answers"),
                f"{phase}: conclusion is not meaning-defined: {conclusion.get('id')}")
        require(conclusion.get("zero_observation_acceptance") is True,
                f"{phase}: correct conclusion cannot pass at zero observations")

    text = "\n".join(walk_strings(case)).lower()
    for forbidden in (
        "mechanism_type", "source-touch required", "click every", "six were one",
        "avernyn", "server log puzzle", "f3 required",
    ):
        require(forbidden not in text, f"{phase}: forbidden stale content present: {forbidden}")


def main() -> None:
    index = json.loads(INDEX.read_text(encoding="utf-8"))
    require(index["closed_mechanism_taxonomy"] is False,
            "campaign pack became a closed mechanism taxonomy")
    require(index["observation_receipts_gate_answers"] is False,
            "campaign pack reintroduced observation gating")
    phases = index["phases"]
    require([row["id"] for row in phases] == [f"P{i}" for i in range(5, 13)],
            "campaign pack phase order/breadth drift")
    require(index.get("experiential_status") == "offline_redesigned_not_human_approved",
            "campaign pack incorrectly implies experiential acceptance")
    authored = [row for row in phases if row["status"] == "authored_content_scaffolding"]
    require(len(authored) == 8, "campaign pack is not fully authored across P5-P12")
    for row in authored:
        path = PACK / row["file"]
        require(path.is_file(), f"missing authored case file: {row['file']}")
        case = json.loads(path.read_text(encoding="utf-8"))
        require(case["phase"] == row["id"], f"case/index phase mismatch: {row['id']}")
        validate_case(case)
    print("P5-P12 authored scaffolding: PASS (8 content cases; ARG redesign offline/not human-approved; zero observation gating)")


if __name__ == "__main__":
    main()
