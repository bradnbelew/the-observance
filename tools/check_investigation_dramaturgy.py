#!/usr/bin/env python3
"""Validate the non-taxonomic investigation dramaturgy and novelty authorities."""
from __future__ import annotations

import json
from pathlib import Path

ROOT = Path(__file__).resolve().parents[1]
STANDARD = ROOT / "design/handoff/INVESTIGATION-DRAMATURGY-STANDARD.json"
HUMAN = ROOT / "design/handoff/INVESTIGATION-DRAMATURGY-STANDARD.md"
AUDIT = ROOT / "design/handoff/INVESTIGATION-NOVELTY-AUDIT.json"


def require(condition: bool, message: str) -> None:
    if not condition:
        raise AssertionError(message)


def load(path: Path) -> dict:
    return json.loads(path.read_text(encoding="utf-8"))


def main() -> None:
    standard = load(STANDARD)
    require(standard["schema_version"] == "1.0.0-investigation-dramaturgy"
            and standard["status"] == "binding_cross_phase_non_exhaustive_authority",
            "dramaturgy authority identity drift")
    require(standard["m4_authority"] == "closed"
            and standard["brad_visual_approval"] is None,
            "dramaturgy authority opens M4 or fabricates approval")
    require(standard["recurring_surface_authorities"]
                == ["design/handoff/COPPERLINE-COMMUNITY-ARCHIVE-STANDARD.json"],
            "dramaturgy authority lost Copperline recurring-surface routing")
    boundary = standard["taxonomy_boundary"]
    require(boundary["examples_are_non_exhaustive"] is True
            and all(boundary[key] is False for key in (
                "closed_taxonomy", "allowlist", "enum", "quota",
                "runtime_mechanism_dispatch_catalog")),
            "non-exhaustive/non-taxonomic boundary weakened")
    primitives = standard["generic_primitives"]
    require(set(primitives) == {"evidence", "state", "answer", "content", "event", "composition_rule"}
            and "mechanism_type" in primitives["composition_rule"],
            "generic composable primitive contract drift")
    structures = {row["structure"] for row in standard["mined_narrative_structures"]}
    require({"correct_decode_wrong_interpretation", "delayed_callback",
             "situated_professional_grammar", "multiple_independent_in_doors",
             "contradiction_and_provenance", "true_but_not_the_door",
             "quiet_reward", "meaning_changes_later", "asymmetric_theory_and_aftermath"}
            == structures, "mined narrative structures incomplete")
    require(len(standard["case_authoring_contract"]) == 12,
            "twelve-field case authoring contract drift")
    rhythm = standard["dramaturgical_rhythm"]
    require(all(rhythm[key] is False for key in (
        "fixed_sequence", "every_beat_is_a_puzzle", "every_clue_is_an_artifact",
        "every_answer_opens_a_door", "every_case_uses_the_same_rhythm")),
        "varied dramaturgical rhythm weakened")
    retired = " | ".join(standard["retired_brittle_implementations"])
    for phrase in ("exact block geometry", "time", "sound-only", "F3", "click", "voice",
                   "decorative", "keeper-stone"):
        require(phrase in retired, f"retired brittle implementation lost: {phrase}")
    p4 = standard["p4_c02_vnext_case_frame"]
    require(p4["target_active_hours"] == {"min": 1, "max": 2}
            and len(p4["sustained_reversal"]) == 3
            and "C02 only" in p4["exact_canon_belief_earned"]
            and "not a sufficient conclusion" in p4["required_inference"],
            "P4/C02 reversal or ladder boundary drift")

    audit = load(AUDIT)
    require(audit["closed_taxonomy"] is False
            and audit["mechanism_quota"] is False
            and audit["automatic_novelty_score_forbidden"] is True,
            "novelty audit became a taxonomy, quota, or score")
    require(len(audit["campaign_baseline_not_a_catalog"]) == 12
            and [row["phase"] for row in audit["campaign_baseline_not_a_catalog"]]
                == [f"P{i}" for i in range(1, 13)],
            "campaign comparison baseline drift")
    require("Human creative review" in audit["audit_result_policy"]
            and any("four nearby fact lanes" in item
                    for item in audit["p4_vnext_baseline"]["must_not_repeat"]),
            "human novelty gate or P4 repetition finding weakened")

    human = HUMAN.read_text(encoding="utf-8")
    for phrase in ("NON-EXHAUSTIVE AUTHORITY", "revelation and emotional reversal outward",
                   "allowlist", "Iss/Liar engine", "true-but-not-the-door",
                   "Required case brief", "P4 / C02 vNext frame", "M4 remains closed"):
        require(phrase in human, f"human dramaturgy authority missing: {phrase}")
    print("INVESTIGATION DRAMATURGY: PASS (non-taxonomic primitives, reversal-first cases, novelty audit)")


if __name__ == "__main__":
    main()
