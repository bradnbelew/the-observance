"""Static cross-surface contract for the Minecraft -> web relay -> Discord ignition."""

from __future__ import annotations

import pathlib
import sys

ROOT = pathlib.Path(__file__).resolve().parent.parent


def read(relative: str) -> str:
    return (ROOT / relative).read_text(encoding="utf-8")


def require(relative: str, *needles: str) -> list[str]:
    text = read(relative)
    return [f"{relative}: missing {needle!r}" for needle in needles if needle not in text]


def main() -> int:
    failures: list[str] = []
    failures += require(
        "tools/build_hold_prologue.py",
        "find the retired Copperline relay",
        "bind every field name",
        "#the-record",
    )
    failures += require(
        "plugin/src/main/java/com/observance/watcher/structure/StructureTemplates.java",
        "public static Location discordRelay",
        "oldest to newest",
        "copperlinehosting.com/support/ticket.php?id=1842",
        'String[] ports = {"7", "9", "3", "1"}',
    )
    failures += require(
        "plugin/src/main/java/com/observance/watcher/command/ObservanceCommand.java",
        'case "placerelay"',
        'new Site("discord_relay", "discord_relay"',
        "countFacingLecternsNear",
        "relayShellIntact",
    )
    ticket = read("dashboard/src/app/support/ticket.php/page.tsx")
    for needle in (
        "process.env.DISCORD_INVITE_URL",
        "/link YourExactMinecraftUsername",
        "#the-record",
        "type <code>kept</code>",
    ):
        if needle not in ticket:
            failures.append(f"dashboard/src/app/support/ticket.php/page.tsx: missing {needle!r}")
    if "discord.gg/" in ticket or "discord.com/invite/" in ticket:
        failures.append("ticket page hardcodes a Discord invite instead of using the rotatable environment value")
    failures += require(
        "dashboard/src/lib/discord-relay.ts",
        'RELAY_CALLBACK = "9137"',
        "safeDiscordInvite",
        'url.protocol === "https:"',
    )
    failures += require(
        "discord/src/voice.ts",
        "run /link with your exact minecraft username first",
        "when every hand is bound and present in #the-record, write kept once",
        "the record does not exchange one hand for another",
    )
    failures += require(
        "discord/src/bot/commands/link.ts",
        "getPlayerByDiscordId",
        "voice.linkFixed",
    )
    failures += require(
        "discord/src/bot/commands/whisper.ts",
        "getOpenPuzzles",
        "A manually typed key must obey the same progression gate",
    )
    failures += require(
        "discord/src/db/repo.ts",
        "!openByKey.has(row.puzzle_key)",
        "found.discord_id !== discordId",
    )
    failures += require(
        "discord/src/bot/index.ts",
        "auditDiscordSurface",
        "PermissionFlagsBits.ViewChannel",
        "PermissionFlagsBits.SendMessages",
        "PermissionFlagsBits.ReadMessageHistory",
        "PermissionFlagsBits.AttachFiles",
    )

    if failures:
        print("discord onboarding check: FAILED")
        for failure in failures:
            print(f" - {failure}")
        return 1
    print("discord onboarding check: OK - map, relay room, rotatable invite, /link, ignition, and spoiler gates agree")
    return 0


if __name__ == "__main__":
    sys.exit(main())
