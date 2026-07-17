#!/usr/bin/env python3
"""Create-only Paper 1.21.11 validation and localhost review targets for M3 v4."""
from __future__ import annotations

import argparse
import json
from pathlib import Path
import queue
import time

import run_m3_disposable_paper as base


ROOT = Path(__file__).resolve().parents[1]
SLICE_AUTHORITY_SHA256 = "444926db844dfd5e06bd131a3c941a23b85a575c1b56ce09673f690aa5d88b3f"
AUTHORITY_ID = "observance-p4-private-slice-v4"


def wait_for_confirmation(process: base.PaperProcess, timeout: float = 300.0) -> str:
    deadline = time.monotonic() + timeout
    while time.monotonic() < deadline:
        if process.process.poll() is not None:
            raise RuntimeError(f"Paper exited before M3 authority confirmation; exit={process.process.returncode}")
        try:
            line = process.events.get(timeout=0.5)
        except queue.Empty:
            continue
        if "M3_TARGET_CONFIRMED" in line:
            return line
        if "M3 private review mode failed closed:" in line:
            raise RuntimeError(line)
    raise TimeoutError("timed out waiting for M3 authority confirmation")


def verify_authorities() -> None:
    coarse = base.canonical_sha256(ROOT / "design" / "m3" / "coarse-adjacency-v1.json")
    sliced = base.canonical_sha256(ROOT / "design" / "m3" / "vertical-slice-v4.json")
    if coarse != base.COARSE_AUTHORITY_SHA256:
        raise ValueError(f"coarse authority hash drift: {coarse}")
    if sliced != SLICE_AUTHORITY_SHA256:
        raise ValueError(f"v4 slice authority hash drift: {sliced}")


def configure(target: Path, paper: Path, plugin: Path, target_id: str, commit: str, port: int) -> None:
    base.configure(target, paper, plugin, target_id, commit, port)
    properties = target / "server.properties"
    body = properties.read_text(encoding="utf-8")
    base.write_text(properties, body.replace("PRIVATE M3 V2 DISPOSABLE REVIEW",
                                             "PRIVATE M3 V4 DISPOSABLE REVIEW"))


def observe(process: base.PaperProcess, finding: str, source: str, contributor: str = "harness") -> str:
    return process.command(f"obsm3 observe {finding} {source} {contributor} readback",
                           "M3_OBSERVATION_COMMITTED")


