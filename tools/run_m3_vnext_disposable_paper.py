#!/usr/bin/env python3
"""Create-only Paper 1.21.11 validation/review targets for the P4 vNext private slice."""
from __future__ import annotations

import argparse
import json
from pathlib import Path

import run_m3_disposable_paper as base
from run_m3_v4_disposable_paper import wait_for_confirmation


ROOT = Path(__file__).resolve().parents[1]
CASE_SHA256 = "33a29766c1c43189fb18584ad30cac419a0ea4ceb17aeea32b2401ee65559c55"
AUTHORITY_ID = "observance-p4-private-slice-vnext"


def verify_authorities() -> None:
    coarse = base.canonical_sha256(ROOT / "design/m3/coarse-adjacency-v1.json")
    case = base.canonical_sha256(ROOT / "design/m3/P4-VNEXT-CASE-BRIEF.json")
    if coarse != base.COARSE_AUTHORITY_SHA256:
        raise ValueError(f"coarse authority hash drift: {coarse}")
    if case != CASE_SHA256:
        raise ValueError(f"vNext case authority hash drift: {case}")


def configure(target: Path, paper: Path, plugin: Path, target_id: str, commit: str, port: int) -> None:
    base.configure(target, paper, plugin, target_id, commit, port)
    properties = target / "server.properties"
    body = properties.read_text(encoding="utf-8")
    base.write_text(properties, body.replace("PRIVATE M3 V2 DISPOSABLE REVIEW",
                                             "PRIVATE M3 VNEXT DISPOSABLE REVIEW"))


def observe(process: base.PaperProcess, finding: str, source: str, contributor: str) -> str:
    return process.command(f"obsm3 observe {finding} {source} {contributor} custody",
                           "M3_OBSERVATION_COMMITTED")


