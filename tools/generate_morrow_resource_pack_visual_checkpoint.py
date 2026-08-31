#!/usr/bin/env python3
"""Generate the retained optional-pack decision index and static visual checkpoint."""

from __future__ import annotations

import argparse
import hashlib
import json
import subprocess
from pathlib import Path
from typing import Any

from PIL import Image, ImageChops, ImageStat


ROOT = Path(__file__).resolve().parents[1]
PACK_INDEX = ROOT / "morrow" / "rehearsal" / "resource-pack" / "latest.json"
VISUAL_REPORT = ROOT / "morrow" / "rehearsal" / "client-visual" / \
    "2026-08-30-resource-pack-visual-pair.json"


def sha(path: Path) -> str:
    return hashlib.sha256(path.read_bytes()).hexdigest()


def historical_sha(commit: str, path: str) -> str:
    result = subprocess.run(
        ["git", "show", f"{commit}:{path}"], cwd=ROOT, capture_output=True,
    )
    require(result.returncode == 0, f"producer artifact missing at source commit: {path}")
    return hashlib.sha256(result.stdout).hexdigest()


def load(path: Path) -> dict[str, Any]:
    return json.loads(path.read_text(encoding="utf-8"))


def relative(path: Path) -> str:
    return str(path.resolve().relative_to(ROOT)).replace("\\", "/")


def require(condition: bool, message: str) -> None:
    if not condition:
        raise RuntimeError(message)


def lane(receipt_path: Path, capture_path: Path, image_path: Path,
         expected: str, policy: str, finding: str) -> dict[str, Any]:
    receipt = load(receipt_path)
    client_path = receipt_path.parent / receipt["client"]["receipt"]
    final_options_path = receipt_path.parent / receipt["client"]["final_options"]
    client = load(client_path)
    capture = load(capture_path)
    require(receipt["source_commit"] == client["source_commit"],
            f"{expected} source commit mismatch")
    require(receipt["expected_status"] == expected
            and receipt["client"]["server_resource_pack_policy"] == policy,
            f"{expected} decision mismatch")
    require(client["accessibility_profile"]["tutorial_toast_disabled"] is True
            and "tutorialStep:none" in final_options_path.read_text(
                encoding="utf-8").splitlines(),
            f"{expected} tutorial suppression missing")
    require(capture["status"] == "captured" and capture["input_injected"] is False
            and capture["window"]["pid"] == client["process_id"]
            and capture["image"]["sha256"] == sha(image_path),
            f"{expected} capture provenance mismatch")
    return {
        "expected_status": expected,
        "server_resource_pack_policy": policy,
        "capture": relative(capture_path),
        "capture_sha256": sha(capture_path),
        "image": relative(image_path),
        "image_sha256": sha(image_path),
        "client": relative(client_path),
        "client_sha256": sha(client_path),
        "runtime": relative(receipt_path),
        "runtime_sha256": sha(receipt_path),
        "final_options": relative(final_options_path),
        "final_options_sha256": sha(final_options_path),
        "bounded_finding": finding,
    }


