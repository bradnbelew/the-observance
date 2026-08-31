#!/usr/bin/env python3
"""Fail closed over the latest-plugin M03/M04 real-client Entity Replay checkpoint."""

from __future__ import annotations

import base64
import hashlib
import json
import subprocess
from pathlib import Path


ROOT = Path(__file__).resolve().parents[1]
CHECKPOINT = (
    ROOT
    / "morrow/rehearsal/client-visual/"
    "2026-08-31-latest-plugin-cacfaa4-m03-m04-checkpoint.json"
)
ACQUISITION = "9b2c3666d4199901a148a3a1ba75145f8bae7de7"
FINAL = "cacfaa47453e3e260269c24b5bc958d4310dd360"
ACQUISITION_PLUGIN = "0cb0811ae74d13cb0d978b2cfe6864a5ec64e22cf571668b53f22be194d45bad"
FINAL_PLUGIN = "31f02511d90072682c6aaa98df85113fb6045fe68ed3f7460389e977e5141f54"
PAPER = "5ffef465eeeb5f2a3c23a24419d97c51afd7dbb4923ff42df9a3f58bba1ccfba"
M03_CLIP = "43da3ab20bf2e8154d184972aea9f51ec3bf07fee328c001d6ffcc3245f377cb"
M04_CLIP = "d327c0198172b92dfed393ab8cebf0b1278e6291439d7519e6737df66f8b301e"
TITLE = "Minecraft 1.21.11 - Multiplayer (3rd-party Server)"


def require(condition: bool, message: str) -> None:
    if not condition:
        raise SystemExit(f"MORROW LATEST-PLUGIN ENTITY REPLAY: FAIL {message}")


def sha256(path: Path) -> str:
    digest = hashlib.sha256()
    with path.open("rb") as handle:
        for chunk in iter(lambda: handle.read(1024 * 1024), b""):
            digest.update(chunk)
    return digest.hexdigest()


def load(path: Path) -> dict:
    require(path.is_file(), f"missing {path.relative_to(ROOT)}")
    return json.loads(path.read_text(encoding="utf-8"))


def decode_payload(encoded: str) -> dict:
    return json.loads(base64.urlsafe_b64decode(encoded + "=" * (-len(encoded) % 4)))


checkpoint = load(CHECKPOINT)
require(
    checkpoint.get("schema_version")
    == "1.0.0-morrow-latest-plugin-entity-replay-checkpoint",
    "schema",
)
require(
    checkpoint.get("status")
    == "latest_plugin_m03_m04_physical_loop_pass_with_positioning_assist",
    "status",
)
source = checkpoint.get("source", {})
require(source.get("acquisition_commit") == ACQUISITION, "acquisition source")
require(source.get("final_validation_commit") == FINAL, "final source")
require(source.get("acquisition_plugin_sha256") == ACQUISITION_PLUGIN, "acquisition plugin")
require(source.get("final_plugin_sha256") == FINAL_PLUGIN, "final plugin")
for commit in (ACQUISITION, FINAL):
    require(
        subprocess.run(
            ["git", "merge-base", "--is-ancestor", commit, "HEAD"],
            cwd=ROOT,
            capture_output=True,
        ).returncode
        == 0,
        f"{commit[:7]} is not an ancestor of HEAD",
    )
delta = subprocess.run(
    ["git", "diff", "--name-only", f"{ACQUISITION}..{FINAL}"],
    cwd=ROOT,
    capture_output=True,
    text=True,
    check=True,
).stdout.splitlines()
require(
    set(delta)
    == {
        "plugin/src/main/java/com/observance/watcher/morrow/room04/replay/BukkitEntityReplay.java",
        "plugin/src/test/java/com/observance/watcher/morrow/room04/replay/EntityReplayLoopSelfTest.java",
    },
    "acquisition-to-final delta escaped the proof-marker implementation/self-test",
)

runtime = checkpoint.get("runtime", {})
require(
    runtime.get("paper_version") == "1.21.11"
    and runtime.get("paper_build") == 132
    and runtime.get("paper_sha256") == PAPER,
    "Paper identity",
)
require(runtime.get("acquisition_server") == "127.0.0.1:25601", "acquisition server")
require(runtime.get("final_validation_server") == "127.0.0.1:25602", "final server")
require(runtime.get("dummy_offline_identity") == "MorrowWitness", "dummy identity")
require(runtime.get("production_credentials_loaded") is False, "credential boundary")
require(runtime.get("production_contacted") is False, "production contact boundary")
require(runtime.get("resource_pack_runtime_policy") == "disabled", "resource-pack disclosure")

