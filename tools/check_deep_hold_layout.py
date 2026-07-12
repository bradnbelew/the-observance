#!/usr/bin/env python3
from __future__ import annotations

from dataclasses import dataclass
from pathlib import Path
import re
import sys
import csv


ROOT = Path(__file__).resolve().parents[1]
COMMAND_FILE = ROOT / "plugin" / "src" / "main" / "java" / "com" / "observance" / "watcher" / "command" / "ObservanceCommand.java"
ANSWER_SIGN_LISTENER_FILE = ROOT / "plugin" / "src" / "main" / "java" / "com" / "observance" / "watcher" / "signal" / "listener" / "AnswerSignListener.java"
PUZZLES_FILE = ROOT / "discord" / "supabase" / "seeds" / "puzzles_seed.sql"
METAPUZZLE_FILE = ROOT / "discord" / "supabase" / "seeds" / "metapuzzle_seed.sql"
HINTS_FILE = ROOT / "discord" / "supabase" / "seeds" / "hints_seed.sql"
DASHBOARD_FILE = ROOT / "dashboard" / "src" / "components" / "author" / "PriorAcceptingProgress.tsx"
CONFIG_FILE = ROOT / "plugin" / "src" / "main" / "resources" / "config.yml"
LECTERN_LISTENER_FILE = ROOT / "plugin" / "src" / "main" / "java" / "com" / "observance" / "watcher" / "signal" / "listener" / "LecternLockListener.java"
GROUP_WALK_FILE = ROOT / "plugin" / "src" / "main" / "java" / "com" / "observance" / "watcher" / "signal" / "listener" / "GroupWalkListener.java"


@dataclass(frozen=True)
class HoldSite:
    id: str
    type: str
    radius: int
    vertical: int
    x: int
    y: int
    z: int
    half_x: int
    half_z: int


@dataclass(frozen=True)
class HoldGate:
    id: str
    x: int
    y: int
    z: int
    open_initially: bool
    label: str


@dataclass(frozen=True)
class HoldGateSpan:
    across_x: bool
    half_across: int
    height: int
    depth: int
    door_half: int


failures: list[str] = []


def fail(message: str) -> None:
    failures.append(message)


def read(path: Path) -> str:
    if not path.exists():
        fail(f"missing required file: {path.relative_to(ROOT)}")
        return ""
    return path.read_text(encoding="utf-8")


def require_text(label: str, text: str, needle: str) -> None:
    if needle not in text:
        fail(f"{label} missing expected text: {needle}")


def parse_sites(source: str) -> dict[str, HoldSite]:
    sites: dict[str, HoldSite] = {}
    pattern = re.compile(
        r'new\s+HoldSite\(\s*"([^"]+)"\s*,\s*"([^"]+)"\s*,\s*'
        r"(-?\d+)\s*,\s*(-?\d+)\s*,\s*(-?\d+)\s*,\s*(-?\d+)\s*,\s*(-?\d+)\s*,\s*(-?\d+)\s*,\s*(-?\d+)\s*\)"
    )
    for match in pattern.finditer(source):
        site = HoldSite(
            id=match.group(1),
            type=match.group(2),
            radius=int(match.group(3)),
            vertical=int(match.group(4)),
            x=int(match.group(5)),
            y=int(match.group(6)),
            z=int(match.group(7)),
            half_x=int(match.group(8)),
            half_z=int(match.group(9)),
        )
        if site.id in sites:
            fail(f"duplicate HoldSite id: {site.id}")
        sites[site.id] = site
    return sites


def parse_gates(source: str) -> dict[str, HoldGate]:
    gates: dict[str, HoldGate] = {}
    pattern = re.compile(
        r'new\s+HoldGate\(\s*"([^"]+)"\s*,\s*'
        r"(-?\d+)\s*,\s*(-?\d+)\s*,\s*(-?\d+)\s*,\s*(true|false)\s*,\s*"
        r'"([^"]+)"\s*\)'
    )
    for match in pattern.finditer(source):
        gate = HoldGate(
            id=match.group(1),
            x=int(match.group(2)),
            y=int(match.group(3)),
            z=int(match.group(4)),
            open_initially=match.group(5) == "true",
            label=match.group(6),
        )
        if gate.id in gates:
            fail(f"duplicate HoldGate id: {gate.id}")
        gates[gate.id] = gate
    return gates


