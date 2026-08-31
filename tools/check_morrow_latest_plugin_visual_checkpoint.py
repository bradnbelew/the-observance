#!/usr/bin/env python3
"""Fail closed over the latest-plugin d738788 M02 interaction checkpoint."""

from __future__ import annotations

import base64
import hashlib
import json
import subprocess
from pathlib import Path


ROOT = Path(__file__).resolve().parents[1]
CHECKPOINT = ROOT / "morrow/rehearsal/client-visual/2026-08-31-latest-plugin-d738788-m02-interaction-checkpoint.json"
SOURCE = "d7387880d8fe2e2d74235160fab2acd109bccca9"
PLUGIN_SHA256 = "ea2cbdfd56eca83580c1dcf71722c3391f85e556c3ea4c25f4cc69561e0bf342"
PAPER_SHA256 = "5ffef465eeeb5f2a3c23a24419d97c51afd7dbb4923ff42df9a3f58bba1ccfba"


def require(condition: bool, message: str) -> None:
    if not condition:
        raise SystemExit(f"MORROW LATEST-PLUGIN VISUAL CHECKPOINT: FAIL {message}")


def load_json(path: Path) -> dict:
    require(path.is_file(), f"missing {path.relative_to(ROOT)}")
    return json.loads(path.read_text(encoding="utf-8"))


def sha256(path: Path) -> str:
    digest = hashlib.sha256()
    with path.open("rb") as handle:
        for chunk in iter(lambda: handle.read(1024 * 1024), b""):
            digest.update(chunk)
    return digest.hexdigest()


checkpoint = load_json(CHECKPOINT)
require(checkpoint.get("status") == "latest_plugin_m02_apply_wrong_correct_pass", "status")
require(checkpoint.get("plugin_source_commit") == SOURCE, "plugin source")
require(checkpoint.get("launcher_source_commit") == SOURCE, "launcher source")
require(subprocess.run(["git", "merge-base", "--is-ancestor", SOURCE, "HEAD"], cwd=ROOT).returncode == 0,
        "source commit is not an ancestor of HEAD")
runtime = checkpoint.get("runtime", {})
require(runtime.get("paper_version") == "1.21.11" and runtime.get("paper_build") == 132, "Paper identity")
require(runtime.get("paper_sha256") == PAPER_SHA256, "Paper hash")
require(runtime.get("plugin_sha256") == PLUGIN_SHA256, "plugin hash")
require(runtime.get("interaction_server") == "127.0.0.1:25597", "interaction server")
require(runtime.get("lifecycle_server") == "127.0.0.1:25598", "lifecycle server")
require(runtime.get("production_credentials_loaded") is False, "production credentials boundary")
require(runtime.get("resource_pack_policy") == "disabled", "resource-pack disclosure")

artifacts = checkpoint.get("artifacts", {})
required_artifacts = {
    "pre_image", "pre_capture_receipt", "applied_image", "applied_capture_receipt",
    "apply_client_receipt", "apply_client_log", "b06_dialog_image", "b06_dialog_capture_receipt",
    "b06_wrong_image", "b06_wrong_capture_receipt", "b06_correct_image",
    "b06_correct_capture_receipt", "classify_client_receipt", "classify_client_log",
    "server_log", "journal", "paper_runtime_receipt",
}
require(set(artifacts) == required_artifacts, "artifact set")
for name, artifact in artifacts.items():
    path = ROOT / artifact.get("path", "")
    require(path.is_file(), f"missing artifact {name}")
    require(sha256(path) == artifact.get("sha256"), f"artifact hash {name}")

capture_contract = checkpoint.get("capture", {})
require(capture_contract.get("computer_use_input_sent") is True, "Computer Use action disclosure")
require(capture_contract.get("fallback_input_injected") is False, "fallback input boundary")
require("0x80004002" in capture_contract.get("computer_use_wgc_result", ""), "capture incompatibility disclosure")
require(len(capture_contract.get("computer_use_actions", [])) == 6, "bounded action set")
require(capture_contract.get("classification_tutorial_overlay_present") is True, "tutorial overlay disclosure")
require("teleport" in capture_contract.get("classification_positioning_assist", "").lower(),
        "positioning assist disclosure")


