#!/usr/bin/env python3
"""Validate the bounded normal/silent Morrow static accessibility checkpoint."""

from __future__ import annotations

import hashlib
import json
import subprocess
import sys
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


def require(condition: bool, message: str) -> None:
    if not condition:
        raise AssertionError(message)


def historical_sha(commit: str, path: str) -> str:
    result = subprocess.run(
        ["git", "show", f"{commit}:{path}"], cwd=ROOT, capture_output=True,
    )
    require(result.returncode == 0, f"producer artifact missing at source commit: {path}")
    return hashlib.sha256(result.stdout).hexdigest()


def lane(report: dict[str, Any], name: str, expected_audio_disabled: bool) -> Image.Image:
    row = report["lanes"][name]
    require(row["audio_disabled"] is expected_audio_disabled, f"{name} audio binding drifted")
    paths: dict[str, Path] = {}
    for field in ("runtime", "client", "launch_options", "final_options", "capture", "image"):
        path = (ROOT / row[field]).resolve()
        require(path.is_file() and sha(path) == row[f"{field}_sha256"],
                f"{name} {field} drifted")
        paths[field] = path
    runtime, client, capture = load(paths["runtime"]), load(paths["client"]), load(paths["capture"])
    profile = client["accessibility_profile"]
    expected_categories = set(client_fixture.SOUND_CATEGORIES) if expected_audio_disabled else set()
    require(runtime["source_commit"] == report["source_commit"]
            and runtime["expected_status"] == "LOADED"
            and runtime["client"]["audio_disabled"] is expected_audio_disabled
            and runtime["proof"]["human_media_reviewed"] is False
            and runtime["proof"]["production_mutated"] is False,
            f"{name} runtime boundary drifted")
    require(profile["audio_disabled"] is expected_audio_disabled
            and set(profile["sound_categories_zeroed"]) == expected_categories
            and profile["tutorial_toast_disabled"] is True,
            f"{name} accessibility profile drifted")
    option_lines = paths["launch_options"].read_text(encoding="utf-8").splitlines()
    zeroed = {
        line.removeprefix("soundCategory_").removesuffix(":0.0")
        for line in option_lines
        if line.startswith("soundCategory_") and line.endswith(":0.0")
    }
    require(zeroed == expected_categories and "tutorialStep:none" in option_lines,
            f"{name} launch options drifted")
    require(capture["status"] == "captured" and capture["input_injected"] is False
            and capture["window"]["pid"] == client["process_id"]
            and capture["image"]["sha256"] == row["image_sha256"],
            f"{name} capture provenance drifted")
    return Image.open(paths["image"]).convert("RGB")


def validate() -> None:
    report = load(REPORT)
    require(report["status"]
            == "bounded_static_visual_equivalence_pass_full_cue_parity_open"
            and report["comparison"]["static_visual_equivalence_pass"] is True
            and report["comparison"]["required_cue_parity_proven"] is False
            and report["production_enablement"] == "blocked",
            "audio accessibility checkpoint overclaims completion")
    require(subprocess.run(
        ["git", "merge-base", "--is-ancestor", report["source_commit"], "HEAD"],
        cwd=ROOT, capture_output=True).returncode == 0,
        "audio accessibility source commit is not an ancestor of HEAD")
    for artifact in report["producer_artifacts"].values():
        require(historical_sha(report["source_commit"], artifact["path"])
                == artifact["sha256"], f"producer artifact drifted: {artifact['path']}")
    normal_image = lane(report, "normal", False)
    silent_image = lane(report, "silent", True)
    require(report["lanes"]["normal"]["username"]
            == report["lanes"]["silent"]["username"]
            and report["lanes"]["normal"]["uuid"]
                == report["lanes"]["silent"]["uuid"],
            "normal and silent identities differ")
    require(normal_image.size == silent_image.size
            and list(normal_image.size) == report["comparison"]["image_size"],
            "accessibility image dimensions drifted")
    difference = ImageChops.difference(normal_image, silent_image)
    histogram = difference.histogram()
    metrics = report["comparison"]
    require((list(difference.getbbox()) if difference.getbbox() else None)
                == metrics["difference_bbox"]
            and [round(value, 3) for value in ImageStat.Stat(difference).mean]
                == metrics["mean_absolute_rgb_difference"]
            and sum(histogram) - sum(histogram[0::256]) == metrics["changed_channel_samples"]
            and normal_image.width * normal_image.height * 3
                == metrics["total_channel_samples"],
            "accessibility pixel metrics drifted")
    require(metrics["same_identity"] is True
            and metrics["same_pack_bytes"] is True
            and metrics["common_geometry_visible"] is True
            and metrics["terminal_label_visible_in_both"] is True
            and metrics["body_interaction_target_visible_in_both"] is True
            and metrics["hud_visible_in_both"] is True
            and metrics["input_injected"] is False,
            "audio accessibility finding drifted")


def main() -> int:
    try:
        validate()
    except Exception as failure:  # noqa: BLE001 - checker emits one concise failure
        print(f"MORROW AUDIO ACCESSIBILITY CHECKPOINT: FAIL: {failure}", file=sys.stderr)
        return 1
    print("MORROW AUDIO ACCESSIBILITY CHECKPOINT: PASS static-visual=1 "
          "sound-categories=11 cue-parity=required")
    return 0


if __name__ == "__main__":
    raise SystemExit(main())
