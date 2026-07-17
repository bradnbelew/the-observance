#!/usr/bin/env python3
"""Validate the bounded M3 V5 client-render and physical-affordance revision."""
from __future__ import annotations

import argparse
import hashlib
import json
import re
import subprocess
from pathlib import Path


ROOT = Path(__file__).resolve().parents[1]
M3 = ROOT / "design" / "m3"
AUTHORITY = M3 / "vertical-slice-v5.json"
INVENTORY = M3 / "AFFORDANCE-INVENTORY-V5.json"
WORLD = ROOT / "plugin/src/main/java/com/observance/watcher/m3runtime/PrivateSliceWorld.java"
LAYOUT = WORLD.with_name("BookPageLayout.java")
STATE = WORLD.with_name("PrivateSliceState.java")
RUNTIME = WORLD.with_name("PrivateSliceReviewRuntime.java")
SELF_TEST = ROOT / "plugin/src/test/java/com/observance/watcher/m3runtime/PrivateSliceStateSelfTest.java"
EXPECTED_AUTHORITY_SHA256 = "a0580902b8f8633579820f1a5adf7419d0151b1259026c554b2d6974c1a95e1c"
HISTORICAL_PACKAGE_CHECKPOINT = "967979057f192db5c111bfffeac57ede098ab633"


def require(condition: bool, message: str) -> None:
    if not condition:
        raise AssertionError(message)


def load(path: Path) -> dict:
    return json.loads(path.read_text(encoding="utf-8"))


def normalized_sha(path: Path) -> str:
    return hashlib.sha256(path.read_bytes().replace(b"\r\n", b"\n")).hexdigest()


def historical_bytes(path: Path) -> bytes:
    relative = path.relative_to(ROOT).as_posix()
    result = subprocess.run(
        ["git", "show", f"{HISTORICAL_PACKAGE_CHECKPOINT}:{relative}"],
        cwd=ROOT, check=True, capture_output=True,
    )
    return result.stdout


def check_authority() -> dict:
    data = load(AUTHORITY)
    require(data["schema_version"] == "1.0.0-m3-private-slice-v5"
            and data["authority_id"] == "observance-p4-private-slice-v5",
            "V5 authority identity drift")
    require(normalized_sha(AUTHORITY) == EXPECTED_AUTHORITY_SHA256, "V5 authority hash drift")
    require(data["scope"] == {
        "target": "fresh disposable localhost-only Paper 1.21.11",
        "envelope": {"min": [-34,-24,-10], "max": [34,10,92], "cells": 248745},
        "m4_authority": "closed", "production_mutation": False,
        "brad_visual_approval": None,
    }, "V5 scope/approval boundary drift")
    difficulty = data["difficulty_contract"]
    require(difficulty["cold_read_status"] == "voluntarily_aborted_for_time_pressure_inconclusive"
            and difficulty["campaign_target_active_hours"] == [20, 30]
            and "dense" in difficulty["deduction_rule"]
            and "tutorial signage" in difficulty["forbidden_simplifications"],
            "Brad's corrected high-difficulty authority was weakened")
    ui = data["book_ui"]
    require(ui["supported_client"] == "Minecraft Java 1.21.11"
            and ui["page_pixel_width"] == 114 and ui["maximum_rendered_lines"] == 13
            and ui["option_page_policy"] == "one complete selectable clause per page"
            and ui["no_cross_page_option"] is True,
            "written-book client budget authority drift")
    findings = ui["findings"]
    require(len(findings) == 5 and all(len(row["choices"]) == 4 for row in findings),
            "exact 20-clause inventory drift")
    ids = [(row["finding_id"], choice["id"]) for row in findings for choice in row["choices"]]
    require(len(ids) == len(set(ids)) == 20, "filing clause ids are incomplete or duplicated")
    shelves = data["physical_affordances"]["interactive_chiseled_shelves"]
    require(len(shelves) == 4 and all(row["occupied_slot"] == 0 for row in shelves),
            "interactive occupied-shelf inventory drift")
    seats = data["seat_compositions"]
    require(sum(len(row["cells"]) for row in seats) == 22
            and len({tuple(cell) for row in seats for cell in row["cells"]}) == 22,
            "seat composition count/overlap drift")
    return data


def check_inventory() -> None:
    data = load(INVENTORY)
    require(data["slice_authority"] == "design/m3/vertical-slice-v5.json"
            and data["book_ui"]["selectable_option_pages"] == 20
            and data["book_ui"]["click_targets_per_option"] == 2
            and data["book_ui"]["cross_page_options"] == 0,
            "V5 player-facing book inventory drift")
    require(len(data["interactive_shelf_inventory"]) == 4
            and data["seat_inventory"]["total"] == 22,
            "V5 physical inventory drift")
    require(data["cold_player_interface_affordance"]["difficulty_reduction"] is False
            and data["cold_player_interface_affordance"]["deduction_speed_requirement"] is None
            and data["approval"] == {"brad_visual_approval": None, "m4_authority": "closed",
                                      "production_mutated": False},
            "V5 interface gate overclaims comprehension or approval")