artifacts = checkpoint.get("artifacts", {})
required_artifacts = {
    "paper_runtime_receipt", "paper_first_log", "paper_restart_log", "journal",
    "m03_clip", "m04_clip", "m03_consent", "m04_consent",
    "acquisition_server_log", "final_server_log",
    "m03_auth_image", "m03_auth_capture", "m03_authorized_image",
    "m03_authorized_capture", "m03_entry_image", "m03_entry_capture",
    "m04_seal_image", "m04_seal_capture", "m04_replay_image",
    "m04_replay_capture", "m04_proof_image", "m04_proof_capture",
    "m04_proof_feedback_image", "m04_proof_feedback_capture",
    "m03_auth_client_receipt", "m03_auth_client_log",
    "m03_success_client_receipt", "m03_success_client_log",
    "m04_route_client_receipt", "m04_route_client_log",
    "m04_replay_client_receipt", "m04_replay_client_log",
    "m04_proof_client_receipt", "m04_proof_client_log",
}
require(set(artifacts) == required_artifacts, "artifact set")
for name, artifact in artifacts.items():
    path = ROOT / artifact.get("path", "")
    require(path.is_file(), f"missing artifact {name}")
    require(sha256(path) == artifact.get("sha256"), f"artifact hash {name}")

interaction = checkpoint.get("interaction", {})
require(interaction.get("window_title") == TITLE, "window title")
require(interaction.get("computer_use_input_sent") is True, "Computer Use disclosure")
require(interaction.get("fallback_input_injected") is False, "fallback input boundary")
require("0x80004002" in interaction.get("computer_use_wgc_result", ""), "WGC disclosure")
require(len(interaction.get("bounded_actions", [])) == 4, "bounded action disclosure")
require("teleports" in interaction.get("positioning_assist", ""), "positioning disclosure")
require("mechanically seeded" in interaction.get("final_fixture_seed", ""), "fixture disclosure")


def check_capture(prefix: str, expected_pid: int) -> None:
    receipt = load(ROOT / artifacts[f"{prefix}_capture"]["path"])
    image = receipt.get("image", {})
    window = receipt.get("window", {})
    require(receipt.get("status") == "captured", f"{prefix} capture status")
    require(receipt.get("input_injected") is False, f"{prefix} capture input")
    require(window.get("title") == TITLE, f"{prefix} title")
    require(window.get("pid") == expected_pid and window.get("minimized") is False, f"{prefix} PID")
    require(image.get("path") == artifacts[f"{prefix}_image"]["path"], f"{prefix} image path")
    require(image.get("sha256") == artifacts[f"{prefix}_image"]["sha256"], f"{prefix} image hash")
    require(image.get("width") == 1282 and image.get("height") == 752, f"{prefix} dimensions")
    require(image.get("grayscale_entropy", 0) > 6, f"{prefix} entropy")


for capture_prefix, pid in {
    "m03_auth": 33404,
    "m03_authorized": 33404,
    "m03_entry": 25364,
    "m04_seal": 34468,
    "m04_replay": 11452,
    "m04_proof": 31104,
    "m04_proof_feedback": 31104,
}.items():
    check_capture(capture_prefix, pid)


def check_client(prefix: str, expected_source: str, server: str, pid: int) -> None:
    receipt = load(ROOT / artifacts[f"{prefix}_client_receipt"]["path"])
    require(receipt.get("source_commit") == expected_source, f"{prefix} source")
    require(receipt.get("server") == server and receipt.get("process_id") == pid, f"{prefix} runtime")
    require(
        receipt.get("status") == "harness_stopped_after_wait"
        and receipt.get("stop_reason") == "wait",
        f"{prefix} stop",
    )
    require(receipt.get("loopback_only") is True, f"{prefix} loopback")
    require(receipt.get("account_files_read") is False, f"{prefix} account boundary")
    require(receipt.get("production_credentials_loaded") is False, f"{prefix} production boundary")
    require(receipt.get("identity", {}).get("username") == "MorrowWitness", f"{prefix} identity")
    log_path = ROOT / artifacts[f"{prefix}_client_log"]["path"]
    require(receipt.get("log_sha256") == sha256(log_path), f"{prefix} client log")
    log_text = log_path.read_text(encoding="utf-8", errors="replace")
    port = server.rsplit(":", 1)[1]
    require(f"Connecting to 127.0.0.1, {port}" in log_text, f"{prefix} connection")
    require("MorrowWitness joined the game" in log_text, f"{prefix} join")


