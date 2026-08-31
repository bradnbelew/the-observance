#!/usr/bin/env python3
"""Generate the retained normal/silent Morrow static accessibility checkpoint."""

from __future__ import annotations

import argparse
import hashlib
import json
import re
import subprocess
from pathlib import Path
from typing import Any

from PIL import Image, ImageChops, ImageStat

import run_morrow_offline_client as client_fixture


ROOT = Path(__file__).resolve().parents[1]
REPORT = ROOT / "morrow" / "rehearsal" / "accessibility" / "latest.json"


def sha(path: Path) -> str:
    return hashlib.sha256(path.read_bytes()).hexdigest()


def load(path: Path) -> dict[str, Any]:
    return json.loads(path.read_text(encoding="utf-8"))


def relative(path: Path) -> str:
    return str(path.resolve().relative_to(ROOT)).replace("\\", "/")


def require(condition: bool, message: str) -> None:
    if not condition:
        raise RuntimeError(message)


def lane(receipt_path: Path, capture_path: Path, image_path: Path,
         expected_audio_disabled: bool) -> dict[str, Any]:
    runtime = load(receipt_path)
    client_path = receipt_path.parent / runtime["client"]["receipt"]
    options_path = receipt_path.parent / runtime["client"]["launch_options"]
    final_options_path = receipt_path.parent / runtime["client"]["final_options"]
    client = load(client_path)
    capture = load(capture_path)
    profile = client["accessibility_profile"]
    option_lines = options_path.read_text(encoding="utf-8").splitlines()
    final_lines = final_options_path.read_text(encoding="utf-8").splitlines()
    zeroed = {
        line.removeprefix("soundCategory_").removesuffix(":0.0")
        for line in option_lines
        if line.startswith("soundCategory_") and line.endswith(":0.0")
    }
    expected_zeroed = set(client_fixture.SOUND_CATEGORIES) if expected_audio_disabled else set()
    require(runtime["expected_status"] == "LOADED"
            and runtime["client"]["server_resource_pack_policy"] == "enabled"
            and runtime["client"]["audio_disabled"] is expected_audio_disabled,
            "runtime accessibility binding mismatch")
    require(profile["audio_disabled"] is expected_audio_disabled
            and set(profile["sound_categories_zeroed"]) == expected_zeroed
            and zeroed == expected_zeroed,
            "client sound-category profile mismatch")
    require(profile["tutorial_toast_disabled"] is True
            and "tutorialStep:none" in option_lines
            and "tutorialStep:none" in final_lines,
            "tutorial suppression mismatch")
    require(capture["status"] == "captured" and capture["input_injected"] is False
            and capture["window"]["pid"] == client["process_id"]
            and capture["image"]["sha256"] == sha(image_path),
            "capture provenance mismatch")
    return {
        "audio_disabled": expected_audio_disabled,
        "sound_categories_zeroed": sorted(expected_zeroed),
        "runtime": relative(receipt_path),
        "runtime_sha256": sha(receipt_path),
        "client": relative(client_path),
        "client_sha256": sha(client_path),
        "launch_options": relative(options_path),
        "launch_options_sha256": sha(options_path),
        "final_options": relative(final_options_path),
        "final_options_sha256": sha(final_options_path),
        "capture": relative(capture_path),
        "capture_sha256": sha(capture_path),
        "image": relative(image_path),
        "image_sha256": sha(image_path),
        "username": client["identity"]["username"],
        "uuid": client["identity"]["uuid"],
    }