def exercise_validation(target: Path, java: str) -> tuple[dict[str, object], list[str]]:
    epoch = 1_789_200_000
    first = base.PaperProcess(target, java)
    try:
        confirmation = wait_for_confirmation(first)
        first.wait_for("Done (", 300)
        status = first.command("obsm3 status", "M3_STATUS")
        build = first.command("obsm3 build", "M3_BUILD_COMPLETE", 300)
        closed = first.command("obsm3 audit", "M3_AUDIT PASS")
        security_closed = first.command("obsm3 security", "M3_SECURITY_PASS")
        ui_audit = first.command("obsm3 ui-audit", "M3_UI_AUDIT_PASS")
        guided = first.command("obsm3 guided-client-model", "M3_GUIDED_CLIENT_MODEL_PASS")
        naive = first.command(f"obsm3 naive-negative naive-zero {epoch}", "M3_NAIVE_NEGATIVE_PASS")
        after_naive = first.command("obsm3 audit", "M3_AUDIT PASS")
        brute = first.command(f"obsm3 brute-negative blind-zero {epoch + 10}", "M3_BRUTE_NEGATIVE_PASS")
        after_brute = first.command("obsm3 audit", "M3_AUDIT PASS")
        first.command("save-all flush", "Saved the game")
    finally:
        first.stop()
    base.write_text(target / "m3-vnext-first-start.log", "\n".join(first.lines) + "\n")

    second = base.PaperProcess(target, java)
    try:
        negative_restart_confirmation = wait_for_confirmation(second)
        second.wait_for("Done (", 300)
        negative_restart_audit = second.command("obsm3 audit", "M3_AUDIT PASS")
        persistent_throttle = second.command(
            f"obsm3 report-correct blind-zero {epoch + 14}", "M3_FAIL action=report-correct")
        report = second.command(f"obsm3 report-correct shared-zero-receipts {epoch + 1000}",
                                "M3_REPORT_CORRECT_PASS")
        report_audit = second.command("obsm3 audit", "M3_AUDIT PASS")
        watcher = second.command("obsm3 watcher-approve vnext-harness WestReviewer EastReviewer 10",
                                 "M3_WATCHER_APPROVED")
        synthesis = second.command(f"obsm3 synthesis-correct shared-zero-receipts {epoch + 1001}",
                                   "M3_SYNTHESIS_CORRECT_PASS")
        opened = second.command("obsm3 audit", "M3_AUDIT PASS")
        security_open = second.command("obsm3 security", "M3_SECURITY_PASS")
        observations = [
            observe(second, "P4.F1", "child_copybook", "orris"),
            observe(second, "P4.F1", "early_smoke_notice", "sela"),
            observe(second, "P4.F2", "hinge_repair", "toma"),
            observe(second, "P4.F2", "market_note", "neri"),
            observe(second, "P4.F3", "early_smoke_notice", "eda"),
            observe(second, "P4.F3", "late_attendance_ruling", "iven"),
            observe(second, "P4.F4", "bell_register", "lio"),
            observe(second, "P4.F4", "node_clock_extract", "brann"),
        ]
        replay = second.command("obsm3 replay", "M3_REPLAY_PASS")
        second.command("save-all flush", "Saved the game")
    finally:
        second.stop()

    third = base.PaperProcess(target, java)
    try:
        restart_confirmation = wait_for_confirmation(third)
        third.wait_for("Done (", 300)
        restarted = third.command("obsm3 audit", "M3_AUDIT PASS")
        restart_ui = third.command("obsm3 ui-audit", "M3_UI_AUDIT_PASS")
        restart_security = third.command("obsm3 security", "M3_SECURITY_PASS")
        restart_replay = third.command("obsm3 replay", "M3_REPLAY_PASS")
        final = third.command("obsm3 audit", "M3_AUDIT PASS")
        third.command("save-all flush", "Saved the game")
    finally:
        third.stop()
    restart_lines = second.lines + ["--- SECOND RESTART ---"] + third.lines
    base.write_text(target / "m3-vnext-restart.log", "\n".join(restart_lines) + "\n")
    return {
        "platform_confirmation": confirmation, "status": status, "build": build,
        "closed_audit": closed, "security_closed": security_closed,
        "ui_audit": ui_audit, "guided_client_model": guided,
        "naive_zero_receipt_negative": naive, "after_naive_audit": after_naive,
        "brute_zero_receipt_negative": brute, "after_brute_audit": after_brute,
        "negative_restart_confirmation": negative_restart_confirmation,
        "negative_restart_audit": negative_restart_audit,
        "persistent_throttle": persistent_throttle,
        "zero_observation_report_correct": report, "report_closed_audit": report_audit,
        "watcher_approval": watcher, "zero_observation_synthesis_correct": synthesis,
        "open_audit": opened, "security_open": security_open,
        "post_solution_optional_observations": observations, "replay": replay,
        "restart_confirmation": restart_confirmation, "restart_audit": restarted,
        "restart_ui_audit": restart_ui, "restart_security": restart_security,
        "restart_replay": restart_replay, "final_audit": final,
    }, restart_lines


def prepare_review(target: Path, java: str) -> tuple[dict[str, object], list[str]]:
    process = base.PaperProcess(target, java)
    try:
        confirmation = wait_for_confirmation(process)
        process.wait_for("Done (", 300)
        status = process.command("obsm3 status", "M3_STATUS")
        build = process.command("obsm3 build", "M3_BUILD_COMPLETE", 300)
        audit = process.command("obsm3 audit", "M3_AUDIT PASS")
        security = process.command("obsm3 security", "M3_SECURITY_PASS")
        ui_audit = process.command("obsm3 ui-audit", "M3_UI_AUDIT_PASS")
        guided = process.command("obsm3 guided-client-model", "M3_GUIDED_CLIENT_MODEL_PASS")
        process.command("save-all flush", "Saved the game")
    finally:
        process.stop()
    base.write_text(target / "m3-vnext-review-prepare.log", "\n".join(process.lines) + "\n")
    return {"platform_confirmation": confirmation, "status": status, "build": build,
            "closed_audit": audit, "security": security, "ui_audit": ui_audit,
            "guided_client_model": guided}, []


