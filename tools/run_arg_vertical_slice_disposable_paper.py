#!/usr/bin/env python3
"""Create-only fresh Paper 1.21.11 validation for the real-input P4/P5 ARG slice."""

from __future__ import annotations

import argparse
import json
from pathlib import Path

import run_m3_disposable_paper as base
import run_m3_vnext_disposable_paper as vnext
from run_m3_v4_disposable_paper import wait_for_confirmation

ROOT = Path(__file__).resolve().parents[1]
AUTHORITY = ROOT / "design/m3/P4-P5-ARG-VERTICAL-SLICE.json"


def stop_if_running(process: base.PaperProcess) -> None:
    if process.process.poll() is None:
        process.stop()


def configure(target: Path, paper: Path, plugin: Path, target_id: str, commit: str, port: int) -> None:
    vnext.configure(target, paper, plugin, target_id, commit, port)
    config = target / "plugins/Observance/config.yml"
    body = config.read_text(encoding="utf-8")
    body = body.replace("  source-git-commit:",
                        "  arg-experience:\n    enabled: true\n    dialog-enabled: true\n  source-git-commit:")
    base.write_text(config, body)


def run_lifecycle(target: Path, java: str) -> tuple[dict[str, str], list[str]]:
    first = base.PaperProcess(target, java)
    try:
        runtime_ready = first.wait_for("ARG_VERTICAL_SLICE_READY", 300)
        confirmation = wait_for_confirmation(first)
        first.wait_for("Done (", 300)
        build = first.command("obsm3 build", "M3_BUILD_COMPLETE", 300)
        initial = first.command("obsm3 arg-status", "M3_ARG_STATUS theory=false")
        closed = first.command("obsm3 audit", "M3_AUDIT PASS")
        security_closed = first.command("obsm3 security", "M3_SECURITY_PASS")
        wrong = first.command("obsm3 arg-theory the mouth was only a cult ruin", "M3_ARG_THEORY result=wrong")
        after_wrong = first.command("obsm3 arg-status", "M3_ARG_STATUS theory=false")
        wrong_audit = first.command("obsm3 audit", "M3_AUDIT PASS")
        first.command("save-all flush", "Saved the game")
    except BaseException:
        base.write_text(target / "arg-slice-first-start.failed.log", "\n".join(first.lines) + "\n")
        raise
    finally:
        stop_if_running(first)
    base.write_text(target / "arg-slice-first-start.log", "\n".join(first.lines) + "\n")

    second = base.PaperProcess(target, java)
    try:
        second_ready = second.wait_for("ARG_VERTICAL_SLICE_READY", 300)
        second_confirmation = wait_for_confirmation(second)
        second.wait_for("Done (", 300)
        before_correct = second.command("obsm3 arg-status", "M3_ARG_STATUS theory=false")
        correct = second.command(
            "obsm3 arg-theory the hold sheltered families before safety became control",
            "M3_ARG_THEORY result=accepted")
        open_audit = second.command("obsm3 audit", "M3_AUDIT PASS")
        security_open = second.command("obsm3 security", "M3_SECURITY_PASS")
        service = second.command("obsm3 arg-select-service", "M3_ARG_SELECT control=service_public result=accepted")
        partial = second.command("obsm3 arg-status", "service_public=true penalty_custody=false curated=false")
        penalty = second.command("obsm3 arg-select-penalty", "M3_ARG_SELECT control=penalty_custody result=accepted")
        curated = second.command("obsm3 arg-status", "service_public=true penalty_custody=true curated=true")
        curated_audit = second.command("obsm3 audit", "M3_AUDIT PASS")
        second.command("save-all flush", "Saved the game")
    except BaseException:
        base.write_text(target / "arg-slice-second-start.failed.log", "\n".join(second.lines) + "\n")
        raise
    finally:
        stop_if_running(second)

    third = base.PaperProcess(target, java)
    try:
        restart_ready = third.wait_for("ARG_VERTICAL_SLICE_READY", 300)
        restart_confirmation = wait_for_confirmation(third)
        third.wait_for("Done (", 300)
        restarted = third.command("obsm3 arg-status", "theory=true service_public=true penalty_custody=true curated=true")
        restart_audit = third.command("obsm3 audit", "M3_AUDIT PASS")
        restart_security = third.command("obsm3 security", "M3_SECURITY_PASS")
        duplicate = third.command(
            "obsm3 arg-theory the hold sheltered families before safety became control",
            "M3_ARG_THEORY result=accepted")
        idempotent = third.command("obsm3 arg-status", "receipts=4")
        final = third.command("obsm3 audit", "M3_AUDIT PASS")
        third.command("save-all flush", "Saved the game")
    except BaseException:
        base.write_text(target / "arg-slice-third-start.failed.log", "\n".join(third.lines) + "\n")
        raise
    finally:
        stop_if_running(third)
    restart_lines = second.lines + ["--- THIRD START ---"] + third.lines
    base.write_text(target / "arg-slice-restart.log", "\n".join(restart_lines) + "\n")
    return {
        "platform_confirmation": confirmation, "runtime_ready": runtime_ready, "build": build,
        "initial_state": initial, "closed_audit": closed, "security_closed": security_closed,
        "wrong_theory": wrong, "state_after_wrong": after_wrong, "wrong_audit": wrong_audit,
        "second_runtime_ready": second_ready, "second_confirmation": second_confirmation,
        "before_zero_observation_correct": before_correct,
        "zero_observation_correct": correct, "open_audit": open_audit, "security_open": security_open,
        "service_selection": service, "partial_curation": partial, "penalty_selection": penalty,
        "curated_state": curated, "curated_audit": curated_audit,
        "restart_confirmation": restart_confirmation, "restart_runtime_ready": restart_ready,
        "restart_state": restarted, "restart_audit": restart_audit, "restart_security": restart_security,
        "duplicate_correct": duplicate, "idempotent_receipts": idempotent, "final_audit": final,
    }, restart_lines


