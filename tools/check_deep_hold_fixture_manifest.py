#!/usr/bin/env python3
"""Validate the rebuild fixture/player-frame manifest without trusting prose claims."""

from __future__ import annotations

import csv
import re
from pathlib import Path


ROOT = Path(__file__).resolve().parents[1]
COMMAND = ROOT / "plugin/src/main/java/com/observance/watcher/command/ObservanceCommand.java"
FIXTURES = ROOT / "design/DEEP-HOLD-FIXTURE-MANIFEST.csv"
BOXES = ROOT / "design/DEEP-HOLD-ROOM-BOXES.csv"
RECORDS = ROOT / "design/DEEP-HOLD-RECORD-STATION-MANIFEST.csv"
GATES = ROOT / "design/DEEP-HOLD-GATE-MANIFEST.csv"


def read_csv(path: Path) -> list[dict[str, str]]:
    with path.open(encoding="utf-8", newline="") as handle:
        return list(csv.DictReader(handle))


def point(row: dict[str, str], prefix: str = "") -> tuple[int, int, int]:
    return tuple(int(row[prefix + axis]) for axis in ("x", "y", "z"))  # type: ignore[return-value]


def inside(p: tuple[int, int, int], box: dict[str, str]) -> bool:
    x, y, z = p
    return (
        int(box["min_x"]) <= x <= int(box["max_x"])
        and int(box["min_y"]) <= y <= int(box["max_y"])
        and int(box["min_z"]) <= z <= int(box["max_z"])
    )


def expected_front(fixture: tuple[int, int, int], stand: tuple[int, int, int]) -> str:
    dx = stand[0] - fixture[0]
    dz = stand[2] - fixture[2]
    if dx == 0 and dz == 0:
        return "NONE"
    if abs(dx) == abs(dz):
        return "DIAGONAL"
    if abs(dx) > abs(dz):
        return "EAST" if dx > 0 else "WEST"
    return "SOUTH" if dz > 0 else "NORTH"


def boxes_overlap(a: dict[str, str], b: dict[str, str]) -> bool:
    return all(
        int(a["min_" + axis]) <= int(b["max_" + axis])
        and int(b["min_" + axis]) <= int(a["max_" + axis])
        for axis in ("x", "y", "z")
    )


def main() -> int:
    source = COMMAND.read_text(encoding="utf-8")
    runtime_ids = re.findall(r'new HoldSite\("([^"]+)"', source)
    fixtures = read_csv(FIXTURES)
    records = read_csv(RECORDS)
    gates = read_csv(GATES)
    boxes = {row["room_id"]: row for row in read_csv(BOXES)}
    failures: list[str] = []

    manifest_ids = [row["site_id"] for row in fixtures]
    missing = sorted(set(runtime_ids) - set(manifest_ids))
    extra = sorted(set(manifest_ids) - set(runtime_ids))
    duplicates = sorted({site_id for site_id in manifest_ids if manifest_ids.count(site_id) > 1})
    if missing:
        failures.append("runtime sites missing from manifest: " + ", ".join(missing))
    if extra:
        failures.append("manifest sites absent from runtime table: " + ", ".join(extra))
    if duplicates:
        failures.append("duplicate manifest site ids: " + ", ".join(duplicates))

    box_rows = list(boxes.values())
    for index, first in enumerate(box_rows):
        for second in box_rows[index + 1:]:
            if boxes_overlap(first, second):
                failures.append(
                    f"room ownership boxes overlap: {first['room_id']} and {second['room_id']}"
                )

    for gate in gates:
        gate_box = {
            "room_id": gate["gate_id"],
            **{key: gate[key] for key in ("min_x", "max_x", "min_y", "max_y", "min_z", "max_z")},
        }
        if int(gate["min_x"]) > int(gate["max_x"]) or int(gate["min_y"]) > int(gate["max_y"]) \
                or int(gate["min_z"]) > int(gate["max_z"]):
            failures.append(f"{gate['gate_id']}: inverted gate volume")
        for room in box_rows:
            if boxes_overlap(gate_box, room):
                failures.append(f"gate ownership overlaps room ownership: {gate['gate_id']} and {room['room_id']}")
        if not gate["open_condition"].strip():
            failures.append(f"{gate['gate_id']}: blank open condition")

    occupied: dict[tuple[int, int, int], str] = {}
    for row in fixtures + records:
        site_id = row.get("site_id") or row["station_id"]
        room_id = row["room_id"]
        box = boxes.get(room_id)
        if box is None:
            failures.append(f"{site_id}: unknown room {room_id}")
            continue
        fixture = point(row)
        stand = point(row, "stand_")
        if not inside(fixture, box):
            failures.append(f"{site_id}: fixture {fixture} lies outside {room_id}")
        if not inside(stand, box):
            failures.append(f"{site_id}: standing point {stand} lies outside {room_id}")
        if fixture in occupied:
            failures.append(f"{site_id}: anchor duplicates {occupied[fixture]} at {fixture}")
        occupied[fixture] = site_id
        declared = row["expected_front"]
        derived = expected_front(fixture, stand)
        if declared != derived:
            failures.append(f"{site_id}: declared front {declared}, player frame derives {derived}")

    runtime_record_table = re.search(
        r"private static final HoldRecordStation\[] DEEP_HOLD_RECORD_STATIONS\s*=\s*\{([\s\S]*?)\n\s*};",
        source,
    )
    runtime_record_ids = re.findall(r'new HoldRecordStation\("([^"]+)"', runtime_record_table.group(1)) \
        if runtime_record_table else []
    manifest_record_ids = [row["station_id"] for row in records]
    if set(runtime_record_ids) != set(manifest_record_ids):
        failures.append(
            "record-station id mismatch; runtime=" + ",".join(sorted(runtime_record_ids))
            + " manifest=" + ",".join(sorted(manifest_record_ids))
        )

    print(
        f"Deep Hold fixture manifest: {len(fixtures)}/{len(runtime_ids)} sites, "
        f"{len(records)} record stations, {len(gates)} gate volumes, {len(boxes)} owned room modules"
    )
    if failures:
        for failure in failures:
            print("  FAIL " + failure)
        return 1
    print("  PASS: complete ids, unique anchors, room containment, and player-frame fronts")
    return 0


if __name__ == "__main__":
    raise SystemExit(main())
