#!/usr/bin/env python3
"""Validate P4 vNext's story-first, zero-receipt, open-ended private revision."""
from __future__ import annotations

import json
import re
from pathlib import Path


ROOT = Path(__file__).resolve().parents[1]
CASE = ROOT / "design/m3/P4-VNEXT-CASE-BRIEF.json"
PLAN = ROOT / "design/m3/M3-VNEXT-CROSS-MEDIA-PLAN.json"
STATE = ROOT / "plugin/src/main/java/com/observance/watcher/m3runtime/PrivateSliceState.java"
WORLD = STATE.with_name("PrivateSliceWorld.java")
RUNTIME = STATE.with_name("PrivateSliceReviewRuntime.java")
SELF_TEST = ROOT / "plugin/src/test/java/com/observance/watcher/m3runtime/PrivateSliceStateSelfTest.java"
COPPERLINE = ROOT / "dashboard/src/lib/copperline-p4-archive.ts"
VALIDATION = ROOT / "design/m3/PAPER-VNEXT-DISPOSABLE-RECEIPT.json"
REVIEW = ROOT / "design/m3/PAPER-VNEXT-REVIEW-SERVER-RECEIPT.json"
FAILED = ROOT / "design/m3/PAPER-VNEXT-FAILED-ATTEMPTS.json"
VISUAL = ROOT / "design/m3/VNEXT-BLOCK-STATE-VISUAL-AUDIT.json"


def require(condition: bool, message: str) -> None:
    if not condition:
        raise AssertionError(message)


def load(path: Path) -> dict:
    return json.loads(path.read_text(encoding="utf-8"))


def main() -> None:
    case = load(CASE)
    plan = load(PLAN)
    require(case["schema_version"] == "1.0.0-p4-vnext-case-brief", "case identity drift")
    require(len(case["case_authoring_contract"]) == 12, "revelation-first twelve-field brief incomplete")
    require(case["scope"]["active_hour_target"] == {"min": 1, "max": 2}
            and case["scope"]["difficulty"] == "high"
            and case["scope"]["brad_visual_approval"] is None
            and case["scope"]["production_mutation"] is False,
            "difficulty, approval, or production boundary drift")
    require("OVERNIGHT-PRIVATE-LAUNCH-AUTHORITY.json" in " ".join(case["binding_authorities"])
            and "automated private staging" in case["scope"]["phase_progression"],
            "overnight supersession missing from P4 authority")
    require(case["generic_primitives"]["mechanism_type_field"] is False,
            "closed puzzle taxonomy introduced")
    findings = case["findings"]
    require([row["id"] for row in findings] == ["P4.F1", "P4.F2", "P4.F3", "P4.F4", "P4.F5"],
            "finding ladder drift")
    require(all(row["canonical_answer"] not in row["question"] for row in findings),
            "answer leaked by filing question")
    require(plan["status"] == "implementation_active_under_overnight_private_staging_authority"
            and plan["implementation_hold"] is None
            and plan["generic_runtime_and_authoring_primitives"]["forbidden_architecture"].startswith("mechanism_type"),
            "vNext plan still held or taxonomy-closed")

    state = STATE.read_text(encoding="utf-8")
    world = WORLD.read_text(encoding="utf-8")
    runtime = RUNTIME.read_text(encoding="utf-8")
    self_test = SELF_TEST.read_text(encoding="utf-8")
    for row in findings:
        require(row["canonical_answer"] in state, f"runtime lacks {row['id']} canonical conclusion")
    require("observationsByFinding.getOrDefault" in state
            and "Correct wording passes even with zero observation receipts" in state
            and "sourcesByFinding.keySet().containsAll(BASE_FINDINGS)" in state,
            "zero-receipt finding acceptance or synthesis prerequisite drift")
    require("observationsByFinding.getOrDefault(findingId, Set.of())" in state,
            "optional custody is not projected into accepted receipts")
    require("CONCLUSION_OPTIONS" not in state and "CONCLUSION_OPTIONS" not in runtime,
            "stale closed multiple-choice catalog remains active")
    require("ClickEvent.suggestCommand" in world and "addFilingEntry" in world
            and "A copied phrase is not an explanation" in world,
            "free-text physical filing affordance missing")
    require("observation_receipts_required=0" in runtime
            and "COPY THE NEAREST PHRASE" in runtime
            and "lodgeFinding" in runtime,
            "negative and zero-observation harness coverage missing")
    for token in ("requires zero observation receipts", "touching every source cannot make a wrong report correct",
                  "zero-receipt solution does not fabricate custody", "true-but-not-door theory"):
        require(token in self_test, f"state test missing: {token}")

    copperline = COPPERLINE.read_text(encoding="utf-8")
    entries = re.findall(r"\{ id: '([^']+)'.*?clueRole: '(direct|texture|mixed)'", copperline)
    direct = sum(role == "direct" for _, role in entries)
    require(len(entries) >= 20 and direct == 4 and (len(entries) - direct) >= direct * 3,
            "Copperline clue-to-texture density drift")
    require("mkept" in copperline and "cl_amy" in copperline and "not a production deployment" in copperline,
            "Copperline human voice or receipt boundary missing")
    require(case["receipt_boundary"]["not_yet_real"], "external integration gaps were concealed")

    if VALIDATION.is_file():
        validation = load(VALIDATION)
        review = load(REVIEW)
        failed = load(FAILED)
        visual = load(VISUAL)
        evidence = validation["evidence"]
        require(validation["source_git_commit"] == "8a51f26814914e89fe857a929266e807b2c96586"
                and validation["paper"] == {"build": 132,
                    "jar_sha256": "5ffef465eeeb5f2a3c23a24419d97c51afd7dbb4923ff42df9a3f58bba1ccfba",
                    "version": "1.21.11"}
                and "findings=0" in evidence["closed_audit"]
                and "gate_collision=88" in evidence["closed_audit"]
                and "findings=0" in evidence["final_audit"]
                and "gate_collision=0" in evidence["final_audit"],
                "fresh Paper structural/restart receipt drift")
        require("findings=0 gate=closed" in evidence["naive_zero_receipt_negative"]
                and "throttled=true" in evidence["brute_zero_receipt_negative"]
                and "findings=4 gate=closed" in evidence["zero_observation_report_correct"]
                and "gate=open" in evidence["zero_observation_synthesis_correct"]
                and "observation_receipts_required=0" in evidence["guided_client_model"],
                "Paper negative or zero-observation proof drift")
        require(review["journal_state"] == "absent_pristine_review_target"
                and review["brad_visual_approval"] is None
                and review["server_configuration"]["bind"] == "127.0.0.1"
                and review["server_configuration"]["port"] == 25591,
                "pristine private review target drift")
        require(len(failed["attempts"]) == 3
                and all(row.get("journal_created") is False for row in failed["attempts"])
                and visual["paper_findings_closed_open_restart"] == 0
                and visual["physical_affordance"]["unclassified_floating_blocks"] == 0
                and visual["limits"]["brad_visual_approval"] is None,
                "failed-attempt or internal visual audit history drift")
        print("M3 vNext story/zero-receipt/Copperline/Paper checks PASS")
    else:
        print("M3 vNext story/zero-receipt/Copperline source checks PASS; Paper receipt pending")


if __name__ == "__main__":
    main()
