#!/usr/bin/env python3
"""Validate Copperline's recurring human archive authority and receipt boundary."""
from __future__ import annotations

import json
from pathlib import Path

ROOT = Path(__file__).resolve().parents[1]
MACHINE = ROOT / "design/handoff/COPPERLINE-COMMUNITY-ARCHIVE-STANDARD.json"
HUMAN = ROOT / "design/handoff/COPPERLINE-COMMUNITY-ARCHIVE-STANDARD.md"


def require(condition: bool, message: str) -> None:
    if not condition:
        raise AssertionError(message)


def main() -> None:
    data = json.loads(MACHINE.read_text(encoding="utf-8"))
    require(data["schema_version"] == "1.0.0-copperline-community-archive"
            and data["status"] == "binding_cross_phase_open_ended_content_authority",
            "Copperline authority identity drift")
    require(data["m4_authority"] == "closed" and data["brad_visual_approval"] is None,
            "Copperline authority opens M4 or invents approval")
    ladder = data["c01_ladder_boundary"]
    require(ladder["exact_earned_belief"]
                == "mkept was a real person who deliberately preserved a damaged server"
            and ladder["later_case_truths_may_not_be_revealed_early"] is True
            and ladder["locked_ambiguities_preserved"] is True,
            "C01 or revelation-ladder boundary drift")
    require({row["area"] for row in data["information_architecture"]} == {
        "hosting directory", "accounts", "support desk", "community archive",
        "release archive", "reactive history"}, "Copperline information architecture incomplete")
    density = data["content_density_standard"]
    require("one part for at least three parts texture" in density["planning_ratio"]
            and "not a prose quota" in density["anti_padding_rule"],
            "clue/texture credibility or anti-padding guard weakened")
    identities = {row["identity"] for row in data["user_voice_map"]}
    require({"mkept", "Ash", "Rook", "Wren", "server regulars", "moderators",
             "support staff", "other fictional users"} == identities,
            "Copperline user/voice map incomplete")
    require(len(data["time_layers"]) == 6
            and data["provenance_rules"]["source_touch_receipts_never_gate_correct_answers"] is True
            and all(data["response_accessibility_and_recovery"].values()),
            "time, provenance, accessibility, or recovery rules weakened")
    forbidden = " | ".join(data["forbidden_presentations"])
    for phrase in ("purple", "fake ARG", "numbered", "every broken link", "every line",
                   "ancient", "C01"):
        require(phrase in forbidden, f"Copperline forbidden presentation lost: {phrase}")
    receipt = data["offline_vs_live_receipts"]
    require(receipt["production_mutation_now"] is False
            and "production deployment" in receipt["not_proven_by_offline_work"]
            and "Brad approval" in receipt["future_required_receipts"],
            "Copperline offline/live receipt boundary weakened")
    human = HUMAN.read_text(encoding="utf-8")
    for phrase in ("BINDING CROSS-PHASE AUTHORITY", "three parts lived texture",
                   "not a writing quota", "mkept was a real person", "Production remains untouched"):
        require(phrase in human, f"human Copperline authority missing: {phrase}")
    print("COPPERLINE ARCHIVE: PASS (lived-in density, C01 ladder, provenance, accessibility, honest receipts)")


if __name__ == "__main__":
    main()
