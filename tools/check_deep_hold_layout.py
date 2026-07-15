#!/usr/bin/env python3
"""Fail-closed static V5 Deep Hold geometry/runtime integration check."""

from __future__ import annotations

import csv
import re
import subprocess
import sys
from pathlib import Path


ROOT = Path(__file__).resolve().parents[1]
PLAN = ROOT / "plugin/src/main/java/com/observance/watcher/structure/DeepHoldV4Plan.java"
GEOMETRY = ROOT / "plugin/src/main/java/com/observance/watcher/structure/DeepHoldV4Geometry.java"
MANIFEST = ROOT / "plugin/src/main/java/com/observance/watcher/structure/DeepHoldV5Manifest.java"
AUTHORITY = ROOT / "plugin/src/main/java/com/observance/watcher/structure/V5AuthorityManifest.java"
COMMAND = ROOT / "plugin/src/main/java/com/observance/watcher/command/ObservanceCommand.java"
PROTECTION = ROOT / "plugin/src/main/java/com/observance/watcher/signal/listener/HoldProtectionListener.java"


def read(path: Path, failures: list[str]) -> str:
    if not path.is_file():
        failures.append(f"missing {path.relative_to(ROOT)}")
        return ""
    return path.read_text(encoding="utf-8")


def csv_rows(relative: str, failures: list[str]) -> list[dict[str, str]]:
    path = ROOT / relative
    if not path.is_file():
        failures.append(f"missing {relative}")
        return []
    with path.open("r", encoding="utf-8", newline="") as handle:
        return list(csv.DictReader(handle))


def require(text: str, label: str, tokens: tuple[str, ...], failures: list[str]) -> None:
    for token in tokens:
        if token not in text:
            failures.append(f"{label} missing {token}")


