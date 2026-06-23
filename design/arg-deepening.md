# The Observance — ARG Deepening (broadened design)

Companion to `DESIGN.md`, `FLOW.md`, and `arc/_SEALED_ARC_BIBLE.md`. This document widens
the ARG layer: it makes every player-facing element *concrete and buildable*, gives the
Keeper and the six prior keepers real branching character, broadens the story without
touching the single sealed twist (the presence = the accumulated keepers; acceptance =
induction), and lays out the mod-stack tradeoff with an opinionated call.

Nothing here breaks the anti-jank contract (`DESIGN.md §3`): the spine stays deterministic,
the LLM stays a text-only scalpel, reveals stay out of line of sight, structures stay
curated schematics, and tolls stay reversible.

---

## 1. HOW it melds — every ARG element as a concrete Minecraft mechanic

For each element: **what the player sees**, **what they do**, **how the plugin detects and
responds**. All server-side unless flagged `[PACK]` (one auto-pushed resource pack — a single
"accept?" prompt on join, not a modpack) or `[MODPACK]` (a real client install).

### 1.1 Encountering + decoding a cipher (the substitution / rune layer)

**Sees.** A keeper-stone with carved glyphs (a sign, a lectern book, or a banner wall) rendered
in the prior keepers' alphabet `[PACK]` — the rune font maps glyphs to ASCII so any sign / book /
title / lectern shows runes. Beside the first stone, a **Rosetta**: a short sign showing 4–5 runes
next to their vanilla letters (`ᚱ = R`), so the alphabet is *learned*, not handed over.

**Does.** Reads the glyphs against the Rosetta, writes the plaintext down (out-of-game on paper,
or in Discord), then **acts on the decoded word** — the decode is never the end, it always points
at a verb (a named item, a coordinate, a direction word like `DESCEND`, or a sign to fill in).

**Plugin detects / responds.** The font is pure resource-pack; the *content* is authored by the
showrunner and written into the stone via `Sign`/`Lectern`/book NBT. When the answer is "type it
back into the world," a blank sign at the stone is validated with `SignChangeEvent`: compare the
typed text (case-folded, trimmed) to the expected plaintext; on match fire the next beat from the
curated library (a door, a particle bloom, a report excerpt). No match = silence (soft-pressure).
The cipher alphabet **builds across Movements** — by II–III the group half-reads the world unaided,
which is the real payoff.

> Cipher → verb variety (so no two stones feel alike) — assign each its own decode *and* its own act:
> substitution→fill-a-sign, Caesar→rotate item-frames, Vigenère→keeper-name keyword, Atbash→read a
> water reflection, book-cipher→walk a lectern shelf, coordinate→travel. See §1.2 and the verb menu in §1.7.

### 1.2 Running ONE Keeper-Stone expedition, end to end (the expedition engine)

This is the spine verb of Movement II. Six stones, each a *different* cipher + *different* physical
verb so the six expeditions never feel same-y. Worked example — **Stone 3, the Atbash mirror at a lake:**

1. **Hook (no marker).** The server-icon rune ring (the master rabbit hole) and Stone 2's carving
   both carry a **coordinate cipher**: rune-digits → numbers → an X/Z. No `/tp`, no waypoint — the
   group reads the coordinate and **physically travels**. The decode→walk→arrive loop is what makes
   the world feel *navigated*. A faint proximity bossbar (`BossBar`, intensity scaling with distance)
   can read as "the land's attention," never as a UI waypoint.