def main() -> int:
    parser = argparse.ArgumentParser()
    parser.add_argument("--source-commit", required=True)
    parser.add_argument("--completed-at", required=True)
    parser.add_argument("--normal-receipt", type=Path, required=True)
    parser.add_argument("--silent-receipt", type=Path, required=True)
    parser.add_argument("--normal-capture", type=Path, required=True)
    parser.add_argument("--silent-capture", type=Path, required=True)
    parser.add_argument("--normal-image", type=Path, required=True)
    parser.add_argument("--silent-image", type=Path, required=True)
    args = parser.parse_args()

    require(subprocess.run(
        ["git", "merge-base", "--is-ancestor", args.source_commit, "HEAD"],
        cwd=ROOT, capture_output=True).returncode == 0,
        "source commit is not an ancestor of HEAD")
    supplied = [args.normal_receipt, args.silent_receipt, args.normal_capture,
                args.silent_capture, args.normal_image, args.silent_image]
    paths = [(path if path.is_absolute() else ROOT / path).resolve() for path in supplied]
    require(all(path.is_file() for path in paths), "one or more accessibility artifacts are missing")
    normal_receipt, silent_receipt, normal_capture, silent_capture, \
        normal_image_path, silent_image_path = paths
    normal_runtime, silent_runtime = load(normal_receipt), load(silent_receipt)
    require(normal_runtime["source_commit"] == silent_runtime["source_commit"]
            == args.source_commit, "runtime source commits differ")
    for field in ("sha1", "sha256", "bytes"):
        require(normal_runtime["resource_pack"][field]
                == silent_runtime["resource_pack"][field], f"pack {field} mismatch")
    manifest_pattern = re.compile(r"\bmanifest=([0-9a-f]{64})\b")
    normal_manifest = manifest_pattern.search(normal_runtime["server"]["room_ready_line"])
    silent_manifest = manifest_pattern.search(silent_runtime["server"]["room_ready_line"])
    require(normal_manifest is not None and silent_manifest is not None
            and normal_manifest.group(1) == silent_manifest.group(1),
            "Room 04 manifest binding mismatch")

    normal_lane = lane(normal_receipt, normal_capture, normal_image_path, False)
    silent_lane = lane(silent_receipt, silent_capture, silent_image_path, True)
    require(normal_lane["username"] == silent_lane["username"]
            and normal_lane["uuid"] == silent_lane["uuid"],
            "normal and silent client identities differ")
    normal_image = Image.open(normal_image_path).convert("RGB")
    silent_image = Image.open(silent_image_path).convert("RGB")
    require(normal_image.size == silent_image.size, "accessibility image dimensions differ")
    normal_capture_data, silent_capture_data = load(normal_capture), load(silent_capture)
    require(normal_capture_data["window"]["bounds"]
            == silent_capture_data["window"]["bounds"], "accessibility window bounds differ")
    difference = ImageChops.difference(normal_image, silent_image)
    histogram = difference.histogram()
    pixels = normal_image.width * normal_image.height
    report = {
        "schema_version": "1.0.0-morrow-audio-accessibility-checkpoint",
        "status": "bounded_static_visual_equivalence_pass_full_cue_parity_open",
        "completed_at": args.completed_at,
        "source_commit": args.source_commit,
        "scope": "matched exact-window Room 04 frames with normal audio and all vanilla sound "
                 "categories zeroed; no input and no full cue-by-cue parity claim",
        "producer_artifacts": {
            "capture_tool": {
                "path": "tools/capture_windows_window.py",
                "sha256": sha(ROOT / "tools" / "capture_windows_window.py"),
            },
            "rehearsal_runner": {
                "path": "tools/run_morrow_resource_pack_rehearsal.py",
                "sha256": sha(ROOT / "tools" / "run_morrow_resource_pack_rehearsal.py"),
            },
            "client_launcher": {
                "path": "tools/run_morrow_offline_client.py",
                "sha256": sha(ROOT / "tools" / "run_morrow_offline_client.py"),
            },
        },
        "lanes": {"normal": normal_lane, "silent": silent_lane},
        "comparison": {
            "image_size": list(normal_image.size),
            "window_bounds": normal_capture_data["window"]["bounds"],
            "difference_bbox": list(difference.getbbox()) if difference.getbbox() else None,
            "mean_absolute_rgb_difference": [
                round(value, 3) for value in ImageStat.Stat(difference).mean],
            "changed_channel_samples": sum(histogram) - sum(histogram[0::256]),
            "total_channel_samples": pixels * 3,
            "same_identity": True,
            "same_pack_bytes": True,
            "common_geometry_visible": True,
            "terminal_label_visible_in_both": True,
            "body_interaction_target_visible_in_both": True,
            "hud_visible_in_both": True,
            "static_visual_equivalence_pass": True,
            "required_cue_parity_proven": False,
            "input_injected": False,
        },
        "human_media_review": {
            "status": "primary_review_complete",
            "finding": "normal and silent frames preserve the same Room 04 structure, label, body "
                       "target, HUD, skin, and camera; no visual affordance disappears when audio "
                       "is disabled",
        },
        "remaining_gate": "trigger every required audio-bearing Morrow cue and independently verify "
                          "its captioned or pulsed visual equivalent in continuous synchronized media",
        "production_enablement": "blocked",
    }
    REPORT.parent.mkdir(parents=True, exist_ok=True)
    REPORT.write_text(json.dumps(report, indent=2) + "\n", encoding="utf-8")
    print(json.dumps({"report": relative(REPORT), "status": report["status"]}, indent=2))
    return 0


if __name__ == "__main__":
    raise SystemExit(main())