check_client("m03_auth", ACQUISITION, "127.0.0.1:25601", 33404)
check_client("m03_success", ACQUISITION, "127.0.0.1:25601", 25364)
check_client("m04_route", ACQUISITION, "127.0.0.1:25601", 34468)
check_client("m04_replay", ACQUISITION, "127.0.0.1:25601", 11452)
check_client("m04_proof", FINAL, "127.0.0.1:25602", 31104)

paper = load(ROOT / artifacts["paper_runtime_receipt"]["path"])
require(paper.get("status") == "pass" and paper.get("source_commit") == FINAL, "Paper source/status")
require(paper.get("plugin_sha256") == FINAL_PLUGIN, "Paper plugin")
require(paper.get("paper", {}).get("sha256") == PAPER, "Paper hash")
require(paper.get("server", {}).get("port") == 25602, "Paper port")
for artifact_name, receipt_key in (
    ("paper_first_log", "first_sha256"),
    ("paper_restart_log", "restart_sha256"),
):
    require(
        artifacts[artifact_name]["sha256"] == paper.get("logs", {}).get(receipt_key),
        f"Paper lifecycle log {artifact_name}",
    )
for artifact_name in ("paper_first_log", "paper_restart_log"):
    text = (ROOT / artifacts[artifact_name]["path"]).read_text(encoding="utf-8", errors="replace")
    require("MORROW_RUNTIME_READY" in text, f"{artifact_name} runtime ready")
    require("replay_entities=8" in text, f"{artifact_name} replay entity baseline")
    require("MORROW_RUNTIME_CLOSED entities_and_tasks=cleaned" in text, f"{artifact_name} cleanup")

for artifact_name, minimum_joins in (("acquisition_server_log", 5), ("final_server_log", 3)):
    text = (ROOT / artifacts[artifact_name]["path"]).read_text(encoding="utf-8", errors="replace")
    require("Paper version 1.21.11-132" in text, f"{artifact_name} Paper")
    require("MORROW_RUNTIME_READY" in text, f"{artifact_name} runtime ready")
    require(text.count("MORROW_PLAYER_SAFE_ENTRY player=MorrowWitness") >= minimum_joins,
            f"{artifact_name} join count")
    require("inventory_mutations=0" in text, f"{artifact_name} inventory boundary")
    require("MORROW_RUNTIME_CLOSED entities_and_tasks=cleaned" in text, f"{artifact_name} cleanup")

journal_path = ROOT / artifacts["journal"]["path"]
rows = [line.split("\t") for line in journal_path.read_text(encoding="utf-8").splitlines() if line]
require(len(rows) == 10 and all(len(row) == 6 for row in rows), "journal row count/shape")
require([int(row[0]) for row in rows] == list(range(1, 11)), "journal sequence")
require(all(rows[index][1] == rows[index - 1][2] for index in range(1, len(rows))),
        "journal chain continuity")
expected_final = [
    ("paper:room04:m03:missing-role:v1", "morrow.act2.missing_role_completed"),
    ("paper:room04:m04:live-test:v1", "morrow.act2.live_test_recorded"),
    ("paper:room04:m04:behavior-reuse:v1", "morrow.act2.behavior_reuse_proven"),
]
require([(row[3], row[4]) for row in rows[-3:]] == expected_final, "M03/M04 event order")
require(all(sum(row[3] == expected[0] for row in rows) == 1 for expected in expected_final),
        "M03/M04 idempotency count")