2. **Pre-placement (reveal discipline).** Before the group arrives, FastAsyncWorldEdit pastes Stone 3's
   shrine + cairn schematic **async, in an unloaded/unwitnessed chunk** hundreds of blocks ahead, so it
   is *discovered*, never seen appearing (`DESIGN.md §2.2`, `§3.3`). Footprint-checked schematic = no
   floaters in rock/air (anti-jank #2, #4).
3. **Arrival detect.** Scheduled `player.getLocation()` proximity sampling (never `PlayerMoveEvent`)
   fires the site's "arrived" state once a player is within radius.
4. **The puzzle = a physical verb.** Stone 3's ciphertext is **mirrored** — readable only in the lake's
   water reflection (Atbash A↔Z reinforces "the mirror"). The *act* is positional: a `TextDisplay`
   glyph completes only when the player stands where the reflection lines up. Detection = proximity to
   the "reflection-complete" coordinate; the beat fires when they stand in it. (Other stones: Stone 1
   Caesar → rotate a ring of item-frames, read rotation via comparator or `ItemFrame.getRotation()`;
   Stone 2 book-cipher → page/line/word triples walked across a lectern shelf, `Lectern.getPage()`;
   Stone 4 banner-glyph wall; Stone 5 beacon-beam colour sequence; Stone 6 Vigenère keyed on Stone 1's
   keeper name.)
5. **The keeper vision.** A per-player packet NPC (ZNPCsPlus) or Citizens2 ghost appears — only to the
   player who solved the prior stone — tells that keeper's story, and **gives a rune-key fragment**
   (a custom-named item, validated by name/lore + a `PersistentDataContainer` tag). See §2.
6. **Fragment economy.** The fragment writes to Supabase (`FLOW §1`); 6 fragments + the final
   coordinate cipher = the Movement III gate. Honoring the stone's local custom can unlock the next
   Discord fragment automatically (cross-surface, §1.5).

Everything in this loop is **Tier-0 server-side** except the rune font `[PACK]`.

### 1.3 Performing / breaking a custom (the "do real things" layer)

Take **The Offering** (`arc` custom table) as the worked case; the same shape covers the other nine.

**Sees.** A cairn/altar near where they mine (a small FAWE-pasted structure with a marked deposit slot —
an item frame or a `Barrel`). The Archivist reports (lecterns + Discord) are the only "tutorial": they
name the law and, later, the offender (`DESIGN §2.6`).

**Does — performing.** On breaking the **first ore of a session**, the player walks to the cairn and
**drops** (or frames) a piece of it. Honoring is a deliberate gesture, not automatic.

**Plugin detects — performing.** `BlockBreakEvent` flags "first ore this session, this player" in the
tracker; `PlayerDropItemEvent` / `EntityDropItemEvent` + proximity to the cairn coordinate + a PDC/name
check on the dropped item = compliance logged. Right item, right place → quiet acknowledgement (a soft
`player.playSound`, a single particle). Honoring updates custom-compliance in the dossier.

**Does — breaking.** They skip it and keep mining.

**Plugin detects / escalates — breaking.** Tracker marks the session non-compliant. The escalation is
**soft and reversible** (anti-jank #10): the next descent, "the deep goes dark" — a **Kept Light**-style
toll that removes *warmth* (douse a wall torch via `WALL_TORCH`→air outside line of sight, a cold sound),
never *progress*. Repeat violations escalate per the `arc` escalation column, and a **report** eventually
names the player ("...has not given back to the deep, not once") — true because measured. The discovery
loop is **break → consequence → hypothesis → test**, legible through reports, nudgeable via Whispers.

Other customs reuse the same detect-pattern set already in `DESIGN §2.4`: gesture (`PlayerToggleSneakEvent`),
time/moon taboo (`fullTime/24000 % 8`), depth taboo (`block.getY()`), forbidden word (`AsyncChatEvent` —
`The Unspoken`, which inverts the verb: the puzzle is *not* typing it), ward/kept-light base scan,
protected mob (`EntityDeathEvent` on a tagged entity), container/covering (`Openable` at dusk).

### 1.4 Learning the rune language (incremental literacy as a mechanic)

**Sees.** Movement I: a Rosetta teaching ~5 glyphs. Each subsequent stone/report quietly introduces a
few more in context (a known word printed in runes so the new glyph is inferable). By Movement II the
group reads short words; by III, full lines.

**Does.** Builds a personal/shared key (the Discord channel naturally becomes the group's evolving
"list of laws" and glyph table). Literacy compounds — the same font is used in-world *and* in Discord
(CLUE FORGE), so a glyph learned at a stone unlocks a Discord artifact and vice versa.

**Plugin / authoring.** One `[PACK]` rune font + an optional **negative-space font** (AmberWat's
NegativeSpaceFont) for precise glyph positioning and font-image overlays. The CLUE FORGE renders Discord
artifacts in the *same* font, so the alphabet is a single shared asset across both surfaces. No special
detection — literacy is emergent; the only gates are the decoded answers (§1.1).

### 1.5 A cross-surface handoff (the TINAG congruence rule, `DESIGN §2.8`)

The magic is a clue that **starts in one surface and resolves in the other.** Concrete worked loop
(**World → Discord → World**):

1. **World.** A banner-glyph wall at a stone decodes (substitution) to a **password word**.
2. **Discord.** A player types that word to **The Watcher** in the `/whisper`-style channel. The bot
   (pre-authored, never improvising — `DESIGN §2.7`) replies *in character* with the **next coordinate**
   (itself in runes, so it must be decoded again).
3. **World.** The group travels to that coordinate and finds the next stone / the descent entrance.

Build: the plugin emits the "password accepted" event over the Discord bridge (webhook/bot); the bot's
reply is a **pre-authored tier** keyed to that puzzle (no hallucination). Supabase is the shared state so
a custom honored in-world can unlock the matching Discord fragment automatically (`FLOW §1`, fragment
economy). **Design rule: every major cipher crosses at least once** — the crossing *is* the "one world"
payoff. Variant loops: **Discord → World** (CLUE FORGE posts a rune artifact only readable once glyphs are
learned in-world; its plaintext is a coordinate), and the **reflection handoff** (Discord posts half a
mirror-cipher; the in-world water reflection is the other half; overlaid they complete).

### 1.6 The final rite — "The Accepting" (`arc` ending ritual)

**Sees.** The altar room (FAWE-pasted set-piece) with marked deposit places — item frames / barrels at
exact coordinates, each labelled with a `TextDisplay` rune naming its component. The Keeper NPC (Citizens2),
woken, presides.

**Does — together:**
1. **Gather named components** — *the deep's first heart*, *an unbroken light*, *salt of the old keepers*
   (custom-named items) **plus one personal token per keeper** (six, from the expeditions).
2. **Wake the Keeper** (NPC interaction).
3. **Deposit** each component into its exact slot.
4. **All present, right hour, bow together.**

**Plugin detects / responds.** Component validation = **name + lore + PDC tag**, and `custom_model_data`
for the look `[PACK]` (`arc` "validates by name/lore"). Slot validation = right item in right frame/barrel
at exact coords (read `ItemFrame`/`Barrel` contents). The final trigger = **proximity (all present) + time
window (`world.getTime()` / moon) + simultaneous `PlayerToggleSneakEvent` from everyone** within a short
sync window. On success: persistent world flips to **kept**; the presence manifests and changes (curated
schematic swap, out of sight); a **hidden custom advancement** with a custom toast seals it diegetically
("⟡ The record receives you") — granted via `awardCriteria(...)`, icon `[PACK]`. The outcome bends to the
group's **record** (the collective reckoning — faithful kept, careless cast out; **no chosen one**,
`arc`). On refusal/failure at the threshold: the land stops tolerating them. Everything here is idempotent
and persistent (anti-jank #9) so it can never double-fire or replay.

### 1.7 The verb menu (so nothing repeats)

Rotate physical "answer verbs" so each puzzle *feels* different — assign each stone and each Undercroft
door a distinct one:

| Verb | Mechanic | Detect |
|---|---|---|
| **Rotate** a rune dial | item-frame ring (Caesar) | `ItemFrame.getRotation()` / comparator |
| **Turn to a page** | lectern + comparator | `Lectern.getPage()` / redstone |
| **Reflect** | stand where water/ice completes a mirror-cipher | proximity to completion coord |
| **Fill in a sign** | type the decoded word | `SignChangeEvent` |
| **Bring & deposit** | named items in exact slots | frame/barrel content check |
| **Gesture / knock** | crouch-bow, rhythmic multi-crouch | `PlayerToggleSneakEvent` timing |
| **Throw / pour** | drop item into well/cauldron | `PlayerDropItemEvent` + proximity |
| **Light / extinguish** | flint-and-steel a net of campfires in order | `BlockPlaceEvent` / state |
| **Look / align** | line up two map-arts or a beacon beam | `MapView` / glass-move detect |
| **Refrain** | solve by *not* typing the taboo word | `AsyncChatEvent` (inverted) |
| **Time it** | act on the right moon phase / hour | `fullTime/24000 % 8` |
| **Walk a glyph** | pressure-plate path that traces a rune | plate sequence |

---

## 2. NPC depth — the Keeper + the six prior keepers as real branching characters

**Framework call (opinionated):**

- **Body of the Keeper** = **Citizens2** (already in `DESIGN §4`; GPL, server-side, no install). One
  persistent presiding NPC. Entry hook = `NPCRightClickEvent`.
- **Bodies of the six prior keepers** = **ZNPCsPlus** packet NPCs — 100% packet-based, off-main-thread,
  shown **per-player**. They cost nothing when nobody's there and can appear to *one* player only
  (congruent with stalking/reveal discipline). Each keeper is an **ephemeral apparition at its stone**,
  not an always-loaded entity.
- **Dialogue brain** = **build the branching tree inside our own plugin**, not in a second scripting
  system (Denizen/CharacterDialogue exist but live outside the JVM). Reason: conversations must branch on
  **measured conduct** from the Supabase dossier ("you, Marcus, broke the Deep Line"), and our grounding /
  validation already lives in the plugin. Render branches as JSON-chat clickable options on
  `NPCRightClickEvent`; persist conversation state per player. Vanilla 1.21+ also has a native
  **dialogue screen** datapack feature for simple choice menus as a zero-dependency fallback.
- The **scalpel (LLM)** writes at most **one** keeper's marquee line — schema-validated, referencing a
  *verified* tracker fact, deterministic fallback behind it (`DESIGN §2.5`). Rare, earned, one moment.

**How they remember the group.** Every keeper NPC reads the player's dossier (customs honored, fragments
held, bond/Whisper tally, named transgressions) and **branches on it**: a keeper you honored speaks
plainly and gives its fragment; one you transgressed against speaks in riddle, or **withholds** its
fragment until you atone (a Movement IV hook — honor the broken custom, then return). Dialogue thereby
**gates progress**: the fragment is the key, and conduct is the lock.

**The six as distinct characters (voice constraints — each *characterizes* a cipher type):**

| # | Keeper (working name) | Voice constraint | What they give / withhold | Fate that rhymes with a player |
|---|---|---|---|---|
| 1 | **The Hoarder** (Vaun) | speaks plainly, but only of *what he kept* | gives the Caesar key; withholds warmth if you hoard | the solo-miner who hoards (mirrors a tracked player's hoarding ratio) |
| 2 | **The Reader** (Mara) | speaks only in **book-cipher page refs** — you must walk the lectern shelf to "hear" her | gives the Vigenère keyword (her own name) | the one who always has the map, never the shovel |
| 3 | **The Drowned** (Sella) | speaks only as a **reflection** — face the water or get silence | gives the mirror-cipher half | the one who wandered off alone (mirrors high distance-from-group) |
| 4 | **The Silent** (Orin) | speaks **only when you are crouched** (`PlayerToggleSneakEvent`) | gives a banner-glyph fragment | the one who never bowed — until too late |
| 5 | **The Night-Walker** (Brann) | speaks **only at night / on the black moon** | gives the beacon-sequence | the one who slept on the Dark Hours |
| 6 | **The Liar** (Iss) | speaks plainly but **lies** — a later stone reveals the lie, recontextualizing his dialogue | gives the final coordinate fragment — the *true* one only after you catch the lie | the one who leaned hardest on Whispers (the bond tally) |

The Liar (6) is the per-Movement-II surprise: his branch *re-reads* once Stone-after reveals the deception
(a dialogue tree whose earlier nodes change meaning). The six fates **rhyme** with the current players'
tracked behaviors without ever singling anyone out — pure flavor, judged collectively (`arc`).

`[MODPACK]` option (skip by default): **Simple Voice Chat** lets a keeper *whisper* spatial audio — large
dread gain, but a client install (see §4).

---

## 3. Broadened story — wider, richer, one genuine surprise per Movement

**Constraint:** the single deepest sealed twist is untouched and unspoiled past — the presence is the
**accumulated keepers**; to be *accepted* is **induction** into the watching, for whoever comes next
(`arc`). Everything below sits *before* that line and feeds it.

### The five Movements, broadened

**Movement I — The Notice.** *Set-piece:* the first report appears on a lectern in the group's own base
(found, not delivered), and the **server icon itself** is a rune ring (the master rabbit hole) — the
metadata of "their" server is already the keepers' alphabet. *Surprise:* a report names a player for a
habit they didn't know was a law — the world was **already grading them** before they knew the rules
existed. *Verbs: read, travel.*

**Movement II — The Keeper-Stones.** *Set-piece:* six expeditions, each a different cipher + verb + a
distinct prior-keeper apparition (§2). The stones "appear" between sessions (FTF-style structure-building,
reimplemented server-side, out of sight). *Surprise:* **The Liar** (Keeper 6) — a keeper's testimony is
false; a later stone proves it, forcing the group to re-walk a clue they thought was solved. *Verbs:
rotate, page-turn, reflect, look, light, recall-key.*

**Movement III — The Undercroft.** *Set-piece:* a lectern-comparator door descent into a dedicated
**custom dimension** (Multiverse void world with ambient-light-0 / no-sky / thick built-in fog — the only
no-install way to get true environmental fog). The descent is "into the keepers' memory" made literal —
you leave your world to enter theirs. *Surprise (false climax / reversal):* the altar room **visibly
rebuilds itself into something wrong** — a curated schematic A→B swap done out of sight, so the group walks
back into a changed room — and The Watcher reveals the rite "is not a transaction." This is the midpoint
gut-punch that *points toward* the sealed truth without naming it. *Verbs: descend, page-turn, witness change.*

**Movement IV — The Reckoning.** *Set-piece:* the keeper NPCs branch on the dossier and **turn on the
group** — withholding fragments, demanding **atonement** (honor previously-broken customs: gesture / offer
/ time / refrain). The record stops being passive. *Surprise:* a keeper you trusted (or The Liar, now
exposed) reveals that a "haunting" the group suffered in Movement I was **a specific keeper's fate
re-enacted at them** — the dread had a biography. *Verbs: gesture, offer, time, refrain (The Unspoken).*

**Movement V — The Accepting.** *Set-piece:* the altar combination lock + simultaneous group-crouch at
the right hour (§1.6). *Surprise (the sealed turn — do not spoil past):* the advancement seals it and the
world flips to **kept**; the recontextualization lands (every early haunting was the entrance exam). *Verb:
collective deposit + synchronized gesture.*

### The six prior keepers as a rhyming chorus

The fates in §2's table are the story's emotional engine: each prior keeper's downfall **rhymes** with a
behavior the tracker is *already measuring* in the current group (the Hoarder ↔ the hoarder, the Drowned ↔
the wanderer, the Silent ↔ the one who won't bow). The group slowly realizes the stones are **warnings
shaped like the people standing next to them** — without the land ever naming a favorite (collective
judgment, `arc`).

### 2–3 optional side-mysteries (soft-pressure, ignorable)

1. **The Seventh Stone.** The reports imply six keepers, but a stray glyph hints at a **seventh** who was
   *cast out* (not kept). Optional expedition to a ruined, light-doused shrine; finding it earns Whisper
   budget and reframes "acceptance" as a real *choice the land makes* — foreshadowing the cast-out ending
   without revealing the induction twist. Pure lore; ignoring it just leaves it quiet.
2. **The Self-Rewriting Journal.** A keeper's journal on a lectern in the base **changes its pages between
   sessions** (plugin swaps book `pages` NBT out of line of sight; new pages authored by the showrunner).
   Players who re-read across nights catch it evolving — a private, creeping mystery that rewards
   attention and never blocks anyone who skips it.
3. **The Haunted Herd.** A pale animal in the local herd is **the Sacred Beast** (a tagged mob); the herd
   subtly "watches" (FTF haunted-herd, server-side mob retag). Optional: a player who protects it across
   the run earns a quiet boon at the Accepting; killing it is a tracked transgression. Ambient, opt-in.

---

## 4. Recommended mod stack + the big tradeoff

The decisive fact: **on Java there is NO way to show a custom texture / model / sound without a resource
pack on the client** — but since MC 1.20.3, Paper's `setResourcePack(url, hash, required, prompt)` lets the
**server auto-push the pack on join**. The player gets one "use pack?" prompt (or it's forced). **That is
not a modpack install.** So the real axis is *how much custom content vs. pure vanilla tricks*, not
"install or not."

### Path A — Server-side only (Paper plugins / datapacks / one auto-pushed resource pack)

**What it unlocks (everything in this arc):**
- **Tier 0 (zero player friction):** Paper + **FastAsyncWorldEdit** (async schematics — stones, cairns,
  Undercroft, the reversal; GPL-3.0, Java 21, Paper 1.21.x) + **Citizens2** (Keeper; GPL) + **ZNPCsPlus**
  (per-player keeper apparitions; GPL) + **Multiverse-Core** (the Undercroft as a custom-dimension with
  real fog/darkness; GPL) + **Display Entities** (vanilla floating glyphs/labels, per-player) +
  **PacketEvents** (per-player fake blocks/signs/sounds — the "it knows *me*" beats; open-source) +
  **Darkness `MobEffect`**, **`player.playSound`**, **particles**, **datapack/command-block redstone &
  scoreboard logic**. This already delivers the full stalking vocabulary (line-of-sight discipline, not
  custom assets — a vanilla husk only ever *discovered* reads supernatural), all customs, all expeditions,
  the descent, the Keeper, and the reckoning.
- **Tier 1 (one auto-pushed `[PACK]`):** the **rune font** (the whole substitution cipher — required for
  the arc) + **mono ambient sound bed** (keeper whispers, per-stone drones — author `.ogg` **mono** or it
  won't attenuate) + **custom item textures / `custom_model_data`** for the named ritual components +
  **negative-space font** for HUD/positioning. One pack carries fonts + sounds + item art. Highest-leverage
  non-Tier-0 step; the cipher needs the font anyway.
- Vanilla tricks that cover ~90% with no framework: **`custom_model_data`** for unique item looks,
  **`PrepareAnvilEvent`** ("rename an item and it answers"), **`Lectern.getPage()` + comparator**
  (page-turn = answer), **item-frame dials**, **`SignChangeEvent`** (type the answer into the world),
  **self-rewriting books** (swap `pages` NBT out of sight), **hidden advancement toasts** (diegetic "the
  record notes you"), **structure-block / FAWE rooms**.
- **Optional one framework, only if needed:** **Nova** (free, LGPLv3, framework-you-extend) for
  *interactable custom furniture/blocks* (altars, cairns, keeper-stones as real placeable objects) if you
  want them as more than schematic decoration. Or **MythicCrucible** / free **Item Caster** to author many
  *varied* item-use rites in config without writing Java per interaction (its core works **without a
  pack**). Default: skip both — our own plugin + vanilla tricks already cover the authored customs.

**What it costs the friend group:** one "accept resource pack?" click on first join. Nothing else. Everyone
"just joins."

**Why it's the right default:** it satisfies the anti-jank contract (deterministic spine, no client deps
for Phase 0), carries **zero third-party update risk** on the spine, preserves the **"it's just our
Minecraft server" TINAG illusion**, and — per the research — gets ~90% of the modpack "look" (fog via the
custom dimension, atmosphere via the pack) **without** asking veterans to install anything. License posture
is clean: FAWE/Citizens2/ZNPCsPlus/Multiverse (GPL) and PacketEvents (open) are all fine to *depend on* as
a server operator; **From The Fog is proprietary — reference the techniques only, never vendor the code.**

### Path B — Add content mods / a small client modpack (players install)

**What it unlocks (and only this is worth a real install):**
- **Simple Voice Chat** `[MODPACK]` — spatial keeper *whispers*, proximity dread, "the world speaks." The
  one install with a payoff (late-arc dread, Movements III–V) that the server already plans (`DESIGN §4`,
  Phase 3).
- **Fabric/Forge custom-dimension or shader pack** `[MODPACK]` — true per-player render-fog tuning, a
  visually distinct "kept vs. cast-out" world flip, custom models/shaders. High friction.

**What it costs the friend group:** every player installs and maintains a mod loader + matching mod
versions; version drift breaks "everyone can just join"; it **damages the TINAG illusion** (now it's
visibly "a modded thing") and raises the barrier for a veteran group that just wants to log in.

**Recommendation:** **Default to Path A for the entire spine.** Spend **exactly one** Path-B install —
**Simple Voice Chat** — and only in the **late arc (Movements III–V)**, where whispered spatial audio
justifies a single one-time ask of veterans. **Avoid the full-modpack tier (custom dimension/shaders client
side, custom models)** unless a specific marquee beat demands it — the custom-dimension fog + the
auto-pushed pack already get you there server-side. Also update `DESIGN.md §4`: **From The Fog's current
2025 release is proprietary, not CC BY-NC-SA** — it stays a design reference either way, but the license
note is wrong.

---

## Appendix — concrete plugin/datapack primitives referenced

`Sign`/`Lectern`/book NBT · `SignChangeEvent` · `PrepareAnvilEvent` · `PlayerInteractEvent` ·
`PlayerDropItemEvent`/`EntityDropItemEvent` + proximity · `PlayerToggleSneakEvent` (single + synchronized) ·
`AsyncChatEvent` · `BlockBreakEvent` + `block.getY()` · `fullTime/24000 % 8` (moon) · scheduled
`player.getLocation()` proximity (never `PlayerMoveEvent`) · `ItemFrame.getRotation()` / comparator dials ·
`Lectern.getPage()` / comparator doors · `MapView`+`MapRenderer` map-art · `BossBar` proximity pull ·
`TextDisplay`/`BlockDisplay`/`ItemDisplay` per-player labels · `Player#hideEntity` / packet NPCs ·
`custom_model_data` + name/lore + `PersistentDataContainer` validation · hidden advancement + custom toast
(`awardCriteria`) · `player.sendBlockChange` / PacketEvents per-player surfaces · Darkness `MobEffect` ·
`player.playSound` + mono `.ogg` ambience · FAWE async paste + relight · Multiverse custom dimension-type ·
async LLM (`runTaskAsynchronously`) → main-thread world write (`runTask`).