def check_capture(prefix: str, expected_pid: int) -> None:
    capture = load_json(ROOT / artifacts[f"{prefix}_capture_receipt"]["path"])
    image = capture.get("image", {})
    window = capture.get("window", {})
    require(capture.get("input_injected") is False, f"{prefix} capture input boundary")
    require(window.get("title") == capture_contract["window_title"], f"{prefix} window title")
    require(window.get("pid") == expected_pid, f"{prefix} PID binding")
    require(window.get("minimized") is False, f"{prefix} window visibility")
    require(image.get("path") == artifacts[f"{prefix}_image"]["path"], f"{prefix} retained path")
    require(image.get("sha256") == artifacts[f"{prefix}_image"]["sha256"], f"{prefix} image binding")
    require(image.get("width") == 1282 and image.get("height") == 752, f"{prefix} dimensions")
    require(image.get("grayscale_entropy", 0) > 6, f"{prefix} entropy")


for capture_name in ("pre", "applied"):
    check_capture(capture_name, capture_contract["apply_window_pid"])
for capture_name in ("b06_dialog", "b06_wrong", "b06_correct"):
    check_capture(capture_name, capture_contract["classify_window_pid"])


def check_client(prefix: str, expected_pid: int, tutorial_disabled: bool) -> None:
    client = load_json(ROOT / artifacts[f"{prefix}_client_receipt"]["path"])
    require(client.get("process_id") == expected_pid, f"{prefix} client PID")
    require(client.get("source_commit") == SOURCE, f"{prefix} launcher source")
    require(client.get("status") == "harness_stopped_after_wait" and client.get("stop_reason") == "wait",
            f"{prefix} stop disclosure")
    require(client.get("loopback_only") is True and client.get("account_files_read") is False,
            f"{prefix} privacy boundary")
    require(client.get("production_credentials_loaded") is False, f"{prefix} production boundary")
    require(client.get("server") == runtime["interaction_server"], f"{prefix} server binding")
    require(client.get("server_resource_pack_policy") == "disabled", f"{prefix} pack policy")
    require(client.get("accessibility_profile", {}).get("tutorial_toast_disabled") is tutorial_disabled,
            f"{prefix} tutorial disclosure")
    client_log_path = ROOT / artifacts[f"{prefix}_client_log"]["path"]
    require(sha256(client_log_path) == client.get("log_sha256"), f"{prefix} client log hash")
    client_log = client_log_path.read_text(encoding="utf-8", errors="replace")
    require("Connecting to 127.0.0.1, 25597" in client_log, f"{prefix} connection evidence")
    require("MorrowWitness joined the game" in client_log, f"{prefix} join evidence")


check_client("apply", capture_contract["apply_window_pid"], True)
check_client("classify", capture_contract["classify_window_pid"], False)

server_log = (ROOT / artifacts["server_log"]["path"]).read_text(encoding="utf-8", errors="replace")
for marker in (
    "Paper version 1.21.11-132", "MORROW_ROOM04_READY status=ALREADY_PRESENT",
    "copper_aging_repairs=0", "MORROW_RUNTIME_READY",
    "MORROW_PLAYER_SAFE_ENTRY player=MorrowWitness", "inventory_mutations=0",
    "MORROW_ACCESSIBLE_CUE id=static_restore_pass_1",
    "MORROW_ACCESSIBLE_CUE id=static_restore_pass_2",
    "MORROW_ACCESSIBLE_CUE id=static_restore_pass_3",
    "MORROW_RUNTIME_CLOSED entities_and_tasks=cleaned",
):
    require(marker in server_log, f"server log omitted {marker}")
