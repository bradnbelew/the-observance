#!/usr/bin/env python3
"""Validate Brad's superseding private automated staging authority."""
from __future__ import annotations

import json
from pathlib import Path

ROOT = Path(__file__).resolve().parents[1]
MACHINE = ROOT / "design/handoff/OVERNIGHT-PRIVATE-LAUNCH-AUTHORITY.json"
HUMAN = ROOT / "design/handoff/OVERNIGHT-PRIVATE-LAUNCH-AUTHORITY.md"


def require(condition: bool, message: str) -> None:
    if not condition:
        raise AssertionError(message)


def main() -> None:
    data = json.loads(MACHINE.read_text(encoding="utf-8"))
    require(data["status"] == "binding_superseding_private_automated_staging_authority"
            and data["source_thread_id"] == "019f68de-05a0-7673-945b-104b36c2f9ac"
            and data["base_checkpoint"] == "518372297979dc5820b4485e556323e698ecdac7",
            "overnight authority source/checkpoint drift")
    supersession = data["supersession"]
    require(supersession["interim_m4_closed_pause_for_automated_staging"] == "waived"
            and supersession["brad_visual_approval_of_v5_or_unseen_build"] is None
            and "morning" in supersession["final_human_gate"],
            "automated staging or human-approval boundary drift")
    forbidden = " | ".join(data["forbidden_scope"])
    for phrase in ("public", "Crafty", "production Supabase", "media originals",
                   "ambiguous", "secret", "reset", "weakening"):
        require(phrase in forbidden, f"overnight forbidden boundary lost: {phrase}")
    require(data["external_target_rule"]["production_mutation"] is False
            and "never report" in data["external_target_rule"]["receipt_rule"]
            and "zero source-touch" in data["creative_and_system_contract"]["answers"]
            and "fixed puzzle enum" in data["creative_and_system_contract"]["runtime"],
            "private target, receipt, answer, or open-ended runtime boundary drift")
    require(len(data["mandatory_execution_evidence"]) >= 7,
            "overnight mandatory evidence inventory weakened")
    human = HUMAN.read_text(encoding="utf-8")
    for phrase in ("BINDING SUPERSESSION", "does **not** record V5", "final human acceptance gate",
                   "production Supabase", "zero-observation correct answers", "one linear cold playthrough"):
        require(phrase in human, f"human overnight authority missing: {phrase}")
    print("OVERNIGHT PRIVATE LAUNCH AUTHORITY: PASS (automated staging open; public/production closed; human gate deferred)")


if __name__ == "__main__":
    main()
