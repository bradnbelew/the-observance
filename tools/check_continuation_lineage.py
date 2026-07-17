#!/usr/bin/env python3
"""Validate the canonical continuation lineage and its fail-closed M3 authority."""
from __future__ import annotations

import json
from pathlib import Path
import subprocess


ROOT = Path(__file__).resolve().parents[1]
LINEAGE = ROOT / "design" / "handoff" / "CANONICAL-LINEAGE.json"


def require(condition: bool, message: str) -> None:
    if not condition:
        raise AssertionError(message)


def git(*args: str) -> str:
    return subprocess.run(
        ["git", *args], cwd=ROOT, check=True, text=True, capture_output=True
    ).stdout.strip()


def main() -> None:
    data = json.loads(LINEAGE.read_text(encoding="utf-8"))
    require(data["schema_version"] == "1.2.0-continuation-lineage", "lineage schema drift")
    checkpoint = data["checkpoint_identity"]
    require(checkpoint["branch"] == "codex/m3-disposable-paper-gate", "canonical branch drift")
    require(checkpoint["production_mutated"] is False, "integration cannot claim production mutation")
    require(checkpoint["m4_authority"] == "closed", "M4 must remain closed")

    starting = data["starting_authority"]
    require(starting["commit"] == "8f938e0558b545a33039bb112ad091cf511a6ffe",
            "failed-review starting authority drift")
    require(starting["visual_decision"] == "failed_revision_required_2026-07-16",
            "failed Brad decision lost")
    require(starting["current_authority"] is False
            and starting["superseded_for_revision_by"] == "observance-p4-private-slice-v2",
            "v1 rejection history/current-revision distinction drift")

    expected = {
        "84899e0d367c8ad6151ea74b0c338e92ac8a9b7c":
            "design/m2/M2-SUPABASE-VALIDATION.md",
        "a6069aabae705f18255428f612476552c8770e33":
            "design/m3/EXTERNAL-GAP-AUDIT-2026-07-16.md",
    }
    rows = {row["source_commit"]: row for row in data["incorporated_evidence"]}
    require(set(rows) == set(expected), "sibling evidence provenance is incomplete or duplicated")
    for source_commit, authority in expected.items():
        row = rows[source_commit]
        require(row["authority"] == authority, f"authority drift for {source_commit}")
        require((ROOT / authority).is_file(), f"missing incorporated authority: {authority}")
        require(bool(row["unresolved"]), f"unresolved gaps lost for {source_commit}")
        git("cat-file", "-e", f"{source_commit}^{{commit}}")
        linear = row["linear_commit"]
        git("cat-file", "-e", f"{linear}^{{commit}}")
        require(subprocess.run(
            ["git", "merge-base", "--is-ancestor", linear, "HEAD"], cwd=ROOT
        ).returncode == 0, f"linear incorporation commit is not an ancestor: {linear}")

    revision = data["revision_evidence"]
    require(revision["integration_checkpoint"] == "69d729460382576d07676103b54048b1a648c412",
            "canonical integration checkpoint drift")
    for commit in (revision["authored_source_commit"], revision["paper_persistence_fix_commit"],
                   revision["pristine_receipt_fix_commit"]):
        git("cat-file", "-e", f"{commit}^{{commit}}")
        require(subprocess.run(["git", "merge-base", "--is-ancestor", commit, "HEAD"], cwd=ROOT).returncode == 0,
                f"M3 revision checkpoint is not an ancestor: {commit}")
    paper = revision["paper_result"]
    require(paper == {"version": "1.21.11", "build": 132, "cells": 248745,
                      "closed_gate_collision_cells": 88, "open_gate_collision_cells": 0,
                      "evidence_surfaces": 14, "filing_surfaces": 6,
                      "restart_replay_receipts": 28, "validation_status": "passed"},
            "M3 v2 Paper result lineage drift")
    require(bool(revision["unresolved"]), "M3 v2 unresolved human/client gates lost")
    failed = revision["preserved_failed_attempts"]
    require([row["result"] for row in failed] == ["failed_closed",
            "world_build_and_audit_passed_receipt_generation_failed_closed"]
            and all(row["reuse_forbidden_and_observed"] for row in failed),
            "fresh-target failure provenance lost or weakened")
    for authority in (revision["current_authority"], revision["rejected_authority_preserved_at"],
                      revision["validation_receipt"], revision["review_target_receipt"]):
        require((ROOT / authority).is_file(), f"missing M3 revision lineage authority: {authority}")

    active = data["active_brad_review"]
    require(active["status"] == "in_progress_binding_findings_recorded_not_approved"
            and active["brad_visual_approval"] is None and active["m4_authority"] == "closed",
            "active Brad review approval/M4 gate drift")
    require(active["implementation_state"] == "paused_until_full_feedback_pass_complete"
            and active["live_server_directive"] == "do_not_mutate_or_stop_while_brad_continues_walk",
            "active-review pause or live-server preservation directive lost")
    require(len(active["findings"]) == 14
            and "not yet fully authored or legible in-world" in active["interpretation"],
            "active Brad findings or binding interpretation drift")
    require(active["machine_review_overlay"] == "design/m3/BRAD-V2-ACTIVE-REVIEW.json"
            and (ROOT / active["machine_review_overlay"]).is_file(),
            "active Brad machine-review overlay missing")
    final = data["final_brad_v2_review"]
    require(final["status"] == "complete_not_approved_revision_required"
            and final["brad_visual_approval"] is None and final["m4_authority"] == "closed",
            "final Brad v2 rejection or M4 gate drift")
    require(final["decision_authority"] == "design/m3/BRAD-V2-REVIEW-DECISION.json"
            and (ROOT / final["decision_authority"]).is_file()
            and final["required_v3_machine_check_count"] == 12,
            "final Brad decision authority or v3 check count drift")
    require(final["review_server_state"] == "clean_save_flush_stop_verified_pid_and_port_ended"
            and final["v3_state"] == "ready_to_begin_after_clean_v2_rejection_checkpoint",
            "review-server clean stop or v3 checkpoint gate drift")
    require(final["review_server_stop_receipt"] == "design/m3/PAPER-V2-REVIEW-STOP-RECEIPT.json"
            and (ROOT / final["review_server_stop_receipt"]).is_file(),
            "review-server stop receipt missing")

    v3 = data["v3_revision_evidence"]
    require(v3["v2_rejection_checkpoint"] == "18da113f097fffa8ac489ea684e2dc139c8c3af1"
            and v3["passing_source_commit"] == "ebc731ff3bfc0eb42572246b814ac541811190f2",
            "v3 source/rejection checkpoint lineage drift")
    for commit in (v3["v2_rejection_checkpoint"], v3["authored_source_commit"],
                   v3["composition_purpose_fix_commit"], v3["reader_sightline_fix_commit"],
                   v3["passing_source_commit"]):
        git("cat-file", "-e", f"{commit}^{{commit}}")
        require(subprocess.run(["git", "merge-base", "--is-ancestor", commit, "HEAD"],
                               cwd=ROOT).returncode == 0,
                f"M3 v3 checkpoint is not an ancestor: {commit}")
    require(v3["authority_sha256"] == "0181b5566ea49a653b9cc95a650246c52ce670735d6ec2d6e4b1f6b9bc2a7ae5",
            "v3 authority hash lineage drift")
    require(v3["paper_result"] == {
        "version": "1.21.11", "build": 132, "cells": 248745,
        "findings_closed_open_restart": 0, "closed_gate_collision_cells": 88,
        "open_gate_collision_cells": 0, "evidence_books": 8, "filing_surfaces": 6,
        "reference_books": 1, "eye_level_signs": 4, "water_cells": 62,
        "restart_replay_receipts": 28, "validation_status": "passed",
    }, "M3 v3 Paper result lineage drift")
    require(v3["preserved_failed_targets"] == [
        "m3-v3-validation-0aac448-20260717-a",
        "m3-v3-validation-46ba980-20260717-b",
        "m3-v3-validation-5025d73-20260717-c",
    ] and bool(v3["unresolved"]), "v3 failure provenance or unresolved client gates lost")
    routed = v3["routed_regression"]
    require(routed["aggregate_status"] == "honestly_blocked_at_secret_dependent_discord_resolvecheck"
            and len(routed["missing_environment"]) == 6
            and "27-task Paper plugin clean/check/build" in routed["separate_local_passes"]
            and routed["production_credentials_loaded"] is False
            and routed["production_mutated"] is False,
            "routed regression boundary or local-pass provenance drift")
    for authority in (v3["current_authority"], v3["failed_attempts_receipt"],
                      v3["validation_receipt"], v3["review_target_receipt"]):
        require((ROOT / authority).is_file(), f"missing M3 v3 lineage authority: {authority}")
    validation = json.loads((ROOT / v3["validation_receipt"]).read_text(encoding="utf-8"))
    review_receipt = json.loads((ROOT / v3["review_target_receipt"]).read_text(encoding="utf-8"))
    require(validation["source_git_commit"] == v3["passing_source_commit"]
            and validation["evidence"]["closed_audit"].find("findings=0") >= 0
            and validation["evidence"]["open_audit"].find("gate_collision=0") >= 0
            and validation["evidence"]["restart_audit"].find("findings=0") >= 0,
            "v3 passing closed/open/restart receipt drift")
    require(review_receipt["source_git_commit"] == v3["passing_source_commit"]
            and review_receipt["journal_state"] == "absent_pristine_review_target"
            and review_receipt["evidence"]["closed_audit"].find("findings=0") >= 0,
            "v3 pristine review-target receipt drift")

    active_v3 = data["active_brad_v3_review"]
    require(active_v3["status"] == "in_progress_decision_not_approved_revision_required"
            and active_v3["brad_visual_approval"] is None and active_v3["m4_authority"] == "closed",
            "active Brad v3 review approval/M4 gate drift")
    require(active_v3["machine_review_overlay"] == "design/m3/BRAD-V3-ACTIVE-REVIEW.json"
            and (ROOT / active_v3["machine_review_overlay"]).is_file()
            and active_v3["decision_authority"] == "design/m3/BRAD-V3-REVIEW-DECISION.json"
            and (ROOT / active_v3["decision_authority"]).is_file()
            and len(active_v3["current_noncompliance"]) == 4
            and active_v3["required_future_check_ids"] == [
                "M3.V3R.DISTINCT_FILING_AFFORDANCE",
                "M3.V3R.DIEGETIC_FILING_INSTRUCTION",
                "M3.V3R.COLD_PLAYER_OBJECTIVE_COMPREHENSION",
            ], "active Brad v3 finding/check overlay drift")
    require(active_v3["implementation_state"] == "paused_until_full_v3_visual_pass_and_disconnect"
            and active_v3["live_server_directive"]
                == "do_not_stop_or_mutate_until_brad_disconnects",
            "active Brad v3 revision/server hold weakened")

    decisive = data["brad_v3_decisive_mechanic_finding"]
    require(decisive["decision"] == "not_approved_revision_required"
            and decisive["review_state"] == "mechanic_complete_visual_pass_and_disconnect_pending"
            and decisive["brad_visual_approval"] is None and decisive["m4_authority"] == "closed",
            "decisive Brad v3 rejection state drift")
    require(decisive["decision_authority"] == active_v3["decision_authority"]
            and decisive["required_next_revision_check_count"] == 9
            and len(decisive["required_negative_tests"]) == 2
            and decisive["live_server_directive"] == "do_not_stop_or_mutate_until_brad_disconnects",
            "v3 reasoning/negative-test/server-hold authority drift")

    gate = data["current_gate"]
    require(gate["m4_open"] is False
            and "Brad's complete v3 visual pass and confirmed disconnect while the live target remains unchanged"
                in gate["required_next_evidence"]
            and "cold-player comprehension plus naive-click-through and brute-force negative receipts"
                in gate["required_next_evidence"],
            "M3 v3 client/approval gate weakened")
    review = (ROOT / "design" / "m3" / "BRAD-REVIEW-PACKAGE.md").read_text(encoding="utf-8")
    require("FAILED / REVISION REQUIRED" in review and "M4 district implementation authority: **CLOSED" in review,
            "Brad rejection authority missing")
    v3_review = (ROOT / "design" / "m3" / "BRAD-V3-REVIEW-PACKAGE.md").read_text(encoding="utf-8")
    require("VISUAL APPROVAL PENDING" in v3_review and "M4 is **closed**" in v3_review,
            "v3 review package approval/M4 gate drift")
    print("CONTINUATION LINEAGE: PASS (V3 NOT APPROVED; functional receipts preserved; checklist bypass rejected; disconnect/server hold; M4 closed)")


if __name__ == "__main__":
    main()
