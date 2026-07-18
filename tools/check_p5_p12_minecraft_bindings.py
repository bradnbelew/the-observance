#!/usr/bin/env python3
"""Bind every authored P5-P12 functional space to exact existing Hold rooms/fixtures."""
from __future__ import annotations

import csv
import json
from pathlib import Path


ROOT = Path(__file__).resolve().parents[1]
CAMPAIGN = ROOT / "campaign/p5-p12"


def require(ok: bool, message: str) -> None:
    if not ok:
        raise AssertionError(message)


def main() -> None:
    index = json.loads((CAMPAIGN / "campaign.json").read_text(encoding="utf-8"))
    spaces: dict[str, str] = {}
    for phase in index["phases"]:
        case = json.loads((CAMPAIGN / phase["file"]).read_text(encoding="utf-8"))
        for space in case["spaces"]:
            require(space["id"] not in spaces, f"duplicate authored space {space['id']}")
            spaces[space["id"]] = case["phase"]

    with (ROOT / "design/ARG-V5-ROOM-ASSIGNMENTS.csv").open(encoding="utf-8", newline="") as stream:
        room_ids = {row["room_id"] for row in csv.DictReader(stream)}
    with (ROOT / "design/ARG-V5-FIXTURE-OWNERSHIP.csv").open(encoding="utf-8", newline="") as stream:
        fixture_ids = {row["site_id"] for row in csv.DictReader(stream)}

    authority = json.loads((CAMPAIGN / "minecraft-bindings.json").read_text(encoding="utf-8"))
    require(authority["fresh_client_receipt"] is False,
            "offline binding must not claim a fresh client receipt")
    bindings = authority["bindings"]
    bound = set()
    for row in bindings:
        space_id = row["space_id"]
        require(space_id in spaces, f"binding names unknown authored space {space_id}")
        require(space_id not in bound, f"duplicate Minecraft binding {space_id}")
        bound.add(space_id)
        require(row["room_ids"] and set(row["room_ids"]) <= room_ids,
                f"{space_id} has missing/unknown room binding")
        require(row["fixture_ids"] and set(row["fixture_ids"]) <= fixture_ids,
                f"{space_id} has missing/unknown fixture binding")
        require(row["placement_mode"].strip(), f"{space_id} missing placement mode")
    require(bound == set(spaces),
            f"Minecraft binding coverage drift missing={sorted(set(spaces)-bound)} extra={sorted(bound-set(spaces))}")
    require(len(bindings) == 27, f"expected 27 authored space bindings, found {len(bindings)}")
    print(f"P5-P12 Minecraft bindings: PASS ({len(bindings)} spaces -> {len(room_ids)} rooms / {len(fixture_ids)} fixtures authority)")


if __name__ == "__main__":
    main()
