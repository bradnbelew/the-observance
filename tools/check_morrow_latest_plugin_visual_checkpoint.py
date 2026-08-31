#!/usr/bin/env python3
"""Fail closed over the latest-plugin 28c6810 Room 04 dialog/Escape checkpoint."""

from __future__ import annotations

import hashlib
import json
from pathlib import Path


ROOT = Path(__file__).resolve().parents[1]
CHECKPOINT = ROOT / "morrow/rehearsal/client-visual/2026-08-31-latest-plugin-28c6810-room04-dialog-checkpoint.json"
SOURCE = "28c6810d8b770a939cd86fc8eb8be7a49aa39023"
PLUGIN_SHA256 = "1b5ba21b09ac2ece5763a58ae756b385073947cc42fd5113e70e5f89ec723f57"
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
require(checkpoint.get("status") == "latest_plugin_room04_dialog_escape_pass", "status")
require(checkpoint.get("plugin_source_commit") == SOURCE, "plugin source")
require(checkpoint.get("launcher_source_commit") == SOURCE, "launcher source")
runtime = checkpoint.get("runtime", {})
require(runtime.get("paper_version") == "1.21.11" and runtime.get("paper_build") == 132, "Paper identity")
require(runtime.get("paper_sha256") == PAPER_SHA256, "Paper hash")
require(runtime.get("plugin_sha256") == PLUGIN_SHA256, "plugin hash")
require(runtime.get("server") == "127.0.0.1:25595", "loopback server")
require(runtime.get("production_credentials_loaded") is False, "production credentials boundary")
require(runtime.get("resource_pack_policy") == "disabled", "resource-pack disclosure")
require(runtime.get("tutorial_toast_disabled") is True, "tutorial disclosure")

artifacts = checkpoint.get("artifacts", {})
required_artifacts = {
    "dialog_image", "dialog_capture_receipt", "dialog_client_receipt", "dialog_client_log",
    "escape_image", "escape_capture_receipt", "escape_client_receipt", "escape_client_log",
    "server_log", "paper_runtime_receipt",
}
require(set(artifacts) == required_artifacts, "artifact set")
for name, artifact in artifacts.items():
    path = ROOT / artifact.get("path", "")
    require(path.is_file(), f"missing artifact {name}")
    require(sha256(path) == artifact.get("sha256"), f"artifact hash {name}")

capture_contract = checkpoint.get("capture", {})
require(capture_contract.get("computer_use_input_sent") is True, "Computer Use action disclosure")
require(capture_contract.get("fallback_input_injected") is False, "fallback input boundary")
require(capture_contract.get("computer_use_actions") == ["observed terminal right-click", "observed Escape"], "bounded action set")

def check_client_pass(prefix: str, expected_pid: int, expected_status: str, expected_reason: str) -> None:
    capture = load_json(ROOT / artifacts[f"{prefix}_capture_receipt"]["path"])
    client = load_json(ROOT / artifacts[f"{prefix}_client_receipt"]["path"])
    image = capture.get("image", {})
    window = capture.get("window", {})
    require(capture.get("input_injected") is False, f"{prefix} capture input boundary")
    require(window.get("title") == capture_contract["window_title"], f"{prefix} window title")
    require(window.get("pid") == expected_pid == client.get("process_id"), f"{prefix} PID binding")
    require(window.get("minimized") is False, f"{prefix} window visibility")
    require(image.get("sha256") == artifacts[f"{prefix}_image"]["sha256"], f"{prefix} image binding")
    require(image.get("width") == 1282 and image.get("height") == 752, f"{prefix} dimensions")
    require(image.get("grayscale_entropy", 0) > 6, f"{prefix} entropy")
    require(client.get("source_commit") == SOURCE, f"{prefix} launcher source")
    require(client.get("status") == expected_status and client.get("stop_reason") == expected_reason, f"{prefix} stop disclosure")
    require(client.get("loopback_only") is True and client.get("account_files_read") is False, f"{prefix} privacy boundary")
    require(client.get("production_credentials_loaded") is False, f"{prefix} production boundary")
    require(client.get("server") == runtime["server"], f"{prefix} server binding")
    require(client.get("server_resource_pack_policy") == "disabled", f"{prefix} pack policy")
    require(client.get("accessibility_profile", {}).get("tutorial_toast_disabled") is True, f"{prefix} tutorial setting")
    client_log = (ROOT / artifacts[f"{prefix}_client_log"]["path"]).read_text(encoding="utf-8", errors="replace")
    require("Connecting to 127.0.0.1, 25595" in client_log, f"{prefix} connection evidence")
    require("MorrowWitness joined the game" in client_log, f"{prefix} join evidence")


check_client_pass("dialog", capture_contract["dialog_window_pid"], "harness_stopped_after_wait", "wait")
check_client_pass("escape", capture_contract["escape_window_pid"], "harness_stopped_after_request", "request")

server_log = (ROOT / artifacts["server_log"]["path"]).read_text(encoding="utf-8", errors="replace")
for marker in (
    "Paper version 1.21.11-132",
    "MORROW_ROOM04_READY status=ALREADY_PRESENT",
    "copper_aging_repairs=0",
    "MORROW_RUNTIME_READY",
    "MORROW_PLAYER_SAFE_ENTRY player=MorrowWitness",
    "inventory_mutations=0",
    "MORROW_RUNTIME_CLOSED entities_and_tasks=cleaned",
):
    require(marker in server_log, f"server log omitted {marker}")
require(server_log.count("MORROW_PLAYER_SAFE_ENTRY player=MorrowWitness") >= 2, "two client passes")

paper = load_json(ROOT / artifacts["paper_runtime_receipt"]["path"])
require(paper.get("status") == "pass" and paper.get("source_commit") == SOURCE, "Paper receipt status/source")
require(paper.get("paper", {}).get("sha256") == PAPER_SHA256, "Paper receipt hash")
require(paper.get("plugin_sha256") == PLUGIN_SHA256, "Paper receipt plugin hash")
require(paper.get("server", {}).get("port") == 25595, "Paper receipt port")

findings = checkpoint.get("findings", {})
for finding in (
    "real_minecraft_client_rendered", "safe_entry_logged", "room04_visible",
    "native_dialog_opened", "bounded_proposal_copy_readable", "apply_button_visible",
    "review_button_visible", "receipt_tooltip_visible", "escape_returned_to_room04",
    "proposal_not_applied_in_escape_pass", "tutorial_overlay_absent",
    "giant_overlay_regression_absent", "server_stopped_cleanly", "runtime_cleanup_logged",
):
    require(findings.get(finding) is True, f"finding {finding}")
require(findings.get("dialog_title") == "Static restoration proposal", "dialog title finding")
require(findings.get("inventory_mutations") == 0, "inventory finding")
require(checkpoint.get("interaction_status") == "dialog_open_and_escape_closed", "interaction disclosure")
require(len(checkpoint.get("not_proven", [])) == 5, "open-lane disclosure")
require(checkpoint.get("production_mutated") is False, "production mutation boundary")
require(checkpoint.get("production_release_authorized") is False, "production authorization boundary")

print("MORROW LATEST-PLUGIN VISUAL CHECKPOINT: PASS source=28c6810 room04=dialog-open escape=closed")
