#!/usr/bin/env python3
"""Fail closed on pre-V5 operational drift.

Historical prose may remain for provenance, but it must identify itself before a reader reaches
instructions. Historical generators may remain only as guards that terminate before mutation.
"""

from __future__ import annotations

import json
import re
import subprocess
import sys
from pathlib import Path


ROOT = Path(__file__).resolve().parents[1]
DESIGN = ROOT / "design"

CURRENT_DESIGN_DOCS = {
    "ARG-V5-MASTER-PLAN.md",
    "CLUE-LEDGER.md",
    "EXPERIENCE-MANIFEST.md",
    "V5-BOOKS.md",
    "V5-EXTERNAL-MEDIA-RECEIPT.md",
    "V5-FINALE.md",
    "V5-KEEPER-DOSSIERS.md",
    "V5-PRODUCTION-LAUNCH-RUNBOOK.md",
    "V5-SUPERSESSION-MAP.md",
    "V5-UNLIT.md",
    "V5-WORLD-SETUP-AND-TESTING.md",
    "V5-WREN-EVIDENCE.md",
    "V5.1-REDESIGN.md",
    "CODEX-PROMPT-V5.1-DEPLOY.md",
}

CURRENT_DOCS = {
    ROOT / "README.md",
    ROOT / "GOAL.md",
    ROOT / "FLOW.md",
    ROOT / "DESIGN.md",
    ROOT / "COSTS.md",
    ROOT / "arc/WORLD-BIBLE.md",
    ROOT / "arc/v5/README.md",
    ROOT / "datapack/README.md",
    ROOT / "resourcepack/README.md",
    ROOT / "tools/README.md",
    ROOT / "plugin/README.md",
    ROOT / "plugin/V5-RUNBOOK.md",
    ROOT / "discord/README.md",
    ROOT / "dashboard/README.md",
    ROOT / "design/content/npc-dialogue.md",
    *(DESIGN / name for name in CURRENT_DESIGN_DOCS),
}

ACTIVE_TOOLS = {
    "audit_all.ps1",
    "build_hold_prologue.py",
    "check_assets.ps1",
    "check_arg_experience_authority.py",
    "check_active_world_canon.py",
    "check_campaign_web.py",
    "check_arg_vertical_slice.py",
    "run_arg_vertical_slice_disposable_paper.py",
    "test_arg_experience_negative_contracts.py",
    "check_deep_hold_fixture_manifest.py",
    "check_deep_hold_layout.py",
    "check_deploy_manifest.ps1",
    "check_external_media_readiness.ps1",
    "check_plugin_jar.ps1",
    "check_repository_integrity.py",
    "check_v5_content.py",
    "check_v5_freshness.py",
    "check_v5_physical_predicates.py",
    "package_assets.ps1",
    "package_launch_bundle.ps1",
    "package_plugin.ps1",
    "render_v5_map_art.py",
    "set_resource_pack_config.ps1",
    "simulate_v5_scenarios.py",
    "write_deploy_manifest.ps1",
}

RETIRED_TOOLS = {
    "apply_launch_coords.ps1": "throw",
    "build_storymap.py": "raise systemexit",
    "build_viz.py": "raise systemexit",
    "new_director_packet.ps1": "throw",
    "new_launch_placement_packet.ps1": "throw",
    "new_rehearsal_packet.ps1": "throw",
    "prepare_friend_launch.ps1": "throw",
    "prepare_server_test.ps1": "throw",
    "rebuild_hold_invitation.ps1": "throw",
}

STALE_VERSION = re.compile(r"(?i)(?:observance[- ]?)?0\.3\.(?:22|29)\b")
RETIRED_RUNTIME = re.compile(
    r"(?i)(?:observance:undercroft|seventh_choice|SeventhChoiceListener|"
    r"undercroft_open|bowed_as_one|prior_witness_ready|rosetta_known)"
)
RETIRED_GENERATOR_NAMES = {
    "apply_launch_coords.ps1",
    "new_director_packet.ps1",
    "new_launch_placement_packet.ps1",
    "new_rehearsal_packet.ps1",
    "prepare_friend_launch.ps1",
    "prepare_server_test.ps1",
    "rebuild_hold_invitation.ps1",
}

FORBIDDEN_FINALE_BRANCH_COMMAND = re.compile(
    r"(?i)/(?:obs|observance)\s+finale\s+arm\s+(?:condemn|understand|free|<[^>]*branch[^>]*>)"
)


def rel(path: Path) -> str:
    return path.relative_to(ROOT).as_posix()


