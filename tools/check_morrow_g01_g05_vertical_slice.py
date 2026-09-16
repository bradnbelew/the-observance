#!/usr/bin/env python3
"""Validate the first local Morrow G01-G05 vertical-slice receipt."""
from __future__ import annotations

import json
import re
import sys
from pathlib import Path
from typing import Any

from run_morrow_g01_g05_vertical_slice import build_receipt, canonical_json, sha256_text

ROOT = Path(__file__).resolve().parents[1]
RECEIPT = ROOT / "morrow" / "rehearsal" / "g01-g05-vertical-slice" / "latest.json"


def require(condition: bool, message: str) -> None:
    if not condition:
        raise AssertionError(message)


def load_json(relative: str) -> Any:
    return json.loads((ROOT / relative).read_text(encoding="utf-8"))


def validate_hash_chain(receipt: dict[str, Any]) -> None:
    previous = "0" * 64
    for row in receipt["event_receipts"]:
        require(row["payload_sha256"] == sha256_text(canonical_json(row["payload"])), f"payload hash drifted for {row['gate']}")
        core = {key: row[key] for key in ["sequence", "event_key", "gate", "discovery", "owner", "idempotency_key", "payload_sha256", "previous_receipt_sha256"]}
        require(row["previous_receipt_sha256"] == previous, f"broken previous hash before {row['gate']}")
        require(row["receipt_sha256"] == sha256_text(canonical_json(core)), f"receipt hash drifted for {row['gate']}")
        previous = row["receipt_sha256"]
    consequence = receipt["delayed_consequence_receipt"]
    require(consequence["previous_receipt_sha256"] == previous, "delayed consequence is not chained after G05")
    require(consequence["payload_sha256"] == sha256_text(canonical_json(consequence["payload"])), "delayed consequence payload hash drifted")
    core = {key: consequence[key] for key in ["sequence", "kind", "idempotency_key", "payload_sha256", "previous_receipt_sha256"]}
    require(consequence["receipt_sha256"] == sha256_text(canonical_json(core)), "delayed consequence hash drifted")
    require(receipt["restart_safety"]["journal_head"] == consequence["receipt_sha256"], "journal head drifted")


def main() -> int:
    try:
        require(RECEIPT.is_file(), f"missing retained receipt: {RECEIPT}")
        retained = json.loads(RECEIPT.read_text(encoding="utf-8"))
        require(retained == build_receipt(), "retained receipt is not deterministic; rerun the local slice runner")
        ledger = load_json("morrow/authority/PUZZLE-LEDGER.json")
        catalog = load_json("morrow/contracts/event-catalog.json")
        states = load_json("morrow/contracts/relationship-states.json")
        fixtures = load_json("morrow/review/review-fixtures.json")
        plugin_config = (ROOT / "plugin" / "src" / "main" / "resources" / "config.yml").read_text(encoding="utf-8")
        require(retained["status"] == "local_contract_pass_not_playable", "receipt overclaims runtime status")
        require(retained["production_enablement"] == "blocked", "production must remain blocked")
        require(all(value is False for value in retained["boundaries"].values()), "local slice crossed a live boundary")
        require(re.search(r"morrow-reboot:\s*\n\s+enabled: false", plugin_config), "morrow-reboot.enabled must remain false")
        expected_gates = [f"G{number:02d}" for number in range(1, 6)]
        expected_discoveries = [f"D{number:02d}" for number in range(1, 6)]
        require(retained["implemented_gates"] == expected_gates, "implemented gate list drifted")
        require(retained["implemented_discoveries"] == expected_discoveries, "implemented discovery list drifted")
        require("nextjs_routes" not in retained["missing_runtime_surfaces"], "G01-G03 static routes should no longer be listed missing")
        require(retained["implemented_static_routes"] == ["/game-servers.php", "/community/forum?thread=6118", "/support/tickets/6118", "/status/incident-6118", "/recovery/mossfield"], "static route list drifted")
        event_by_gate = {event["gate"]: event for event in catalog["events"] if event.get("gate")}
        ledger_gate_by_id = {gate["id"]: gate for gate in ledger["gates"]}
        ledger_discovery_by_id = {discovery["id"]: discovery for discovery in ledger["discoveries"]}
        for row in retained["event_receipts"]:
            gate = ledger_gate_by_id[row["gate"]]
            discovery = ledger_discovery_by_id[row["discovery"]]
            event = event_by_gate[row["gate"]]
            require(row["event_key"] == event["key"], f"wrong event key for {row['gate']}")
            require(row["owner"] == event["owner"], f"wrong owner for {row['gate']}")
            require(row["payload"]["evidence"] == gate["evidence"], f"evidence drifted for {row['gate']}")
            require(len(row["payload"]["evidence"]) >= 3, f"{row['gate']} has too few clue vectors")
            require(row["payload"]["accessible_equivalent"] == gate["accessible_equivalent"], f"accessibility drift for {row['gate']}")
            require(discovery["required"] is True and discovery["feeds_gate"] == row["gate"], f"{row['discovery']} discovery binding drifted")
        require(retained["event_receipts"][0]["owner"] == "copperline", "G01 must start on Copperline")
        require(retained["event_receipts"][3]["owner"] == "minecraft", "G04 must move into Minecraft")
        require(retained["event_receipts"][4]["payload"]["local_observation"]["anti_room_shape"] == "ordinary home/storehouse routine", "G05 slipped toward puzzle-room grammar")
        transition = retained["morrow_state_projection"]
        require(transition["initial"] == states["initial_state"] == "support_software", "initial state drifted")
        require(transition["transition_event"] == "morrow.gate.g04_first_connection", "G04 transition event drifted")
        require(transition["after_g04"] == "observant" and transition["dialogue_advanced_state"] is False, "Morrow state projection drifted")
        validate_hash_chain(retained)
        consequence = retained["delayed_consequence_receipt"]["payload"]
        chest = next(beat for beat in fixtures["dread_score"] if beat["id"] == "chest_correction")
        require(consequence["trigger_after_event"] == "morrow.gate.g05_storehouse_trail", "consequence must follow G05")
        require(consequence["maximum_seconds"] == chest["maximum_seconds"] <= 1, "chest correction must stay bounded")
        require(consequence["required_for_progress"] is False and consequence["consumes_or_moves_required_evidence"] is False, "consequence overclaimed progress or consumed evidence")
        replay = retained["idempotency_proof"]
        require(replay["duplicate_replay"]["result"] == "prior_receipt_returned", "duplicate replay is not idempotent")
        require(replay["altered_replay"]["result"] == "failed_closed_collision", "altered replay must fail closed")
        require(retained["restart_safety"]["restart_after_every_gate"] is True and len(retained["restart_safety"]["boundaries"]) == 5, "restart proof drifted")
        require(retained["cohort_semantics"]["supported_players"] == "1-6" and retained["cohort_semantics"]["six_player_requires_simultaneous_humans"] is False, "cohort support drifted")
        require(not any(retained["anti_puzzle_room_firewall"].values()), "anti-puzzle-room firewall failed")
    except Exception as failure:
        print(f"MORROW G01-G05 LOCAL SLICE CHECK: FAIL: {failure}", file=sys.stderr)
        return 1
    print("MORROW G01-G05 LOCAL SLICE CHECK: PASS gates=G01-G05 discoveries=D01-D05 static_routes=G01-G03 receipt_chain=6 production=blocked playable=false")
    return 0


if __name__ == "__main__":
    raise SystemExit(main())
