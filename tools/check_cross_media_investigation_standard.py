#!/usr/bin/env python3
"""Validate Brad's binding cross-media investigation and robust-Minecraft authority."""
from __future__ import annotations

import json
from pathlib import Path


ROOT = Path(__file__).resolve().parents[1]
MACHINE = ROOT / "design/handoff/CROSS-MEDIA-INVESTIGATION-STANDARD.json"
HUMAN = ROOT / "design/handoff/CROSS-MEDIA-INVESTIGATION-STANDARD.md"
PLAN = ROOT / "design/m3/M3-VNEXT-CROSS-MEDIA-PLAN.json"
ACTIVE = ROOT / "design/m3/BRAD-V5-ACTIVE-REVIEW.json"


def require(condition: bool, message: str) -> None:
    if not condition:
        raise AssertionError(message)


def load(path: Path) -> dict:
    return json.loads(path.read_text(encoding="utf-8"))


def main() -> None:
    authority = load(MACHINE)
    require(authority["schema_version"] == "1.0.0-cross-media-investigation"
            and authority["authority_id"] == "observance-cross-media-investigation-standard"
            and authority["status"] == "binding", "cross-media authority identity drift")
    require(authority["m4_authority"] == "closed"
            and authority["brad_visual_approval"] is None,
            "cross-media authority opens M4 or fabricates approval")
    core = authority["core_identity"]
    require("cross-media ARG" in core["statement"] and len(core["evidence_surfaces"]) == 8
            and all(token in core["lore_requirement"] for token in (
                "consequential", "spooky", "uncanny", "human", "central mystery")),
            "cross-media surface or lore identity weakened")
    geometry = authority["minecraft_geometry_guard"]
    require(geometry["compact_robust_authored_geometry_required"] is True
            and geometry["mandatory_bespoke_block_simulation_forbidden"] is True
            and geometry["mandatory_literal_cart_path_tracing_forbidden"] is True
            and geometry["mandatory_block_by_block_archaeology_forbidden"] is True
            and "fragile" in geometry["brittleness_rule"],
            "robust Minecraft geometry boundary weakened")
    reference = authority["approved_reference_pattern"]
    require(reference["name"] == "Mara library cipher"
            and reference["sample_coordinates"] == ["3-1-4", "2-1-5", "6-3-1"]
            and "not a template to repeat" in reference["reuse_rule"],
            "Mara reference pattern/fairness boundary drift")
    require(len(authority["candidate_mechanism_families"]) == 12,
            "cross-media mechanism family palette drift")
    guards = authority["guardrails"]
    require(all(value is True for key, value in guards.items()
                if isinstance(value, bool) and not key.endswith("forbidden"))
            and guards["arbitrary_uncorroborated_cipher_forbidden"] is True
            and guards["answer_touch_receipt_gate_forbidden"] is True
            and guards["single_surface_mandatory_path_forbidden_when_any_subset_requires_alternatives"] is True
            and guards["p4_active_hour_target"] == {"min": 1, "max": 2},
            "cross-media fairness/difficulty guard weakened")
    arcs = authority["arc_grammar_diversity"]
    require([row["arc"] for row in arcs] == [f"P{index}" for index in range(1, 13)]
            and len({row["grammar"] for row in arcs}) == 12,
            "P1-P12 investigation grammar diversity drift")
    integrity = authority["receipt_integrity"]
    require(all(integrity.values()), "external asset/receipt honesty weakened")
    require(len(authority["required_static_checks"]) == 7,
            "cross-media static gate inventory drift")

    plan = load(PLAN)
    require(plan["status"] == "authored_plan_only_implementation_held_for_active_review_and_disconnect"
            and plan["scope"]["target_active_hours"] == {"min": 1, "max": 2}
            and plan["scope"]["m4_authority"] == "closed"
            and plan["scope"]["brad_visual_approval"] is None,
            "P4 next-revision plan scope/hold drift")
    compact = plan["compact_minecraft_plan"]
    require(len(compact["simplify_for_robustness"]) == 4
            and "zero observation receipts" in compact["submission_semantics"],
            "P4 compact geometry or non-gating submission plan drift")
    require(len(plan["candidate_p4_mechanism_mix_not_final_until_full_feedback"]) == 4
            and plan["clue_graph_requirements"]["out_of_order_safe"] is True
            and plan["clue_graph_requirements"]["correct_answer_zero_receipt_safe"] is True
            and plan["clue_graph_requirements"]["locked_ambiguities_preserved"] is True,
            "P4 cross-media clue graph plan weakened")
    require(len(plan["offline_authoring_and_tests_after_disconnect"]) == 9
            and len(plan["real_future_integration_gates"]) == 9
            and "not currently claimed" not in plan["receipt_honesty"]["currently_available"].lower()
            and "No specification" in plan["receipt_honesty"]["rule"],
            "offline/future integration or receipt-honesty boundary drift")
    require("Do not implement" in plan["implementation_hold"],
            "live-server implementation hold lost")

    active = load(ACTIVE)
    correction = active["binding_cross_media_and_geometry_correction"]
    require(correction["cross_phase_authority"]
                == "design/handoff/CROSS-MEDIA-INVESTIGATION-STANDARD.json"
            and correction["next_revision_plan"]
                == "design/m3/M3-VNEXT-CROSS-MEDIA-PLAN.json"
            and "do not implement" in correction["implementation_hold"].lower(),
            "active V5 cross-media correction routing/hold drift")

    human = HUMAN.read_text(encoding="utf-8")
    for phrase in ("BINDING CROSS-PHASE AUTHORITY", "cross-media ARG",
                   "Robust Minecraft boundary", "Mara/library cipher",
                   "discoverable key", "Campaign grammar", "Receipt honesty",
                   "Brad approval is null", "M4"):
        require(phrase in human, f"human cross-media authority missing: {phrase}")

    print("CROSS-MEDIA INVESTIGATION: PASS "
          "(robust Minecraft boundary, P1-P12 diversity, zero-receipt answers, and honest future gates)")


if __name__ == "__main__":
    main()
