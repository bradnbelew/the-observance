# Backlog Treatment — The Keeper-NPC Framework

> MASTER-PLAN **P1.14**. The presiding Keeper (Citizens2) + per-player prior-keeper
> apparitions (ZNPCsPlus), branching on the dossier; Movement-IV atonement gating; the
> M-IV node that closes **FACT 9 / LORE-BIBLE TODO-3** by naming the logged M-I beat
> from `event_log`. Carries the human-facing voice of **becoming-the-keepers**, the
> **false-law** reveal, and the **Seventh** choice.
>
> Scope of this doc per the task framing: **(a)** reconcile P1.14 against the new web so
> the backlog and the new ideas interlock; **(b)** specify the genuinely-unbuilt
> remainder precisely; **(c)** keep it coherent + de-slopped. "EXPOUND" = *what remains
> to build + how it fuses with the new ideas* — not a re-invention of finished code.

---

## 0. GROUND TRUTH — what already compiles (do NOT re-invent)

A read of the repo before writing. The Keeper framework is **further along than the
backlog line implies** — both plugin halves and the entire text bank exist:

| Asset | State | File / symbol |
|---|---|---|
| **Delivery beat** | **BUILT, self-tested** | `plugin/.../beats/lib/KeeperNpcBeat.java` — `DIRECTED`; reads resolved `lines` + optional `rune_line` (pack-gated) from payload; hardcodes **zero** dialogue; idempotent via `AbstractBeat`. |
| **Interaction listener** | **BUILT, self-tested** | `plugin/.../signal/listener/KeeperNpcListener.java` — `PlayerInteractEntityEvent` + `observance:keeper_npc` PDC tag (NOT the Citizens API — Path-A self-containment); gates on rite-side site (`the_threshold` / `keeper_altar`); writes ONE `event_log` row `type=keeper, context=npc.open` with `{surface,site,node_hint}`. |
| **Full Keeper text bank** | **AUTHORED, de-slopped** | `arc/corpus/npc-and-watcher-voice.md` **SET C** — `keeper.{greet,falseLaw,seventhChoice,becomingKeepers,endings,collectiveRestraint,deadEndTaunt,nameWhere}` (+ skins). Canon register 3. Mimic-checked against FACT 15. |
| **FACT 9 offline-skin door** | **BUILT (pure policy)** | `discord/src/showrunner/offline-skin.ts` — precision-gated worn-skin orchestrator; the M4 named line is an authored key (`offlineSkinNamed_m4`, BN15). |
| **FACT 9 Hold-Book door** | **AUTHORED** | NEW-D19/D19b (`keeper-record.ts` + corpus); the document carrier. |
| **Tables** | **MIGRATED** | `punishment_state` (`custom_key, transgression_count, toll_tier, deciphered, teaching_site_id`), `npc_dialogue_state` (jsonb cursor), `npc_quests`, `event_log`. |

**Therefore the genuinely-unbuilt remainder is one seam:** the **showrunner resolver**
that consumes the `keeper/npc.open` row, reads the dossier + `punishment_state` +
`event_log`, picks the `keeper.<node>[.<skin>]` key, applies the **M-IV atonement
gate**, binds the authored lines, and **enqueues `KeeperNpcBeat`**. Plus the NPC bodies
(placement + PDC tag), the `sites.yml` rite-side rows, and one seed row for the M-IV
fragment-withhold quest. That is what this treatment specifies.

---

## 1. EXPOUND — the full mechanic + story + mystery treatment

### 1.1 The two NPC classes (one presider, six apparitions) and why they differ

There are **two distinct NPC populations**, and conflating them is a bug:

