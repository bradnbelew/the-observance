#!/usr/bin/env python3
"""Validate the canonical continuation lineage and its fail-closed M3 authority."""
from __future__ import annotations

import json
from pathlib import Path
import subprocess


ROOT = Path(__file__).resolve().parents[1]
LINEAGE = ROOT / "design" / "handoff" / "CANONICAL-LINEAGE.json"


def require(condition: bool, message: str) -> None:
    if not condition:
        raise AssertionError(message)


def git(*args: str) -> str:
    return subprocess.run(
        ["git", *args], cwd=ROOT, check=True, text=True, capture_output=True
    ).stdout.strip()


def main() -> None:
    data = json.loads(LINEAGE.read_text(encoding="utf-8"))
    require(data["schema_version"] == "1.0.0-continuation-lineage", "lineage schema drift")
    checkpoint = data["checkpoint_identity"]
    require(checkpoint["branch"] == "codex/m3-disposable-paper-gate", "canonical branch drift")
    require(checkpoint["production_mutated"] is False, "integration cannot claim production mutation")
    require(checkpoint["m4_authority"] == "closed", "M4 must remain closed")

    starting = data["starting_authority"]
    require(starting["commit"] == "8f938e0558b545a33039bb112ad091cf511a6ffe",
            "failed-review starting authority drift")
    require(starting["visual_decision"] == "failed_revision_required_2026-07-16",
            "failed Brad decision lost")

    expected = {
        "84899e0d367c8ad6151ea74b0c338e92ac8a9b7c":
            "design/m2/M2-SUPABASE-VALIDATION.md",
        "a6069aabae705f18255428f612476552c8770e33":
            "design/m3/EXTERNAL-GAP-AUDIT-2026-07-16.md",
    }
    rows = {row["source_commit"]: row for row in data["incorporated_evidence"]}
    require(set(rows) == set(expected), "sibling evidence provenance is incomplete or duplicated")
    for source_commit, authority in expected.items():
        row = rows[source_commit]
        require(row["authority"] == authority, f"authority drift for {source_commit}")
        require((ROOT / authority).is_file(), f"missing incorporated authority: {authority}")
        require(bool(row["unresolved"]), f"unresolved gaps lost for {source_commit}")
        git("cat-file", "-e", f"{source_commit}^{{commit}}")
        linear = row["linear_commit"]
        git("cat-file", "-e", f"{linear}^{{commit}}")
        require(subprocess.run(
            ["git", "merge-base", "--is-ancestor", linear, "HEAD"], cwd=ROOT
        ).returncode == 0, f"linear incorporation commit is not an ancestor: {linear}")

    gate = data["current_gate"]
    require(gate["m4_open"] is False and "Brad's new in-game visual decision" in gate["required_next_evidence"],
            "M3 rejection gate weakened")
    review = (ROOT / "design" / "m3" / "BRAD-REVIEW-PACKAGE.md").read_text(encoding="utf-8")
    require("FAILED / REVISION REQUIRED" in review and "M4 district implementation authority: **CLOSED" in review,
            "Brad rejection authority missing")
    print("CONTINUATION LINEAGE: PASS (2 sibling receipts, M3 rejection authoritative, M4 closed)")


if __name__ == "__main__":
    main()
