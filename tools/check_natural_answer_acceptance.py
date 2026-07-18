#!/usr/bin/env python3
"""Fail closed on hidden magic sentences and untested interpretive input shapes."""
from __future__ import annotations

import json
from pathlib import Path

ROOT = Path(__file__).resolve().parents[1]


def require(condition: bool, message: str) -> None:
    if not condition:
        raise SystemExit(f"NATURAL ANSWER ACCEPTANCE: FAIL - {message}")


def load(relative: str):
    return json.loads((ROOT / relative).read_text(encoding="utf-8"))


def main() -> None:
    contract = load("campaign/natural-answer-acceptance.json")
    require(contract["rules"]["correct_without_source_receipts"] is True, "correct knowledge must not need receipts")
    require(contract["rules"]["raw_player_prose_stored"] is False, "raw interpretive prose must not be retained")
    require(contract["rules"]["one_magic_phrase_for_interpretive_findings"] is False, "magic phrases are forbidden")

    inputs = contract["interpretive_inputs"]
    ids = [row["id"] for row in inputs]
    require(len(ids) == len(set(ids)), "interpretive input ids must be unique")
    require({"P3.DISPATCH", "P4.CONCLUSION", "P6.RESPONSIBILITY", "P7.NESSA", "P8.INTERVENTION", "P9.CAMP_PEOPLE", "P9.LEAK_WINDOW", "P10.WREN"} <= set(ids), "active prose/classification inputs are not fully covered")
    for row in inputs:
        require(row["zero_observation"] is True, f"{row['id']} still gates on source observation")
        require(len(row["predicate_owners"]) >= 1, f"{row['id']} has no actual predicate owner")
        require(len(row["fields"]) >= 1, f"{row['id']} has no visible answer shape")
        require(len(row["accepted_voice_tests"]) >= 3, f"{row['id']} lacks independent voice/paraphrase coverage")
        require(len(row["negative_tests"]) >= 4, f"{row['id']} lacks wrong-theory/shape resistance")

    contracts = load("campaign/p1-p4/input-contracts.json")["contracts"] + load("campaign/p5-p12/input-contracts.json")["contracts"]
    for row in (item for item in contracts if item.get("interpretive")):
        require(row["zero_observation_acceptance"] is True, f"{row['id']} is observation gated")
        require(row["runtime_exact_phrase"] is False, f"{row['id']} still requires an exact phrase")

    exact = contract["exact_artifacts"]
    require(len(exact) == 1 and exact[0]["id"] == "P11.AVERYN", "exact artifact exception expanded without authority")
    require(exact[0]["source_receipts_gate"] is False, "exact artifact is receipt gated")
    require("independently" in exact[0]["yield"] and exact[0]["value"] == "AVERYN", "Averyn exact-artifact trail is not explicit")

    sources = "\n".join((ROOT / path).read_text(encoding="utf-8") for path in (
        "plugin/src/test/java/com/observance/watcher/v5runtime/P3SettlementDispatchPredicateSelfTest.java",
        "plugin/src/test/java/com/observance/watcher/v5runtime/P6ResponsibilityPredicateSelfTest.java",
        "plugin/src/test/java/com/observance/watcher/v5runtime/P7NessaCorrectionPredicateSelfTest.java",
        "plugin/src/test/java/com/observance/watcher/v5runtime/P8InterventionPlanPredicateSelfTest.java",
        "plugin/src/test/java/com/observance/watcher/v5runtime/P9CampPredicateSelfTest.java",
        "plugin/src/test/java/com/observance/watcher/v5runtime/P10WrenTransmissionPredicateSelfTest.java",
    ))
    for token in ("ordinary player-language", "field-swapped", "oversized", "destructive latest-copy", "confession-only"):
        require(token in sources, f"runtime negative/paraphrase proof missing token: {token}")

    print(f"NATURAL ANSWER ACCEPTANCE: PASS - {len(inputs)} interpretive shapes, {len(exact)} fair exact artifact, zero receipt gates")


if __name__ == "__main__":
    main()
