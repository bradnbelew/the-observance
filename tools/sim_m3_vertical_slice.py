#!/usr/bin/env python3
"""Faithful solid/collision/reachability proof for the authored M3 v2 slice."""
from __future__ import annotations

from collections import deque
from dataclasses import dataclass
import json
import math
from pathlib import Path


ROOT = Path(__file__).resolve().parents[1]
AUTHORITY = ROOT / "design" / "m3" / "vertical-slice-v2.json"
Cell = tuple[int, int, int]


def fail(message: str) -> None:
    raise AssertionError(message)


@dataclass
class Model:
    authority: dict
    solid: dict[Cell, bool]

    @classmethod
    def load(cls, path: Path = AUTHORITY) -> "Model":
        return cls(json.loads(path.read_text(encoding="utf-8")), {})

    def is_solid(self, cell: Cell) -> bool:
        return self.solid.get(cell, True)

    def write(self, cell: Cell, solid: bool) -> None:
        x, y, z = cell
        e = self.authority["envelope"]
        if not (e["min_x"] <= x <= e["max_x"] and e["min_y"] <= y <= e["max_y"]
                and e["min_z"] <= z <= e["max_z"]):
            fail(f"build write escaped envelope: {cell}")
        self.solid[cell] = solid

    def room(self, bounds: dict) -> None:
        for x in range(bounds["min_x"], bounds["max_x"] + 1):
            for z in range(bounds["min_z"], bounds["max_z"] + 1):
                self.write((x, bounds["floor_y"] - 1, z), True)
                perimeter = x in (bounds["min_x"], bounds["max_x"]) or z in (
                    bounds["min_z"], bounds["max_z"])
                for y in range(bounds["floor_y"], bounds["ceiling_y"] + 1):
                    self.write((x, y, z), perimeter or y == bounds["ceiling_y"])

    def column(self, x: int, floor: int, z: int, headroom: int = 5) -> None:
        self.write((x, floor - 1, z), True)
        for y in range(floor, floor + headroom):
            self.write((x, y, z), False)

    def door(self, x: int, floor: int, z: int, height: int) -> None:
        self.write((x, floor - 1, z), True)
        for y in range(floor, floor + height):
            self.write((x, y, z), False)

    def route(self, points: list[list[int]], width: int) -> None:
        radius = max(0, (width - 1) // 2)
        for a, b in zip(points, points[1:]):
            distance = max(abs(b[0] - a[0]), abs(b[1] - a[1]), abs(b[2] - a[2]))
            for step in range(distance + 1):
                ratio = 0 if distance == 0 else step / distance
                x = round(a[0] + (b[0] - a[0]) * ratio)
                y = round(a[1] + (b[1] - a[1]) * ratio)
                z = round(a[2] + (b[2] - a[2]) * ratio)
                for dx in range(-radius, radius + 1):
                    for dz in range(-radius, radius + 1):
                        self.column(x + dx, y, z)

    def volume(self, bounds: dict, solid: bool = True) -> None:
        for x in range(bounds["min_x"], bounds["max_x"] + 1):
            for y in range(bounds["min_y"], bounds["max_y"] + 1):
                for z in range(bounds["min_z"], bounds["max_z"] + 1):
                    self.write((x, y, z), solid)

    def replay(self) -> None:
        spaces = {space["space_id"]: space for space in self.authority["spaces"]}
        for space_id in ("SURFACE_MOUTH", "INTAKE", "PUBLIC_HALLWAY", "INTAKE_COPY_ROOM",
                         "STAFF_SERVICE_VESTIBULE", "FUTURE_STUB"):
            self.room(spaces[space_id]["bounds"])

        descent = self.authority["descent"]
        for z in range(descent["z_start"], descent["z_end"] + 1):
            floor = -math.floor((z - descent["z_start"]) / 2)
            for x in range(-descent["half_width"], descent["half_width"] + 1):
                self.column(x, floor, z, descent["headroom"])

        for z, floor in ((15, 0), (16, 0), (55, -19), (56, -20), (78, -20),
                         (79, -20), (88, -20), (89, -20), (90, -20)):
            for x in range(-4, 5):
                self.door(x, floor, z, 5)
        for door in self.authority["doors"]:
            for x, floor, z in door["cells"]:
                self.door(x, floor, z, door["height"])
        for route_id in ("INTAKE_STAFF_SERVICE", "COPY_ROOM_PUBLIC_AISLE", "COPY_OFFICE_CROSS_AISLE"):
            route = next(row for row in self.authority["route_reservations"] if row["route_id"] == route_id)
            self.route(route["waypoints"], route["minimum_width"])

        for water in self.authority["waterworks"]["water_volumes"]:
            self.volume(water["bounds"], True)
        for z in range(62, 79):
            self.write((-15, -20, z), True)
            self.write((-8, -20, z), True)
        for x in range(-14, -8):
            self.write((x, -20, 62), True)
            self.write((x, -20, 78), True)
        for z in (64, 66, 72, 76):
            self.write((-12, -20, z), True)
            self.write((-11, -20, z), True)

        for prop in self.authority["composition"]:
            height = prop.get("vertical_height", 1)
            for x, floor, z in prop["floor_cells"]:
                for dy in range(height):
                    self.write((x, floor + dy, z), True)
        for surface in self.authority["evidence_surfaces"]:
            self.write(tuple(surface["cell"]), True)
        for surface in self.authority["submission_surfaces"]:
            self.write(tuple(surface["cell"]), True)
        for sign in self.authority["threshold_signage"]:
            self.write(tuple(sign["cell"]), True)
        self.set_gate(False)

    def set_gate(self, open_gate: bool) -> None:
        plane = self.authority["gate"]["barrier_plane"]
        for x in range(plane["min_x"], plane["max_x"] + 1):
            for y in range(plane["min_y"], plane["max_y"] + 1):
                self.write((x, y, plane["z"]), not open_gate)
        frame = self.authority["gate"]["permanent_frame"]
        z = frame["z"]
        for x in range(frame["min_x"], frame["max_x"] + 1):
            self.write((x, frame["min_y"], z), True)
            self.write((x, frame["max_y"], z), True)
        for y in range(frame["min_y"] + 1, frame["max_y"]):
            self.write((frame["min_x"], y, z), True)
            self.write((frame["max_x"], y, z), True)

    def standable(self, cell: Cell) -> bool:
        x, y, z = cell
        return self.is_solid((x, y - 1, z)) and not self.is_solid((x, y, z)) \
            and not self.is_solid((x, y + 1, z))

    def reachable(self, seed: Cell) -> set[Cell]:
        if not self.standable(seed):
            fail(f"BFS seed is not standable: {seed}")
        e = self.authority["envelope"]
        seen: set[Cell] = set()
        queue = deque([seed])
        while queue:
            cell = queue.popleft()
            if cell in seen:
                continue
            seen.add(cell)
            x, y, z = cell
            for dx, dz in ((1, 0), (-1, 0), (0, 1), (0, -1)):
                for dy in (0, 1, -1):
                    candidate = (x + dx, y + dy, z + dz)
                    cx, cy, cz = candidate
                    if not (e["min_x"] <= cx <= e["max_x"] and e["min_y"] <= cy <= e["max_y"]
                            and e["min_z"] <= cz <= e["max_z"]):
                        continue
                    if candidate not in seen and self.standable(candidate):
                        queue.append(candidate)
                        break
        return seen


def run_simulation() -> dict:
    model = Model.load()
    model.replay()
    gate = model.authority["gate"]
    plane = gate["barrier_plane"]
    barrier = {(x, y, plane["z"])
               for x in range(plane["min_x"], plane["max_x"] + 1)
               for y in range(plane["min_y"], plane["max_y"] + 1)}
    if len(barrier) != gate["closed_collision_cells"] or not all(model.is_solid(cell) for cell in barrier):
        fail("closed gate is not a complete 11x8 collision plane")

    seed = (0, 0, 2)
    closed = model.reachable(seed)
    if (0, -20, 91) in closed:
        fail("future stub reachable through closed gate")
    for row in model.authority["standing_cells"]:
        cell = tuple(row["cell"])
        if row["cell_id"] == "GATE_READBACK_OPEN":
            continue
        if cell not in closed:
            fail(f"standing cell unreachable while gate closed: {row['cell_id']} {cell}")

    model.set_gate(True)
    if any(model.is_solid(cell) for cell in barrier):
        fail("open gate retains a collision cell")
    opened = model.reachable(seed)
    if (0, -20, 91) not in opened:
        fail("future stub remains unreachable after gate opens")
    if not closed < opened:
        fail("open gate does not add reachable cells")

    for route in model.authority["route_reservations"]:
        if route["route_id"] == "PUBLIC_CIRCULATION":
            continue
        for waypoint in route["waypoints"]:
            if tuple(waypoint) not in closed:
                fail(f"route waypoint unreachable: {route['route_id']} {waypoint}")

    return {
        "closed_visited": len(closed),
        "open_visited": len(opened),
        "gate_delta": len(opened - closed),
        "closed_gate_collision_cells": len(barrier),
        "open_gate_collision_cells": sum(model.is_solid(cell) for cell in barrier),
        "standing_cells": len(model.authority["standing_cells"]),
        "evidence_surfaces": len(model.authority["evidence_surfaces"]),
        "submission_surfaces": len(model.authority["submission_surfaces"]),
        "water_blocks": model.authority["waterworks"]["total_water_blocks"],
    }


def main() -> None:
    receipt = run_simulation()
    print("M3 v2 reachability OK — " + ", ".join(f"{key}={value}" for key, value in receipt.items()))


if __name__ == "__main__":
    main()