m03_payload, m04_payload, proof_payload = [decode_payload(row[5]) for row in rows[-3:]]
require(
    m03_payload.get("clip_hash") == M03_CLIP
    and m03_payload.get("summary", {}).get("duration_ticks") == 740
    and m03_payload.get("summary", {}).get("raw_samples") == 370
    and m03_payload.get("compressed_samples") == 3,
    "M03 payload",
)
m04_summary = m04_payload.get("summary", {})
require(
    m04_payload.get("clip_hash") == M04_CLIP
    and m04_summary.get("duration_ticks") == 900
    and m04_summary.get("raw_samples") == 450
    and m04_payload.get("compressed_samples") == 7
    and m04_summary.get("path_milliblocks") == 6591
    and m04_summary.get("heading_changes") == 2
    and m04_summary.get("designated_actions") == 0,
    "M04 payload",
)
require(
    proof_payload.get("clip_hash") == M04_CLIP
    and proof_payload.get("summary", {}).get("replay_duration_ticks") == 900
    and proof_payload.get("summary", {}).get("provenance")
    == "reconstructed_from_current_recording",
    "proof payload",
)


def key_values(path: Path) -> tuple[dict[str, str], list[str]]:
    metadata: dict[str, str] = {}
    samples: list[str] = []
    for line in path.read_text(encoding="utf-8").splitlines()[1:]:
        if "=" in line and "\t" not in line:
            key, value = line.split("=", 1)
            metadata[key] = value
        elif "\t" in line:
            samples.append(line)
    return metadata, samples


m03_meta, m03_samples = key_values(ROOT / artifacts["m03_clip"]["path"])
require(
    m03_meta.get("purpose") == "MISSING_ROLE"
    and m03_meta.get("duration-ticks") == "740"
    and m03_meta.get("raw-samples") == "370"
    and m03_meta.get("compressed-samples") == "3"
    and m03_meta.get("hash") == M03_CLIP,
    "M03 clip metadata",
)
require(len(m03_samples) == 3 and m03_samples[1].startswith("402\t"), "M03 clip samples")
require("SWING,INTERACT,INVENTORY_TRANSFER," in m03_samples[1], "M03 designated transfer")
m04_meta, m04_samples = key_values(ROOT / artifacts["m04_clip"]["path"])
require(
    m04_meta.get("purpose") == "DELIBERATE_LIVE_TEST"
    and m04_meta.get("duration-ticks") == "900"
    and m04_meta.get("raw-samples") == "450"
    and m04_meta.get("compressed-samples") == "7"
    and m04_meta.get("hash") == M04_CLIP,
    "M04 clip metadata",
)
require(len(m04_samples) == 7 and all(row.endswith("\t") for row in m04_samples), "M04 samples")

for artifact_name, purpose, consent_hash in (
    ("m03_consent", "MISSING_ROLE", m03_meta["consent-hash"]),
    ("m04_consent", "DELIBERATE_LIVE_TEST", m04_meta["consent-hash"]),
):
    text = (ROOT / artifacts[artifact_name]["path"]).read_text(encoding="utf-8")
    require(f"purpose={purpose}" in text, f"{artifact_name} purpose")
    require("revision=3" in text and "granted=true" in text, f"{artifact_name} grant")
    require(f"hash={consent_hash}" in text, f"{artifact_name} hash")

findings = checkpoint.get("findings", {})
for key in (
    "m03_native_authorization_rendered", "m03_recording_armed_on_boundary_entry",
    "m04_live_seal_label_rendered", "m04_reconstructed_echo_rendered",
    "m04_isolated_proof_label_rendered", "m04_behavior_reuse_receipt_created_once",
    "server_stopped_cleanly", "runtime_cleanup_logged",
):
    require(findings.get(key) is True, f"finding {key}")
require(findings.get("m03_designated_action_tick") == 402, "M03 action tick finding")
require(findings.get("m03_disclosed_grace_window_ticks") == [380, 500], "M03 grace finding")
require(findings.get("journal_hash_chain_rows") == 10, "journal finding")
require(findings.get("inventory_mutations") == 0, "inventory finding")
require(checkpoint.get("operator_free") is False, "operator disclosure")
require(len(checkpoint.get("not_proven", [])) == 5, "open-lane disclosure")
require(checkpoint.get("production_mutated") is False, "production mutation boundary")
require(checkpoint.get("production_release_authorized") is False, "release boundary")

print(
    "MORROW LATEST-PLUGIN ENTITY REPLAY: PASS "
    "source=cacfaa4 m03=tick402 m04=seal+replay+proof positioning_assist=disclosed"
)