def main() -> int:
    failures: list[str] = []
    plan = read(PLAN, failures)
    geometry = read(GEOMETRY, failures)
    manifest = read(MANIFEST, failures)
    authority = read(AUTHORITY, failures)
    command = read(COMMAND, failures)
    protection = read(PROTECTION, failures)

    content_check = subprocess.run(
        [sys.executable, str(ROOT / "tools/check_v5_content.py")],
        cwd=ROOT, text=True, capture_output=True, check=False,
    )
    if content_check.returncode:
        failures.append("V5 content checker failed:\n" + content_check.stdout.rstrip())

    predicate_check = subprocess.run(
        [sys.executable, str(ROOT / "tools/check_v5_physical_predicates.py")],
        cwd=ROOT, text=True, capture_output=True, check=False,
    )
    if predicate_check.returncode:
        failures.append("V5 physical predicate checker failed:\n" + predicate_check.stdout.rstrip())

    fixture_check = subprocess.run(
        [sys.executable, str(ROOT / "tools/check_deep_hold_fixture_manifest.py")],
        cwd=ROOT, text=True, capture_output=True, check=False,
    )
    if fixture_check.returncode:
        failures.append("fixture manifest checker failed:\n" + fixture_check.stdout.rstrip())

    rooms = csv_rows("design/DEEP-HOLD-ROOM-BOXES.csv", failures)
    fixtures = csv_rows("design/DEEP-HOLD-FIXTURE-MANIFEST.csv", failures)
    gates = csv_rows("design/DEEP-HOLD-GATE-MANIFEST.csv", failures)
    records = csv_rows("design/DEEP-HOLD-RECORD-STATION-MANIFEST.csv", failures)
    counts = {"rooms": len(rooms), "fixtures": len(fixtures), "gates": len(gates), "records": len(records)}
    expected = {"rooms": 32, "fixtures": 76, "gates": 8, "records": 7}
    for label, count in expected.items():
        if counts[label] != count:
            failures.append(f"expected {count} {label}, found {counts[label]}")

    java_counts = {
        "rooms": len(re.findall(r"new Room\(", plan)),
        "fixtures": len(re.findall(r'^\s*fixture\("', plan, re.MULTILINE)),
        "gates": len(re.findall(r"new Gate\(", plan)),
        "records": len(re.findall(r"new RecordStation\(", plan)),
    }
    if java_counts != expected:
        failures.append(f"Java spatial counts {java_counts} do not match {expected}")

    require(plan, "spatial plan", (
        "MIN_SURFACE_COVER = 12", "MIN_BOTTOM_BUFFER = 12", "MIN_Y = -104",
        "MIN_X = -76", "MAX_X = 76", "MAX_Z = 233",
        'GEOMETRY_REVISION = "v5-compact-natural-2026-07-15"',
        "compactX", "compactZ", 'new Link("unwriting", "release")',
        'new Gate("g1"', 'new Gate("g6"', 'new Gate("prior"', 'new Gate("dread"',
        "public static List<String> validate()", "overlapsOwnership", "unreachable rooms",
        "v5_case_c02_complete", "v5_case_c09_complete",
    ), failures)
    require(geometry, "geometry builder", (
        "NATURAL_ZONES", "buildSurfaceMouthAndGrandStair", "buildNaturalZone",
        "carveNaturalCirculation", "dressNaturalDistricts", "neutralizeSupersededShell",
        "assertCompactWritesFrom", "replacingSupersededShell", "buildGatehouse",
        "finishSurfaceMouth",
    ), failures)
    if len(re.findall(r"new PhysicalZone\(", geometry)) != 11:
        failures.append("compact geometry must declare exactly 11 shared natural zones plus Orientation")
    plan_start = geometry.find("public static BuildPlan plan(")
    plan_end = geometry.find("/** Compatibility path", plan_start)
    active_builder = geometry[plan_start:plan_end] if plan_start >= 0 and plan_end > plan_start else ""
    if not active_builder:
        failures.append("could not isolate active compact geometry plan")
    for retired_call in (
        "carveAuthoredCirculation();", "dressRoom(room);",
        "for (DeepHoldV4Plan.Room room : DeepHoldV4Plan.ROOMS) builder.buildRoomShell(room);",
    ):
        if retired_call in active_builder:
            failures.append(f"active geometry plan still invokes retired sprawling builder: {retired_call}")
    require(manifest, "V5 manifest", (
        'CANONICAL_ORIENTATION = "+Z"', "EXPECTED_ROOMS = 32", "EXPECTED_FIXTURES = 76",
        "EXPECTED_GATES = 8", "EXPECTED_NODES = 82", "EXPECTED_BOOKS = 44",
        "V5AuthorityManifest.inspect()", "contentHash()",
        "v5_case_c02_complete", "v5_case_c04_complete", "v5_case_c05_complete",
        "v5_case_c06_complete", "v5_a01_location", "v5_case_c07_complete",
        "v5_case_c08_complete", "v5_case_c09_complete",
    ), failures)
    require(authority, "authority loader", (
        "ARG-V5-NODE-MANIFEST.csv", "ARG-V5-ROOM-ASSIGNMENTS.csv",
        "ARG-V5-FIXTURE-OWNERSHIP.csv", "ARG-V5-PHYSICAL-PREDICATES.json",
        "DEEP-HOLD-GATE-MANIFEST.csv",
        "minecraft-books.json", "npc-dialogue.json", "media-manifest.json",
    ), failures)
    require(command, "operator runtime", (
        "DeepHoldV5Manifest.validate()", "DeepHoldV5Manifest.contentHash()",
        "handlePlaceHoldPlan", "startDeepHoldV5Build", "finishDeepHoldV5Build", "auditV4OpenRoute",
        "handlePlaceHoldRepair", 'state.setProperty("geometry-revision"',
        'state.setProperty("replacement-cutover"', "item recover", "finale",
    ), failures)
    require(protection, "Hold protection", (
        "HOLD_REGION_TYPE", "insideHold", "BlockBreakEvent", "BlockPlaceEvent",
        "PlayerBucketEmptyEvent", "EntityExplodeEvent", "HangingBreakEvent",
    ), failures)

    if "int depth = 392" in command or "Math.max(340, Math.min(520" in command:
        failures.append("legacy caller-selected Hold depth is active")
    for old in ("rosetta_known", "prior_witness_ready_and_accepting_onramp", "bowed_as_one"):
        if old in manifest:
            failures.append(f"V5 gate manifest still contains retired gate condition {old}")

    for visual in (
        ROOT / "design/visuals/deep-hold-v5-blueprint.svg",
        ROOT / "design/visuals/deep-hold-v5-blueprint.png",
    ):
        if not visual.is_file() or visual.stat().st_size == 0:
            failures.append(f"missing generated layout visual {visual.relative_to(ROOT)}")

    print("Deep Hold V5 layout check: " + ("FAILED" if failures else "PASS"))
    if failures:
        for failure in failures:
            print("  FAIL " + failure)
        return 1
    print("  32 rooms / 76 fixtures / 8 persistent gates / 7 Record stations")
    print("  fixed +Z Mouth, compact +/-76 by Z233 footprint, 12 shared spaces, complete reverse route")
    print("  legacy-shell replacement, restart-safe checkpointing, V5 authority and recovery wired")
    return 0


if __name__ == "__main__":
    raise SystemExit(main())
