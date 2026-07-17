#!/usr/bin/env python3
"""Create-only Paper 1.21.11 validation and localhost review targets for M3 V5."""
from __future__ import annotations

import argparse
import json
from pathlib import Path

import run_m3_disposable_paper as base
from run_m3_v4_disposable_paper import wait_for_confirmation


ROOT = Path(__file__).resolve().parents[1]
SLICE_AUTHORITY_SHA256 = "a0580902b8f8633579820f1a5adf7419d0151b1259026c554b2d6974c1a95e1c"
AUTHORITY_ID = "observance-p4-private-slice-v5"


def verify_authorities() -> None:
    coarse = base.canonical_sha256(ROOT / "design/m3/coarse-adjacency-v1.json")
    sliced = base.canonical_sha256(ROOT / "design/m3/vertical-slice-v5.json")
    if coarse != base.COARSE_AUTHORITY_SHA256:
        raise ValueError(f"coarse authority hash drift: {coarse}")
    if sliced != SLICE_AUTHORITY_SHA256:
        raise ValueError(f"V5 slice authority hash drift: {sliced}")


def configure(target: Path, paper: Path, plugin: Path, target_id: str, commit: str, port: int) -> None:
    base.configure(target, paper, plugin, target_id, commit, port)
    properties = target / "server.properties"
    body = properties.read_text(encoding="utf-8")
    base.write_text(properties, body.replace("PRIVATE M3 V2 DISPOSABLE REVIEW",
                                             "PRIVATE M3 V5 DISPOSABLE REVIEW"))


def observe(process: base.PaperProcess, finding: str, source: str) -> str:
    return process.command(f"obsm3 observe {finding} {source} harness readback",
                           "M3_OBSERVATION_COMMITTED")


def exercise_validation(target: Path, java: str) -> tuple[dict[str, object], list[str]]:
    epoch = 1_789_100_000
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
        observations = [
            observe(first, "P4.F3", "ration_tally"),
            observe(first, "P4.F1", "cart_rut_tag"),
            observe(first, "P4.F4", "engineer_letter"),
            observe(first, "P4.F2", "mason_mark"),
            observe(first, "P4.F1", "drainage_plan"),
            observe(first, "P4.F4", "pump_gauge"),
            observe(first, "P4.F2", "revision_letter"),
            observe(first, "P4.F3", "berth_register"),
        ]
        naive = first.command(f"obsm3 naive-negative naive {epoch}", "M3_NAIVE_NEGATIVE_PASS")
        after_naive = first.command("obsm3 audit", "M3_AUDIT PASS")
        brute = first.command(f"obsm3 brute-negative blind {epoch + 10}", "M3_BRUTE_NEGATIVE_PASS")
        after_brute = first.command("obsm3 audit", "M3_AUDIT PASS")
        first.command("save-all flush", "Saved the game")
    finally:
        first.stop()
    base.write_text(target / "m3-v5-first-start.log", "\n".join(first.lines) + "\n")

    second = base.PaperProcess(target, java)
    try:
        negative_restart_confirmation = wait_for_confirmation(second)
        second.wait_for("Done (", 300)
        negative_restart_audit = second.command("obsm3 audit", "M3_AUDIT PASS")
        persistent_throttle = second.command(
            f"obsm3 report-correct blind {epoch + 14}", "M3_FAIL action=report-correct")
        after_throttle = second.command("obsm3 audit", "M3_AUDIT PASS")
        report = second.command(f"obsm3 report-correct reasoned {epoch + 1000}",
                                "M3_REPORT_CORRECT_PASS")
        report_audit = second.command("obsm3 audit", "M3_AUDIT PASS")
        watcher = second.command("obsm3 watcher-approve v5-harness WestReviewer EastReviewer 10",
                                 "M3_WATCHER_APPROVED")
        synthesis = second.command(f"obsm3 synthesis-correct reasoned {epoch + 1001}",
                                   "M3_SYNTHESIS_CORRECT_PASS")
        opened = second.command("obsm3 audit", "M3_AUDIT PASS")
        security_open = second.command("obsm3 security", "M3_SECURITY_PASS")
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
    base.write_text(target / "m3-v5-restart.log", "\n".join(restart_lines) + "\n")
    return {
        "platform_confirmation": confirmation, "status": status, "build": build,
        "closed_audit": closed, "security_closed": security_closed,
        "ui_audit": ui_audit, "guided_client_model": guided,
        "observations": observations, "naive_negative": naive,
        "after_naive_audit": after_naive, "brute_negative": brute,
        "after_brute_audit": after_brute,
        "negative_restart_confirmation": negative_restart_confirmation,
        "negative_restart_audit": negative_restart_audit,
        "persistent_throttle": persistent_throttle,
        "after_persistent_throttle_audit": after_throttle,
        "report_correct": report, "report_closed_audit": report_audit,
        "watcher_approval": watcher, "synthesis_correct": synthesis,
        "open_audit": opened, "security_open": security_open, "replay": replay,
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
    base.write_text(target / "m3-v5-review-prepare.log", "\n".join(process.lines) + "\n")
    return {"platform_confirmation": confirmation, "status": status, "build": build,
            "closed_audit": audit, "security": security, "ui_audit": ui_audit,
            "guided_client_model": guided}, []


def package_world(target: Path) -> tuple[str, str, Path]:
    world_hash, _, old_package = base.package_world(target)
    package = target / "m3-private-slice-v5-world.zip"
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
    parser.add_argument("--port", type=int, default=25587)
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
    journal = target / "plugins/Observance/m3-private-slice-v5.journal"
    if args.mode == "validate" and not journal.is_file():
        raise FileNotFoundError("validation completed without the required durable M3 V5 journal")
    first_log = target / ("m3-v5-first-start.log" if args.mode == "validate"
                          else "m3-v5-review-prepare.log")
    restart_log = target / "m3-v5-restart.log"
    receipt = {
        "schema_version": "5.0.0-m3-paper-receipt" if args.mode == "validate"
            else "5.0.0-m3-review-server-receipt",
        "scope": "disposable local private Paper target; never production",
        "mode": args.mode, "target_id": args.target_id, "target_path": str(target),
        "source_git_commit": args.commit, "manifest_version": base.MANIFEST_VERSION,
        "authority_id": AUTHORITY_ID, "predicate_raw_sha256": base.PREDICATE_RAW_SHA256,
        "coarse_authority_sha256": base.COARSE_AUTHORITY_SHA256,
        "slice_authority_sha256": SLICE_AUTHORITY_SHA256,
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
        "client_receipts": {
            "exact_render_and_click_model": "passed_server-side_for_20_complete_clause_pages",
            "non_op_adventure_join": None, "human_player_view_polish": None,
            "reason": "Automated model and Paper paths pass; Brad's client walk remains the visual authority."
        },
        "brad_visual_approval": None,
        "brad_visual_status": "pending_v5_review_after_revision",
        "m4_authority": "closed", "production_mutated": False,
    }
    base.write_text(args.receipt.resolve(), json.dumps(receipt, indent=2, sort_keys=True) + "\n")
    print(json.dumps(receipt, indent=2, sort_keys=True))


if __name__ == "__main__":
    main()
