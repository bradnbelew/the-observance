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
    evidence_surfaces: dict[str, str] = {}
    for phase in index["phases"]:
        case = json.loads((CAMPAIGN / phase["file"]).read_text(encoding="utf-8"))
        evidence_rows = list(case.get("evidence", []))
        for dossier in case.get("dossiers", []):
            evidence_rows.extend(dossier.get("evidence", []))
        for evidence in evidence_rows:
            evidence_id = evidence["id"]
            require(evidence_id not in evidence_surfaces,
                    f"duplicate authored evidence {evidence_id}")
            evidence_surfaces[evidence_id] = evidence["surface"]
        for space in case["spaces"]:
            require(space["id"] not in spaces, f"duplicate authored space {space['id']}")
            spaces[space["id"]] = case["phase"]
            for evidence_id in space.get("evidence_surfaces", []):
                require(evidence_id in evidence_surfaces,
                        f"{space['id']} references unknown evidence surface {evidence_id}")

    with (ROOT / "design/ARG-V5-ROOM-ASSIGNMENTS.csv").open(encoding="utf-8", newline="") as stream:
        room_ids = {row["room_id"] for row in csv.DictReader(stream)}
    with (ROOT / "design/ARG-V5-FIXTURE-OWNERSHIP.csv").open(encoding="utf-8", newline="") as stream:
        fixture_ids = {row["site_id"] for row in csv.DictReader(stream)}

    authority = json.loads((CAMPAIGN / "minecraft-bindings.json").read_text(encoding="utf-8"))
    require(authority["fresh_client_receipt"] is False,
            "offline binding must not claim a fresh client receipt")
    bindings = authority["bindings"]
    bound = set()
    bound_fixtures: set[str] = set()
    for row in bindings:
        space_id = row["space_id"]
        require(space_id in spaces, f"binding names unknown authored space {space_id}")
        require(space_id not in bound, f"duplicate Minecraft binding {space_id}")
        bound.add(space_id)
        require(row["room_ids"] and set(row["room_ids"]) <= room_ids,
                f"{space_id} has missing/unknown room binding")
        require(row["fixture_ids"] and set(row["fixture_ids"]) <= fixture_ids,
                f"{space_id} has missing/unknown fixture binding")
        bound_fixtures.update(row["fixture_ids"])
        require(row["placement_mode"].strip(), f"{space_id} missing placement mode")
    require(bound == set(spaces),
            f"Minecraft binding coverage drift missing={sorted(set(spaces)-bound)} extra={sorted(bound-set(spaces))}")
    require(len(bindings) == 27, f"expected 27 authored space bindings, found {len(bindings)}")

    books = json.loads((ROOT / "arc/v5/minecraft-books.json").read_text(encoding="utf-8"))
    book_ids = {book["id"] for book in books["books"]}
    expected_local_evidence = {
        evidence_id for evidence_id, surface in evidence_surfaces.items()
        if "minecraft" in surface.lower()
        or "npc_dialogue" in surface.lower()
        or "npc_memory" in surface.lower()
    }
    bound_evidence: set[str] = set()
    for row in authority["evidence_bindings"]:
        evidence_id = row["evidence_id"]
        require(evidence_id not in bound_evidence,
                f"duplicate Minecraft evidence binding {evidence_id}")
        bound_evidence.add(evidence_id)
        require(evidence_id in expected_local_evidence,
                f"non-local or unknown Minecraft evidence binding {evidence_id}")
        require(row["carriers"], f"{evidence_id} has no concrete runtime carrier")
        concrete = False
        for carrier in row["carriers"]:
            require(isinstance(carrier, str) and carrier.strip(),
                    f"{evidence_id} carrier must be non-empty text")
            if carrier.startswith("book:"):
                concrete = True
                require(carrier.removeprefix("book:") in book_ids,
                        f"{evidence_id} references missing authored book {carrier}")
            elif carrier.startswith("fixture:"):
                concrete = True
                require(carrier.removeprefix("fixture:") in bound_fixtures,
                        f"{evidence_id} references fixture outside campaign bindings {carrier}")
            elif carrier == "npc:wren" or carrier.startswith("runtime:"):
                concrete = True
            else:
                raise AssertionError(f"{evidence_id} has unsupported carrier {carrier}")
        require(concrete, f"{evidence_id} exists only as campaign JSON")
    require(bound_evidence == expected_local_evidence,
            "Minecraft evidence carrier coverage drift "
            f"missing={sorted(expected_local_evidence-bound_evidence)} "
            f"extra={sorted(bound_evidence-expected_local_evidence)}")

    print(
        "P5-P12 Minecraft bindings: PASS "
        f"({len(bindings)} spaces / {len(bound_evidence)} local evidence records -> "
        f"{len(room_ids)} rooms / {len(fixture_ids)} fixtures authority)"
    )


if __name__ == "__main__":
    main()
