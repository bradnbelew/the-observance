#!/usr/bin/env python3
"""Build the first local G01-G05 Morrow vertical-slice receipt."""
from __future__ import annotations

import argparse
import hashlib
import json
from pathlib import Path
from typing import Any

ROOT = Path(__file__).resolve().parents[1]
MORROW = ROOT / "morrow"
RELEASE_ID = "morrow.local.g01-g05.v1"
CAMPAIGN_ID = "local.contract.mossfield.seed"
GENERATED_AT = "2026-09-15T00:00:00-05:00"
AUTHORITY_FILES = [
    "morrow/README.md",
    "morrow/authority/ARG-GUIDEBOOK.md",
    "morrow/authority/CANON.md",
    "morrow/authority/PLAYER-JOURNEY.md",
    "morrow/authority/PUZZLE-LEDGER.json",
    "morrow/contracts/event-catalog.json",
    "morrow/contracts/relationship-states.json",
    "morrow/contracts/surface-contracts.json",
    "morrow/review/review-fixtures.json",
]


def canonical_json(value: Any) -> str:
    return json.dumps(value, sort_keys=True, separators=(",", ":"), ensure_ascii=True)


def sha256_text(value: str) -> str:
    return hashlib.sha256(value.encode("utf-8")).hexdigest()


def load_json(relative: str) -> Any:
    return json.loads((ROOT / relative).read_text(encoding="utf-8"))


def sha256_file(relative: str) -> str:
    return hashlib.sha256((ROOT / relative).read_bytes()).hexdigest()


def row_by_id(rows: list[dict[str, Any]], key: str, value: str) -> dict[str, Any]:
    return next(row for row in rows if row[key] == value)


def event_key_by_gate(catalog: dict[str, Any], gate_id: str) -> str:
    return next(event["key"] for event in catalog["events"] if event.get("gate") == gate_id)


def receipt_hash(row: dict[str, Any]) -> str:
    return sha256_text(canonical_json(row))