def read(path: Path, failures: list[str]) -> str:
    if not path.is_file():
        failures.append(f"missing required file: {rel(path)}")
        return ""
    try:
        return path.read_text(encoding="utf-8-sig")
    except (OSError, UnicodeDecodeError) as exc:
        failures.append(f"cannot read {rel(path)}: {exc}")
        return ""


def validate_design_labels(failures: list[str]) -> None:
    actual = {path.name for path in DESIGN.glob("*.md")}
    missing_current = CURRENT_DESIGN_DOCS - actual
    if missing_current:
        failures.append(f"current design documents missing: {sorted(missing_current)}")

    for path in sorted(DESIGN.glob("*.md")):
        head = read(path, failures)[:1200].upper()
        if path.name in CURRENT_DESIGN_DOCS:
            if "STATUS:" not in head:
                failures.append(f"{rel(path)} has no explicit current/authority status")
            if "SUPERSEDED PRE-V5" in head or "SUPERSEDED V4" in head:
                failures.append(f"current authority is labeled superseded: {rel(path)}")
        elif "SUPERSEDED" not in head or "ARCHIVE" not in head:
            failures.append(
                f"historical top-level design file lacks a leading SUPERSEDED ... ARCHIVE banner: {rel(path)}"
            )

    content_dir = DESIGN / "content"
    for path in sorted(content_dir.glob("*.md")):
        if path.name == "npc-dialogue.md":
            continue
        head = read(path, failures)[:1200].upper()
        if "SUPERSEDED" not in head or "ARCHIVE" not in head:
            failures.append(f"historical design/content file is not explicitly archived: {rel(path)}")

    prologue = DESIGN / "prologue/PROLOGUE-VIGNETTE.md"
    head = read(prologue, failures)[:1200].upper()
    if "SUPERSEDED" not in head or "ARCHIVE" not in head:
        failures.append("downloadable pre-V5 prologue is not explicitly archived")

    for path in sorted((DESIGN / "research").glob("*.md")):
        if "RESEARCH REFERENCE" not in read(path, failures)[:1200].upper():
            failures.append(f"research file lacks a non-authority banner: {rel(path)}")

    archive_policy = read(DESIGN / "archive/README.md", failures)[:1200].upper()
    if "NEVER PRODUCTION AUTHORITY" not in archive_policy:
        failures.append("design/archive/README.md does not declare the subtree non-authoritative")
    ideas_policy = read(DESIGN / "ideas/README.md", failures)[:1200].upper()
    if "NOTHING HERE IS APPROVED" not in ideas_policy:
        failures.append("design/ideas/README.md does not declare the backlog non-authoritative")


def validate_current_surfaces(failures: list[str]) -> None:
    for path in sorted(CURRENT_DOCS):
        text = read(path, failures)
        if match := STALE_VERSION.search(text):
            line = text.count("\n", 0, match.start()) + 1
            failures.append(f"stale plugin version on current surface {rel(path)}:{line}")
        if path.name != "V5-SUPERSESSION-MAP.md":
            if match := RETIRED_RUNTIME.search(text):
                line = text.count("\n", 0, match.start()) + 1
                failures.append(f"retired runtime token on current surface {rel(path)}:{line}: {match.group(0)}")
        if path.name != "README.md" or path.parent.name != "tools":
            for retired in RETIRED_GENERATOR_NAMES:
                if retired in text:
                    failures.append(f"current surface {rel(path)} directs readers to retired tool {retired}")
        if match := FORBIDDEN_FINALE_BRANCH_COMMAND.search(text):
            line = text.count("\n", 0, match.start()) + 1
            failures.append(
                f"current surface lets an operator choose a finale branch {rel(path)}:{line}; "
                "V5 arm may accept only an optional timeout"
            )


