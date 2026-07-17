#!/usr/bin/env python3
"""Fail closed if the private launch inventory conceals breadth, gaps, or target ambiguity."""
from __future__ import annotations

import csv
import json
from collections import Counter
from pathlib import Path


ROOT = Path(__file__).resolve().parents[1]
PATH = ROOT / "design/handoff/PRIVATE-LAUNCH-CANDIDATE-INVENTORY.json"
DISCOVERY = ROOT / "design/handoff/EXTERNAL-TARGET-DISCOVERY-RECEIPT-2026-07-17.json"


def require(condition: bool, message: str) -> None:
    if not condition:
        raise AssertionError(message)


def main() -> None:
    data = json.loads(PATH.read_text(encoding="utf-8"))
    discovery = json.loads(DISCOVERY.read_text(encoding="utf-8"))
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
    external = data["external_target_discovery"]
    require(external["vercel_project_binding"] == "verified_prj_UygHA98HGW4IBVMk6AKzXVEG6ZSQ"
            and external["supabase_project_binding"] == "verified_production_fndmhbpxnodrnbrzrlqq"
            and external["railway_project_binding"] == "verified_two_production_families_only",
            "verified external identity bindings drift")
    require(discovery["vercel"]["project_id"] == "prj_UygHA98HGW4IBVMk6AKzXVEG6ZSQ"
            and discovery["supabase"]["project_ref"] == "fndmhbpxnodrnbrzrlqq"
            and len(discovery["railway"]["families"]) == 2,
            "external discovery receipt does not support inventory bindings")
    require(discovery["production_mutated"] is False
            and discovery["public_launch"] is False
            and discovery["railway"]["private_staging_result"] == "not_created"
            and discovery["vercel"]["direct_preview_result"] == "not_deployed",
            "external target boundary weakened or overclaimed")
    require("none of those media bytes is present in this checkout" in data["surfaces"]["media"]["status"],
            "missing media-byte custody blocker concealed")
    require(data["approval"]["unseen_later_builds"] == "not_human_approved"
            and data["supabase_july_2026_boundary"]["production_sql_applied"] is False,
            "approval or production SQL overclaim")
    print("Private launch-candidate inventory PASS: 82 legacy nodes, 12 current arcs, exact external identities and residues explicit")


if __name__ == "__main__":
    main()