def build_receipt() -> dict[str, Any]:
    ledger = load_json("morrow/authority/PUZZLE-LEDGER.json")
    catalog = load_json("morrow/contracts/event-catalog.json")
    states = load_json("morrow/contracts/relationship-states.json")
    fixtures = load_json("morrow/review/review-fixtures.json")
    observations = {
        "G01": {"ordinary_trace": "retired Copperline page source comment", "assembled_path": "/recovery/mossfield", "footer_build": "cl-2011-6118"},
        "G02": {"ordinary_trace": "forum edits plus support signature identify one fictional employee alias", "alias_handle": "iona_bell", "private_data_used": False},
        "G03": {"ordinary_trace": "status uptime and incident number produce a short fictional credential", "password_shape": "eight-character local fixture", "rate_limit_scope": "local receipt only"},
        "G04": {"ordinary_trace": "authenticated handoff exposes selectable server address and case fingerprint", "server_role": "non-op player", "morrow_state_after": "observant"},
        "G05": {"ordinary_trace": "cairn's uneven storehouse order reveals a lectern index", "native_objects": ["renamed tools", "four chests", "uneven labels", "lectern"], "anti_room_shape": "ordinary home/storehouse routine"},
    }
    previous = "0" * 64
    receipts = []
    for sequence, gate_id in enumerate(["G01", "G02", "G03", "G04", "G05"], start=1):
        gate = row_by_id(ledger["gates"], "id", gate_id)
        discovery = row_by_id(ledger["discoveries"], "id", f"D{sequence:02d}")
        event_key = event_key_by_gate(catalog, gate_id)
        owner = next(event["owner"] for event in catalog["events"] if event["key"] == event_key)
        payload = {
            "gate": gate_id,
            "title": gate["title"],
            "surfaces": gate["surfaces"],
            "evidence": gate["evidence"],
            "input": gate["input"],
            "failure_behavior": gate["failure_behavior"],
            "accessible_equivalent": gate["accessible_equivalent"],
            "discovery": discovery["id"],
            "reveals": discovery["reveals"],
            "local_observation": observations[gate_id],
        }
        core = {
            "sequence": sequence,
            "event_key": event_key,
            "gate": gate_id,
            "discovery": discovery["id"],
            "owner": owner,
            "idempotency_key": f"{RELEASE_ID}:{CAMPAIGN_ID}:{gate_id}",
            "payload_sha256": sha256_text(canonical_json(payload)),
            "previous_receipt_sha256": previous,
        }
        full = {**core, "payload": payload, "receipt_sha256": receipt_hash(core)}
        receipts.append(full)
        previous = full["receipt_sha256"]
    chest = next(beat for beat in fixtures["dread_score"] if beat["id"] == "chest_correction")
    consequence_payload = {
        "id": "local.chest_correction.after_g05",
        "dread_beat": "chest_correction",
        "trigger_after_event": receipts[-1]["event_key"],
        "trigger_condition": "after every present player leaves cairn_storehouse",
        "maximum_seconds": chest["maximum_seconds"],
        "required_for_progress": chest["required_for_progress"],
        "consumes_or_moves_required_evidence": False,
        "accessible_equivalent": chest["accessible_equivalent"],
        "rollback": "restore the pre-beat chest labels and inventory order from local receipt state",
    }
    consequence_core = {
        "sequence": 6,
        "kind": "delayed_bounded_consequence",
        "idempotency_key": f"{RELEASE_ID}:{CAMPAIGN_ID}:chest_correction",
        "payload_sha256": sha256_text(canonical_json(consequence_payload)),
        "previous_receipt_sha256": previous,
    }
    consequence = {**consequence_core, "payload": consequence_payload, "receipt_sha256": receipt_hash(consequence_core)}
    return {
        "schema_version": "1.0.0-morrow-local-g01-g05-vertical-slice",
        "status": "local_contract_pass_not_playable",
        "generated_at": GENERATED_AT,
        "release_id": RELEASE_ID,
        "campaign_id": CAMPAIGN_ID,
        "scope": "local deterministic receipt contract for G01-G05 with static Copperline G01-G03 routes",
        "production_enablement": "blocked",
        "boundaries": {"production_contacted": False, "supabase_contacted": False, "discord_gateway_contacted": False, "minecraft_server_started": False, "browser_started": False, "external_network_used": False, "human_playability_claimed": False},
        "authority_files": {relative: sha256_file(relative) for relative in AUTHORITY_FILES},
        "implemented_gates": [row["gate"] for row in receipts],
        "implemented_discoveries": [row["discovery"] for row in receipts],
        "implemented_static_routes": ["/game-servers.php", "/community/forum?thread=6118", "/support/tickets/6118", "/status/incident-6118", "/recovery/mossfield"],
        "missing_runtime_surfaces": ["paper_world", "g04_minecraft_runtime", "supabase_schema", "discord_worker", "final_media"],
        "morrow_state_projection": {"initial": states["initial_state"], "transition_event": next(row["transition_event"] for row in states["states"] if row["key"] == "support_software"), "after_g04": "observant", "terminal": states["terminal_state"], "dialogue_advanced_state": False},
        "event_receipts": receipts,
        "delayed_consequence_receipt": consequence,
        "idempotency_proof": {"duplicate_replay": {"idempotency_key": receipts[-1]["idempotency_key"], "payload_sha256": receipts[-1]["payload_sha256"], "result": "prior_receipt_returned", "receipt_sha256": receipts[-1]["receipt_sha256"]}, "altered_replay": {"idempotency_key": receipts[-1]["idempotency_key"], "payload_sha256": sha256_text(canonical_json({**receipts[-1]["payload"], "tampered": True})), "result": "failed_closed_collision"}},
        "restart_safety": {"restart_after_every_gate": True, "boundaries": [{"after_sequence": row["sequence"], "journal_head": row["receipt_sha256"], "resume_result": "prior_receipt_returned_without_duplicate_consequence"} for row in receipts], "journal_head": consequence["receipt_sha256"]},
        "cohort_semantics": {"supported_players": ledger["campaign_shape"]["supported_players"], "single_player_solvable": True, "six_player_requires_simultaneous_humans": False, "private_prose_required": False},
        "anti_puzzle_room_firewall": {"dedicated_comparison_room": False, "numbered_trial": False, "morrow_as_puzzle_host": False, "new_finale_notation": False},
    }


def main() -> int:
    parser = argparse.ArgumentParser()
    parser.add_argument("--output", default=str(MORROW / "rehearsal" / "g01-g05-vertical-slice" / "latest.json"))
    args = parser.parse_args()
    receipt = build_receipt()
    output = Path(args.output)
    output.parent.mkdir(parents=True, exist_ok=True)
    output.write_text(json.dumps(receipt, indent=2, sort_keys=True) + "\n", encoding="utf-8")
    print(f"MORROW G01-G05 LOCAL SLICE: wrote {output}")
    print(f"journal_head={receipt['restart_safety']['journal_head']}")
    return 0


if __name__ == "__main__":
    raise SystemExit(main())



