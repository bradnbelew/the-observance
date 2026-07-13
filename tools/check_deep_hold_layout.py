#!/usr/bin/env python3
"""Fail-fast static contract check for the Deep Hold V4 implementation."""

from __future__ import annotations

import re
import subprocess
import sys
from pathlib import Path


ROOT = Path(__file__).resolve().parents[1]
PLAN = ROOT / "plugin/src/main/java/com/observance/watcher/structure/DeepHoldV4Plan.java"
GEOMETRY = ROOT / "plugin/src/main/java/com/observance/watcher/structure/DeepHoldV4Geometry.java"
COMMAND = ROOT / "plugin/src/main/java/com/observance/watcher/command/ObservanceCommand.java"
ANSWER = ROOT / "plugin/src/main/java/com/observance/watcher/signal/listener/AnswerSignListener.java"
PROTECTION = ROOT / "plugin/src/main/java/com/observance/watcher/signal/listener/HoldProtectionListener.java"
PUZZLES = ROOT / "discord/supabase/seeds/puzzles_seed.sql"
METAPUZZLE = ROOT / "discord/supabase/seeds/metapuzzle_seed.sql"
HINTS = ROOT / "discord/supabase/seeds/hints_seed.sql"
DASHBOARD = ROOT / "dashboard/src/components/author/PriorAcceptingProgress.tsx"


def read(path: Path, failures: list[str]) -> str:
    if not path.exists():
        failures.append(f"missing {path.relative_to(ROOT)}")
        return ""
    return path.read_text(encoding="utf-8")


def main() -> int:
    failures: list[str] = []
    plan = read(PLAN, failures)
    geometry = read(GEOMETRY, failures)
    command = read(COMMAND, failures)
    answer = read(ANSWER, failures)
    protection = read(PROTECTION, failures)
    puzzles = read(PUZZLES, failures)
    metapuzzle = read(METAPUZZLE, failures)
    hints = read(HINTS, failures)
    dashboard = read(DASHBOARD, failures)

    manifest_check = subprocess.run(
        [sys.executable, str(ROOT / "tools/check_deep_hold_fixture_manifest.py")],
        cwd=ROOT, text=True, capture_output=True, check=False,
    )
    if manifest_check.returncode:
        failures.append("fixture manifest checker failed:\n" + manifest_check.stdout.rstrip())

    counts = {
        "rooms": len(re.findall(r'new Room\(', plan)),
        "fixtures": len(re.findall(r'^\s*fixture\("', plan, re.MULTILINE)),
        "gates": len(re.findall(r'new Gate\(', plan)),
        "records": len(re.findall(r'new RecordStation\(', plan)),
    }
    expected = {"rooms": 32, "fixtures": 76, "gates": 8, "records": 7}
    for key, value in expected.items():
        if counts[key] != value:
            failures.append(f"expected {value} V4 {key}, found {counts[key]}")

    required_plan = (
        "MIN_SURFACE_COVER = 12", "MIN_BOTTOM_BUFFER = 12", "MIN_Y = -104",
        "MAX_Z = 378", 'new Link("unwriting", "release")',
        'new Gate("g1"', 'new Gate("g6"', 'new Gate("prior"', 'new Gate("dread"',
        "public static List<String> validate()", "overlapsOwnership", "unreachable rooms",
        "prior_witness_ready_and_accepting_onramp", "bowed_as_one",
    )
    for needle in required_plan:
        if needle not in plan:
            failures.append(f"V4 plan missing contract token: {needle}")

    required_geometry = (
        "buildSurfaceMouthAndGrandStair", "buildRoomShell", "carveAuthoredCirculation",
        "buildUpperToCivicSwitchback", "buildCivicToLowerSwitchback", "dressRoom",
        "dressKeeperNave", "dressArchiveNave", "dressPuzzleWorks", "dressLowerWorks",
        "dressAccepting", "dressCoda", "buildGatehouse", "finishSurfaceMouth",
    )
    for needle in required_geometry:
        if needle not in geometry:
            failures.append(f"V4 geometry missing: {needle}")

    required_runtime = (
        "buildDeepHoldV4", "DeepHoldV4Plan.validate()", "DeepHoldV4Geometry",
        "deep_hold_entry_stair", "auditV4OpenRoute", "isV4AuditStandable",
        "hasHoldGateOverBypass", "syncPlaceHoldGatesAutomatically",
        'case "keeper", "archive"', 'case "accepting", "coda"',
        "loadHoldLockBooks();", "loadHoldD05Books();",
    )
    for needle in required_runtime:
        if needle not in command:
            failures.append(f"runtime missing V4 integration: {needle}")
    if "int depth = 392" in command or "Math.max(340, Math.min(520" in command:
        failures.append("legacy caller-selected Hold depth is still active")

    for needle in ("TYPE_CASE_BOARD", "TYPE_PRIOR_CAMP", "TYPE_FAILED_ACCEPTING", "ANSWER_SITE_TYPES"):
        if needle not in answer:
            failures.append(f"answer listener missing {needle}")
    for needle in ("HOLD_REGION_TYPE", "insideHold"):
        if needle not in protection:
            failures.append(f"protection listener missing region {needle}")

    expected_puzzles = {
        "prior-absence": ("no witness", "prior_absence_known"),
        "prior-camp-refusal": ("answers are not witness", "prior_camp_read"),
        "prior-vaun-correction": ("return first before count", "prior_vaun_corrected"),
        "prior-mara-correction": ("walk it before filing it", "prior_mara_corrected"),
        "prior-sella-correction": ("count the seventh before the six", "prior_sella_corrected"),
        "prior-orin-correction": ("bowing is proof not payment", "prior_orin_corrected"),
        "prior-brann-correction": ("the watch must be kept", "prior_brann_corrected"),
        "prior-iss-correction": ("test warmth against the land", "prior_iss_corrected"),
        "prior-witness-before-accepting": ("witness before accepting", "prior_witness_ready"),
    }
    for puzzle_id, (answer_text, flag) in expected_puzzles.items():
        for label, text, token in (("puzzle seed", puzzles, puzzle_id),
                                   ("puzzle seed", puzzles, answer_text),
                                   ("puzzle seed", puzzles, flag),
                                   ("hint seed", hints, puzzle_id),
                                   ("dashboard", dashboard, flag)):
            if token not in text:
                failures.append(f"{label} missing {token}")
    for flag in ("prior_camp_read", "prior_vaun_corrected", "prior_mara_corrected",
                 "prior_sella_corrected", "prior_orin_corrected", "prior_brann_corrected",
                 "prior_iss_corrected", "prior_witness_ready"):
        if flag not in metapuzzle:
            failures.append(f"metapuzzle seed missing {flag}")

    print("Deep Hold V4 layout check: " + ("FAILED" if failures else "PASS"))
    if failures:
        for failure in failures:
            print("  FAIL " + failure)
        return 1
    print("  32 owned rooms / 76 canonical fixtures / 8 persistent gates / 7 district records")
    print("  one Mouth, reversible three-stratum route, protected entry, and canonical Prior chain wired")
    return 0


if __name__ == "__main__":
    raise SystemExit(main())