def parse_spans(source: str) -> dict[str, HoldGateSpan]:
    spans: dict[str, HoldGateSpan] = {}
    pattern = re.compile(
        r"case\s+((?:\"[^\"]+\"\s*,\s*)*\"[^\"]+\")\s*->\s*new\s+HoldGateSpan\("
        r"\s*(true|false)\s*,\s*(-?\d+)\s*,\s*(-?\d+)\s*,\s*(-?\d+)\s*,\s*(-?\d+)\s*\)"
    )
    for match in pattern.finditer(source):
        span = HoldGateSpan(
            across_x=match.group(2) == "true",
            half_across=int(match.group(3)),
            height=int(match.group(4)),
            depth=int(match.group(5)),
            door_half=int(match.group(6)),
        )
        for gate_id in re.findall(r'"([^"]+)"', match.group(1)):
            spans[gate_id] = span
    return spans


def room_half_x(site: HoldSite) -> int:
    return max(12, site.half_x + 3)


def room_half_z(site: HoldSite) -> int:
    return max(11, site.half_z + 3)


def room_front(site: HoldSite) -> str:
    if site.x < 0:
        return "EAST"
    if site.x > 0:
        return "WEST"
    return "SOUTH" if site.z < 0 else "NORTH"


def x_min(site: HoldSite) -> int:
    return site.x - room_half_x(site)


def x_max(site: HoldSite) -> int:
    return site.x + room_half_x(site)


def z_min(site: HoldSite) -> int:
    return site.z - room_half_z(site)


def z_max(site: HoldSite) -> int:
    return site.z + room_half_z(site)


def source_slice(text: str, start_needle: str, end_needle: str) -> str:
    start = text.find(start_needle)
    if start < 0:
        return ""
    end = text.find(end_needle, start + len(start_needle))
    if end < 0:
        return text[start:]
    return text[start:end]


