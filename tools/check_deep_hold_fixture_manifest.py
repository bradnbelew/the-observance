#!/usr/bin/env python3
"""Cross-check the V5 Deep Hold spatial CSVs against the executable 32-room plan.

The Java plan class keeps its historical name for binary/source compatibility; this validator treats
only the V5 ownership, gate-condition, and content authorities as production semantics.
"""

from __future__ import annotations

import csv
import re
from pathlib import Path


ROOT = Path(__file__).resolve().parents[1]
PLAN = ROOT / "plugin/src/main/java/com/observance/watcher/structure/DeepHoldV4Plan.java"
FIXTURES = ROOT / "design/DEEP-HOLD-FIXTURE-MANIFEST.csv"
BOXES = ROOT / "design/DEEP-HOLD-ROOM-BOXES.csv"
RECORDS = ROOT / "design/DEEP-HOLD-RECORD-STATION-MANIFEST.csv"
GATES = ROOT / "design/DEEP-HOLD-GATE-MANIFEST.csv"


def rows(path: Path) -> list[dict[str, str]]:
    with path.open(encoding="utf-8", newline="") as handle:
        return list(csv.DictReader(handle))


def front(anchor: tuple[int, int, int], stand: tuple[int, int, int]) -> str:
    dx, dz = stand[0] - anchor[0], stand[2] - anchor[2]
    if abs(dx) == abs(dz):
        return "NONE" if dx == 0 else "DIAGONAL"
    if abs(dx) > abs(dz):
        return "EAST" if dx > 0 else "WEST"
    return "SOUTH" if dz > 0 else "NORTH"


def point(row: dict[str, str], prefix: str = "") -> tuple[int, int, int]:
    return tuple(int(row[prefix + axis]) for axis in ("x", "y", "z"))  # type: ignore[return-value]


def inside(point_: tuple[int, int, int], box: dict[str, str]) -> bool:
    x, y, z = point_
    return (int(box["min_x"]) <= x <= int(box["max_x"])
            and int(box["min_y"]) <= y <= int(box["max_y"])
            and int(box["min_z"]) <= z <= int(box["max_z"]))


