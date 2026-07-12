#!/usr/bin/env python3
"""Static pre-build audit for player-facing Deep Hold production content.

This deliberately audits executable Java/configuration and canonical document data.  It does not
treat a design document as proof that the generated Minecraft world is correct.  Run with
``--strict`` once the rebuild is implemented; report mode is useful while known blockers remain.
"""

from __future__ import annotations

import argparse
import json
import re
from pathlib import Path


ROOT = Path(__file__).resolve().parents[1]
COMMAND = ROOT / "plugin/src/main/java/com/observance/watcher/command/ObservanceCommand.java"
CONFIG = ROOT / "plugin/src/main/resources/config.yml"
DOCUMENT = ROOT / "arc/lore/documents/page-line-word.md"
REFLECTION = ROOT / "plugin/src/main/java/com/observance/watcher/beats/lib/ReflectionBeat.java"
LECTERN_LISTENER = ROOT / "plugin/src/main/java/com/observance/watcher/signal/listener/LecternLockListener.java"
LISTENERS = ROOT / "plugin/src/main/java/com/observance/watcher/signal/listener"
PLUGIN = ROOT / "plugin/src/main/java/com/observance/watcher/ObservancePlugin.java"
TEMPLATES = ROOT / "plugin/src/main/java/com/observance/watcher/structure/StructureTemplates.java"
ISS_ITEM = ROOT / "plugin/src/main/java/com/observance/watcher/structure/IssKeepsakeLampItem.java"


def method(source: str, name: str) -> str:
    start = re.search(rf"\b{name}\s*\([^)]*\)\s*\{{", source)
    if not start:
        return ""
    depth = 0
    for i in range(start.end() - 1, len(source)):
        if source[i] == "{":
            depth += 1
        elif source[i] == "}":
            depth -= 1
            if depth == 0:
                return source[start.start(): i + 1]
    return source[start.start():]


def ints(text: str) -> list[int]:
    return [int(value) for value in re.findall(r"\d+", text)]