def check_layout(source: str, answer_listener: str, sites: dict[str, HoldSite], gates: dict[str, HoldGate],
                 spans: dict[str, HoldGateSpan]) -> None:
    for site_id in (
        "case_board",
        "prior_camp",
        "lampworks_stair",
        "threshold_vault",
        "failed_accepting",
        "unbroken_light",
        "the_threshold",
        "the_unwriting",
    ):
        if site_id not in sites:
            fail(f"missing required Deep Hold site: {site_id}")
    for gate_id in ("archive", "prior", "deep", "threshold", "accepting"):
        if gate_id not in gates:
            fail(f"missing required Deep Hold gate: {gate_id}")
        if gate_id not in spans:
            fail(f"missing required HoldGateSpan case: {gate_id}")
    if failures:
        return

    case_board = sites["case_board"]
    prior = sites["prior_camp"]
    lampworks = sites["lampworks_stair"]
    failed = sites["failed_accepting"]
    vault = sites["threshold_vault"]
    unbroken = sites["unbroken_light"]

    prior_gate = gates["prior"]
    prior_span = spans["prior"]
    threshold_gate = gates["threshold"]
    threshold_span = spans["threshold"]
    accepting_gate = gates["accepting"]

    if prior.type != "prior_camp":
        fail("prior_camp must keep type prior_camp so the bespoke camp builder runs")
    if prior.x != 0 or prior.y != 0 or prior.z <= case_board.z or prior.z >= lampworks.z:
        fail("prior_camp must sit on the central post-case route before lampworks")
    if prior.half_x < 16 or prior.half_z < 10 or prior.radius < 18:
        fail("prior_camp must remain large enough for bedrolls, barrels, lecterns, and the blank witness place")
    if room_front(prior) != "NORTH":
        fail("prior_camp should face north toward the central route")

    prior_front_z = z_min(prior)
    prior_gate_back_z = prior_gate.z + prior_span.depth
    if not (case_board.z < prior_gate.z < prior.z):
        fail("prior gate must sit between the case board and prior camp")
    if not (prior_gate.z <= prior_front_z <= prior_gate_back_z + 1):
        fail(
            "prior gate must physically cover the prior camp mouth "
            f"(gate z {prior_gate.z}..{prior_gate_back_z}, camp front z {prior_front_z})"
        )
    if not prior_span.across_x or prior_span.half_across < 12 or prior_span.height < 11:
        fail("prior gate span must be a tall X-spanning local gatehouse")
    if prior_span.door_half < 4:
        fail("prior gate doorHalf must remain a genuinely walkable opening")
    if prior_span.half_across - prior_span.door_half < 8:
        fail("prior gate needs real side wall thickness around its broad opening")

    if failed.type != "failed_accepting":
        fail("failed_accepting must keep type failed_accepting so the bespoke lower-floor builder runs")
    if failed.x != 0 or failed.y >= 0:
        fail("failed_accepting must stay centered and below the upper Hold level")
    if room_front(failed) != "NORTH":
        fail("failed_accepting should face north toward the lower route")
    if not (threshold_gate.z + threshold_span.depth < z_min(failed) < z_max(failed) < accepting_gate.z):
        fail(
            "failed_accepting must be after threshold but before the accepting gate "
            f"(threshold z {threshold_gate.z}, failed z {z_min(failed)}..{z_max(failed)}, accepting z {accepting_gate.z})"
        )
    if z_max(failed) > z_min(unbroken):
        fail("failed_accepting must not overlap the unbroken_light accepting floor")
    if x_max(failed) >= x_min(vault):
        fail("failed_accepting must not collide with the threshold_vault side chamber")

    require_text("ObservanceCommand.java", source, 'case "prior" -> new HoldGateSpan(true, 12, 11, 3, 4);')
    for needle in (
        'new HoldRecordStation("prior_roster"',
        '"prior roster", "no witness"',
        "private void buildHoldPriorCampCore",
        "private void buildHoldFailedAcceptingCore",
        "private void placeEditableStandingSign",
        "private void placePriorBedrollPacket",
        "hasHoldGateSideBypass",
        "hasHoldGateOverBypass",
        "has walkable air around the sealed return wall",
        "has walkable air over the sealed bulkhead",
        "countEditableSignsNear(loc, Math.max(3, site.radius())) < 7",
        "countEditableSignsNear(Location loc, int radius, int cap)",
        "seven editable filing signs",
        "failed inventory",
        "vaun packet",
        "mara packet",
        "sella packet",
        "orin packet",
        "brann packet",
        "iss packet",
        "Compare cold proof before warm speech.",
        'new String[]{"file", "missing", "condition", ""}',
        'new String[]{"file", "correction", "here", ""}',
        'new String[]{"file", "vaun", "repair", ""}',
        'new String[]{"file", "mara", "repair", ""}',
        'new String[]{"file", "sella", "repair", ""}',
        'new String[]{"file", "orin", "repair", ""}',
        'new String[]{"file", "brann", "repair", ""}',
        'new String[]{"file", "iss", "repair", ""}',
        'new String[]{"file", "witness", "condition", ""}',
        "The unwaxed sign between the labels is a filing slit",
        "The unwaxed signs by the repair files are filing slits",
        "File the repair as order, not math: return first before count.",
        "File the repair as action before archive: walk it before filing it.",
        "File the repair as count order: count the seventh before the six.",
        "File the repair as posture, not tribute: bowing is proof, not payment.",
        "File the repair as duration: the watch must be kept.",
        "File the repair as cross-check: test warmth against the land.",
        "The floor took their tokens and returned nothing.",
        "witness before accepting",
    ):
        require_text("ObservanceCommand.java", source, needle)
    for needle in (
        "TYPE_CASE_BOARD",
        "TYPE_PRIOR_CAMP",
        "TYPE_FAILED_ACCEPTING",
        "case_board",
        "prior_camp",
        "failed_accepting",
        "ANSWER_SITE_TYPES",
    ):
        require_text("AnswerSignListener.java", answer_listener, needle)

    sync_body = source_slice(source, "private void syncPlaceHoldGates", "\n    private void handlePlaceHoldAudit")
    if not sync_body:
        fail("syncPlaceHoldGates method could not be audited")
    else:
        require_text("syncPlaceHoldGates", sync_body, 'directorFlag(flags, "prior_absence_known")')
        require_text("syncPlaceHoldGates", sync_body, 'directorFlag(flags, "prior_camp_read")')
        require_text("syncPlaceHoldGates", sync_body, 'directorFlag(flags, "prior_witness_ready")')
        if "boolean acceptingOpen = acceptingReady &&" not in sync_body:
            fail("accepting gate must require prior_witness_ready through acceptingReady")