def validate_tools(failures: list[str]) -> None:
    tools = ROOT / "tools"
    for name in sorted(ACTIVE_TOOLS):
        text = read(tools / name, failures)
        if match := STALE_VERSION.search(text):
            line = text.count("\n", 0, match.start()) + 1
            failures.append(f"active tool contains stale plugin version tools/{name}:{line}")

    audit = read(tools / "audit_all.ps1", failures)
    required = {
        "check_repository_integrity.py",
        "check_arg_experience_authority.py",
        "check_active_world_canon.py",
        "check_campaign_web.py",
        "test_arg_experience_negative_contracts.py",
        "check_v5_freshness.py",
        "check_v5_content.py",
        "check_v5_physical_predicates.py",
        "render_v5_map_art.py",
        "check_deep_hold_layout.py",
        "simulate_v5_scenarios.py",
        "check_assets.ps1",
        "check_external_media_readiness.ps1",
        'gradlew.bat") @("clean", "check", "build", "--no-daemon")',
        'npm.cmd" @("run", "audit")',
        'npm.cmd" @("run", "build")',
    }
    for token in sorted(required):
        if token not in audit:
            failures.append(f"tools/audit_all.ps1 omits required V5 step: {token}")
    for retired in RETIRED_GENERATOR_NAMES:
        if retired in audit:
            failures.append(f"tools/audit_all.ps1 invokes or mentions retired tool {retired}")

    bundle = read(tools / "package_launch_bundle.ps1", failures)
    for token in (
        "check_v5_freshness.py", "check_v5_content.py", "--runtime",
        "check_v5_physical_predicates.py", "render_v5_map_art.py", "check_deep_hold_layout.py",
        "simulate_v5_scenarios.py", "check_external_media_readiness.ps1",
    ):
        if token not in bundle:
            failures.append(f"tools/package_launch_bundle.ps1 omits V5 guard {token}")

    for name, guard in RETIRED_TOOLS.items():
        text = read(tools / name, failures)
        folded = text.casefold()
        marker = min(
            (index for phrase in ("retired_pre_v5_tool", "retired pre-v5 tool")
             if (index := folded.find(phrase)) >= 0),
            default=-1,
        )
        guard_index = folded.find(guard)
        if marker < 0 or guard_index < 0:
            failures.append(f"retired tool has no explicit terminating guard: tools/{name}")
            continue
        # The guard must appear near the beginning, before the historical body can create/write/remove.
        if guard_index > 1600:
            failures.append(f"retired tool guard is too late to be safe: tools/{name}")
        mutation_indices = [
            index for token in ("remove-item", "new-item", "writeall", "write_text(", "copy-item")
            if (index := folded.find(token)) >= 0
        ]
        if mutation_indices and guard_index > min(mutation_indices):
            failures.append(f"retired tool can mutate before terminating: tools/{name}")


def validate_versions_and_generated_paths(failures: list[str]) -> None:
    build_gradle = read(ROOT / "plugin/build.gradle", failures)
    match = re.search(r"(?m)^version\s*=\s*['\"]([^'\"]+)['\"]", build_gradle)
    if not match or match.group(1) != "0.5.0":
        failures.append(f"plugin/build.gradle must declare exactly 0.5.0; found {match.group(1) if match else 'none'}")

    expected_formats = {
        ROOT / "datapack/observance/pack.mcmeta": ([94, 1], [94, 1]),
        ROOT / "resourcepack/pack.mcmeta": ([75, 0], [75, 0]),
    }
    for path, (minimum, maximum) in expected_formats.items():
        text = read(path, failures)
        if not text:
            continue
        try:
            pack = json.loads(text)["pack"]
        except (json.JSONDecodeError, KeyError, TypeError) as exc:
            failures.append(f"invalid pack metadata {rel(path)}: {exc}")
            continue
        if pack.get("min_format") != minimum or pack.get("max_format") != maximum:
            failures.append(
                f"{rel(path)} format drift: {pack.get('min_format')}/{pack.get('max_format')}"
            )

    retired_data_roots = (
        ROOT / "datapack/observance/data/observance/dimension",
        ROOT / "datapack/observance/data/observance/dimension_type",
        ROOT / "datapack/observance/data/observance/worldgen",
        ROOT / "datapack/observance/data/observance/advancement",
    )
    for path in retired_data_roots:
        if path.exists() and any(child.is_file() for child in path.rglob("*")):
            failures.append(f"retired pre-V5 datapack content exists: {rel(path)}")

    try:
        raw = subprocess.check_output(
            ["git", "ls-files", "-z", "--", "rehearsals", "build/launch-placement"],
            cwd=ROOT,
        )
    except (OSError, subprocess.CalledProcessError) as exc:
        failures.append(f"cannot inspect tracked generated packets: {exc}")
    else:
        tracked = [item.decode("utf-8") for item in raw.split(b"\0") if item]
        if tracked:
            failures.append(f"generated launch/rehearsal packet files are tracked: {tracked}")