def main() -> int:
    parser = argparse.ArgumentParser()
    parser.add_argument("--strict", action="store_true", help="return non-zero while blockers exist")
    args = parser.parse_args()

    command = COMMAND.read_text(encoding="utf-8")
    config = CONFIG.read_text(encoding="utf-8")
    document = DOCUMENT.read_text(encoding="utf-8")
    reflection = REFLECTION.read_text(encoding="utf-8")
    lectern_listener = LECTERN_LISTENER.read_text(encoding="utf-8")
    plugin = PLUGIN.read_text(encoding="utf-8")
    templates = TEMPLATES.read_text(encoding="utf-8")
    blockers: list[str] = []

    site_rows = re.findall(
        r'new HoldSite\("([^"]+)",\s*"([^"]+)"[^;]+?\)', command
    )
    site_ids = [site_id for site_id, _ in site_rows]
    if len(site_ids) != len(set(site_ids)):
        blockers.append("Deep Hold site table contains duplicate ids")

    placement = method(command, "placeHoldFixture")
    if re.search(r"else\s*\{\s*buildLabFixture\(site,\s*loc\);", placement):
        blockers.append(
            "production placeHoldFixture silently falls through to buildLabFixture instead of failing on an unhandled fixture"
        )
    if "StructureTemplates.keeper(id, loc)" in placement or "prepareTemplateVolume" in placement:
        blockers.append("production Keeper templates clear independent volumes instead of respecting V2 room ownership")
    if "StructureTemplates.keeperInOwnedRoom(id, loc)" not in placement:
        blockers.append("production Keeper templates are not using the owned-room placement path")

    lab = method(command, "fillLabLecternBook")
    if "fillLabLecternBook(" in placement:
        for phrase in ("the lab", "turn me for page-lock testing"):
            if phrase in lab:
                blockers.append(f'test-only player text is present in the reachable lab fallback: "{phrase}"')

    mara = method(command, "fillMaraLockBook")
    for phrase in ("Mara shelf ", "the line continues elsewhere, but not here"):
        if phrase in mara:
            blockers.append(f'Mara lock books still contain scaffold text: "{phrase.strip()}"')

    sella = method(command, "fillSellaLockBook")
    for phrase in ("Sella loose page ", "the wet graphite runs into an unfinished bird"):
        if phrase in sella:
            blockers.append(f'Sella lock books still contain repeated scaffold text: "{phrase.strip()}"')

    mara_code = re.search(r"int\[\]\s+markedPages\s*=\s*\{([^}]+)}", placement)
    mara_cfg = re.search(r"mara-lectern-lock:[\s\S]*?marked-pages:\s*\[([^]]+)]", config)
    if not mara_code or not mara_cfg or ints(mara_code.group(1)) != ints(mara_cfg.group(1)):
        blockers.append("Mara book-builder marked pages do not match live config")

    sella_code = re.search(r"int\[\]\s+ringPages\s*=\s*\{([^}]+)}", placement)
    sella_cfg = re.search(r"sella-overlay-lake:[\s\S]*?ring-pages:\s*\[([^]]+)]", config)
    if not sella_code or not sella_cfg or ints(sella_code.group(1)) != ints(sella_cfg.group(1)):
        blockers.append("Sella book-builder ring pages do not match live config")

    if 'placeReadableLectern(b, BlockFace.SOUTH)' in placement or 'placeReadableLectern(b, BlockFace.NORTH)' in placement:
        blockers.append("production lectern facing is hardcoded, not derived from an authored player-standing zone")

    integrated = method(command, "buildHoldIntegratedFixture")
    for phrase in ("fall mark ", "dial turn ", "walked mark ", "follow in order"):
        if phrase in integrated:
            blockers.append(f'production marker signs expose mechanical scaffold text: "{phrase.strip()}"')

    water_core = method(command, "buildHoldWaterMirrorCore")
    if '"ripple " + (i + 1)' in water_core and '"count " + ringPages[i]' in water_core:
        blockers.append("Sella pool prints the five lock answers on literal count signs instead of embodying them in the ring design")
    water_anchor = method(command, "buildHoldWaterAnchorCore")
    if '"stand here"' in water_anchor or '"look down"' in water_anchor:
        blockers.append("Sella overlook uses operator-like instruction signs instead of environmental framing")

    shaping = method(command, "shapeHoldFixtureSetting")
    if 'Set.of("mara_lectern", "sella_lectern"' in shaping and \
            'if ("mara_lectern".equals(type) || "sella_lectern".equals(type))' in shaping:
        blockers.append("Mara/Sella work-table shaping branch is unreachable because the same types return earlier")

    refs = re.findall(r"p\.\s*\d+\s*[Â··]\s*line\s*\d+\s*[Â··]\s*word\s*\d+", document)
    answer_match = re.search(r'^answer:\s*"([^"]+)"', document, re.MULTILINE)
    answer_words = answer_match.group(1).split() if answer_match else []
    d05 = json.loads((ROOT / "design" / "deep-hold-d05-shelf.json").read_text(encoding="utf-8"))
    extracted = []
    for book in d05["books"]:
        ref = book["reference"]
        line = book["pages"][ref["page"] - 1].splitlines()[ref["line"] - 1]
        extracted.append(line.split()[ref["word"] - 1].strip(".,;:!?\"'"))
    expanded = " ".join(extracted)
    for compound, expansion in d05.get("compound_expansions", {}).items():
        expanded = expanded.replace(compound, expansion)
    if expanded != d05.get("expected_extraction"):
        blockers.append("D05 physical six-book extraction does not match its authored expected phrase")

    if "new AxisAngle4f((float) Math.PI, 1f, 0f, 0f)" in reflection and \
            "player-standing" not in reflection and "standing zone" not in reflection:
        blockers.append(
            "Sella reflection uses one fixed X-axis flip with no authored pool-to-player yaw/standing-zone contract"
        )

    if "sites.placedOfType(lecternType)" in lectern_listener:
        blockers.append(
            "lectern locks read every globally placed site of a type; an extra Mara/Sella lectern can invalidate the Hold lock"
        )

    group_walk = (LISTENERS / "GroupWalkListener.java").read_text(encoding="utf-8")
    if "onlineCount()" in group_walk:
        blockers.append("Mara group-walk quorum uses every online player, so staff/players elsewhere can deadlock the rite")
    if "routeProgress" in group_walk and "PlayerQuitEvent" not in group_walk and "PlayerTeleportEvent" not in group_walk:
        blockers.append("Mara route progress survives wrong detours/teleports until completion and can be pre-armed before the alcove opens")

    hoard = (LISTENERS / "HoardSortedListener.java").read_text(encoding="utf-8")
    if "DEFAULT_REQUIRED" in hoard and "PersistentDataContainer" not in hoard:
        blockers.append("Vaun's 'first of the deep' solve accepts ordinary material types, not one authored/PDC-tagged relic")

    frames = (LISTENERS / "FrameDialsListener.java").read_text(encoding="utf-8")
    if "sites.placedOfType(DIAL_TYPE)" in frames:
        blockers.append("Orin's dial lock reads every globally placed dial site instead of the six Hold dial ids")
    if "frame.setFacingDirection(BlockFace.SOUTH" in command:
        blockers.append("Orin item frames are spawned facing SOUTH while the rebuild player frame requires NORTH")

    decorative_shelf = method(command, "placeDecorativeBookshelf")
    mechanic_facing = re.search(
        r"placeMechanicBookshelf\(Block block, BlockFace facing\)([\s\S]{0,1400})", command)
    decorative_facing = re.search(
        r"placeDecorativeBookshelf\(Block block, int seed, BlockFace facing\)([\s\S]{0,1400})", command)
    if not mechanic_facing or "setFacing" not in mechanic_facing.group(1) \
            or not decorative_facing or "setFacing" not in decorative_facing.group(1):
        blockers.append("mechanic/decorative chiseled-bookshelf builders never set facing, reproducing backwards book faces")
    if "new ItemStack(Material.BOOK)" in decorative_shelf:
        blockers.append("decorative chiseled shelves contain removable generic books, creating untracked puzzle supplies/state changes")

    toll = (LISTENERS / "BlackMoonTollListener.java").read_text(encoding="utf-8")
    if toll.count("playSound(") == 1 and "runLater" not in toll:
        blockers.append("Brann's supposed Morse toll plays one bell sound; it cannot encode AWAKE")

    silence = (LISTENERS / "SilenceCorridorListener.java").read_text(encoding="utf-8")
    if "silentRun" in silence and "System.currentTimeMillis" not in silence and "START_TYPE" in silence and "END_TYPE" in silence:
        blockers.append("Brann's silent run has no timeout or continuous corridor-containment check; players can leave and re-enter at the end")
    brann_core = method(command, "buildHoldBrannCorridorCore")
    if "SCULK_SENSOR" not in brann_core and "SHRIEKER" not in brann_core:
        blockers.append("Brann's production corridor core places no sculk sensors/shriekers and does not build the full passage")

    iss_item = ISS_ITEM.read_text(encoding="utf-8")
    if "Base64.getEncoder" in iss_item and "PersistentDataType.STRING" in iss_item \
            and "filing string:" not in iss_item and "decodedPayload" not in templates:
        blockers.append("Iss's required keepsake clue exists only as base64 custom item data with no adventure-mode in-game reading surface")

    rosetta = method(templates, "rosetta")
    if "Ring of 6" in rosetta or "int[][] ring = {{0, -3}, {3, -1}, {3, 2}, {0, 3}, {-3, 2}, {-3, -1}}" in rosetta:
        blockers.append("Rosetta physically builds six ring pillars although the live solution requires seven ways")
    if "Choose which mark players can read" in templates:
        blockers.append("an Unwriting lore book addresses 'players' directly, exposing production/game terminology")

    vault_cfg = re.search(r"spine-threshold-vault:[\s\S]*?combination:\s*\"([^\"]+)\"", config)
    if vault_cfg and all(re.fullmatch(r"[a-z0-9]{4}", group) for group in vault_cfg.group(1).split()):
        blockers.append("Threshold vault displays an opaque implementation-style code rather than an authored diegetic combination")

    accepting_registration = re.search(
        r"new com\.observance\.watcher\.signal\.listener\.AcceptingRiteListener\(([\s\S]*?)\), this\);",
        plugin,
    )
    if accepting_registration and "this::" not in accepting_registration.group(1).split("cooldown-seconds")[-1]:
        blockers.append("Accepting rite is registered through the legacy always-ready constructor; tokens_laid/Threshold readiness is not wired")
    token_listener = LISTENERS / "RiteTokenDepositListener.java"
    token_text = token_listener.read_text(encoding="utf-8") if token_listener.exists() else ""
    if not all(part in token_text for part in (
            "rite_token", "vaun_first_deep", "WRITTEN_BOOK", "SEA_LANTERN", "oracle.resolveWorld")):
        blockers.append("rite-tokens has no physical inventory/deposit listener; the personal-token act is currently only a typed SQL answer")

    coop = (LISTENERS / "CoopPlateListener.java").read_text(encoding="utf-8")
    if 'sites.get("coop_plate")' not in coop:
        blockers.append("three-hands listener also accepts threshold_vault because both share coop_plate type")
    if "Action.LEFT_CLICK_BLOCK" in coop and "CHISELED_TUFF" not in coop:
        blockers.append("three-hands carve leg accepts a left-click on any block inside the site, not the authored carve mark")

    painted = (LISTENERS / "PaintedLineListener.java").read_text(encoding="utf-8")
    if "line.contains" in painted and "ray" not in painted and "plane" not in painted:
        blockers.append("painted-line crossing is detected by entering the site's radius, not crossing the actual painted line")

    finale = method(command, "placeHoldFinaleMarkers")
    if "ARMOR_STAND" in finale and "i * 2.0" in finale:
        blockers.append("restore, erase, and release are generic named armor stands only two blocks apart, allowing visual/interaction ambiguity")
    release = (LISTENERS / "ReleaseRiteListener.java").read_text(encoding="utf-8")
    if "FLAG_BOWED_AS_ONE" in release and "FLAG_SEVENTH_CHOICE" not in release:
        blockers.append("release checks bowed_as_one but not that restore/erase was chosen, so the closing marker can skip the choice")

    memorial = (LISTENERS / "ShoreMemorialListener.java").read_text(encoding="utf-8")
    if "getPitch()" in memorial and "rayTrace" not in memorial and "getDirection" not in memorial:
        blockers.append("Sella memorial solves from position plus downward pitch without verifying the player is looking at the bird/pool")

    listener_text = "\n".join(path.read_text(encoding="utf-8") for path in LISTENERS.glob("*.java"))
    if "dread_route" not in listener_text:
        blockers.append("Dread route has no live proximity/progression listener; its scare sequence exists only behind an operator rehearsal command")

    integrated_types: set[str] = set()
    for labels in re.findall(r"case\s+(.+?)\s*->", integrated):
        integrated_types.update(re.findall(r'"([^"]+)"', labels))
    direct_types = {"mara_lectern", "sella_lectern"}
    special_ids = set(re.findall(r'"([^"]+)"\.equals\(id\)', integrated))
    keeper_table = re.search(
        r"private static final String\[\]\[\] KEEPER_SPINE\s*=\s*\{([\s\S]*?)\n\s*};",
        command,
    )
    keeper_ids = set(re.findall(r'\{\s*"([^"]+)"', keeper_table.group(1))) if keeper_table else set()
    fallback_ids = sorted(
        site_id for site_id, site_type in site_rows
        if site_type not in integrated_types | direct_types
        and site_id not in special_ids
        and site_id not in keeper_ids
    )
    if fallback_ids:
        blockers.append(
            "production sites currently dependent on lab/template fallback: " + ", ".join(fallback_ids)
        )

    print(f"Deep Hold executable-content audit: {len(site_ids)} sites, {len(blockers)} blocker(s)")
    for index, blocker in enumerate(blockers, 1):
        print(f"  {index:02d}. {blocker}")
    if not blockers:
        print("  PASS: no statically detectable content/fallback/orientation blockers")
    elif not args.strict:
        print("  report mode: use --strict to make these blockers fail the command")
    return 1 if args.strict and blockers else 0


if __name__ == "__main__":
    raise SystemExit(main())
