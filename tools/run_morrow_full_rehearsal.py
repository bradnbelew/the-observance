#!/usr/bin/env python3
"""Build a retained full G01-G15 local rehearsal receipt."""
from __future__ import annotations

import argparse
import hashlib
import json
from pathlib import Path
from typing import Any

ROOT = Path(__file__).resolve().parents[1]
MORROW = ROOT / "morrow"
RELEASE_ID = "morrow.local.full-spine.v1"
CAMPAIGN_ID = "local.contract.mossfield.full"


def load_json(relative: str) -> Any:
    return json.loads((ROOT / relative).read_text(encoding="utf-8"))


def canonical(value: Any) -> str:
    return json.dumps(value, sort_keys=True, separators=(",", ":"), ensure_ascii=True)


def sha(value: Any) -> str:
    return hashlib.sha256(canonical(value).encode("utf-8")).hexdigest()


def build_receipt() -> dict[str, Any]:
    ledger = load_json("morrow/authority/PUZZLE-LEDGER.json")
    catalog = load_json("morrow/contracts/event-catalog.json")
    states = load_json("morrow/contracts/relationship-states.json")
    surfaces = load_json("morrow/contracts/surface-contracts.json")
    media = load_json("morrow/contracts/media-catalog.json")
    fixtures = load_json("morrow/review/review-fixtures.json")
    previous = "0" * 64
    gate_receipts = []
    for sequence, gate in enumerate(ledger["gates"], start=1):
        event = next(row for row in catalog["events"] if row.get("gate") == gate["id"])
        discovery = next((row for row in ledger["discoveries"] if row.get("feeds_gate") == gate["id"]), None)
        location = next((row for row in fixtures["minecraft_locations"] if gate["id"] in row["gates"]), None)
        media_keys = [asset["key"] for asset in media["assets"] if event["key"] in asset["prerequisite_events"] or gate["id"].lower() in asset["key"]]
        payload = {
            "gate": gate["id"],
            "title": gate["title"],
            "act": gate["act"],
            "event_key": event["key"],
            "owner": event["owner"],
            "projects_to": event["projects_to"],
            "surfaces": gate["surfaces"],
            "mechanics": gate["mechanics"],
            "evidence": gate["evidence"],
            "input": gate["input"],
            "failure_behavior": gate["failure_behavior"],
            "accessible_equivalent": gate["accessible_equivalent"],
            "discovery": None if discovery is None else discovery["id"],
            "location": None if location is None else location["id"],
            "media_keys": media_keys,
            "production_mutation": False,
        }
        core = {
            "sequence": sequence,
            "gate": gate["id"],
            "event_key": event["key"],
            "idempotency_key": f"{RELEASE_ID}:{CAMPAIGN_ID}:{gate['id']}",
            "payload_sha256": sha(payload),
            "previous_receipt_sha256": previous,
        }
        receipt_sha = sha(core)
        previous = receipt_sha
        gate_receipts.append({**core, "payload": payload, "receipt_sha256": receipt_sha})
    receipt = {
        "schema_version": "1.0.0-morrow-full-spine-local-rehearsal",
        "status": "local_full_spine_contract_not_playable",
        "release_id": RELEASE_ID,
        "campaign_id": CAMPAIGN_ID,
        "production_enablement": "blocked",
        "boundaries": {
            "production_contacted": False,
            "supabase_contacted": False,
            "discord_gateway_contacted": False,
            "minecraft_server_started": False,
            "browser_required": False,
            "human_playability_claimed": False,
        },
        "campaign_shape": ledger["campaign_shape"],
        "morrow_state_order": [row["key"] for row in states["states"]],
        "surface_contracts": list(surfaces["surfaces"].keys()),
        "gate_receipts": gate_receipts,
        "journal_head": previous,
        "media_required": [{"key": asset["key"], "status": asset["status"], "required_hash_before_release": asset["required_hash_before_release"]} for asset in media["assets"]],
        "director_controls_enabled": fixtures["director_fixture"]["controls_enabled"],
        "production_mutations_allowed": fixtures["production_mutations_allowed"],
    }
    return {**receipt, "receipt_sha256": sha(receipt)}


def main() -> int:
    parser = argparse.ArgumentParser()
    parser.add_argument("--output", default=str(MORROW / "rehearsal" / "full-spine" / "latest.json"))
    args = parser.parse_args()
    receipt = build_receipt()
    output = Path(args.output)
    output.parent.mkdir(parents=True, exist_ok=True)
    output.write_text(json.dumps(receipt, indent=2, sort_keys=True) + "\n", encoding="utf-8")
    print(f"MORROW FULL SPINE LOCAL REHEARSAL: wrote {output}")
    print(f"journal_head={receipt['journal_head']}")
    return 0


if __name__ == "__main__":
    raise SystemExit(main())