def validate_operational_surfaces(failures: list[str]) -> None:
    operational = (
        ROOT / "plugin/src/main/java/com/observance/watcher/ObservancePlugin.java",
        ROOT / "plugin/src/main/java/com/observance/watcher/command/ObservanceCommand.java",
        ROOT / "plugin/src/main/resources/config.yml",
        ROOT / "plugin/src/main/resources/plugin.yml",
    )
    for path in operational:
        text = read(path, failures)
        if match := STALE_VERSION.search(text):
            line = text.count("\n", 0, match.start()) + 1
            failures.append(f"stale plugin version on operational surface {rel(path)}:{line}")
        for retired in RETIRED_GENERATOR_NAMES:
            if retired in text:
                line = text.count("\n", 0, text.index(retired)) + 1
                failures.append(f"operational surface names retired generator {rel(path)}:{line}: {retired}")

    config = read(ROOT / "plugin/src/main/resources/config.yml", failures)
    for token in ("seventh_choice", "bowed_as_one", "rosetta_known", "undercroft_open", "prior_witness_ready"):
        if token.casefold() in config.casefold():
            failures.append(f"plugin production config still contains retired V4 flag/config token: {token}")

    command = read(ROOT / "plugin/src/main/java/com/observance/watcher/command/ObservanceCommand.java", failures)
    retired_match = re.search(
        r"V5_RETIRED_COMMANDS\s*=\s*Set\.of\((.*?)\);",
        command,
        flags=re.DOTALL,
    )
    retired_body = retired_match.group(1) if retired_match else ""
    for dangerous in ("director", "coverage", "flag", "site"):
        if f'"{dangerous}"' not in retired_body:
            failures.append(
                f"legacy /obs {dangerous} is not fail-closed in V5_RETIRED_COMMANDS"
            )
    if command.count("auditV5PostPlacement(") < 2:
        failures.append("the exact V5 post-placement fixture/sign/orientation readback has no production caller")

    setup = read(ROOT / "design/V5-WORLD-SETUP-AND-TESTING.md", failures)
    first_prepare = setup.find("/obs placehold prepare")
    first_plan = setup.find("/obs placehold plan")
    if first_prepare < 0 or first_plan < 0 or first_prepare > first_plan:
        failures.append("V5 world setup does not require placehold prepare before placehold plan")
    live_matrix = read(ROOT / "design/V5-LIVE-TEST-MATRIX.csv", failures)
    b003 = next((line for line in live_matrix.splitlines() if line.startswith("B003,")), "")
    if "placehold prepare" not in b003 or "placehold plan" not in b003:
        failures.append("V5 live test B003 does not exercise prepare then plan")
    for test_id in ("B010", "B011", "B012"):
        row = next((line for line in live_matrix.splitlines() if line.startswith(f"{test_id},")), "")
        if "placehold prepare" not in row or "PREPARE PASS" not in row:
            failures.append(f"V5 live test {test_id} does not prepare the persisted footprint before audit/repair")
    for test_id in ("E004", "E005", "E006", "E007", "E008", "E009"):
        row = next((line for line in live_matrix.splitlines() if line.startswith(f"{test_id},")), "")
        if "each conduct verdict" not in row and "all four conduct" not in row:
            failures.append(f"V5 live finale test {test_id} does not cover all four conduct renderings")

    master = read(ROOT / "design/ARG-V5-MASTER-PLAN.md", failures)
    lifecycle = master[master.find("## Safe build lifecycle"):master.find("## Protection and failure rules")]
    if lifecycle.find("**Prepare:**") < 0 or lifecycle.find("**Plan:**") < 0 \
            or lifecycle.find("**Prepare:**") > lifecycle.find("**Plan:**"):
        failures.append("V5 master lifecycle does not require prepare before plan")

    launch = read(ROOT / "design/V5-PRODUCTION-LAUNCH-RUNBOOK.md", failures)
    if "/support/ticket.php?id=1842" in launch:
        failures.append("V5 launch runbook uses service id 1842 as the support ticket id")
    if "POST /record/terminal/inscribe" not in launch or "410" not in launch:
        failures.append("V5 launch runbook does not test the terminal's read-only HTTP 410 contract")
    for required_secret in ("AUTHOR_USERNAME", "AUTHOR_PASSWORD"):
        if required_secret not in launch:
            failures.append(f"V5 launch runbook omits required Vercel secret {required_secret}")

    deploy_writer = read(ROOT / "tools/write_deploy_manifest.ps1", failures)
    deploy_check = read(ROOT / "tools/check_deploy_manifest.ps1", failures)
    if "sha256" not in deploy_writer.casefold() or "sha256" not in deploy_check.casefold():
        failures.append("deploy receipt toolchain does not emit and verify SHA-256")


def main() -> int:
    failures: list[str] = []
    validate_design_labels(failures)
    validate_current_surfaces(failures)
    validate_tools(failures)
    validate_versions_and_generated_paths(failures)
    validate_operational_surfaces(failures)

    if failures:
        print(f"V5 FRESHNESS CHECK: FAIL ({len(failures)} issue(s))")
        for failure in failures:
            print(f"- {failure}")
        return 1
    print(
        "V5 FRESHNESS CHECK: PASS - current/archive boundaries explicit; active toolchain is V5-only; "
        "10 retired generators terminate before mutation; versions and pack formats exact"
    )
    return 0


if __name__ == "__main__":
    raise SystemExit(main())