def main() -> None:
    parser = argparse.ArgumentParser()
    parser.add_argument("--paper-jar", type=Path, required=True)
    parser.add_argument("--plugin-jar", type=Path, required=True)
    parser.add_argument("--target", type=Path, required=True)
    parser.add_argument("--target-id", required=True)
    parser.add_argument("--commit", required=True)
    parser.add_argument("--receipt", type=Path, required=True)
    parser.add_argument("--port", type=int, required=True)
    parser.add_argument("--java", default="java")
    args = parser.parse_args()
    authority = json.loads(AUTHORITY.read_text(encoding="utf-8"))
    if authority["brad_approval"] is not None or authority["target"] != "disposable_private_paper_1.21.11_only":
        raise ValueError("ARG vertical-slice authority drift")
    paper = args.paper_jar.resolve()
    plugin = args.plugin_jar.resolve()
    target = args.target.resolve()
    if base.sha256(paper) != base.PAPER_EXPECTED_SHA256:
        raise ValueError("Paper JAR does not match platform-confirmed stable build 132")
    configure(target, paper, plugin, args.target_id, args.commit, args.port)
    evidence, restart_lines = run_lifecycle(target, args.java)
    world_hash, package_hash, package = vnext.package_world(target)
    journal = target / "plugins/Observance/arg-p4-p5-vertical-slice.journal"
    if not journal.is_file():
        raise FileNotFoundError("ARG validation completed without its durable local-primary journal")
    receipt = {
        "schema_version": "1.0.0-p4-p5-arg-paper-receipt",
        "scope": "fresh disposable localhost Paper target; never production",
        "target_id": args.target_id, "target_path": str(target), "source_git_commit": args.commit,
        "authority_sha256": base.sha256(AUTHORITY),
        "paper": {"version": base.PAPER_VERSION, "build": base.PAPER_BUILD,
                  "jar_sha256": base.sha256(paper)},
        "plugin_jar_sha256": base.sha256(plugin), "world_tree_sha256": world_hash,
        "world_package_sha256": package_hash, "world_package_name": package.name,
        "journal_sha256": base.sha256(journal),
        "first_log_sha256": base.sha256(target / "arg-slice-first-start.log"),
        "restart_log_sha256": base.sha256(target / "arg-slice-restart.log"),
        "evidence": evidence,
        "server_configuration": {"bind": "127.0.0.1", "port": args.port, "online_mode": False,
            "force_gamemode": True, "gamemode": "adventure", "default_op": False,
            "arg_dialog_enabled": True, "production_credentials_loaded": False},
        "client_receipts": {"actual_dialog_visual": None, "non_op_adventure_join": None,
            "human_experience": None, "reason": "Exact API runtime startup and server paths pass; a real client walk remains required."},
        "brad_approval": None, "production_mutated": False, "public_launch": False,
    }
    base.write_text(args.receipt.resolve(), json.dumps(receipt, indent=2, sort_keys=True) + "\n")
    print(json.dumps(receipt, indent=2, sort_keys=True))


if __name__ == "__main__":
    main()
