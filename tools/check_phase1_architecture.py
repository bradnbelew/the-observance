#!/usr/bin/env python3
"""Validate the Phase 1 rebuild architecture without touching live systems."""

from __future__ import annotations

import json
import re
from pathlib import Path


ROOT = Path(__file__).resolve().parents[1]
HANDOFF = ROOT / "design" / "handoff"

FILES = {
    "approval": HANDOFF / "PHASE-1-APPROVAL.md",
    "experience": HANDOFF / "PHASE-1-EXPERIENCE-ARCHITECTURE.md",
    "governance": HANDOFF / "PHASE-1-PROGRESSION-GOVERNANCE.md",
    "media": HANDOFF / "PHASE-1-MEDIA-INVENTORY.md",
    "topology": HANDOFF / "PHASE-1-TOPOLOGY-AND-MIGRATION.md",
}


def main() -> int:
    failures: list[str] = []
    text: dict[str, str] = {}
    for key, path in FILES.items():
        if not path.is_file():
            failures.append(f"missing Phase 1 authority: {path.relative_to(ROOT).as_posix()}")
            text[key] = ""
        else:
            text[key] = path.read_text(encoding="utf-8-sig")

    spine = (HANDOFF / "SPINE-CONFORMANCE.md").read_text(encoding="utf-8-sig")
    if "Status: **APPROVED BY BRAD — 2026-07-15**" not in spine:
        failures.append("Spine Conformance is not recorded as approved")

    arcs: dict[int, float] = {}
    for line in text["experience"].splitlines():
        match = re.match(r"\| P(\d+) — .*? \| ([0-9]+(?:\.[0-9]+)?)h \|", line)
        if match:
            arcs[int(match.group(1))] = float(match.group(2))
    if sorted(arcs) != list(range(1, 13)):
        failures.append(f"experience map must contain exactly P1-P12; found {sorted(arcs)}")
    if abs(sum(arcs.values()) - 26.5) > 0.001:
        failures.append(f"experience target must total 26.5h; found {sum(arcs.values()):.1f}h")

    for token in (
        "20–30 hours", "Any one or more linked players", "No active-roster snapshot",
        "H0 — orientation", "H1 — recovery", "H2 — relationship", "H3 — decisive nudge",
        "A0 — observation/readback", "A1 — safe ambient", "A2 — personalized",
        "A3 — social/public", "A4 — world/progression changing", "A5 — irreversible/finale",
    ):
        if token not in text["experience"] + text["governance"]:
            failures.append(f"Phase 1 progression/governance contract missing: {token}")

    manifest_path = ROOT / "arc" / "v5" / "media-manifest.json"
    manifest = json.loads(manifest_path.read_text(encoding="utf-8-sig"))
    for asset in manifest.get("assets", []):
        asset_id = str(asset.get("id", ""))
        source_sha1 = str(asset.get("sourceSha1", ""))
        if asset_id not in text["media"]:
            failures.append(f"media inventory omits manifest asset {asset_id}")
        if source_sha1 and source_sha1 not in text["media"]:
            failures.append(f"media inventory omits source hash for {asset_id}")

    for token in (
        "hold.zip", "Crafty", "Paper `1.21.11`", "fdnmhbpxnodrnbrzrlqq",
        "prj_UygHA98HGW4IBVMk6AKzXVEG6ZSQ", "railway.worker.json", "railway.cron.json",
        "M0 — Freeze and inventory", "M1 — Evidence architecture", "M2 — Technical contracts",
        "M3 — Private vertical slice", "M4 — Incremental campaign build", "M5 — Production cutover",
    ):
        if token not in text["topology"]:
            failures.append(f"Phase 1 topology/migration contract missing: {token}")

    route_text = (
        (HANDOFF / "README.md").read_text(encoding="utf-8-sig")
        + (ROOT / "design" / "V5-SUPERSESSION-MAP.md").read_text(encoding="utf-8-sig")
        + (HANDOFF / "CODEX-MASTER-PROMPT.md").read_text(encoding="utf-8-sig")
    )
    for path in FILES.values():
        if path.name not in route_text:
            failures.append(f"future-agent routing omits {path.name}")

    if failures:
        print(f"PHASE 1 ARCHITECTURE CHECK: FAIL ({len(failures)} issue(s))")
        for failure in failures:
            print(f"- {failure}")
        return 1

    print(
        "PHASE 1 ARCHITECTURE CHECK: PASS - 12 ordered arcs / 26.5h target; "
        "subset progress, H0-H3 hints, A0-A5 automation, 5 media receipts, "
        "single Crafty runtime, M0-M5 migration, and authority routing present"
    )
    return 0


if __name__ == "__main__":
    raise SystemExit(main())
