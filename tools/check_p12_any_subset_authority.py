#!/usr/bin/env python3
"""Prove the current RP03/RP04 any-subset overlay without rewriting M2 evidence."""
from __future__ import annotations

import hashlib
import json
from pathlib import Path


ROOT = Path(__file__).resolve().parents[1]
BASE = ROOT / "design/ARG-V5-PHYSICAL-PREDICATES.json"
OVERLAY = ROOT / "design/ARG-P12-ANY-SUBSET-OVERLAY.json"
CONTROLLER = ROOT / "plugin/src/main/java/com/observance/watcher/v5runtime/ritual/V5RitualWorldController.java"
BALLOTS = ROOT / "plugin/src/main/java/com/observance/watcher/v5runtime/ritual/VisibleBallotRite.java"
PRESENCE = ROOT / "plugin/src/main/java/com/observance/watcher/v5runtime/ritual/CollectivePresenceRite.java"
TEST = ROOT / "plugin/src/test/java/com/observance/watcher/v5runtime/ritual/RitualFinaleSelfTest.java"
BASE_SHA256 = "16de527496a6c4e3ae0fc093db07b74754be55193059f1c8d3fe9ab0c29a595a"


def require(ok: bool, message: str) -> None:
    if not ok:
        raise AssertionError(message)


def main() -> None:
    require(hashlib.sha256(BASE.read_bytes()).hexdigest() == BASE_SHA256,
            "immutable M2 predicate bytes changed")
    overlay = json.loads(OVERLAY.read_text(encoding="utf-8"))
    require(overlay["base_physical_authority"]["raw_sha256"] == BASE_SHA256,
            "overlay base receipt drifted")
    require(overlay["base_physical_authority"]["bytes_immutable"] is True,
            "overlay must preserve base bytes")
    rules = overlay["global_rules"]
    require(rules["source_observation_gates_correctness"] is False,
            "source observations cannot gate the release")
    require(rules["nearby_online_or_absent_players_required"] is False,
            "nearby/online/absent players cannot gate the release")
    require(rules["one_linked_participant_sufficient"] is True,
            "one linked participant must be sufficient")
    require(rules["timer_required"] is False, "release cannot require a timer")

    nodes = {row["node_id"]: row for row in overlay["nodes"]}
    require(set(nodes) == {"RP03", "RP04"}, "overlay must cover exactly RP03/RP04")
    require(nodes["RP03"]["effective_kind"] == "protected_any_subset_name_choice",
            "RP03 effective kind drifted")
    require(nodes["RP03"]["consequence_book"]["required_open_before_choice"] is False,
            "RP03 book became a prerequisite")
    require(nodes["RP03"]["consequence_book"]["observation_receipt_non_gating"] is True,
            "RP03 book receipt must be non-gating")
    require(nodes["RP04"]["effective_kind"] == "any_subset_branch_confirmation",
            "RP04 effective kind drifted")
    require(nodes["RP04"]["sector_rule"]["required_reachable_lit_sectors"] == 1,
            "RP04 must require exactly one reachable sector")
    require(nodes["RP04"]["sector_rule"]["time_limit"] is False,
            "RP04 cannot require timed attendance")

    controller = CONTROLLER.read_text(encoding="utf-8")
    require("Set<UUID> participants = Set.of(player.getUniqueId());" in controller
            and "ballots.startRp03(participants)" in controller,
            "Paper RP03 is not actor-only")
    require("Set<UUID> participants = Set.of(starter.getUniqueId());" in controller
            and "presence.start(participants, bridge)" in controller,
            "Paper RP04 is not actor-only")
    require("CONSEQUENCE_BOOK_UNREAD" not in controller,
            "Paper controller still gates RP03 on book observations")
    ballots = BALLOTS.read_text(encoding="utf-8")
    start = ballots[ballots.index("public synchronized StartResult startRp03"):
                    ballots.index("public void markConsequenceBookRead")]
    require("unread" not in start.lower() and "CONSEQUENCE_READ_BIT" not in start,
            "RP03 start still reads observation receipts")
    presence = PRESENCE.read_text(encoding="utf-8")
    require("WINDOW_TICKS" not in presence and "clock.tick() +" not in presence,
            "RP04 runtime still has an attendance timer")
    tests = TEST.read_text(encoding="utf-8")
    require("zero consequence-book observation receipts" in tests,
            "zero-observation RP03 test is missing")
    require("nearby non-participant is not enrolled and cannot block RP04" in tests,
            "RP04 nearby-player negative test is missing")
    print("P12 any-subset overlay: PASS (M2 bytes preserved; RP03 zero-receipt actor choice; RP04 one-participant untimed confirmation)")


if __name__ == "__main__":
    main()
