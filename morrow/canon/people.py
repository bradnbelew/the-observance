"""The cast, their time zones, and the facts players must write down in Act 1."""

from __future__ import annotations

from dataclasses import dataclass
from zoneinfo import ZoneInfo

SERVER_TZ = ZoneInfo("America/Chicago")  # Copperline Chicago, node CHI-07


@dataclass(frozen=True)
class Resident:
    handle: str
    name: str
    location: str  # as shown on the 2011 forum profile
    tz: str
    joined: str  # forum join date, ISO; defines the D15 join order
    role: str
    lwc_password: str  # their LWC chest password; the D15 key material
    password_source: str
    sheep_dye: int  # cairn's census colour, as a 2011 dye damage value (351:x)


RESIDENTS: tuple[Resident, ...] = (
    Resident("rookery", "Rowan Keel", "Ottawa, ON", "America/Toronto", "2010-10-30",
             "Mossfield admin and founder", "bracken", "Act 1 security answer; C1 world seed", 0),
    Resident("cairn", "Sam Calder", "Ames, IA", "America/Chicago", "2010-11-04",
             "farms and the storehouse", "hingetin", "C16 listening wall, Act 8", 3),
    Resident("finchline", "Mina Finch", "Grand Rapids, MI", "America/Detroit", "2010-11-20",
             "rails and redstone", "booster", "D1 torch lock", 11),
    Resident("juni_lantern", "June Harrow", "Halifax, NS", "America/Halifax", "2010-12-11",
             "the lighthouse, screenshots", "sambro", "C4 lighthouse characteristic", 12),
    Resident("patchcord", "Eli Mercer", "Manchester, UK", "Europe/London", "2011-01-08",
             "Mumble server, jukeboxes, recordings", "baggage", "C2 note blocks", 5),
    Resident("kitbag", "Kit Vale", "Naperville, IL", "America/Chicago", "2011-02-26",
             "Theo's younger brother, 14", "biscuit", "D3 fossil base", 10),
)

STAFF = {
    "i_bell": {"name": "Iona Bell", "irc": "ibell", "role": "Copperline sysadmin",
                "tz": "America/Chicago", "forum_joined": "2009-03-02",
                "habit": "always writes CDT in summer and CST in winter, correctly"},
    "tvale": {"name": "Theo Vale", "irc": "tvale", "role": "Copperline support lead",
               "tz": "America/Chicago", "forum_joined": "2009-01-19",
               "habit": "two spaces after a full stop; 'No worries at all!'; signs 'Cheers, T'"},
}

FABRICATED = {
    "lowpath": {"created_by": "morrow session continuation", "forum_created": "2016-08-03",
                 "named_after": "2011 Dynmap path marker 'low path'",
                 "skin": {"head": "juni_lantern", "torso": "cairn", "arms": "kitbag", "legs": "finchline"}},
}

THEO_MACRO = "No worries at all!  Your world will be back by tomorrow."


def join_order() -> list[Resident]:
    return sorted(RESIDENTS, key=lambda r: r.joined)


def by_handle(handle: str) -> Resident:
    for r in RESIDENTS:
        if r.handle == handle:
            return r
    raise KeyError(handle)
