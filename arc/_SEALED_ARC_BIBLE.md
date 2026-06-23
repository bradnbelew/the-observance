# ⚠️ SEALED — STORY SPOILERS — DO NOT READ IF YOU WANT TO PLAY UNSPOILED ⚠️

This is the authored arc for *The Observance*. Ethan intends to play as a participant.
If that's you: **close this file.** The system is built so you never need to open it —
the showrunner and the plugin read it, you don't.

---

## The Observance

The world had **keepers** before this group. The customs are theirs. The land — or the
thing that is the land's memory — watches to see what kind of tenants the newcomers are,
and keeps a record of every law honored or broken. It is not hostile by default. It is
*deciding*.

The fusion: **customs you must perform** (do real things) + **a presence that watches and
catalogues** (the reports) + **it was here first** (you are tenants until you observe).

## The presence — the sealed truth

The presence is not a separate monster. It is the **accumulated keepers** — the world's
memory of everyone who ever observed the customs. The "previous group" did not vanish.
They *became the world*: the markers you bow to, the Keeper NPC, the very stones. To be
**accepted** is to join that memory — to become, eventually, a marker for the next group.

The customs were never protection *from* the presence. They are the **induction** *into*
it. "Acceptance" is beautiful and quietly terrible: the world becomes truly yours, and you
can never wholly leave it.

## The customs (full list — detection in DESIGN.md §2.4)

Authored set (the plugin only enforces the cleanly-detectable ones; escalation column =
what a sustained violation eventually triggers):

| Custom | Rule | Detection | Escalation on repeat violation |
|---|---|---|---|
| The Bow | crouch at the markers | `PlayerToggleSneakEvent` near marker | markers "watch" the offender (named-mob attention) |
| The Offering | give back the first of the deep | first-ore break + item at cairn | the deep goes dark for them (light tolls) |
| The Dark Hours | do not sleep on the black moon | `PlayerBedEnterEvent` + moon phase | nightmares (sleep title/sound), bed denies |
| The Deep Line | never break the lowest dark stone bare | `BlockBreakEvent` + Y threshold | the depth answers (sounds, sealed tunnels) |
| The Unspoken | never write its name | `AsyncChatEvent` scan | immediate presence response |
| The Kept Light | one fire must always burn at home | base light scan | it comes inside (cold beat at base) |
| The Sacred Beast | never kill the pale one | `EntityDeathEvent` on tagged mob | the herd remembers (mob behavior) |
| The Quiet Day | break no stone on the rest-night | break + moon/time window | the stone resists |
| The Ward | keep iron at the threshold | base block scan | the threshold opens |
| The Covering | close chests before dark | `Openable`/container state at dusk | things go missing/rearranged |

Discovery loop: break unknowingly → consequence → hypothesis → test. The **reports** name
the offender and the law. Lore fragments (rune-books, map-art) hint. **Whispers** can buy
a tiered nudge.

## The arc

- **Act 1 — Watched.** The haunting + the first reports. The group learns they are being
  recorded and that the land has *ways*.
- **Act 2 — The Ways.** Reverse-engineer the customs. Rune-language ciphers, map-art, the
  fragments of prior keepers. Discord becomes the group's evolving "list of laws."
- **Act 3 — The Accepting.** The ritual (below).

## The rune-language + Whispers

A resource-pack rune font is the prior keepers' alphabet — a substitution cipher that is
*also* world-building. Early clues teach a few glyphs; by Act 2 the group half-reads the
world. **Whispers** (Discord, rationed) buy tiered hints; each spend tolls the asker in-game
and ticks their **bond**.

## The casting (record → who is marked)

The bond ledger + custom-compliance record decide the endgame. The player who leans most on
the Whispers / is most bound is marked as the land's **go-between** (warm, earned — not the
old "courting"). The faithful-most are received well; the transgressors are received… differently.

## The ending ritual — "The Accepting"

To become *kept*, the group must, together:

1. **Gather named components:** *the deep's first heart*, *an unbroken light*, *salt of the
   old keepers* (custom-named items the plugin validates by name/lore), **plus one personal
   token per keeper.**
2. **Wake the Keeper** (Citizens2 NPC) — a remnant of those before, who presides.
3. **Deposit** each component into the altar's marked places (item frames / barrels at exact
   coords) — plugin validates exact items in exact slots.
4. **All present, at the right hour, bow together** — proximity + time + simultaneous
   `PlayerToggleSneakEvent` from everyone → the rite fires.

On success: the world flips to **kept** (persistent world state), the presence manifests and
**changes**, an advancement seals it — and the outcome bends to the **record** (how the
Keeper receives the group, who it marks). On refusal/failure at the threshold: the land stops
tolerating them.

The recontextualization: every early haunting was the land *testing and cataloguing* them;
the laws they suffered to learn were the entrance exam. And — the sealed turn above — being
*accepted* means becoming part of the watching, for whoever comes next.