def main() -> int:
    source = PLAN.read_text(encoding="utf-8")
    fixture_rows, room_rows, record_rows, gate_rows = (
        rows(FIXTURES), rows(BOXES), rows(RECORDS), rows(GATES)
    )
    failures: list[str] = []

    fixture_pattern = re.compile(
        r'fixture\("([^"]+)",\s*"([^"]+)",\s*"([^"]+)",\s*'
        r'(-?\d+),\s*(-?\d+),\s*(-?\d+),\s*(\d+),\s*(\d+),\s*'
        r'"([A-Z]+)",\s*(-?\d+),\s*(-?\d+),\s*(-?\d+),\s*"([^"]+)"\)'
    )
    plan_fixtures = {
        m.group(1): (m.group(3), *(int(m.group(i)) for i in range(4, 7)),
                     m.group(9), *(int(m.group(i)) for i in range(10, 13)), m.group(13))
        for m in fixture_pattern.finditer(source)
    }
    room_pattern = re.compile(
        r'new Room\("([^"]+)",\s*(-?\d+),\s*(-?\d+),\s*(-?\d+),\s*'
        r'(-?\d+),\s*(-?\d+),\s*(-?\d+),\s*"([^"]+)"\)'
    )
    plan_rooms = {
        m.group(1): tuple(int(m.group(i)) for i in range(2, 8))
        for m in room_pattern.finditer(source)
    }
    record_pattern = re.compile(
        r'new RecordStation\("([^"]+)",\s*"([^"]+)",\s*(-?\d+),\s*'
        r'(-?\d+),\s*(-?\d+),\s*"([A-Z]+)",\s*"([^"]+)"\)'
    )
    plan_records = {
        m.group(1): (m.group(2), *(int(m.group(i)) for i in range(3, 6)), m.group(6), m.group(7))
        for m in record_pattern.finditer(source)
    }
    plan_gate_ids = set(re.findall(r'new Gate\("([^"]+)"', source))
    # Serialized spatial gate IDs predate V5. They are geometry identifiers only;
    # role/open_condition in the current CSV own the player-facing semantics.
    runtime_to_plan = {
        "keeper": "g1", "archive": "g2", "undercroft": "g3", "deep": "g4",
        "prior": "prior", "dread": "dread", "accepting": "g5", "coda": "g6",
    }

    if len(plan_fixtures) != 76 or len(fixture_rows) != 76:
        failures.append(f"expected 76 fixtures; source={len(plan_fixtures)} csv={len(fixture_rows)}")
    if len(plan_rooms) != 32 or len(room_rows) != 32:
        failures.append(f"expected 32 rooms; source={len(plan_rooms)} csv={len(room_rows)}")
    if len(plan_records) != 7 or len(record_rows) != 7:
        failures.append(f"expected 7 district records; source={len(plan_records)} csv={len(record_rows)}")
    if len(plan_gate_ids) != 8 or len(gate_rows) != 8:
        failures.append(f"expected 8 gates; source={len(plan_gate_ids)} csv={len(gate_rows)}")

    boxes = {row["room_id"]: row for row in room_rows}
    for room_id, expected in plan_rooms.items():
        row = boxes.get(room_id)
        if row is None:
            failures.append(f"missing room row: {room_id}")
            continue
        actual = tuple(int(row[key]) for key in
                       ("min_x", "max_x", "min_y", "max_y", "min_z", "max_z"))
        if actual != expected:
            failures.append(f"room {room_id}: csv {actual} != source {expected}")

    manifest_fixture_ids: set[str] = set()
    occupied: dict[tuple[int, int, int], str] = {}
    for row in fixture_rows:
        site_id = row["site_id"]
        if site_id in manifest_fixture_ids:
            failures.append(f"duplicate fixture id: {site_id}")
        manifest_fixture_ids.add(site_id)
        expected = plan_fixtures.get(site_id)
        actual = (row["room_id"], *point(row), row["expected_front"],
                  *point(row, "stand_"), row["content_role"])
        if expected is None:
            failures.append(f"fixture absent from source: {site_id}")
        elif actual != expected:
            failures.append(f"fixture {site_id}: csv does not match executable source")
        box = boxes.get(row["room_id"])
        if box is None or not inside(point(row), box) or not inside(point(row, "stand_"), box):
            failures.append(f"fixture {site_id}: anchor or standing point outside owner room")
        derived = front(point(row), point(row, "stand_"))
        if derived != row["expected_front"]:
            failures.append(f"fixture {site_id}: front {row['expected_front']} derives {derived}")
        if point(row) in occupied:
            failures.append(f"fixture {site_id}: duplicate anchor with {occupied[point(row)]}")
        occupied[point(row)] = site_id
    if manifest_fixture_ids != set(plan_fixtures):
        failures.append("fixture id sets differ between CSV and executable plan")

    for row in record_rows:
        station_id = row["station_id"]
        expected = plan_records.get(station_id)
        actual = (row["room_id"], *point(row), row["expected_front"], row["title"])
        if expected != actual:
            failures.append(f"record {station_id}: csv does not match executable source")
        box = boxes.get(row["room_id"])
        if box is None or not inside(point(row), box) or not inside(point(row, "stand_"), box):
            failures.append(f"record {station_id}: anchor or standing point outside owner room")
        if front(point(row), point(row, "stand_")) != row["expected_front"]:
            failures.append(f"record {station_id}: player-frame front mismatch")
    if {r["station_id"] for r in record_rows} != set(plan_records):
        failures.append("record id sets differ between CSV and executable plan")

    mapped_gate_ids = {runtime_to_plan.get(row["gate_id"], "") for row in gate_rows}
    if mapped_gate_ids != plan_gate_ids:
        failures.append(f"gate id sets differ: manifest maps to {sorted(mapped_gate_ids)}, source={sorted(plan_gate_ids)}")
    for row in gate_rows:
        if not row["open_condition"].strip() or row["initial_state"] != "sealed":
            failures.append(f"gate {row['gate_id']}: must have condition and sealed initial state")
        for axis in ("x", "y", "z"):
            if int(row[f"min_{axis}"]) > int(row[f"max_{axis}"]):
                failures.append(f"gate {row['gate_id']}: inverted {axis} bounds")

    print(f"Deep Hold V5 spatial manifest: {len(fixture_rows)} fixtures, {len(room_rows)} rooms, "
          f"{len(record_rows)} records, {len(gate_rows)} gates")
    if failures:
        for failure in failures:
            print("  FAIL " + failure)
        return 1
    print("  PASS: executable coordinates, ownership, player frames, unique anchors, and gate ids agree")
    return 0


if __name__ == "__main__":
    raise SystemExit(main())
