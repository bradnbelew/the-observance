#!/usr/bin/env python3
"""Validate local Morrow reboot implementation contracts."""
from __future__ import annotations

import json
import re
import sys
from pathlib import Path
from typing import Any

ROOT = Path(__file__).resolve().parents[1]
MORROW = ROOT / "morrow"


def load(relative: str) -> Any:
    return json.loads((MORROW / relative).read_text(encoding="utf-8"))


def require(condition: bool, message: str) -> None:
    if not condition:
        raise AssertionError(message)


def main() -> int:
    try:
        ledger = load("authority/PUZZLE-LEDGER.json")
        clue_objects = load("contracts/clue-objects.json")
        discord = load("contracts/discord-fragment-flow.json")
        director = load("contracts/director-command-schema.json")
        gate_ids = {gate["id"] for gate in ledger["gates"]}
        object_gate_ids = {gate for location in clue_objects["locations"] for gate in location["gates"]}
        require(object_gate_ids <= gate_ids, "clue object manifest references unknown gates")
        require({"G04", "G05", "G06", "G07", "G08", "G09", "G11", "G12", "G14", "G15"} <= object_gate_ids, "Minecraft manifest lacks required in-world gates")
        require(clue_objects["global_rules"]["required_objects_have_text_equivalent"] is True, "Minecraft text equivalent weakened")
        require(clue_objects["global_rules"]["scare_can_destroy_required_evidence"] is False, "scare evidence safety weakened")
        require(discord["gate"] == "G10" and discord["private_prose_required"] is False, "Discord G10 prose boundary drifted")
        require(all(fragment["references_only_arg_actions"] for fragment in discord["fragments"]), "Discord fragment references unsafe data")
        require(not any(discord["safety"].values()), "Discord safety flags must all be false")
        require(director["controls_enabled"] is False and director["production_mutations_allowed"] is False, "director controls should remain disabled")
        require(any(command["type"] == "arm_finale" for command in director["commands"]), "director schema lacks finale arm")
        require("arbitrary_command" in director["forbidden"] and "invent_dialogue" in director["forbidden"], "director forbidden list weakened")
        schema = (MORROW / "db" / "schema-proposal-g01-g15.sql").read_text(encoding="utf-8").lower()
        for required in ("gate_definitions", "event_projections", "media_assets", "director_actions", "morrow.gate.g15_shutdown_complete"):
            require(required in schema, f"schema proposal lacks {required}")
        config = (ROOT / "plugin" / "src" / "main" / "resources" / "config.yml").read_text(encoding="utf-8")
        require(re.search(r"morrow-reboot:\s*\n\s+enabled: false", config), "morrow reboot must remain disabled")
    except Exception as failure:
        print(f"MORROW LOCAL CONTRACTS CHECK: FAIL: {failure}", file=sys.stderr)
        return 1
    print("MORROW LOCAL CONTRACTS CHECK: PASS minecraft=allowlisted discord=G10 director=disabled schema=proposal")
    return 0


if __name__ == "__main__":
    raise SystemExit(main())