def check_source(authority: dict) -> None:
    world = WORLD.read_text(encoding="utf-8")
    layout = LAYOUT.read_text(encoding="utf-8")
    state = STATE.read_text(encoding="utf-8")
    runtime = RUNTIME.read_text(encoding="utf-8")
    self_test = SELF_TEST.read_text(encoding="utf-8")
    for token in ("PAGE_PIXEL_WIDTH = 114", "MAX_RENDERED_LINES = 13",
                  "each filing heading requires four clauses", "OptionPage", "duplicate filing command"):
        require(token in layout, f"book-page executable budget missing {token}")
    for token in ("addChoicePages", "BookPageLayout.optionPages", "MARK THIS CLAUSE",
                  "occupiedShelfData", "slot_0_occupied=true", "writeShelfBook",
                  "shelfBookMatches", "checkSeats", "exact classified seat inventory expected=22"):
        require(token in world, f"V5 world predicate missing {token}")
    require("m3-private-slice-v5.journal" in runtime
            and "authority=observance-p4-private-slice-v5" in runtime
            and "M3_UI_AUDIT_PASS" in runtime and "M3_GUIDED_CLIENT_MODEL_PASS" in runtime,
            "V5 runtime identity/UI checks incomplete")
    require(":v5" in state and "m3-private-slice-v5/named-test-players" in state,
            "V5 durable state identity drift")
    require("bookPageBudget" in self_test and "v5 evidence and filing feedback" in self_test,
            "V5 model test coverage missing")
    source_pairs = re.findall(r'choice\("([^"]+)", "([^"]+)"\)', world)
    authored_pairs = [(choice["label"], choice["id"])
                      for finding in authority["book_ui"]["findings"]
                      for choice in finding["choices"]]
    require(source_pairs == authored_pairs, "authority/runtime choice labels or order drift")
    exact_facing = {
        'x < 0 ? "west" : "east", new Cell(x < 0 ? -7 : 7, 0, z)': "Mouth wall-backed seats",
        '"south", new Cell(23, -20, z - 2)': "copy seat 23",
        '"south", new Cell(25, -20, z - 2)': "copy seat 25",
        '"north", new Cell(x, -20, z + 2)': "intake clerk seats",
        'z < 70 ? "north" : "south"': "intake waiting seats",
    }
    for token, label in exact_facing.items():
        require(token in world, f"chair-facing correction missing: {label}")


def check_receipts() -> None:
    validation_path = M3 / "PAPER-V5-DISPOSABLE-RECEIPT.json"
    review_path = M3 / "PAPER-V5-REVIEW-SERVER-RECEIPT.json"
    manifest_path = M3 / "PACKAGE-MANIFEST-V5.json"
    for path in (validation_path, review_path, manifest_path):
        require(path.is_file(), f"missing V5 final receipt: {path.relative_to(ROOT)}")
    validation = load(validation_path)
    review = load(review_path)
    manifest = load(manifest_path)
    require(validation["authority_id"] == "observance-p4-private-slice-v5"
            and validation["slice_authority_sha256"] == EXPECTED_AUTHORITY_SHA256
            and "options=20" in validation["evidence"]["ui_audit"]
            and "clause_pages=20" in validation["evidence"]["guided_client_model"]
            and "findings=0 gate=closed" in validation["evidence"]["naive_negative"]
            and "throttled=true" in validation["evidence"]["brute_negative"]
            and "gate=open" in validation["evidence"]["restart_audit"],
            "V5 disposable UI/negative/restart receipt drift")
    require(review["journal_state"] == "absent_pristine_review_target"
            and "options=20" in review["evidence"]["ui_audit"]
            and review["brad_visual_approval"] is None
            and review["m4_authority"] == "closed",
            "V5 pristine review target/approval drift")
    require(manifest["authority_id"] == "observance-p4-private-slice-v5"
            and manifest["brad_visual_approval"] is None and manifest["m4_authority"] == "closed",
            "V5 package manifest approval drift")
    canonical = bytearray()
    for row in manifest["files"]:
        path = ROOT / row["path"]
        digest = hashlib.sha256(historical_bytes(path).replace(b"\r\n", b"\n")).hexdigest()
        require(digest == row["sha256"],
                f"V5 manifest file drift: {row['path']}")
        canonical.extend((row["path"] + "\n" + row["sha256"] + "\n").encode())
    require(hashlib.sha256(canonical).hexdigest() == manifest["package_set_sha256"],
            "V5 package-set hash drift")


def main() -> None:
    parser = argparse.ArgumentParser()
    parser.add_argument("--source-only", action="store_true")
    args = parser.parse_args()
    authority = check_authority()
    check_inventory()
    check_source(authority)
    if not args.source_only:
        check_receipts()
    print("M3 V5 source/authority checks PASS" + ("" if args.source_only else "; Paper/package receipts PASS"))


if __name__ == "__main__":
    main()
