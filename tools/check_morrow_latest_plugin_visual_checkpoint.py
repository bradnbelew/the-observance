#!/usr/bin/env python3
"""Fail closed over the latest-plugin fedac89 Room 04 visual checkpoint."""

from __future__ import annotations

import hashlib
import json
from pathlib import Path


ROOT = Path(__file__).resolve().parents[1]
CHECKPOINT = ROOT / "morrow/rehearsal/client-visual/2026-08-31-latest-plugin-fedac89-room04-checkpoint.json"


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
require(checkpoint.get("status") == "latest_plugin_room04_visual_pass_interaction_open", "status")
require(checkpoint.get("plugin_source_commit") == "fedac8966bdccd099b6ab8d443134286ec4dc177", "plugin source")
require(checkpoint.get("launcher_source_commit") == "03ba091cc04eadb0a3f3610467d1fb1d164c1644", "launcher source")
runtime = checkpoint.get("runtime", {})
require(runtime.get("paper_version") == "1.21.11" and runtime.get("paper_build") == 132, "Paper identity")
require(runtime.get("paper_sha256") == "5ffef465eeeb5f2a3c23a24419d97c51afd7dbb4923ff42df9a3f58bba1ccfba", "Paper hash")
require(runtime.get("plugin_sha256") == "76660daabba28d0aa13eb547bc5e5854ed93107a80c038f94e79c002e15f228c", "plugin hash")
require(runtime.get("production_credentials_loaded") is False, "production credentials boundary")
require(runtime.get("resource_pack_policy") == "disabled", "resource-pack disclosure")
require(runtime.get("tutorial_toast_disabled") is True, "tutorial disclosure")

artifacts = checkpoint.get("artifacts", {})
require(set(artifacts) == {"image", "capture_receipt", "offline_client_receipt", "client_log", "server_log", "paper_runtime_receipt"}, "artifact set")
for name, artifact in artifacts.items():
    path = ROOT / artifact.get("path", "")
    require(path.is_file(), f"missing artifact {name}")
    require(sha256(path) == artifact.get("sha256"), f"artifact hash {name}")

capture = load_json(ROOT / artifacts["capture_receipt"]["path"])
image = capture.get("image", {})
window = capture.get("window", {})
require(capture.get("input_injected") is False, "capture input boundary")
require(window.get("title") == checkpoint["capture"]["window_title"], "window title")
require(window.get("pid") == checkpoint["capture"]["window_pid"], "window PID")
require(window.get("minimized") is False, "window visibility")
require(image.get("sha256") == artifacts["image"]["sha256"], "capture/image binding")
require(image.get("width") == 1282 and image.get("height") == 752, "image dimensions")
require(image.get("grayscale_entropy", 0) > 6, "image entropy")

client = load_json(ROOT / artifacts["offline_client_receipt"]["path"])
require(client.get("source_commit") == checkpoint["launcher_source_commit"], "client launcher source")
require(client.get("status") == "harness_stopped_after_request", "client stop status")
require(client.get("stop_reason") == "request", "client stop reason")
require(client.get("loopback_only") is True and client.get("account_files_read") is False, "client privacy boundary")
require(client.get("production_credentials_loaded") is False, "client production boundary")
require(client.get("server") == runtime["server"], "client server binding")
require(client.get("server_resource_pack_policy") == "disabled", "client pack policy")
require(client.get("accessibility_profile", {}).get("tutorial_toast_disabled") is True, "tutorial setting")

client_log = (ROOT / artifacts["client_log"]["path"]).read_text(encoding="utf-8", errors="replace")
server_log = (ROOT / artifacts["server_log"]["path"]).read_text(encoding="utf-8", errors="replace")
require("Connecting to 127.0.0.1, 25594" in client_log, "client connection evidence")
require("MorrowWitness joined the game" in client_log, "client join evidence")
require("Paper version 1.21.11-132" in server_log, "visual server Paper identity")
require("MORROW_RUNTIME_READY" in server_log, "runtime-ready evidence")
require("MORROW_PLAYER_SAFE_ENTRY player=MorrowWitness" in server_log, "safe-entry evidence")
require("inventory_mutations=0" in server_log, "inventory evidence")
require("MORROW_RUNTIME_CLOSED entities_and_tasks=cleaned" in server_log, "cleanup evidence")

paper = load_json(ROOT / artifacts["paper_runtime_receipt"]["path"])
require(paper.get("status") == "pass" and paper.get("source_commit") == checkpoint["plugin_source_commit"], "Paper receipt status/source")
require(paper.get("paper", {}).get("sha256") == runtime["paper_sha256"], "Paper receipt hash")
require(paper.get("plugin_sha256") == runtime["plugin_sha256"], "Paper receipt plugin hash")
require(paper.get("server", {}).get("port") == 25594, "Paper receipt port")

findings = checkpoint.get("findings", {})
require(findings.get("tutorial_overlay_absent_in_frame") is True, "tutorial-overlay finding")
require(findings.get("giant_overlay_regression_absent_in_frame") is True, "giant-overlay finding")
require(checkpoint.get("interaction_status") == "open", "interaction disclosure")
require(len(checkpoint.get("not_proven", [])) == 6, "open-lane disclosure")
require(checkpoint.get("production_mutated") is False, "production mutation boundary")
require(checkpoint.get("production_release_authorized") is False, "production authorization boundary")

print("MORROW LATEST-PLUGIN VISUAL CHECKPOINT: PASS source=fedac89 room04=visible tutorial=absent interaction=open")
