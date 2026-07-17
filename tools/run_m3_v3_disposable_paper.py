#!/usr/bin/env python3
"""Create-only Paper 1.21.11 validation and localhost review targets for M3 v3."""
from __future__ import annotations

import argparse
import json
from pathlib import Path

import run_m3_disposable_paper as base


ROOT = Path(__file__).resolve().parents[1]
SLICE_AUTHORITY_SHA256 = "0181b5566ea49a653b9cc95a650246c52ce670735d6ec2d6e4b1f6b9bc2a7ae5"
AUTHORITY_ID = "observance-p4-private-slice-v3"


def verify_authorities() -> None:
    coarse = base.canonical_sha256(ROOT / "design" / "m3" / "coarse-adjacency-v1.json")
    sliced = base.canonical_sha256(ROOT / "design" / "m3" / "vertical-slice-v3.json")
    if coarse != base.COARSE_AUTHORITY_SHA256:
        raise ValueError(f"coarse authority hash drift: {coarse}")
    if sliced != SLICE_AUTHORITY_SHA256:
        raise ValueError(f"v3 slice authority hash drift: {sliced}")


def configure(target: Path, paper: Path, plugin: Path, target_id: str, commit: str, port: int) -> None:
    base.configure(target, paper, plugin, target_id, commit, port)
    properties = target / "server.properties"
    body = properties.read_text(encoding="utf-8")
    base.write_text(properties, body.replace("PRIVATE M3 V2 DISPOSABLE REVIEW",
                                             "PRIVATE M3 V3 DISPOSABLE REVIEW"))


def package_world(target: Path) -> tuple[str, str, Path]:
    world_hash, _, old_package = base.package_world(target)
    package = target / "m3-private-slice-v3-world.zip"
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
    parser.add_argument("--port", type=int, default=25579)
    parser.add_argument("--java", default="java")
    args = parser.parse_args()
    paper = args.paper_jar.resolve()
    plugin = args.plugin_jar.resolve()
    target = args.target.resolve()
    if base.sha256(paper) != base.PAPER_EXPECTED_SHA256:
        raise ValueError("Paper JAR does not match platform-confirmed stable build 132")
    verify_authorities()
    configure(target, paper, plugin, args.target_id, args.commit, args.port)
    evidence, _, second_lines = (base.exercise_validation(target, args.java)
                                 if args.mode == "validate" else base.prepare_review(target, args.java))
    world_hash, package_hash, package = package_world(target)
    journal = target / "plugins" / "Observance" / "m3-private-slice-v3.journal"
    if args.mode == "validate" and not journal.is_file():
        raise FileNotFoundError("validation completed without the required durable M3 v3 journal")
    receipt = {
        "schema_version": "3.0.0-m3-paper-receipt" if args.mode == "validate"
            else "3.0.0-m3-review-server-receipt",
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
        "log_sha256": base.sha256(target / ("m3-first-start.log" if args.mode == "validate"
                                             else "m3-review-prepare.log")),
        "restart_log_sha256": base.sha256(target / "m3-restart.log") if second_lines else None,
        "evidence": evidence,
        "server_configuration": {
            "bind": "127.0.0.1", "port": args.port, "online_mode": False, "whitelist": False,
            "force_gamemode": True, "gamemode": "adventure", "default_op": False,
            "inventory_escrow": False, "production_credentials_loaded": False,
        },
        "client_receipts": {
            "non_op_adventure_join": None, "protected_region_bypass": None,
            "two_client_asymmetry": None, "solo_accessibility": None,
            "reason": "Server configuration, pure state tests, and Paper security readback exist; no authenticated Minecraft client was automated.",
        },
        "brad_visual_approval": None,
        "brad_visual_status": "pending_v3_re_review_after_revision",
        "m4_authority": "closed",
    }
    base.write_text(args.receipt.resolve(), json.dumps(receipt, indent=2, sort_keys=True) + "\n")
    print(json.dumps(receipt, indent=2, sort_keys=True))


if __name__ == "__main__":
    main()