def check_sql_and_dashboard(puzzles: str, metapuzzle: str, hints: str, dashboard: str) -> None:
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
    for puzzle_id, (answer, flag) in expected_puzzles.items():
        require_text("puzzles_seed.sql", puzzles, f"'{puzzle_id}'")
        require_text("puzzles_seed.sql", puzzles, f"'{answer}'")
        require_text("puzzles_seed.sql", puzzles, f"'{flag}'")
        require_text("hints_seed.sql", hints, f"'{puzzle_id}'")

    for needle in (
        "where puzzle_key = 'prior-absence'",
        "where puzzle_key = 'prior-camp-refusal'",
        "'prior_camp_read', true",
        "'prior_vaun_corrected', true",
        "'prior_mara_corrected', true",
        "'prior_sella_corrected', true",
        "'prior_orin_corrected', true",
        "'prior_brann_corrected', true",
        "'prior_iss_corrected', true",
        "'prior_witness_ready', true",
        "where puzzle_key = 'rite-tokens'",
    ):
        require_text("metapuzzle_seed.sql", metapuzzle, needle)

    for needle in (
        "prior_absence_known",
        "prior_camp_read",
        "prior_vaun_corrected",
        "prior_mara_corrected",
        "prior_sella_corrected",
        "prior_orin_corrected",
        "prior_brann_corrected",
        "prior_iss_corrected",
        "prior_witness_ready",
        "blocks rite-tokens",
    ):
        require_text("PriorAcceptingProgress.tsx", dashboard, needle)


def check_layout_v2(source: str, answer_listener: str, sites: dict[str, HoldSite],
                    gates: dict[str, HoldGate], spans: dict[str, HoldGateSpan]) -> None:
    fixture_path = ROOT / "design" / "DEEP-HOLD-FIXTURE-MANIFEST.csv"
    with fixture_path.open(encoding="utf-8", newline="") as handle:
        fixtures = list(csv.DictReader(handle))
    if len(fixtures) != 76:
        fail(f"V2 fixture manifest must contain 76 sites, found {len(fixtures)}")
    for row in fixtures:
        site = sites.get(row["site_id"])
        if site is None:
            fail(f"missing V2 fixture site: {row['site_id']}")
            continue
        expected = (int(row["x"]), int(row["y"]), int(row["z"]))
        if (site.x, site.y, site.z) != expected:
            fail(f"{site.id} executable anchor {(site.x, site.y, site.z)} != manifest {expected}")

    expected_gates = {
        "entry": (0, -24, 99, True), "keeper": (0, -28, 152, False),
        "archive": (0, -24, 292, False), "undercroft": (0, -24, 506, False),
        "deep": (0, -28, 589, False), "prior": (-150, -28, 648, False),
        "dread": (120, -28, 602, False), "accepting": (0, -32, 774, False),
        "coda": (0, -32, 876, False),
    }
    for gate_id, expected in expected_gates.items():
        gate = gates.get(gate_id)
        if gate is None:
            fail(f"missing V2 gate: {gate_id}")
            continue
        actual = (gate.x, gate.y, gate.z, gate.open_initially)
        if actual != expected:
            fail(f"{gate_id} executable gate {actual} != V2 contract {expected}")
        if gate_id not in spans:
            fail(f"missing explicit HoldGateSpan case: {gate_id}")

    build = source_slice(source, "private int buildDeepHold", "\n    private void registerHoldRegion")
    for needle in ("buildHoldV2Shells", "buildHoldV2Mouth", "by - 36", "loadHoldChunks"):
        require_text("buildDeepHold", build, needle)
    if "buildHoldSpine(" in build or "buildHoldSurfaceStair(" in build:
        fail("V2 buildDeepHold still invokes a legacy overlapping shell/stair builder")
    for needle in (
        "private void buildHoldOwnedRoom", "private void buildHoldDreadPassage",
        "private void dressHoldOwnedRoomArchitecture",
        "private void buildMaraD05Shelf", "private void placeKeeperRiteToken",
        'case "coda" -> new HoldGateSpan', "syncPlaceHoldGatesAutomatically",
    ):
        require_text("ObservanceCommand.java", source, needle)
    for needle in ("TYPE_CASE_BOARD", "TYPE_PRIOR_CAMP", "TYPE_FAILED_ACCEPTING", "ANSWER_SITE_TYPES"):
        require_text("AnswerSignListener.java", answer_listener, needle)


