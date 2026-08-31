#!/usr/bin/env python3
"""Validate the bounded loaded/declined Morrow visual checkpoint without closing parity."""

from __future__ import annotations

import hashlib
import json
import subprocess
import sys
from pathlib import Path
from typing import Any

from PIL import Image, ImageChops, ImageStat


ROOT = Path(__file__).resolve().parents[1]
REPORT = ROOT / "morrow" / "rehearsal" / "client-visual" / \
    "2026-08-30-resource-pack-visual-pair.json"


def sha(path: Path) -> str:
    return hashlib.sha256(path.read_bytes()).hexdigest()


def load(path: Path) -> dict[str, Any]:
    return json.loads(path.read_text(encoding="utf-8"))


def require(condition: bool, message: str) -> None:
    if not condition:
        raise AssertionError(message)


def lane(report: dict[str, Any], name: str, expected: str, policy: str) -> tuple[dict[str, Any], Image.Image]:
    row = report["lanes"][name]
    require(row["expected_status"] == expected
            and row["server_resource_pack_policy"] == policy,
            f"{name} lane binding drifted")
    paths: dict[str, Path] = {}
    for field in ("capture", "image", "client", "runtime", "final_options"):
        path = (ROOT / row[field]).resolve()
        require(path.is_file() and sha(path) == row[f"{field}_sha256"],
                f"{name} {field} drifted")
        paths[field] = path
    capture = load(paths["capture"])
    client = load(paths["client"])
    runtime = load(paths["runtime"])
    require(capture["status"] == "captured" and capture["input_injected"] is False
            and capture["window"]["pid"] == client["process_id"]
            and capture["window"]["title"]
                == "Minecraft 1.21.11 - Multiplayer (3rd-party Server)"
            and capture["image"]["sha256"] == row["image_sha256"],
            f"{name} capture provenance drifted")
    require(client["source_commit"] == report["source_commit"]
            and client["server_resource_pack_policy"] == policy
            and client["loopback_only"] is True
            and client["production_credentials_loaded"] is False,
            f"{name} client boundary drifted")
    require(runtime["source_commit"] == report["source_commit"]
            and runtime["expected_status"] == expected
            and f"status={expected}" in runtime["observed_status_line"]
            and runtime["proof"]["status_observed_from_real_client"] is True
            and runtime["proof"]["vanilla_fallback_visual_equivalence"] is False
            and runtime["proof"]["human_media_reviewed"] is False
            and runtime["resource_pack"]["required"] is False
            and runtime["resource_pack"]["sha256"]
                == report["artifacts"]["resource_pack_sha256"],
            f"{name} runtime receipt drifted or overclaimed")
    gets = [request for request in runtime["resource_pack"]["requests"]
            if request["method"] == "GET" and request["status"] == 200]
    require(bool(gets) is (expected == "LOADED"), f"{name} pack fetch behavior drifted")
    require("tutorialStep:movement" in paths["final_options"].read_text(encoding="utf-8").splitlines(),
            f"{name} vanilla tutorial state drifted")
    return capture, Image.open(paths["image"]).convert("RGB")


def validate() -> None:
    report = load(REPORT)
    require(report["status"]
            == "bounded_pair_checkpoint_common_room_pass_tutorial_overlay_incomparable"
            and report["overlay_classification"]["status"]
                == "classified_non_morrow_capture_timing"
            and report["production_enablement"] == "blocked",
            "visual checkpoint overclaims parity")
    require(subprocess.run(
        ["git", "merge-base", "--is-ancestor", report["source_commit"], "HEAD"],
        cwd=ROOT, capture_output=True).returncode == 0,
        "visual checkpoint source commit is not an ancestor of HEAD")
    for field in ("capture_tool", "pack_harness"):
        artifact = report["artifacts"][field]
        path = ROOT / artifact["path"]
        require(path.is_file() and sha(path) == artifact["sha256"],
                f"visual checkpoint artifact drifted: {field}")

    loaded_capture, loaded_image = lane(report, "loaded", "LOADED", "enabled")
    declined_capture, declined_image = lane(report, "declined", "DECLINED", "disabled")
    require(loaded_capture["window"]["bounds"] == declined_capture["window"]["bounds"]
            == report["comparison"]["window_bounds"],
            "visual checkpoint window bounds differ")
    require(list(loaded_image.size) == list(declined_image.size)
            == report["comparison"]["image_size"],
            "visual checkpoint image sizes differ")
    difference = ImageChops.difference(loaded_image, declined_image)
    histogram = difference.histogram()
    pixels = loaded_image.width * loaded_image.height
    metrics = report["comparison"]
    require(list(difference.getbbox()) == metrics["difference_bbox"]
            and [round(value, 3) for value in ImageStat.Stat(difference).mean]
                == metrics["mean_absolute_rgb_difference"]
            and sum(histogram) - sum(histogram[0::256]) == metrics["changed_channel_samples"]
            and pixels * 3 == metrics["total_channel_samples"],
            "visual checkpoint pixel comparison drifted")
    require(metrics["common_geometry_visible"] is True
            and metrics["terminal_label_visible_in_both"] is True
            and metrics["body_interaction_target_visible_in_both"] is True
            and metrics["vanilla_tutorial_overlay_phase_equal"] is False
            and metrics["morrow_authored_surface_contradiction_observed"] is False
            and metrics["input_injected"] is False,
            "visual checkpoint finding drifted or full parity was silently closed")


def main() -> int:
    try:
        validate()
    except Exception as failure:  # noqa: BLE001 - checker emits one concise failure
        print(f"MORROW RESOURCE PACK VISUAL CHECKPOINT: FAIL: {failure}", file=sys.stderr)
        return 1
    print("MORROW RESOURCE PACK VISUAL CHECKPOINT: PASS common-room=1 tutorial-overlay=incomparable parity=required")
    return 0


if __name__ == "__main__":
    raise SystemExit(main())
