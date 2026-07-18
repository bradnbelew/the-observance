#!/usr/bin/env python3
"""Fail-closed implementation gate for the disposable P4/P5 ARG vertical slice."""

from __future__ import annotations

import json
import sys
from pathlib import Path

ROOT = Path(__file__).resolve().parents[1]


def text(path: str, failures: list[str]) -> str:
    target = ROOT / path
    if not target.is_file():
        failures.append(f"missing {path}")
        return ""
    return target.read_text(encoding="utf-8-sig")


def main() -> int:
    failures: list[str] = []
    authority_path = ROOT / "design/m3/P4-P5-ARG-VERTICAL-SLICE.json"
    try:
        authority = json.loads(authority_path.read_text(encoding="utf-8-sig"))
    except (OSError, json.JSONDecodeError) as exc:
        print(f"ARG vertical slice FAIL: unreadable authority: {exc}")
        return 1

    if authority.get("brad_approval") is not None:
        failures.append("authority must not invent Brad approval")
    if authority.get("conclusion_printed_verbatim_by_any_source") is not False:
        failures.append("a source may not print the conclusion")
    if authority.get("cipher", {}).get("result_is_final_answer") is not False:
        failures.append("cipher result may not be the final answer")
    if authority.get("cipher", {}).get("coordinates") != ["3-1-5", "2-1-6", "6-3-1"]:
        failures.append("page-line-word coordinates drifted")
    state = authority.get("state_contract", {})
    for key in ("local_primary", "hash_chained_journal", "monotonic", "idempotent",
                "restart_safe", "any_subset", "correct_theory_with_zero_observations"):
        if state.get(key) is not True:
            failures.append(f"state contract no longer requires {key}")
    if state.get("source_click_or_possession_gate") is not False:
        failures.append("source touches or possession may not gate correctness")
    surfaces = {row.get("platform") for row in authority.get("inputs", [])}
    if surfaces != {"Paper 1.21.11", "Next.js 16.2.10"}:
        failures.append(f"platform input coverage drifted: {sorted(surfaces)}")
    relationship_web = authority.get("relationship_web", {})
    if relationship_web.get("isolated_subsections_forbidden") is not True:
        failures.append("isolated clue subsections are no longer forbidden")
    if len(relationship_web.get("route", [])) < 7:
        failures.append("P4 relationship web lost its cross-surface plant-to-payoff chain")

    runtime = text("plugin/src/main/java/com/observance/watcher/arg/ArgVerticalSliceRuntime.java", failures)
    for needle in (
        "DialogResponseView", "DialogInput.text", "PlayerCustomClickEvent", "LifecycleEvents.COMMANDS",
        "new CaseCommand()", "maxLength(96)", "maxLength(64)", "width(320)", "List.of()",
        "DialogInput.text(\"purpose\"", "DialogInput.text(\"change\"", "DialogInput.text(\"anomaly\"",
        "state.submitConclusion(", "world.setGate(true)", "No source-click receipt is required"
    ):
        if needle not in runtime:
            failures.append(f"Paper real-input invariant missing: {needle}")
    for forbidden in ("AsyncPlayerChatEvent", "PlayerChatEvent", "lectern text input"):
        if forbidden in runtime:
            failures.append(f"forbidden required-input implementation present: {forbidden}")

    state_source = text("plugin/src/main/java/com/observance/watcher/arg/ArgVerticalSliceState.java", failures)
    if "LocalPrimaryJournal" not in state_source or any(token in state_source for token in (
            "observationReceipt", "hasObserved", "sourcePossession", "requiredObservations")):
        failures.append("vertical-slice predicate must be local-primary and observation-independent")
    for needle in ("matchesPurpose", "matchesChange", "matchesAnomaly", "canonicalMeaning"):
        if needle not in state_source:
            failures.append(f"structured meaning predicate missing: {needle}")
    for forbidden in ("ACCEPTED_THEORIES", "THE HOLD SHELTERED FAMILIES BEFORE SAFETY BECAME CONTROL"):
        if forbidden in state_source:
            failures.append(f"hidden canonical sentence predicate remains: {forbidden}")

    evidence = text("plugin/src/main/java/com/observance/watcher/arg/ArgVerticalSliceEvidence.java", failures)
    for needle in ("3, 1, 5", "2, 1, 6", "6, 3, 1", '"COPY BEFORE SOURCE"', "BookPageLayout::measure"):
        if needle not in evidence:
            failures.append(f"cipher/render authority missing: {needle}")
    if "ArgVerticalSliceEvidence.bookBody()" not in text(
            "plugin/src/main/java/com/observance/watcher/m3runtime/PrivateSliceWorld.java", failures):
        failures.append("Paper world does not consume the audited evidence pages")
    world_source = text("plugin/src/main/java/com/observance/watcher/m3runtime/PrivateSliceWorld.java", failures)
    for needle in ('new Cell(-3,-18,90),"P5_SERVICE_PUBLIC_SIGN"',
                   'new Cell(3,-18,90),"P5_PENALTY_CUSTODY_SIGN"'):
        if needle not in world_source:
            failures.append(f"P5 player-eye standing frame drifted: {needle}")

    form = text("dashboard/src/app/community/archive/intake-copies/RestoreArchiveForm.tsx", failures)
    action = text("dashboard/src/app/community/archive/intake-copies/actions.ts", failures)
    validator = text("dashboard/src/lib/copperline-p4-restore.ts", failures)
    archive_page = text("dashboard/src/app/community/archive/intake-copies/page.tsx", failures)
    route = text("dashboard/src/lib/copperline-p4-route.ts", failures)
    disagreement_post = text("dashboard/src/app/community/2011/02/11/old-copy/page.tsx", failures)
    ticket_page = text("dashboard/src/app/support/ticket.php/page.tsx", failures)
    harness = text("tools/run_arg_vertical_slice_disposable_paper.py", failures)
    for needle in ("action={formAction}", "aria-live=\"polite\"", "disabled={pending}",
                   "type=\"hidden\"", "restore-retained-attachments"):
        if needle not in form:
            failures.append(f"semantic Copperline form invariant missing: {needle}")
    for forbidden in ('name="ticket"', 'name="attachment"', 'name="order"', 'name="idempotency"'):
        if forbidden in form:
            failures.append(f"Copperline restore asks player to retype archive fact: {forbidden}")
    for needle in ("'use server'", "new URL(origin).host !== host", "validateP4Restore(formData)"):
        if needle not in action:
            failures.append(f"Copperline Server Action invariant missing: {needle}")
    for needle in ("createHash('sha256')", "P4-RESTORE", "RESTORE-RETAINED-ATTACHMENTS",
                   "RETAINED-ATTACHMENTS", "READ-ONLY"):
        if needle not in validator:
            failures.append(f"Copperline predicate/receipt invariant missing: {needle}")
    for forbidden in ("formData.get('ticket')", "formData.get('attachment')", "formData.get('order')",
                      "formData.get('idempotency')"):
        if forbidden in validator:
            failures.append(f"Copperline server still validates player-retyped archive fact: {forbidden}")
    for forbidden in ("mouth_notice.compare.txt", "03 was imaged before 04"):
        if forbidden in archive_page:
            failures.append(f"retained page prints discovery before restore: {forbidden}")
    for needle in ("accountFilter", "priorBackupPost", "disagreementPost", "custodyTicket",
                   "retainedAttachments"):
        if needle not in route:
            failures.append(f"Copperline relationship route missing: {needle}")
    if "P4_COPPERLINE_ROUTE.custodyTicket" not in disagreement_post:
        failures.append("ordinary mkept post does not lead to Ticket 2184")
    if "P4_COPPERLINE_ROUTE.retainedAttachments" not in ticket_page:
        failures.append("Ticket 2184 does not lead to its retained rows")
    for needle in ("arg-experience", "arg-theory", "zero_observation_correct", "receipts=4",
                   "arg-p4-p5-vertical-slice.journal", "actual_dialog_visual", "prepare-review",
                   "absent_pristine_review_target"):
        if needle not in harness:
            failures.append(f"disposable Paper lifecycle invariant missing: {needle}")

    if failures:
        print("ARG vertical slice FAIL")
        for failure in failures:
            print(f" - {failure}")
        return 1
    print("ARG vertical slice PASS: real Paper/web inputs, zero-receipt predicate, audited index, and player-caused response")
    return 0


if __name__ == "__main__":
    sys.exit(main())