require(server_log.count("MORROW_PLAYER_SAFE_ENTRY player=MorrowWitness") >= 3, "three client joins")
require(server_log.count("Morrow fallback body drift detected; rebuilding") >= 2, "dialog-body self-heal evidence")
require("Morrow dialog consequence halted safely" not in server_log, "dialog consequence safe-halted")
require("Morrow fallback body deferred audit failed" not in server_log, "deferred body audit failed")

journal_path = ROOT / artifacts["journal"]["path"]
rows = [line.split("\t") for line in journal_path.read_text(encoding="utf-8").splitlines() if line]
require(len(rows) == 6 and all(len(row) == 6 for row in rows), "journal row shape/count")
require([int(row[0]) for row in rows] == list(range(1, 7)), "journal sequence")
require(sum(row[3] == "paper:room04:proposal:v1" for row in rows) == 1, "proposal receipt count")
require(sum(row[3] == "paper:room04:m02:intention-error:v1" for row in rows) == 1,
        "intention-error receipt count")
require(rows[-1][4] == "morrow.act1.intention_error_proven", "final journal event")
padding = "=" * (-len(rows[-1][5]) % 4)
payload = json.loads(base64.urlsafe_b64decode(rows[-1][5] + padding))
require(payload.get("classification") == "inferred" and payload.get("coordinate") == "B06",
        "B06 classification payload")
require(payload.get("world_mutation") is False and payload.get("factual_error") == "discussed_never_placed",
        "B06 factual/world-mutation payload")

paper = load_json(ROOT / artifacts["paper_runtime_receipt"]["path"])
require(paper.get("status") == "pass" and paper.get("source_commit") == SOURCE, "Paper receipt status/source")
require(paper.get("paper", {}).get("sha256") == PAPER_SHA256, "Paper receipt hash")
require(paper.get("plugin_sha256") == PLUGIN_SHA256, "Paper receipt plugin hash")
require(paper.get("server", {}).get("port") == 25598, "Paper receipt port")
for name, key in (("morrow-first-start.log", "first_sha256"), ("morrow-restart.log", "restart_sha256")):
    path = ROOT / "morrow/rehearsal/runtime/d738788-current" / name
    require(path.is_file() and sha256(path) == paper.get("logs", {}).get(key), f"Paper {name} hash")

findings = checkpoint.get("findings", {})
for finding in (
    "real_minecraft_client_rendered", "safe_entry_logged", "room04_visible", "proposal_applied",
    "all_three_restore_pass_cues_logged", "invalid_dialog_body_self_healed",
    "dialog_consequence_safe_halt_absent", "physical_b06_dialog_opened",
    "wrong_authenticated_feedback_rendered", "wrong_attempt_progress_receipt_absent",
    "correct_inferred_feedback_rendered", "canonical_intention_error_receipt_created_once",
    "b06_visual_mark_changed_after_correct_choice", "server_stopped_cleanly", "runtime_cleanup_logged",
    "tutorial_overlay_absent_in_apply_pass", "tutorial_overlay_present_in_classification_pass",
):
    require(findings.get(finding) is True, f"finding {finding}")
require(findings.get("physical_dialog_title") == "Mark restored cell B06", "physical dialog title")
require(findings.get("classification_world_block_mutation") is False, "classification mutation finding")
require(findings.get("inventory_mutations") == 0, "inventory finding")
require(checkpoint.get("interaction_status") == "proposal_applied_and_physical_b06_wrong_then_correct",
        "interaction disclosure")
require(checkpoint.get("operator_free") is False, "operator-assist disclosure")
require(len(checkpoint.get("not_proven", [])) == 5, "open-lane disclosure")
require(checkpoint.get("production_mutated") is False, "production mutation boundary")
require(checkpoint.get("production_release_authorized") is False, "production authorization boundary")

print("MORROW LATEST-PLUGIN VISUAL CHECKPOINT: PASS source=d738788 m02=apply+physical-b06-wrong+correct")