def package_world(target: Path) -> tuple[str, str, Path]:
    world_hash, _, old_package = base.package_world(target)
    package = target / "m3-private-slice-vnext-world.zip"
    old_package.replace(package)
    return world_hash, base.sha256(package), package


def main() -> None:
    parser = argparse.ArgumentParser()
    parser.add_argument("--paper-jar", type=Path, required=True)
    parser.add_argument("--plugin-jar", type=Path, required=True)
    parser.add_argument("--target", type=Path, required=True)
    parser.add_argument("--target-id", required=True)
    parser.add_argument("--commit", required=True)
    parser.add_argument("--receipt", type=Path, required=True)
    parser.add_argument("--mode", choices=("validate", "prepare-review"), default="validate")
    parser.add_argument("--port", type=int, default=25590)
    parser.add_argument("--java", default="java")
    args = parser.parse_args()
    paper = args.paper_jar.resolve()
    plugin = args.plugin_jar.resolve()
    target = args.target.resolve()
    if base.sha256(paper) != base.PAPER_EXPECTED_SHA256:
        raise ValueError("Paper JAR does not match platform-confirmed stable build 132")
    verify_authorities()
    configure(target, paper, plugin, args.target_id, args.commit, args.port)
    evidence, restart_lines = (exercise_validation(target, args.java)
                               if args.mode == "validate" else prepare_review(target, args.java))
    world_hash, package_hash, package = package_world(target)
    journal = target / "plugins/Observance/m3-private-slice-vnext.journal"
    if args.mode == "validate" and not journal.is_file():
        raise FileNotFoundError("validation completed without the required durable vNext journal")
    first_log = target / ("m3-vnext-first-start.log" if args.mode == "validate"
                          else "m3-vnext-review-prepare.log")
    restart_log = target / "m3-vnext-restart.log"
    receipt = {
        "schema_version": "1.0.0-m3-vnext-paper-receipt",
        "scope": "disposable local private Paper target; never production",
        "mode": args.mode, "target_id": args.target_id, "target_path": str(target),
        "source_git_commit": args.commit, "manifest_version": base.MANIFEST_VERSION,
        "authority_id": AUTHORITY_ID, "predicate_raw_sha256": base.PREDICATE_RAW_SHA256,
        "coarse_authority_sha256": base.COARSE_AUTHORITY_SHA256,
        "case_authority_sha256": CASE_SHA256,
        "paper": {"version": base.PAPER_VERSION, "build": base.PAPER_BUILD,
                  "jar_sha256": base.sha256(paper)},
        "plugin_jar_sha256": base.sha256(plugin), "world_tree_sha256": world_hash,
        "world_package_sha256": package_hash, "world_package_name": package.name,
        "journal_sha256": base.sha256(journal) if journal.is_file() else None,
        "journal_state": "present" if journal.is_file() else "absent_pristine_review_target",
        "log_sha256": base.sha256(first_log),
        "restart_log_sha256": base.sha256(restart_log) if restart_lines else None,
        "evidence": evidence,
        "server_configuration": {"bind":"127.0.0.1","port":args.port,"online_mode":False,
            "whitelist":False,"force_gamemode":True,"gamemode":"adventure","default_op":False,
            "inventory_escrow":False,"production_credentials_loaded":False},
        "client_receipts": {"server_side_prompt_budget":"passed_for_five_free_text_prompts",
            "non_op_adventure_join":None,"human_player_view_polish":None,
            "reason":"Automated Paper paths pass; no unseen build is human-approved."},
        "brad_visual_approval": None,
        "private_automated_staging": "authorized",
        "public_or_production_launch": False,
        "production_mutated": False,
    }
    base.write_text(args.receipt.resolve(), json.dumps(receipt, indent=2, sort_keys=True) + "\n")
    print(json.dumps(receipt, indent=2, sort_keys=True))


if __name__ == "__main__":
    main()
