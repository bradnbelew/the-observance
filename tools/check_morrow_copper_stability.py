#!/usr/bin/env python3
"""Fail closed over Morrow's authored-copper aging policy and retained live stress proof."""

from __future__ import annotations

import hashlib
import json
from pathlib import Path


ROOT = Path(__file__).resolve().parents[1]
RECEIPT = ROOT / "morrow/rehearsal/copper-stability/latest.json"
SOURCE = "28c6810d8b770a939cd86fc8eb8be7a49aa39023"


def require(condition: bool, message: str) -> None:
    if not condition:
        raise SystemExit(f"MORROW COPPER STABILITY: FAIL {message}")


def sha256(path: Path) -> str:
    # These console logs are repository text (`eol=lf`). Normalize a Windows
    # working tree before comparing with the exact staged/repository blob hash.
    return hashlib.sha256(path.read_bytes().replace(b"\r\n", b"\n")).hexdigest()


require(RECEIPT.is_file(), "receipt missing")
receipt = json.loads(RECEIPT.read_text(encoding="utf-8"))
require(receipt.get("status") == "pass" and receipt.get("source_commit") == SOURCE, "status/source")
policy = receipt.get("policy", {})
require(policy.get("weathering_families") == ["block", "cut", "chiseled", "grate", "bulb"], "weathering families")
require(policy.get("bounded_structures") == 9, "bounded structure count")
require(policy.get("untouched_transitions") == ["waxed", "foreign", "reverse-stage", "outside Morrow bounds"], "fail-closed boundary")

artifacts = receipt.get("artifacts", {})
require(set(artifacts) == {"room04_failure", "m05_failure", "random_tick_stress", "post_stress_restart"}, "artifact set")
texts: dict[str, str] = {}
for name, artifact in artifacts.items():
    path = ROOT / artifact.get("path", "")
    require(path.is_file(), f"missing artifact {name}")
    require(sha256(path) == artifact.get("sha256"), f"artifact hash {name}")
    texts[name] = path.read_text(encoding="utf-8", errors="replace")

require("expected=minecraft:cut_copper actual=minecraft:exposed_cut_copper" in texts["room04_failure"], "Room04 failure reproduction")
require("expected=minecraft:copper_grate actual=minecraft:exposed_copper_grate" in texts["m05_failure"], "M05 failure reproduction")
stress = texts["random_tick_stress"]
require("Gamerule random_tick_speed is now set to: 1000" in stress, "accelerated random ticks")
require("Gamerule random_tick_speed is now set to: 3" in stress, "random tick restoration")
require("MORROW_RUNTIME_READY" in stress and "MORROW_RUNTIME_CLOSED entities_and_tasks=cleaned" in stress, "stress lifecycle")

restart = texts["post_stress_restart"]
for marker in (
    "MORROW_ROOM04_READY status=ALREADY_PRESENT",
    "MORROW_VERSION_ROOMS_READY status=ALREADY_PRESENT",
    "MORROW_CONSENSUS_AUDIT_READY status=ALREADY_PRESENT",
    "MORROW_WITNESS_ANCHOR_READY status=ALREADY_PRESENT",
    "MORROW_ALMOST_HOME_READY status=ALREADY_PRESENT",
    "MORROW_ACCOUNT_CONTINUITY_READY status=ALREADY_PRESENT",
    "MORROW_MAINTENANCE_WINDOW_READY status=ALREADY_PRESENT",
    "MORROW_COLD_STORAGE_READY status=ALREADY_PRESENT",
    "MORROW_BRANCH_GOVERNANCE_READY status=ALREADY_PRESENT",
    "MORROW_RUNTIME_READY",
    "MORROW_RUNTIME_CLOSED entities_and_tasks=cleaned",
):
    require(marker in restart, f"post-stress restart omitted {marker}")
require("copper_aging_repairs=0" in restart, "post-stress repair count")

policy_source = (ROOT / "plugin/src/main/java/com/observance/watcher/morrow/CopperAgingPolicy.java").read_text(encoding="utf-8")
listener_source = (ROOT / "plugin/src/main/java/com/observance/watcher/morrow/BukkitMorrowCopperStability.java").read_text(encoding="utf-8")
runtime_source = (ROOT / "plugin/src/main/java/com/observance/watcher/morrow/MorrowRuntime.java").read_text(encoding="utf-8")
test_source = (ROOT / "plugin/src/test/java/com/observance/watcher/morrow/CopperAgingPolicySelfTest.java").read_text(encoding="utf-8")
for token in ("copper_block", "cut_copper", "chiseled_copper", "copper_grate", "copper_bulb"):
    require(token in policy_source, f"policy omitted {token}")
require("target.index() > source.index()" in policy_source, "forward-stage-only policy")
require('key.startsWith("waxed_")' in policy_source, "waxed refusal")
require("BlockFormEvent" in listener_source and "event.setCancelled(true)" in listener_source, "live form guard")
require("CopperAgingPolicy.isNaturalAging(before, after)" in listener_source, "listener policy binding")
require(runtime_source.count("copperStability.addBounds(") == 9, "all structure bounds registration")
require("copperStability.start();" in runtime_source and runtime_source.count("copperStability.close();") >= 2, "listener lifecycle")
require("MORROW COPPER AGING POLICY: PASS families=5 foreign-drift=refused" in test_source, "policy self-test")
for relative in (
    "versionrooms/VersionRoomsInstaller.java",
    "consensus/ConsensusAuditInstaller.java",
    "witnessanchor/WitnessAnchorInstaller.java",
    "almost/AlmostHomeInstaller.java",
    "continuity/AccountContinuityInstaller.java",
    "maintenance/MaintenanceWindowInstaller.java",
    "coldstorage/ColdStorageInstaller.java",
    "branch/BranchGovernanceInstaller.java",
):
    text = (ROOT / "plugin/src/main/java/com/observance/watcher/morrow" / relative).read_text(encoding="utf-8")
    require("CopperAgingPolicy.restoreNaturalAging(" in text, f"receipt repair omitted {relative}")
room04 = (ROOT / "plugin/src/main/java/com/observance/watcher/morrow/room04/RecoveryRoom04Installer.java").read_text(encoding="utf-8")
require("CopperAgingPolicy.isNaturalAging" in room04 and "naturalAgingRepairs" in room04, "Room04 receipt repair")

proof = receipt.get("proof", {})
for claim in (
    "room04_cut_copper_failure_reproduced", "m05_copper_grate_failure_reproduced",
    "receipt_bound_startup_repair", "live_block_form_guard",
    "all_structures_ready_before_stress", "all_structures_ready_after_stress_restart",
    "clean_shutdowns",
):
    require(proof.get(claim) is True, f"proof claim {claim}")
require(proof.get("random_tick_speed") == 1000 and proof.get("random_tick_speed_restored") == 3, "stress settings")
require(proof.get("post_stress_room04_repairs") == 0, "post-stress repairs")
require(proof.get("production_mutated") is False, "production boundary")

print("MORROW COPPER STABILITY: PASS families=5 structures=9 stress=1000 restart=stable")