def main() -> int:
    source = read(COMMAND_FILE)
    answer_listener = read(ANSWER_SIGN_LISTENER_FILE)
    puzzles = read(PUZZLES_FILE)
    metapuzzle = read(METAPUZZLE_FILE)
    hints = read(HINTS_FILE)
    dashboard = read(DASHBOARD_FILE)
    config = read(CONFIG_FILE)
    lectern_listener = read(LECTERN_LISTENER_FILE)
    group_walk = read(GROUP_WALK_FILE)
    if failures:
        print("deep hold layout check: FAILED")
        for failure in failures:
            print(f"  - {failure}")
        return 1

    sites = parse_sites(source)
    gates = parse_gates(source)
    spans = parse_spans(source)
    if not sites:
        fail("no HoldSite definitions parsed from ObservanceCommand.java")
    if not gates:
        fail("no HoldGate definitions parsed from ObservanceCommand.java")
    if not spans:
        fail("no HoldGateSpan cases parsed from ObservanceCommand.java")

    check_layout_v2(source, answer_listener, sites, gates, spans)
    check_sql_and_dashboard(puzzles, metapuzzle, hints, dashboard)

    for i in range(1, 6):
        if f"sella_lectern_{i}" not in sites:
            fail(f"missing Sella overlay lectern {i}")
    for i in range(1, 5):
        if f"mara_route_marker_{i}" not in sites:
            fail(f"missing Mara route checkpoint {i}")
    for needle in (
        "sella-overlay-lake:",
        'token: "s3k9 vq2m x7d4 p1n6 the rings she counted"',
        "ring-pages: [2, 3, 5, 7, 11]",
        "dial-rotations: [0, 2, 4, 6, 1, 5]",
    ):
        require_text("config.yml", config, needle)
    require_text("LecternLockListener.java", lectern_listener, "private final String lecternType")
    require_text("GroupWalkListener.java", group_walk, 'ROUTE_TYPE = "mara_route_marker"')
    require_text("GroupWalkListener.java", group_walk, "routeProgress")
    require_text("ObservanceCommand.java", source, "syncPlaceHoldGatesAutomatically")

    if failures:
        print("deep hold layout check: FAILED")
        for failure in failures:
            print(f"  - {failure}")
        return 1

    print(
        "deep hold layout check: OK - "
        f"{len(sites)} Hold sites, {len(gates)} gates, V2 ownership/progression wired"
    )
    print(
        "  surface mouth y=origin; deepest authored floor=origin-32; "
        "entry -> keepers -> archive -> lower works -> prior/Accepting -> Unwriting"
    )
    return 0


if __name__ == "__main__":
    sys.exit(main())
