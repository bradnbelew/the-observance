#!/usr/bin/env python3
"""Reject retired literal-Seventh props in the active Paper world generator.

The approved mystery is a category error: six Keeper administrators, seven practical Ways,
and a registrar/custody role omitted from later attribution. A grey seventh stone, empty seat,
or missing-Keeper monument turns that earned relationship into a literal answer prop.
"""
from __future__ import annotations

from pathlib import Path


ROOT = Path(__file__).resolve().parents[1]
WORLD = ROOT / "plugin/src/main/java/com/observance/watcher/command/ObservanceCommand.java"


def require(condition: bool, message: str) -> None:
    if not condition:
        raise AssertionError(message)


def main() -> None:
    source = WORLD.read_text(encoding="utf-8")
    folded = source.casefold()
    retired_literals = (
        "grey seventh",
        "the grey one",
        "the seventh mark",
        "hollow seventh",
        "missing seventh",
        "six stones plus grey",
        "six stones and one grey",
        "the seventh share",
        "the seventh measure",
        "seventh line: no witness",
    )
    for literal in retired_literals:
        require(literal not in folded,
                f"active Paper world generator revives retired literal-Seventh prop: {literal!r}")

    required_relationships = (
        "Seven jobs keep a shelter running",
        "They count different things",
        "Nessa returned both good jars",
        "six office formats",
        "Do not invent a seventh official",
        "mkept, Ash, Rook, Wren",
        "It does not by itself prove who sent it",
    )
    for text in required_relationships:
        require(text in source, f"active category/prior-company relationship is missing: {text!r}")

    for title in ("camp return list", "frame log", "change order", "supply note"):
        require(source.count(f'"{title}"') == 1,
                f"prior-company camp evidence title is missing or duplicated: {title}")
    for retired_packet in ("vaun packet", "mara packet", "sella packet",
                           "orin packet", "brann packet", "iss packet"):
        require(f'"{retired_packet}"' not in folded,
                f"prior-company camp still uses Keeper packet station: {retired_packet}")

    # The helper may remain for migration history, but no active builder may call it.
    require(source.count("placePriorBedrollPacket(") <= 1,
            "active Paper builder still calls retired Keeper-themed camp packet helper")
    print("active Paper world canon: PASS")


if __name__ == "__main__":
    main()