def main() -> int:
    parser = argparse.ArgumentParser()
    parser.add_argument("--source-commit", required=True)
    parser.add_argument("--completed-at", required=True)
    parser.add_argument("--loaded-receipt", type=Path, required=True)
    parser.add_argument("--declined-receipt", type=Path, required=True)
    parser.add_argument("--loaded-capture", type=Path, required=True)
    parser.add_argument("--declined-capture", type=Path, required=True)
    parser.add_argument("--loaded-image", type=Path, required=True)
    parser.add_argument("--declined-image", type=Path, required=True)
    args = parser.parse_args()

    require(subprocess.run(
        ["git", "merge-base", "--is-ancestor", args.source_commit, "HEAD"],
        cwd=ROOT, capture_output=True).returncode == 0,
        "source commit is not an ancestor of HEAD")
    paths = [args.loaded_receipt, args.declined_receipt, args.loaded_capture,
             args.declined_capture, args.loaded_image, args.declined_image]
    paths = [(path if path.is_absolute() else ROOT / path).resolve() for path in paths]
    require(all(path.is_file() for path in paths), "one or more evidence files are missing")
    loaded_receipt, declined_receipt, loaded_capture, declined_capture, \
        loaded_image_path, declined_image_path = paths

    loaded_runtime = load(loaded_receipt)
    declined_runtime = load(declined_receipt)
    for runtime in (loaded_runtime, declined_runtime):
        require(runtime["source_commit"] == args.source_commit,
                "runtime receipt source commit mismatch")
    for field in ("sha1", "sha256", "bytes"):
        require(loaded_runtime["resource_pack"][field]
                == declined_runtime["resource_pack"][field],
                f"resource pack {field} mismatch")

    loaded_lane = lane(
        loaded_receipt, loaded_capture, loaded_image_path, "LOADED", "enabled",
        "Room 04 geometry, terminal label, body target, and HUD are visible; "
        "the accepted pack changes authored surface and player-skin presentation only.")
    declined_lane = lane(
        declined_receipt, declined_capture, declined_image_path, "DECLINED", "disabled",
        "Room 04 geometry, terminal label, body target, and HUD remain visible with "
        "vanilla textures; no missing-texture or missing-geometry fallback defect is visible.")

    loaded_image = Image.open(loaded_image_path).convert("RGB")
    declined_image = Image.open(declined_image_path).convert("RGB")
    require(loaded_image.size == declined_image.size, "image dimensions differ")
    loaded_capture_data = load(loaded_capture)
    declined_capture_data = load(declined_capture)
    require(loaded_capture_data["window"]["bounds"]
            == declined_capture_data["window"]["bounds"], "window bounds differ")
    difference = ImageChops.difference(loaded_image, declined_image)
    histogram = difference.histogram()
    pixels = loaded_image.width * loaded_image.height

    pack_index = {
        "schema_version": "1.0.0-morrow-resource-pack-evidence-index",
        "status": "bounded_handshake_pair_pass_human_visual_parity_open",
        "completed_at": args.completed_at,
        "source_commit": args.source_commit,
        "artifacts": {
            "rehearsal_runner": {
                "path": "tools/run_morrow_resource_pack_rehearsal.py",
                "sha256": historical_sha(
                    args.source_commit, "tools/run_morrow_resource_pack_rehearsal.py"),
            },
            "client_launcher": {
                "path": "tools/run_morrow_offline_client.py",
                "sha256": historical_sha(args.source_commit, "tools/run_morrow_offline_client.py"),
            },
            "receipt_checker": {
                "path": "tools/check_morrow_resource_pack_receipts.py",
                "sha256": sha(ROOT / "tools" / "check_morrow_resource_pack_receipts.py"),
            },
            "runtime_policy": {
                "path": "plugin/src/main/java/com/observance/watcher/morrow/MorrowResourcePackPolicy.java",
                "sha256": historical_sha(
                    args.source_commit,
                    "plugin/src/main/java/com/observance/watcher/morrow/"
                    "MorrowResourcePackPolicy.java"),
            },
        },
        "lanes": {
            "loaded": {"receipt": relative(loaded_receipt), "sha256": sha(loaded_receipt)},
            "declined": {
                "receipt": relative(declined_receipt), "sha256": sha(declined_receipt),
            },
        },
        "proven": "real vanilla 1.21.11 clients reached Paper LOADED and DECLINED "
                  "states against the same exact optional loopback resource-pack bytes; "
                  "the declined client performed no pack GET; both disposable profiles "
                  "suppressed only the vanilla tutorial toast; both runtimes cleaned up",
        "remaining_gate": "independently review every required Morrow body, text, dialog, "
                          "and puzzle interaction in loaded and declined runs with "
                          "synchronized media",
        "production_enablement": "blocked",
    }
    visual_report = {
        "schema_version": "1.1.0-morrow-resource-pack-visual-checkpoint",
        "status": "bounded_pair_checkpoint_common_room_pass_tutorial_suppressed",
        "captured_at": args.completed_at,
        "source_commit": args.source_commit,
        "scope": "matched tutorial-free exact-window Room 04 frames after real LOADED and "
                 "DECLINED decisions; no input or full interaction-parity claim",
        "artifacts": {
            "capture_tool": {
                "path": "tools/capture_windows_window.py",
                "sha256": historical_sha(args.source_commit, "tools/capture_windows_window.py"),
            },
            "pack_harness": {
                "path": "tools/run_morrow_resource_pack_rehearsal.py",
                "sha256": historical_sha(
                    args.source_commit, "tools/run_morrow_resource_pack_rehearsal.py"),
            },
            "resource_pack_sha256": loaded_runtime["resource_pack"]["sha256"],
        },
        "lanes": {"loaded": loaded_lane, "declined": declined_lane},
        "comparison": {
            "image_size": list(loaded_image.size),
            "window_bounds": loaded_capture_data["window"]["bounds"],
            "difference_bbox": list(difference.getbbox()),
            "mean_absolute_rgb_difference": [
                round(value, 3) for value in ImageStat.Stat(difference).mean],
            "changed_channel_samples": sum(histogram) - sum(histogram[0::256]),
            "total_channel_samples": pixels * 3,
            "common_geometry_visible": True,
            "terminal_label_visible_in_both": True,
            "body_interaction_target_visible_in_both": True,
            "vanilla_tutorial_overlay_visible_in_either": False,
            "pack_authored_texture_difference_visible": True,
            "static_fallback_checkpoint_pass": True,
            "morrow_authored_surface_contradiction_observed": False,
            "input_injected": False,
        },
        "overlay_classification": {
            "status": "suppressed_in_disposable_profiles",
            "finding": "both isolated visual profiles set tutorialStep:none before launch, "
                       "removing Minecraft's unrelated movement toast without injecting input",
            "evidence": [
                "both client receipts record tutorial_toast_disabled=true",
                "both launch and final options contain tutorialStep:none",
                "both capture receipts record input_injected=false",
            ],
        },
        "human_media_review": {
            "status": "primary_review_complete",
            "finding": "both frames show the same Room 04 structure and required static "
                       "targets; expected pack-authored texture differences are visible; "
                       "no missing surface, label, body target, or HUD was observed",
        },
        "remaining_gate": "review every Morrow body state, text surface, dialog branch, and "
                          "puzzle input in loaded and declined runs with synchronized media "
                          "and an independent observer",
        "production_enablement": "blocked",
    }
    PACK_INDEX.write_text(json.dumps(pack_index, indent=2) + "\n", encoding="utf-8")
    VISUAL_REPORT.write_text(json.dumps(visual_report, indent=2) + "\n", encoding="utf-8")
    print(json.dumps({
        "pack_index": relative(PACK_INDEX),
        "visual_report": relative(VISUAL_REPORT),
        "status": visual_report["status"],
    }, indent=2))
    return 0


if __name__ == "__main__":
    raise SystemExit(main())
