#!/usr/bin/env python3
"""Faithful block-level reachability proof for the exact M3 private slice authority.

This simulator deliberately models only the disposable P4 slice. It replays the
authority's build sequence as solid/air writes in the declared coordinate system,
then audits closed- and open-gate states. It is not evidence of a Paper build.
"""
from __future__ import annotations

from collections import deque
from dataclasses import dataclass
import json
import math
from pathlib import Path
from typing import Iterable


ROOT = Path(__file__).resolve().parents[1]
AUTHORITY = ROOT / "design" / "m3" / "vertical-slice-v1.json"
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
            fail(f"build write escaped the slice envelope: {cell}")
        self.solid[cell] = solid

    def carve_column(self, x: int, floor: int, z: int, headroom: int) -> None:
        self.write((x, floor - 1, z), True)
        for y in range(floor, floor + headroom):
            self.write((x, y, z), False)

    def carve_room(self, bounds: dict) -> None:
        for x in range(bounds["min_x"], bounds["max_x"] + 1):
            for z in range(bounds["min_z"], bounds["max_z"] + 1):
                self.write((x, bounds["floor_y"] - 1, z), True)
                perimeter = x in (bounds["min_x"], bounds["max_x"]) or z in (
                    bounds["min_z"], bounds["max_z"])
                for y in range(bounds["floor_y"], bounds["ceiling_y"] + 1):
                    self.write((x, y, z), perimeter or y == bounds["ceiling_y"])

    def carve_door_cells(self, cells: Iterable[list[int]], height: int) -> None:
        for x, floor, z in cells:
            self.write((x, floor - 1, z), True)
            for y in range(floor, floor + height):
                self.write((x, y, z), False)

    def carve_route(self, waypoints: list[list[int]], width: int, headroom: int = 4) -> None:
        radius = max(0, (width - 1) // 2)
        for start, end in zip(waypoints, waypoints[1:]):
            x1, y1, z1 = start
            x2, y2, z2 = end
            distance = max(abs(x2 - x1), abs(y2 - y1), abs(z2 - z1))
            for step in range(distance + 1):
                ratio = 0 if distance == 0 else step / distance
                x = round(x1 + (x2 - x1) * ratio)
                y = round(y1 + (y2 - y1) * ratio)
                z = round(z1 + (z2 - z1) * ratio)
                for dx in range(-radius, radius + 1):
                    for dz in range(-radius, radius + 1):
                        self.carve_column(x + dx, y, z + dz, headroom)

    def replay(self) -> None:
        spaces = {space["space_id"]: space for space in self.authority["spaces"]}
        for space_id in ("SURFACE_MOUTH", "INTAKE", "PUBLIC_HALLWAY", "INTAKE_COPY_ROOM", "FUTURE_STUB"):
            self.carve_room(spaces[space_id]["bounds"])

        descent = self.authority["descent"]
        for z in range(descent["z_start"], descent["z_end"] + 1):
            floor = -math.floor((z - descent["z_start"]) / 2)
            for x in range(-descent["half_width"], descent["half_width"] + 1):
                self.carve_column(x, floor, z, descent["headroom"])

        # Exact transition apertures are cut after shells so later walls cannot re-close them.
        transition_cells = []
        for z, floor in ((12, 0), (13, 0), (44, -15), (45, -16), (60, -16), (61, -16),
                         (76, -16), (77, -16), (78, -16)):
            transition_cells.extend([[x, floor, z] for x in range(-3, 4)])
        self.carve_door_cells(transition_cells, 4)
        for door in self.authority["doors"]:
            self.carve_door_cells(door["cells"], door["height"])

        service = next(route for route in self.authority["route_reservations"]
                       if route["route_id"] == "INTAKE_STAFF_SERVICE")
        self.carve_route(service["waypoints"], service["minimum_width"], 4)

        for prop in self.authority["composition"]:
            if prop["kind"] == "recessed_water":
                # Water is below the standing floor and curbed; it is not walkable air.
                for cell in prop["cells"]:
                    self.write(tuple(cell), True)
                continue
            for x, y, z in prop["cells"]:
                self.write((x, y, z), True)

        self.set_gate(open_gate=False)

    def set_gate(self, open_gate: bool) -> None:
        gate = self.authority["gate"]
        for x, floor, z in gate["barrier_cells"]:
            self.write((x, floor - 1, z), True)
            for y in range(floor, floor + gate["height"]):
                self.write((x, y, z), not open_gate)

    def standable(self, cell: Cell) -> bool:
        x, y, z = cell
        return self.is_solid((x, y - 1, z)) and not self.is_solid((x, y, z)) \
            and not self.is_solid((x, y + 1, z))

    def reachable(self, seed: Cell) -> set[Cell]:
        if not self.standable(seed):
            fail(f"BFS seed is not standable: {seed}")
        envelope = self.authority["envelope"]
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
                    if not (envelope["min_x"] <= cx <= envelope["max_x"]
                            and envelope["min_y"] <= cy <= envelope["max_y"]
                            and envelope["min_z"] <= cz <= envelope["max_z"]):
                        continue
                    if candidate not in seen and self.standable(candidate):
                        queue.append(candidate)
                        break
        return seen


def assert_space_reachable(model: Model, visited: set[Cell], space: dict) -> None:
    b = space["bounds"]
    candidates = ((x, b["floor_y"], z)
                  for x in range(b["min_x"] + 1, b["max_x"])
                  for z in range(b["min_z"] + 1, b["max_z"]))
    if not any(cell in visited for cell in candidates):
        fail(f"unreachable space: {space['space_id']}")


def run_simulation(path: Path = AUTHORITY) -> dict[str, int]:
    model = Model.load(path)
    model.replay()
    seed = (0, 0, 2)
    closed = model.reachable(seed)

    for space in model.authority["spaces"]:
        if space["space_id"] != "FUTURE_STUB":
            assert_space_reachable(model, closed, space)
    for standing in model.authority["standing_cells"]:
        cell = tuple(standing["cell"])
        if standing["cell_id"] == "GATE_READBACK_OPEN":
            if cell in closed:
                fail("future-stub readback is reachable while the gate is closed")
            continue
        if not model.standable(cell):
            fail(f"declared standing cell is not standable: {standing['cell_id']} {cell}")
        if cell not in closed:
            fail(f"declared standing cell is unreachable: {standing['cell_id']} {cell}")

    model.set_gate(open_gate=True)
    opened = model.reachable(seed)
    future = next(space for space in model.authority["spaces"] if space["space_id"] == "FUTURE_STUB")
    assert_space_reachable(model, opened, future)
    open_readback = tuple(next(cell["cell"] for cell in model.authority["standing_cells"]
                              if cell["cell_id"] == "GATE_READBACK_OPEN"))
    if open_readback not in opened:
        fail("open gate does not expose its declared readback cell")

    e = model.authority["envelope"]
    boundary = [cell for cell in opened if cell[0] in (e["min_x"], e["max_x"])
                or cell[1] in (e["min_y"], e["max_y"])
                or cell[2] in (e["min_z"], e["max_z"])]
    if boundary:
        fail(f"walkable route touches/escapes protected envelope boundary: {boundary[0]}")

    return {"closed_visited": len(closed), "open_visited": len(opened),
            "newly_reachable_after_gate": len(opened - closed)}


def main() -> None:
    receipt = run_simulation()
    print("M3 vertical-slice reachability OK — "
          f"closed={receipt['closed_visited']}, open={receipt['open_visited']}, "
          f"gate_delta={receipt['newly_reachable_after_gate']}")


if __name__ == "__main__":
    main()
