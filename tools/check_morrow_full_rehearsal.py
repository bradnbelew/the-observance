#!/usr/bin/env python3
"""Validate the retained full G01-G15 local rehearsal receipt."""
from __future__ import annotations

import json
import re
import sys
from pathlib import Path
from typing import Any

from run_morrow_full_rehearsal import build_receipt, canonical, sha

ROOT = Path(__file__).resolve().parents[1]
RECEIPT = ROOT / "morrow" / "rehearsal" / "full-spine" / "latest.json"


def require(condition: bool, message: str) -> None:
    if not condition:
        raise AssertionError(message)


def main() -> int:
    try:
        require(RECEIPT.is_file(), f"missing receipt: {RECEIPT}")
        retained = json.loads(RECEIPT.read_text(encoding="utf-8"))
        require(retained == build_receipt(), "full-spine receipt is not deterministic")
        require(retained["status"] == "local_full_spine_contract_not_playable", "receipt overclaims playability")
        require(retained["production_enablement"] == "blocked", "production must remain blocked")
        require(all(value is False for value in retained["boundaries"].values()), "receipt crossed a live boundary")
        require([row["gate"] for row in retained["gate_receipts"]] == [f"G{number:02d}" for number in range(1, 16)], "gate order drifted")
        previous = "0" * 64
        for row in retained["gate_receipts"]:
            require(row["previous_receipt_sha256"] == previous, f"hash chain broke before {row['gate']}")
            require(row["payload_sha256"] == sha(row["payload"]), f"payload hash drifted for {row['gate']}")
            core = {key: row[key] for key in ["sequence", "gate", "event_key", "idempotency_key", "payload_sha256", "previous_receipt_sha256"]}
            require(row["receipt_sha256"] == sha(core), f"receipt hash drifted for {row['gate']}")
            require(len(row["payload"]["evidence"]) >= 3, f"{row['gate']} lacks clue vectors")
            require(row["payload"]["accessible_equivalent"], f"{row['gate']} lacks accessibility equivalent")
            previous = row["receipt_sha256"]
        require(retained["journal_head"] == previous, "journal head drifted")
        require(len(retained["media_required"]) == 12, "media count drifted")
        require(all(asset["required_hash_before_release"] for asset in retained["media_required"]), "media hash boundary weakened")
        require(retained["director_controls_enabled"] is False, "director controls should remain disabled")
        config = (ROOT / "plugin" / "src" / "main" / "resources" / "config.yml").read_text(encoding="utf-8")
        require(re.search(r"morrow-reboot:\s*\n\s+enabled: false", config), "morrow-reboot.enabled must remain false")
    except Exception as failure:
        print(f"MORROW FULL SPINE LOCAL CHECK: FAIL: {failure}", file=sys.stderr)
        return 1
    print("MORROW FULL SPINE LOCAL CHECK: PASS gates=15 media=12 production=blocked playable=false")
    return 0


if __name__ == "__main__":
    raise SystemExit(main())