- **The presiding Keeper** — ONE persistent **Citizens2** NPC, body present at exactly
  two rite-side sites: **the Threshold** (surface mouth of the descent) and the
  **Undercroft altar** (M5 rite room). He is **canon register 3** (SET C): second-person
  to *all of you*, lowercase like the Watcher but warmer-by-offer, the only voice
  permitted the half-veiled *we* of the kept dead. He **never** names a player, never
  picks a side, never states FACT 15. He is the *human face of the rite* — greet,
  false-law warning, the Seventh choice, the becoming-instruction, the endings, the
  collective-restraint latch, the dead-end counterweight to Iss.

- **The six prior-keeper apparitions** — **ZNPCsPlus** packet NPCs, **per-player**,
  ephemeral, one bound to each Keeper-Stone, shown ONLY to the player who solved that
  stone's cipher. They cost nothing when nobody is there and never load as world
  entities. Each *characterizes a cipher type* and hands a **rune-key fragment**: Vaun
  the Hoarder (Caesar), Mara the Reader (book-cipher / Vigenère keyword = her name),
  Sella the Drowned (mirror / Atbash), Orin the Silent (banner-glyph, crouch-only),
  Brann the Night-Walker (beacon sequence, night-only), Iss the Liar (final coordinate
  — the *true* one only after you catch the lie). Their hands are the journals'
  `keeperPage*` lines; the apparition is the journal's author standing where you read it.

The differentiator that earns "it knows your name": **every apparition reads the
dossier and the six fates *rhyme* with measured behavior in the current group** — the
Hoarder appears to the high `solo_ratio`+hoard player, the Drowned to the high
`group_distance` wanderer — *without ever naming them*. Collective judgment, neutral
colorant (INV-11/16). A flat player meets the apparition with a flat fate.

### 1.2 How a Keeper open actually plays, end to end (the unbuilt resolver)

The runtime loop, with the unbuilt piece in **bold**:

1. Player right-clicks the tagged Keeper entity at the Threshold/altar →
   `KeeperNpcListener` (BUILT) writes `event_log {type:keeper, context:npc.open,
   detail:{surface,site,node_hint}}`. No world write; reveal-safe.
2. **`keeper.run.ts` (UNBUILT)** — the cron showrunner pass (or an event-driven tail of
   `event_log`) reads the new `npc.open` row, loads the opener's **dossier**
   (`conduct skin` warm/neutral/cold), the **arc flags** (`forged_eighth_found`,
   `seventh_named`, `deep_open`, `iss_caught`, `ending_fate`, `the_unlit_deep`), and the
   opener's **`punishment_state`** rows.
3. **`keeper.ts` (UNBUILT, PURE)** — a deterministic resolver picks the `node` by a
   priority ladder (movement-aware via arc flags, NOT via a player-visible step counter):
   `endings` (if `ending_fate` set) → `becomingKeepers` (near rite, M5) → `seventhChoice`
   (`seventh_named && deep_open`) → **M-IV atonement gate** (see §1.3) → `falseLaw`
   (`forged_eighth_found`) → `deadEndTaunt` (last solve was a `dead_end`) → `greet`
   (default, conduct-skinned). The `node_hint` from the NPC's PDC can pin an entry node
   (e.g. the altar NPC opens `becomingKeepers` directly). The resolver returns
   `{node_key, skin, lines[], rune_line?}` by **looking up the authored SET-C keys** —
   it generates no text.
4. **`apply.ts` (extend)** enqueues `KeeperNpcBeat` with `{node_key, speaker?, lines,
   rune_line?, pack_loaded, line_delay_ticks, color}`. The beat (BUILT) drips the lines
   privately to that player.

The LLM is **not** in this path by default. SET C is fully authored, so the deterministic
key-lookup *is* the spine and *is* the fallback. The one sanctioned scalpel call is the
**single M-IV FACT-9 line** (§1.4), schema-validated, with `offlineSkinWhisperDefer` /
the authored `keeper` line behind it.

### 1.3 The Movement-IV atonement gate (the "withhold until honored" mechanic)

This is the mechanic that makes the keepers *turn*. It reuses `punishment_state`
end-to-end — no new tracking:

- A prior-keeper apparition that, in M-II, gave its fragment freely will in **M-IV**
  **withhold** it from a player whose `punishment_state[that keeper's custom].deciphered
  = false` (they broke the custom and never atoned). The resolver returns a
  **riddle/refusal node** instead of the fragment-grant node, and opens an
  `npc_quests` row `quest_key = atone.<custom_key>` status `active`.
- **Atonement = honoring the broken custom**, detected by the *existing* custom
  listeners (the bow `PlayerToggleSneakEvent`, the offering `PlayerDropItemEvent`, the
  moon taboo, the refrain `AsyncChatEvent`). When the listener logs compliance, the
  customs pipeline flips `punishment_state.deciphered = true` and clears `toll_tier`.
- On the **next** Keeper/apparition open, the gate passes: the resolver returns the
  fragment-grant node, marks `npc_quests` `done`, and the fragment writes to state.
- The withhold is **per-conduct, reversible, never the group-as-a-whole, never an absent
  member** (canon-spine §6 rules 3+4; satisfiability gates on ACTIVE players). The
  escalated `report` that names the conduct ("...has not given back to the deep, not
  once") fires via `LecternFillBeat` + `voice.reportEscalated(name)` — true because
  measured.

The lock is conduct; the key is the fragment; **dialogue is the door**. This satisfies
the consistency law: the mechanic (withhold), the lore (the record stops being passive),
the NPC (the keeper turns), and the clue (the fragment) all move in lockstep.

### 1.4 FACT 9 — closing TODO-3 by naming the logged M-I beat

FACT 9 = "the first hauntings were a specific keeper's fate re-enacted at the group." It
already has **three doors** designed (the two-door web rule is over-satisfied): the
**Hold-Book** re-read (NEW-D19, `keeper-record.ts`), the **offline-skin** named glimpse
(`offline-skin.ts`, BN15), and the **Iss re-walk** (D09→D10). P1.14 adds the **keeper-NPC
dialogue door**, which is the one that reads the plugin's OWN log:

- When the M-IV resolver opens for a player whose M-I haunting is on record, it **reads
  `event_log`** for that group's earliest `type=haunt` row (the M-I beat — e.g. a
  `NamedMobBeat`/`PrivateDarknessBeat` fire), extracts which **apparition shape** it was,
  and binds the authored `keeper`/`offlineSkinNamed_m4` line that connects that shape to
  the named keeper whose fate it re-enacted. The line is **spoken once**, human-approved
  (CONFIRM-gated), with `offlineSkinWhisperDefer` ("there is a thing at the edge i will
  not name yet. ask again when it has come closer.") as the deterministic fallback if not
  approved. This is the dialogue door; the document doors carry the same fact in writing.
- The naming is **precision-gated like the offline skin**: FACT 9 is spoken only against
  a haunting the `event_log` *actually recorded* for this group. No log row → the line is
  never spoken (a wrong "your dread was Brann" is worse than silence).

### 1.5 The false-law and Seventh nodes (already-authored, wired here)

- **`keeper.falseLaw`** opens when `arc_state.flags.forged_eighth_found` is set: he warns
  *of* the forged eighth custom (the Covering, F7b) — "count them yourself… keep the six.
  let the seventh-and-a-half lie where it was hung." He never credits its forger.
- **`keeper.seventhChoice.{offer,restored,erased}`** opens when `seventh_named &&
  deep_open` (after the Seventh-Stone expedition). He lays the **restore/erase** choice
  down without weighting it — "neither opens the road. both are kept." This **colors,
  never gates** (INV-12). The restore branch pairs with the `fateInheritorsCodicil` +1
  codicil (FACT 14).
- **`keeper.becomingKeepers`** opens near the M5 rite (D12 register) — the
  summons-instruction, the corpus's nearest approach to FACT 15, **stopped before** it
  names induction ("we would keep you, if you would keep the ways.").
- **`keeper.endings.<fate>`** is read AFTER the bow — one tinted clause mirroring the
  composer's base fate; collective, no name, no side.

### 1.6 Arc placement across the ~2-week / 5-movement run

- **M-I (Notice):** the Keeper is **not yet present** (the body sleeps). The offline-skin
  *plant* and the first haunting are logged to `event_log` here — the seed FACT 9 reads.
- **M-II (Keeper-Stones):** the **six apparitions** appear per-player at the stones,
  freely granting fragments; Iss lies. The presiding Keeper may give a single
  half-veiled `greet.neutral` at the Threshold (nothing measured yet).
- **M-III (Undercroft):** the Keeper presides at the altar in `greet` skin; the room
  rebuilds wrong (`some-laws-are-lies` false-climax) — the `falseLaw` door may open if
  the forged eighth was found. The deniable offline-skin glimpse fires.
- **M-IV (Reckoning):** the **turn**. Apparitions withhold (§1.3); the **FACT-9 named
  line** fires once (§1.4); the Iss re-walk re-reads his tree cold. `keeper.greet.cold`
  for broken groups.
- **M-V (Accepting):** `becomingKeepers` instruction, then the rite, then
  `endings.<fate>`. Collective, active-only.

---

## 2. CRITIQUE — adversarial, honest

**R-1 (P0, the real one) — orphaned bodies.** The text bank, the beat, and the listener
exist; the **resolver that connects them does not**. If P1.14 ships the Citizens2/ZNPCsPlus
NPCs without `keeper.ts`/`keeper.run.ts`, a player right-clicks the Keeper and **nothing
happens** (a row in `event_log`, no spoken line). That is the single sharpest risk:
**a mute Keeper on camera is worse than no Keeper.**
*Mitigation:* ship the resolver as the **first** unit, behind a deterministic
`greet.<skin>` default so the Keeper **always** speaks at least the conduct-skinned
greet even before the branch ladder is filled. A self-test (`keeper.selftest.ts`) asserts
every reachable arc-flag combination resolves to a non-empty `lines[]` — a mute branch is
a build failure, not a live discovery.

**R-2 (P1) — the atonement gate can soft-lock a fragment (Path-A / no-punish-absent
law).** If a fragment is withheld and the broken custom's teaching site is far/unclear,
a player can be stranded; worse, if the gate ever reads a *group* custom it could punish
an absent member. *Mitigation:* the gate keys on **per-player** `punishment_state` only
(the schema is `(player_id, custom_key)` — structurally per-player); `teaching_site_id`
on the row makes the atonement loop **fair** (the riddle node points the player at the
exact site that teaches the custom); the Whisper backstop remains the player-controlled
rail (INV-15 — the gate must NOT touch `whisper_budgets`, asserted by a grep self-test
mirroring `reckoning.ts`). And: **withholding a fragment never blocks the spine** — the
oracle resolver ignores `movement`, so M-III/V doors open from *other* in-roads; the
withheld fragment is a *warmth* toll (a slower path), not a wall.

**R-3 (P1) — FACT 9 mis-naming (precision law).** If the resolver names a keeper-fate for
a haunting the group never actually suffered, the whole "it knows you" illusion inverts
into "it's guessing." *Mitigation:* bind the FACT-9 line **only** to an `event_log`
`haunt` row that exists for this group, and only via the offline-skin precision gate
(measured shape-rhyme ≥ floor + margin). No row, no rhyme → `offlineSkinWhisperDefer`,
never a fabricated biography.

**R-4 (P2) — two "reckonings" collide in the namespace.** `reckoning.ts` already exists
and is the **difficulty** engine (A10), NOT the M-IV turn. Naming the M-IV module
`reckoning.ts` would clobber it. *Mitigation:* the M-IV turn logic lives in
**`keeper.ts`** (resolver) + the existing customs pipeline (atonement detection) +
`apply.ts` (escalated reports). Do **not** add a second `reckoning.ts`.

**R-5 (P2) — Citizens2/ZNPCsPlus as a hard dep breaks Path-A self-containment.** The
listener already dodges this (PDC tag, not the Citizens API) — but the **resolver** must
not assume an NPC framework either. *Mitigation:* the resolver is Discord-side TS reading
`event_log`; it never imports Citizens. If neither framework is installed, no tagged
entity exists, the listener is inert, and the resolver simply never sees an open. The
*placement* of the NPC (one admin step) is the only framework touchpoint, and it degrades
to a vanilla armor-stand with the PDC tag if Citizens is absent (the Keeper still speaks;
he just doesn't gesture).

**R-6 (camera) — a wall of dripped chat reads as a monologue dump.** SET C nodes are
multi-sentence. *Mitigation:* the beat already drips one line per `line_delay_ticks`
(~1.75s); split each node's authored paragraph into ≤3 short lines in the seed so the
cadence reads as speech, and never open two nodes in one window.

**CUT / SCALE call:** **Keep — scaled.** Keep the full Keeper (presider) and the M-IV
atonement gate — they are the spine of the Reckoning and the human voice of the sealed
turn. **Scale the six apparitions to authored, deterministic, fragment-granting glimpses
only** (no LLM, no walking, no pathfinding — they speak their journal hand and hand the
fragment, then despawn). The "Liar re-reads cold" surprise is delivered by the **resolver
swapping Iss's node text** on `iss_caught`, not by any live AI. The optional ZNPCsPlus
*per-player skin realism* is P2 polish; a `TextDisplay` + private sound is an acceptable
Path-A fallback if ZNPCsPlus drifts.

---

## 3. DE-SLOP TEST — exemplar lines in-voice (proof it's authorable cold)

These are NEW lines for the **unbuilt** nodes (the riddle/refusal of the M-IV gate and the
resolver's fallback), written to the SET-C contract: lowercase, second-person to the
group, half-veiled *we*, no named feeling, no bow, no three-adjective list, no "not just
X but Y", no exclamation. They count and record; they do not threaten.

**`keeper.atone.withheld`** *(M-IV — a fragment refused until the broken custom is honored;
names the conduct, never the player):*
> `the one whose mark you wear coming down kept the offering. you did not, the once it was asked. she will hand you nothing until the deep has had its first ore back from you. go up. give it. come down. the hand opens to a hand that gave.`

**`keeper.atone.cleared`** *(after the custom is honored — flat acknowledgement, no praise):*
> `you gave it back. she felt the weight leave the seam. the fragment is yours now. it was always yours; it was only held.`

**`keeper.fact9.named`** *(M-IV, spoken ONCE, human-approved — binds the logged M-I haunt to
the keeper it re-enacted; the iceberg: it does not say why):*
> `the cold that stood in your doorway the first week wore a walking that stopped at night. brann walked it first. the land kept the walking after him and set it at your door to see if you would learn the hour. you did not, then. you are learning it now.`

**`keeper.greet.deniable`** *(the deterministic resolver fallback before the branch ladder
fills — always non-empty, so the Keeper is never mute):*
> `you came to the stone. i am here, the way i am always here. ask the rite when you have a thing to ask it. the record keeps the asking and the not-asking the same.`

Each passes the mimic check: the FACT-9 line **stops** before stating induction; no line
adds a tidy lesson; the warmth in `atone.cleared` is in *what is returned*, not adjectives.

---

## 4. THREAD IT (the consistency law) — exactly where this lives

**Canon / FACTs it carries or touches** (it *adds* none — it is the **dialogue carrier**
of facts already canon, which is the point):
- **FACT 9** (M-I haunting = a keeper's fate re-enacted) — P1.14 is its **dialogue door**;
  closes **LORE-BIBLE TODO-3 / R11**. (Other doors: NEW-D19 Hold-Book, D08 offline-skin.)
- **FACT 7b** (the forged eighth / Covering) — `keeper.falseLaw`.
- **FACT 10b** (acceptance is a choice the land makes; it can refuse) — `keeper.seventhChoice`.
- **FACT 13 / 14** (the rite receives; the +1 codicil) — `keeper.becomingKeepers`,
  `fateInheritorsCodicil`.
- **FACT 16** (filed by place, not by foot) — `keeper.nameWhere` (the half-veiled M4 twin).
- **FACT 15** (induction) — **NEVER stated**; `becomingKeepers` is the felt door only.
- **INV-1** (no story in the engine), **INV-11/16** (neutral colorant, never names a player
  or a side), **INV-12** (no fork gates a spine puzzle), **INV-15** (the gate never touches
  Whispers).

**Found-documents / journals that foreshadow or pay this off:**
- The six **`keeperPage*`** journal hands (`journals-vaun-mara-sella.md`,
  `journals-orin-brann-iss.md`) ARE the apparitions' voices — the apparition is the journal
  author standing at the stone.
- **NEW-D19 Hold-Book** (`keeper-record.ts`) — the FACT-9 document twin of the dialogue.
- **D08** (`do-not-close-your-eyes-here`, Brann) — the offline-skin / FACT-9 second door.
- **D11 / NEW-D13** — the Seventh, gating `keeper.seventhChoice`.
- **D09→D10** — the Iss lie + refutation, driving the M-IV node-text swap on `iss_caught`.

**NPC / Watcher voice lines that carry it:** the whole of **SET C** (`keeper.*`), plus the
new `keeper.atone.{withheld,cleared}`, `keeper.fact9.named`, `keeper.greet.deniable` above.
Cross-surface truth: every Keeper node has a non-contradicting Watcher key (Set B) and the
offline-skin's `voice.reportEscalated` carries the M-IV report.

**Ciphers / puzzles it expresses** (reusing the 11 built transforms — the apparitions
*characterize* a cipher each, so the framework is the human face of the cipher web, not a
new puzzle layer):
- **caesar** (Vaun / Stone 1, rotate item-frames), **bookCipher** + **vigenere** (Mara /
  Stone 2, lectern shelf, keyword = her name), **atbash** (Sella / Stone 3, water
  reflection), **substitution** (Orin / Stone 4, banner-glyph wall), a **beacon colour
  sequence** (Brann / Stone 5), **coordEncode** (Iss / Stone 6 → the final coordinate
  fragment, the *true* one only after the lie is caught). The presiding Keeper's
  `rune_line` codas are rendered in the shared rune font (`forge/runes.ts`) so a glyph
  learned at a stone reads in his mouth too.

**Beats / listeners / tables / seeds / sites / voice keys it realizes:**
- **Beats:** `KeeperNpcBeat` (BUILT, delivery), `NamedMobBeat`/`PrivateDarknessBeat` (M-I
  haunt that FACT 9 reads back), `LecternFillBeat` (M-IV escalated report).
- **Listener:** `KeeperNpcListener` (BUILT) → `event_log {keeper, npc.open}`.
- **Tables:** `punishment_state` (the atonement gate's lock), `npc_quests`
  (`atone.<custom_key>`), `npc_dialogue_state` (cursor for repeat opens), `event_log`
  (the FACT-9 read + the open signal), `arc_state.flags`.
- **Showrunner (UNBUILT — the remainder):** **`discord/src/showrunner/keeper.ts`** (PURE
  resolver: dossier+flags+punishment → `{node_key, skin, lines, rune_line?}`),
  **`keeper.run.ts`** (the DB/clock/event_log wrapper firing `KeeperNpcBeat` via
  `apply.ts`), **`keeper.selftest.ts`** (every flag combo resolves non-empty; grep guard
  that `whisper_budgets` is never read).
- **sites.yml:** add `the_threshold` (surface) + `keeper_altar` (Undercroft) rite-side
  rows the listener already looks for (`placedOfType`).
- **Seed:** one `npc_quests`/`punishment_state`-fed `atone.<custom_key>` flow per gated
  keeper; the SET-C lines + the four new keys land in the `keeper.*` voice registry
  (the human twin of `voice.ts`, under the build guards, never inlined).
- **NPC placement:** one Citizens2 Keeper at each rite site, PDC-tagged `observance:keeper_npc`
  (value = optional `node_hint`); six ZNPCsPlus apparitions bound per stone, per-player.

---

## 5. PLANT THE PAYOFF — the "OH, that is what that was for" seed

**Plant (M-I, inert/ambiguous):** the **first haunting** the group suffers — a
`NamedMobBeat`/`PrivateDarknessBeat` glimpse in week one — is logged to `event_log` as a
neutral `haunt` row with its **apparition shape**. At the time it reads as generic dread:
*the server is creepy.* No keeper is named; no meaning is offered. (Parallel inert plant:
the M-I **offline-player report**, BN15 — "the record noted it anyway.")

**Payoff (M-IV, `keeper.fact9.named`):** the presiding Keeper, reading that exact logged
row, names the shape's keeper — *"the cold that stood in your doorway the first week wore a
walking that stopped at night. brann walked it first."* The week-one dread retroactively
acquires a **biography**: it was never random; it was a specific dead keeper's fate
**rehearsed at you**, and the land was already grading whether you'd learn the hour. The
plant is the raw `event_log` row; the payoff is the resolver reading its own server's
history aloud. **No payoff without the plant** — if no M-I haunt row exists for this group,
the line is never spoken (precision gate, R-3). This *is* the closure of FACT 9 / TODO-3.

Secondary plant/payoff already in the web: **Iss's M-II warm testimony** (apparition gives
the coordinate, plainly, trusted) → **M-IV `iss_caught` node-text swap** (the resolver
re-binds his whole tree cold; "true, and shut" — `keeper.deadEndTaunt` is the honest
counterweight). The plant is a friendly fragment; the payoff is the same fragment re-read
as a lie that cost the group a re-walk.

---

## 6. MOVEMENT PLACEMENT + DEPENDENCIES + PRIORITY

| Unit | Movement | Depends on | Depended on by | Priority |
|---|---|---|---|---|
| `keeper.ts` resolver (greet default first) | M-II → M-V | dossier feed, `arc_state.flags`, SET-C seed | every Keeper open (mute-Keeper fix) | **P0** |
| NPC placement + PDC tag (Threshold/altar + 6 stones) | M-II | Citizens2/ZNPCsPlus, `sites.yml` rows | the listener firing at all | **P0** |
| `sites.yml` `the_threshold` + `keeper_altar` rows | M-II | FAWE set-pieces | listener `nearestRiteSite` | **P0** |
| M-IV atonement gate (`atone.*` quest + withhold node) | M-IV | `punishment_state`, custom listeners (BUILT), `keeper.ts` | the Reckoning beat; M-V "earned rite" | **P1** |
| FACT-9 `keeper.fact9.named` (reads `event_log`) | M-IV | M-I haunt logged, offline-skin precision gate | closes TODO-3 / R11 | **P1** |
| `falseLaw` / `seventhChoice` / `becomingKeepers` wiring | M-III/V | `forged_eighth_found` / `seventh_named` / rite flags | the endings | **P1** |
| ZNPCsPlus per-player skin realism (vs `TextDisplay` fallback) | M-II | ZNPCsPlus stable on 1.21 | — (polish) | **P2** |
| `endings.<fate>` post-bow line | M-V | M5 composer `ending_fate` | the camera close | **P1** |

**Build order:** resolver-with-greet-default (P0, kills the mute-Keeper risk) → NPC
placement + sites rows (P0) → atonement gate + FACT-9 read (P1, the Reckoning) → the
flag-gated nodes (P1) → skin polish (P2).
