# V5 NPC dialogue integration

Status: current

Canonical text: `arc/v5/npc-dialogue.json`

World truth: `arc/WORLD-BIBLE.md`

The previous draft trees were removed because they repeated the old Ways/seventh thesis and described a state machine that was not the shipped runtime.

## Runtime contract

- Critical Minecraft dialogue is read synchronously from the canonical resource or its generated plugin copy.
- Database logging and mirrored Discord events are asynchronous and never delay a reply.
- Every critical state line is replayable.
- Town dialogue is mostly ordinary local life. Only a minority of lines point at ARG evidence.
- NPC bodies are invulnerable, persistent, protected from equipment changes, and repaired after restart.
- Production uses individually authored anchor sites with yaw toward a clear player approach. `spawnAll` row placement is rehearsal-only and must fail production preflight.
- A generated parity test compares canonical JSON hashes with the plugin and Discord copies.

## Production anchors

| NPC | Site ID | Placement requirement | Critical job |
| --- | --- | --- | --- |
| Aro | `npc_aro_anchor` | inn/common room; clear two-player approach | distinguish drainage-cut Hold Mouth from village well |
| Wenna | `npc_wenna_anchor` | garden near but not blocking well route | teach that the well is the later Unlit entrance |
| Coll | `npc_coll_anchor` | market counter with accessible ledger | supply/service trail and Camp Ash bearing |
| Dob | `npc_dob_anchor` | masonry yard facing drainage axis | safe four-flight Mouth route and structural context |
| Old Pell | `npc_old_pell_anchor` | pump-side bench | Nessa's procedure and Toma's relief shift |
| Wren | `npc_wren_anchor` | state-dependent threshold/relay anchors | prior-company testimony confession and reckoning |

The world operator binds each site at the real chosen village coordinate. The setup guide records world, XYZ, yaw, approach cell, and a screenshot for each anchor.

## Required state families

Townsfolk use the minimal state family they actually need: arrival, relevant case lead, post-case reaction, and coda. Wren uses evidence topics, confession, three reckoning branches, and three coda states. No NPC invents or withholds a critical answer based on a hidden conduct score.

The exact current lines are in `arc/v5/npc-dialogue.json`; do not copy-edit Java or TypeScript by hand.