def exercise_validation(target: Path, java: str) -> tuple[dict[str, object], list[str], list[str]]:
    epoch = 1_789_000_000
    first = base.PaperProcess(target, java)
    try:
        confirmation = wait_for_confirmation(first)
        first.wait_for("Done (", 300)
        status = first.command("obsm3 status", "M3_STATUS")
        build = first.command("obsm3 build", "M3_BUILD_COMPLETE", 300)
        closed = first.command("obsm3 audit", "M3_AUDIT PASS")
        security_closed = first.command("obsm3 security", "M3_SECURITY_PASS")
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
    base.write_text(target / "m3-v4-first-start.log", "\n".join(first.lines) + "\n")

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
        watcher = second.command("obsm3 watcher-approve v4-harness WestReviewer EastReviewer 10",
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
        restart_security = third.command("obsm3 security", "M3_SECURITY_PASS")
        restart_replay = third.command("obsm3 replay", "M3_REPLAY_PASS")
        final = third.command("obsm3 audit", "M3_AUDIT PASS")
        third.command("save-all flush", "Saved the game")
    finally:
        third.stop()
    restart_lines = second.lines + ["--- SECOND RESTART ---"] + third.lines
    base.write_text(target / "m3-v4-restart.log", "\n".join(restart_lines) + "\n")
    evidence: dict[str, object] = {
        "platform_confirmation": confirmation,
        "status": status,
        "build": build,
        "closed_audit": closed,
        "security_closed": security_closed,
        "observations": observations,
        "naive_negative": naive,
        "after_naive_audit": after_naive,
        "brute_negative": brute,
        "after_brute_audit": after_brute,
        "negative_restart_confirmation": negative_restart_confirmation,
        "negative_restart_audit": negative_restart_audit,
        "persistent_throttle": persistent_throttle,
        "after_persistent_throttle_audit": after_throttle,
        "report_correct": report,
        "report_closed_audit": report_audit,
        "watcher_approval": watcher,
        "synthesis_correct": synthesis,
        "open_audit": opened,
        "security_open": security_open,
        "replay": replay,
        "restart_confirmation": restart_confirmation,
        "restart_audit": restarted,
        "restart_security": restart_security,
        "restart_replay": restart_replay,
        "final_audit": final,
    }
    return evidence, first.lines, restart_lines


def prepare_review(target: Path, java: str) -> tuple[dict[str, object], list[str], list[str]]:
    process = base.PaperProcess(target, java)
    try:
        confirmation = wait_for_confirmation(process)
        process.wait_for("Done (", 300)
        status = process.command("obsm3 status", "M3_STATUS")
        build = process.command("obsm3 build", "M3_BUILD_COMPLETE", 300)
        audit = process.command("obsm3 audit", "M3_AUDIT PASS")
        security = process.command("obsm3 security", "M3_SECURITY_PASS")
        process.command("save-all flush", "Saved the game")
    finally:
        process.stop()
    base.write_text(target / "m3-v4-review-prepare.log", "\n".join(process.lines) + "\n")
    return {"platform_confirmation": confirmation, "status": status, "build": build,
            "closed_audit": audit, "security": security}, process.lines, []


def package_world(target: Path) -> tuple[str, str, Path]:
    world_hash, _, old_package = base.package_world(target)
    package = target / "m3-private-slice-v4-world.zip"
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
    parser.add_argument("--port", type=int, default=25583)
    parser.add_argument("--java", default="java")
    args = parser.parse_args()
    paper = args.paper_jar.resolve()
    plugin = args.plugin_jar.resolve()
    target = args.target.resolve()
    if base.sha256(paper) != base.PAPER_EXPECTED_SHA256:
        raise ValueError("Paper JAR does not match platform-confirmed stable build 132")
    verify_authorities()
    configure(target, paper, plugin, args.target_id, args.commit, args.port)
    evidence, _, restart_lines = (exercise_validation(target, args.java)
                                  if args.mode == "validate" else prepare_review(target, args.java))
    world_hash, package_hash, package = package_world(target)
    journal = target / "plugins" / "Observance" / "m3-private-slice-v4.journal"
    if args.mode == "validate" and not journal.is_file():
        raise FileNotFoundError("validation completed without the required durable M3 v4 journal")
    first_log = target / ("m3-v4-first-start.log" if args.mode == "validate"
                          else "m3-v4-review-prepare.log")
    restart_log = target / "m3-v4-restart.log"
    receipt = {
        "schema_version": "4.0.0-m3-paper-receipt" if args.mode == "validate"
            else "4.0.0-m3-review-server-receipt",
        "scope": "disposable local private Paper target; never player-facing or production",
        "mode": args.mode,
        "target_id": args.target_id,
        "target_path": str(target),
        "source_git_commit": args.commit,
        "manifest_version": base.MANIFEST_VERSION,
        "authority_id": AUTHORITY_ID,
        "predicate_raw_sha256": base.PREDICATE_RAW_SHA256,
        "coarse_authority_sha256": base.COARSE_AUTHORITY_SHA256,
        "slice_authority_sha256": SLICE_AUTHORITY_SHA256,
        "paper": {"version": base.PAPER_VERSION, "build": base.PAPER_BUILD,
                  "jar_sha256": base.sha256(paper)},
        "plugin_jar_sha256": base.sha256(plugin),
        "world_tree_sha256": world_hash,
        "world_package_sha256": package_hash,
        "world_package_name": package.name,
        "journal_sha256": base.sha256(journal) if journal.is_file() else None,
        "journal_state": "present" if journal.is_file() else "absent_pristine_review_target",
        "log_sha256": base.sha256(first_log),
        "restart_log_sha256": base.sha256(restart_log) if restart_lines else None,
        "evidence": evidence,
        "server_configuration": {
            "bind": "127.0.0.1", "port": args.port, "online_mode": False, "whitelist": False,
            "force_gamemode": True, "gamemode": "adventure", "default_op": False,
            "inventory_escrow": False, "production_credentials_loaded": False,
        },
        "client_receipts": {
            "non_op_adventure_join": None,
            "protected_region_bypass": None,
            "two_client_asymmetry": None,
            "solo_accessibility": None,
            "cold_player_comprehension": None,
            "reason": "Exact server-side state, Paper, geometry, security, restart, and negative-flow receipts exist; Brad's next client walk remains approval authority.",
        },
        "brad_visual_approval": None,
        "brad_visual_status": "pending_v4_review_after_revision",
        "m4_authority": "closed",
    }
    base.write_text(args.receipt.resolve(), json.dumps(receipt, indent=2, sort_keys=True) + "\n")
    print(json.dumps(receipt, indent=2, sort_keys=True))


if __name__ == "__main__":
    main()
