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


def canonical_bytes(value: object) -> bytes:
    return (json.dumps(value, indent=2, ensure_ascii=False, sort_keys=True) + "\n").encode("utf-8")


def main() -> None:
    index = json.loads((SOURCE / "campaign.json").read_text(encoding="utf-8"))
    cases = []
    for row in index["phases"]:
        if row["status"] != "authored_candidate":
            raise RuntimeError(f"refusing incomplete projection: {row['id']}={row['status']}")
        cases.append(json.loads((SOURCE / row["file"]).read_text(encoding="utf-8")))
    projection = {
        "schema_version": "1.0.0-runtime-campaign-projection",
        "source_schema_version": index["schema_version"],
        "closed_mechanism_taxonomy": False,
        "observation_receipts_gate_answers": False,
        "phases": [case["phase"] for case in cases],
        "cases": cases,
    }
    payload = canonical_bytes(projection)
    digest = hashlib.sha256(payload).hexdigest()
    for target in TARGETS:
        target.parent.mkdir(parents=True, exist_ok=True)
        target.write_bytes(payload)
    manifest = {
        "schema_version": "1.0.0-campaign-projection-manifest",
        "source_files": ["campaign.json"] + [row["file"] for row in index["phases"]],
        "projection_sha256": digest,
        "projection_bytes": len(payload),
        "targets": [str(path.relative_to(ROOT)).replace("\\", "/") for path in TARGETS],
        "phase_count": len(cases),
        "production_deployed": False,
    }
    MANIFEST.write_bytes(canonical_bytes(manifest))
    print(f"P5-P12 projection written: phases={len(cases)} bytes={len(payload)} sha256={digest}")


if __name__ == "__main__":
    main()
