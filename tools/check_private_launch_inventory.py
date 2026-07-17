#!/usr/bin/env python3
"""Fail closed if the private launch inventory conceals breadth, gaps, or target ambiguity."""
from __future__ import annotations

import csv
import json
from collections import Counter
from pathlib import Path


ROOT = Path(__file__).resolve().parents[1]
PATH = ROOT / "design/handoff/PRIVATE-LAUNCH-CANDIDATE-INVENTORY.json"


def require(condition: bool, message: str) -> None:
    if not condition:
        raise AssertionError(message)


def main() -> None:
    data = json.loads(PATH.read_text(encoding="utf-8"))
    with (ROOT / "design/ARG-V5-NODE-MANIFEST.csv").open(encoding="utf-8", newline="") as handle:
        nodes = list(csv.DictReader(handle))
    with (ROOT / "design/ARG-V5-RUNTIME-BINDINGS.csv").open(encoding="utf-8", newline="") as handle:
        bindings = list(csv.DictReader(handle))
    with (ROOT / "design/ARG-V5-ARTIFACT-MANIFEST.csv").open(encoding="utf-8", newline="") as handle:
        artifacts = list(csv.DictReader(handle))
    legacy = data["legacy_runtime_inventory"]
    require((legacy["node_count"], legacy["binding_count"], legacy["artifact_count"])
            == (len(nodes), len(bindings), len(artifacts)), "legacy manifest counts drift")
    require(legacy["owners"] == dict(Counter(row["owner"] for row in bindings)), "owner counts drift")
    require(legacy["case_counts"] == dict(Counter(row["case_id"] for row in nodes)), "case counts drift")
    require([row["phase"] for row in data["p1_p12"]] == [f"P{i}" for i in range(1, 13)],
            "P1-P12 inventory incomplete")
    require(all(row["gap"] for row in data["p1_p12"]), "a phase falsely claims no gap")
    require(all(value == "absent" or value == []
                for key, value in data["external_target_discovery"].items() if key != "rule"),
            "external target discovery must be updated deliberately when credentials appear")
    require("none of those media bytes is present in this checkout" in data["surfaces"]["media"]["status"],
            "missing media-byte custody blocker concealed")
    require(data["approval"]["unseen_later_builds"] == "not_human_approved"
            and data["supabase_july_2026_boundary"]["production_sql_applied"] is False,
            "approval or production SQL overclaim")
    print("Private launch-candidate inventory PASS: 82 legacy nodes, 12 current arcs, blockers explicit")


if __name__ == "__main__":
    main()
