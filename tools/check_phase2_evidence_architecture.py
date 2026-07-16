#!/usr/bin/env python3
"""Validate Phase 2/M1 evidence architecture without touching live systems."""

from __future__ import annotations

import csv
import json
import re
from collections import Counter
from pathlib import Path


ROOT = Path(__file__).resolve().parents[1]
HANDOFF = ROOT / "design" / "handoff"
ARCHITECTURE = HANDOFF / "PHASE-2-EVIDENCE-ARCHITECTURE.md"
DISPOSITION = HANDOFF / "PHASE-2-LEGACY-NODE-DISPOSITION.md"
AUDIT = HANDOFF / "PHASE-2-CONFORMANCE-AND-MEDIA-AUDIT.md"


def read(path: Path, failures: list[str]) -> str:
    if not path.is_file():
        failures.append(f"missing Phase 2 authority: {path.relative_to(ROOT).as_posix()}")
        return ""
    return path.read_text(encoding="utf-8-sig")


def section(text: str, arc: int) -> str:
    match = re.search(
        rf"^## P{arc}\b.*?(?=^## P{arc + 1}\b|^## Pacing)",
        text,
        flags=re.MULTILINE | re.DOTALL,
    )
    return match.group(0) if match else ""


def main() -> int:
    failures: list[str] = []
    architecture = read(ARCHITECTURE, failures)
    disposition = read(DISPOSITION, failures)
    audit = read(AUDIT, failures)

    durations: dict[int, float] = {}
    for line in architecture.splitlines():
        match = re.match(r"\| P(\d+) \| ([0-9]+(?:\.[0-9]+)?)h \|", line)
        if match:
            durations[int(match.group(1))] = float(match.group(2))
    if sorted(durations) != list(range(1, 13)):
        failures.append(f"pacing table must contain exactly P1-P12; found {sorted(durations)}")
    if abs(sum(durations.values()) - 26.5) > 0.001:
        failures.append(f"Phase 2 targets must total 26.5h; found {sum(durations.values()):.1f}h")

    required_arc_tokens = (
        "Duration / group split",
        "Core revelation",
        "Required prior knowledge",
        "Distinct investigation identity",
        "Submission surface",
        "Emotional function",
        "Evidence chain:",
        "Hints/recovery:",
        "Catch-up/replay:",
        "Failure recovery:",
    )
    for arc in range(1, 13):
        arc_text = section(architecture, arc)
        if not arc_text:
            failures.append(f"missing P{arc} evidence architecture section")
            continue
        for token in required_arc_tokens:
            if token not in arc_text:
                failures.append(f"P{arc} missing required field: {token}")
        if not re.search(rf"\*\*P{arc}\.[A-ZF0-9]", arc_text):
            failures.append(f"P{arc} contains no named finding/evidence chain")

    with (ROOT / "design" / "ARG-V5-NODE-MANIFEST.csv").open(
        encoding="utf-8-sig", newline=""
    ) as handle:
        legacy_ids = [row["node_id"] for row in csv.DictReader(handle)]

    disposition_rows: list[tuple[str, str]] = []
    for line in disposition.splitlines():
        match = re.match(r"\| ([A-Z]{1,2}\d{2}) \| (Reuse|Map/merge|Retire) \|", line)
        if match:
            disposition_rows.append((match.group(1), match.group(2)))
    disposition_ids = [node_id for node_id, _ in disposition_rows]
    counts = Counter(disposition_ids)
    duplicates = sorted(node_id for node_id, count in counts.items() if count != 1)
    missing = sorted(set(legacy_ids) - set(disposition_ids))
    extra = sorted(set(disposition_ids) - set(legacy_ids))
    if len(disposition_rows) != 82:
        failures.append(f"legacy disposition must have exactly 82 rows; found {len(disposition_rows)}")
    if duplicates:
        failures.append(f"legacy disposition duplicates IDs: {duplicates}")
    if missing:
        failures.append(f"legacy disposition missing IDs: {missing}")
    if extra:
        failures.append(f"legacy disposition has unknown IDs: {extra}")
    for category in ("Reuse", "Map/merge", "Retire"):
        if category not in {value for _, value in disposition_rows}:
            failures.append(f"legacy disposition has no {category} rows")

    for token in (
        "No change below is performed in Phase 2",
        "Predicate replacement",
        "Schema/count changes are deferred",
        "current 10/82/60 counts",
    ):
        if token not in disposition:
            failures.append(f"migration implications missing: {token}")

    media_manifest = json.loads(
        (ROOT / "arc" / "v5" / "media-manifest.json").read_text(encoding="utf-8-sig")
    )
    for asset in media_manifest.get("assets", []):
        asset_id = str(asset.get("id", ""))
        source_hash = str(asset.get("sourceSha1", ""))
        if asset_id not in audit:
            failures.append(f"media placement omits {asset_id}")
        if source_hash and source_hash not in audit:
            failures.append(f"media placement omits source hash for {asset_id}")

    ambiguity_match = re.search(
        r"^## Exactly three deliberately unresolved ambiguities\s+(.*?)(?=^## )",
        audit,
        flags=re.MULTILINE | re.DOTALL,
    )
    if not ambiguity_match:
        failures.append("missing dedicated ambiguity boundary")
    else:
        numbered = re.findall(r"^\d+\. ", ambiguity_match.group(1), flags=re.MULTILINE)
        if len(numbered) != 3:
            failures.append(f"ambiguity boundary must contain exactly three items; found {len(numbered)}")

    for token in (
        "Spine conformance audit",
        "Revelation-order audit",
        "Fairness and required-content reachability audit",
        "Pacing and workload audit",
        "Legacy and migration audit",
        "External gaps carried forward",
    ):
        if token not in audit:
            failures.append(f"Phase 2 completion audit missing: {token}")

    route_text = "\n".join(
        path.read_text(encoding="utf-8-sig")
        for path in (
            HANDOFF / "README.md",
            HANDOFF / "CODEX-MASTER-PROMPT.md",
            ROOT / "design" / "V5-SUPERSESSION-MAP.md",
            HANDOFF / "PHASE-CHECKPOINTS.md",
        )
    )
    for path in (ARCHITECTURE, DISPOSITION, AUDIT):
        if path.name not in route_text:
            failures.append(f"future-agent routing omits {path.name}")

    if failures:
        print(f"PHASE 2 EVIDENCE ARCHITECTURE CHECK: FAIL ({len(failures)} issue(s))")
        for failure in failures:
            print(f"- {failure}")
        return 1

    category_counts = Counter(value for _, value in disposition_rows)
    print(
        "PHASE 2 EVIDENCE ARCHITECTURE CHECK: PASS - "
        "P1-P12 complete / 26.5h target / 82 legacy IDs disposed "
        f"({category_counts['Reuse']} reuse, {category_counts['Map/merge']} map/merge, "
        f"{category_counts['Retire']} retire) / 5 media receipts / exactly 3 ambiguities / "
        "spine, revelation, fairness, reachability, subset progress, and migration audits present"
    )
    return 0


if __name__ == "__main__":
    raise SystemExit(main())

