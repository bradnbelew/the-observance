# PLAYTHROUGH — MOVEMENT V (The Accepting) + the DIVERGENT ENDINGS

> **Director's continuity shooting-script. Literal "what is there," in strict causal order.**
> Every element is quoted from the REAL repo artifact (file + line where load-bearing). `[GAP — TO BUILD]`
> marks an artifact named by canon/seed but NOT yet authored on disk. Nothing is invented as built.
> Spoiler-bearing. Sources: `design/WEB-MASTER.md` §1.M5/§5/§8, `arc/lore/canon-spine.md` §3b/§5/§8.4,
> `discord/src/voice.ts`, `discord/src/showrunner/{fate,grave,forks,keeper-record}.ts`,
> `plugin/src/main/java/.../AcceptingRiteListener.java`, `plugin/src/main/resources/{config,sites}.yml`,
> `discord/supabase/seeds/{puzzles,seventh,progression,metapuzzle}_seed.sql`,
> `arc/corpus/npc-and-watcher-voice.md` SET C, `design/content/npc-dialogue.md` §7,
> `arc/lore/documents/{bring-the-thing-only-you-can-give,six-were-kept-before-you,the-name-i-cut-myself}.md`,
> `design/structures.md`.
>
> **Continuity from `5-movement-4-end.md`:** that file closes on the catch (`iss_caught`) firing the
> `requires_flags` activation cascade and opening the back half. THIS file opens on the single Iss IV→V
> chain's last legs (already armed by the catch) and runs them to the bow, the world flip, and the five
> divergent closes. The catch's lingering `next_puzzle_key: rite-tokens` shortcut [GAP — see §0] is
> inherited from the prior file unchanged.

---

## 0. THE STATE THIS SEGMENT OPENS ON (the gate into M-V)

M-V is the collective rite (WEB-MASTER §1.M5). It is reached ONLY through the single Iss chain (coherence
P1-1) — the catch does NOT hand it. The chain, in causal order, is:

> **catch (`iss_caught`)** → **`bound-word`** → **`m4-three-hands`** (`threshold_open`) →
> **`threshold-coordinate`** (`true_coord_known`) → **`true-walk-arrive`** (`true_destination_reached`) →
> **`rite-tokens`** (`tokens_laid`) → **`accepting-crouch`** (`bowed_as_one`) → **`record-receives`**
> (`record_received`, `world_kept`).

**The two doors to the bow (WEB-MASTER §2 GATE "Accepting"):** (A) the true walk delivers the group to
`rite-tokens` → `accepting-crouch`; (B) `pressure-glyph-walk` (PROMOTED, `puzzles_seed.sql` L478–490) is a
genuine second in-road to `accepting-crouch` (`next_puzzle_key: accepting-crouch`) that GATES NOTHING on the
spine — "the do-not-read-your-way-out door."

**INHERITED GAP (from `5-movement-4-end.md` §0, still on disk):** `no-wall-catch` carries
`next_puzzle_key: rite-tokens` at `puzzles_seed.sql` L294. WEB-MASTER §2.1 point 2 BANS this (the catch must
set `iss_caught` and STOP; the rite is reached only through the chain). **[GAP — TO BUILD]:** repoint/remove
L294 so the F1 monorail is killed. As shipped, the catch both opens the chain (via `requires_flags`) AND
shortcuts to the rite.

**DIRECTOR posture in M-V:** the climax is player-PERFORMED, not advanced from the console. The director's
only finale jobs are the **approval gate** on Oracle beats (`status='approved'`), binding the **single
Accepting instant** in `arc_state` (§0.4 below), and — if filming — arming the `event-window` /
`/observance window on` so the time-gated curatorial surfaces (grave open, Record re-render, summons) land in
a known take (`config.yml` L262–275).

### 0.4 The single Accepting instant (canon-spine §8.2, owned by the showrunner)

ONE instant, shared by THREE surfaces — the future-dated grave's carved date (FACT 13b), the Record website
timestamp, and the summons `not_before`. **No config owns it** (`config.yml` L264–266, L274–275:
*"The Accepting INSTANT itself is bound in arc_state by the showrunner (NO config owns it)"*). `grave.ts`
INJECTS it (`acceptingInstantMs`, L39). **[GAP — TO BUILD]:** the `arc_state` binding writer + the
`grave.run.ts` / `fate.run.ts` I/O wrappers (`grave.ts` L23–24 names `grave.run.ts` as the I/O owner; it is
not on disk — only the pure `grave.ts` policy is). DIRECTOR ACTION: bind the instant before the rite window.

---

## 1. THE FINALE ASSEMBLY — gather the named components, wake the Keeper (the on-ramp)

### 1.1 The true walk arrives — `true-walk-arrive` (`puzzles_seed.sql` L868–896)

- WHAT IT IS: a `main_beat` row. Title *"the road kept its word."* The group has followed the Threshold's
  true coordinate (a navigation pointer, INV-14) and reads the WORD off the destination carving (leaves
  placed at the on-site tableau). Accepted answers (verbatim): `'kept here before you'`,
  `'the road kept its word'`, `'we were already filed here'`. The answer is the on-site WORD, **never the
  signed coordinate** (INV-14).
- EFFECT (verbatim payload): `voice_key: oracleMainBeat`; `set_flags {true_destination_reached: true}`;
  `next_puzzle_key: rite-tokens`; beat `unlock` → `site_id: the_threshold`, `step: reveal`,
  `step_payload {fragment: 'destination_leaves_read'}`, priority 13.
- WATCHER LINE (`voice.ts` `oracleMainBeat`, L284): *"what was shut is shut no longer. the record keeps the
  hand that opened it."*
- WHERE / GATING: movement 4, `active: false`, `requires_flags {true_coord_known: true}`
  (`metapuzzle_seed.sql` L123). The PrivateSound/ParticleBeat per-presence arrival fires in-world.
- DIRECTOR ACTION: approve the arrival beat (Oracle path).

### 1.2 The components are already paid; the altar still lacks — `rite-tokens` (`puzzles_seed.sql` L445–470)

- WHAT IT IS: the M-V on-ramp `main_beat`. Title *"bring the thing only you can give."* The group lays one
  PERSONAL token in each of six slots, plus the named components. Accepted answers (verbatim):
  `'bring the thing only you can give'`, `'deeps first heart unbroken light salt of the keepers'`,
  `'a piece you cannot read your way out of'`. FACT 13 (the missing tool is YOU).
- EFFECT (verbatim payload): `voice_key: oracleMainBeat`; `set_flags {tokens_laid: true}`;
  `next_puzzle_key: accepting-crouch`; beat `unlock` → `site_id: unbroken_light`, `step: reveal`,
  `step_payload {slots: 6, lit: true}`, priority 10.
- WHERE / GATING: movement 5, `active: true`. Reached via `true-walk-arrive` OR via `pressure-glyph-walk`.
- THE LORE THIS RENDERS — `bring-the-thing-only-you-can-give.md` (the Keeper's letter at the altar, FACT 13,
  movement 5). Load-bearing verbatim (the rite instruction the group reads at `unbroken_light`):
  > *"bring, then, the thing only you can give. one token for each who kept the ways before you — **six**, a
  > hand and one more. not coin, not ore, nothing of worth that another could weigh. a thing that is *yours*
  > … lay one in each marked place."* (L31–33)
  > *"the record does not close when the rite is done. … It **receives**. … they did not depart, you have
  > read that, *they were kept* …"* (L39)
  > *"lay the tokens. wake me fully. and when all of you bow as one, at the hour, in this light — we would
  > keep you, if you would keep the ways."* (L43–45)
  > *[lower margin, smaller:] "do not grieve the giving. you are not losing it. you are leaving it where it
  > will be found."* (L51)
- DIRECTOR ACTION: approve the `reveal` (6 slots lit) beat.

### 1.3 Wake the Keeper — the `becomingKeepers` dialogue (the M5 on-ramp)

- WHAT IT IS: the presiding Keeper's rite instruction. `KeeperNpcBeat` opens on `NPCRightClickEvent` at
  `keeper_altar` (sites.yml L296–304, type `keeper_altar`) or `the_threshold` (the only two presiding types,
  per `KeeperNpcListener`). Node `becomingKeepers`; branch on conduct skin (warm/neutral/cold, read off the
  SAME dossier as `greet`, `npc-dialogue.md` §7.0/§7.1 L468–470). `keeper.ts` is the pure resolver; text is
  corpus SET C verbatim. No English in the Java beat.
- WHO / GATING: near the rite (M5 on-ramp) + conduct skin (`npc-dialogue.md` L436). Branch precedence
  `keeper.ts resolveKeeperDialogue`.
- THE VERBATIM TEXT (`arc/corpus/npc-and-watcher-voice.md` C4, L647–654):
  - **`keeper.becomingKeepers.neutral`** (L648): *"the altar wants a thing only each of you can give. not a
    stone, not a light. the thing the record kept open a column for, against your name, before you came.
    bring it at the dark hour. the rite does not reward. it receives. it keeps. the ones before brought
    theirs, and were received, and are kept — you have read where they are kept; you are reading it now, in
    the same book, in the same hand. we would keep you, if you would keep the ways."*
  - **`keeper.becomingKeepers.warm`** (L651): same spine, opening *"you kept the ways coming to this. the
    altar knows a kept hand when one is laid on it. …"* closes *"we would keep you. you have made the keeping
    easy."*
  - **`keeper.becomingKeepers.cold`** (L654): same spine, opening *"you broke a way or two coming to this.
    the altar takes a broken hand too — it took mine, late. …"* closes *"… we would keep you, if you would
    keep the rest of the road."*
- THE HARD MIMIC CHECK (C4 mimic note, L656–660): every variant STOPS at the half-veil ("we would keep you…"
  / "in the same hand you have been reading") and does NOT add the induction sentence. A draft that finishes
  ("and so you become the watching") is a defect (canon-spine §6.2, npc-dialogue §7.3). The cold variant must
  NOT scold or gate — *"the rite does not refuse for what is already done."*
- DIRECTOR ACTION: none (player-driven right-click); confirm the dossier-skin reads the intended warm/cold.

### 1.4 The summons (the call to gather, at the dark hour)

- WHAT IT IS: the Act-3 call. `voice.ts` `summons()` (L187): *"the way is open. come — all of you — at the
  dark hour. bring what is owed."*
- WHERE / GATING: pinned to the group; honors the `event-window` `not_before` == the single Accepting
  instant (§0.4). One register, collective, names no one.
- DIRECTOR ACTION: ensure the summons `not_before` shares the bound instant (§0.4); if filming, force the
  window (`/observance window on`).

---

## 2. THE FINALE SITE — `unbroken_light` (the Undercroft / Accepting floor)

- WHAT IT IS (sites.yml L130–138): `unbroken_light`, `type: accepting_floor`, world `world`, `radius: 10`,
  `protect: true`, `enabled: true`, coords `null` (UNPLACED until go-live build). The
  `AcceptingRiteListener` watches THIS type for the synchronized group bow.
- BUILD PALETTE / FOOTPRINT (`structures.md` L45–47, site 8 "the deep line"): *"the one fire that never went
  out, centered. A room sized for **6–8 players to gather and bow together** (the Accepting quorum). The
  lectern-comparator **door** that `undercroft-descent` opens … The **pressure-glyph rune walked on the
  floor** (`pressure-glyph-walk`)."* Fog: a datapack dimension/biome (`ambient_light: 0`), NOT the resource
  pack (`structures.md` L47).
- WHAT IT CARRIES: the six lit token-slots (from `rite-tokens` reveal), the central never-doused fire (FACT
  11), the floor rune for `pressure-glyph-walk`, and the persistent FATE DRESSING applied at the close (§5:
  marker facing, floor-light geometry — sites.yml gives no fate dressing fields; this is [GAP — TO BUILD]
  builder dressing, see §5).
- CO-LOCATED but DISTINCT: `keeper_altar` (sites.yml L296–304, `radius: 5`) sits with `unbroken_light` but is
  a tight separate site so the Keeper isn't summonable from the whole wide floor.

---

## 3. THE GROUP-BOW → OPAQUE SENTINEL → TERMINAL BEAT

### 3.1 The detector — `AcceptingRiteListener.java` (UNCHANGED, the spine)

- WHAT IT IS: the terminal group-rite detector. On `PlayerToggleSneakEvent` (the moment a bow BEGINS,
  L141), inside an `accepting_floor` site's radius (L152–153), with EVERY present player on the floor
  sneaking at once (L162–165), AND the cross-surface readiness gate open (L170), it posts the OPAQUE wordless
  token to the oracle (L180–181). Fires ONCE per cooldown per site (L173).
- THE TRIGGER → EFFECT (literal): synchronized group crouch on `unbroken_light` → `oracle.resolveWorld(mc,
  name, token, "accepting-crouch")` → matches the `accepting-crouch` seed row → the climax beat.
- ACTIVE-ONLY QUORUM (INV-19, L160, L193–203): `effectiveQuorum = min(configQuorum, activeRosterSize)`,
  floored at 1. `clampQuorum` (L199–203) lowers the bar for a smaller-than-cast active group (never blocks an
  absent member), never raises it above the cast. Self-test `quorumClampSelfTest` (L264–272) pins:
  `clampQuorum(4,4)=4`, `(4,2)=2`, `(4,9)=4`, `(4,0)=1`, `(0,3)=1`, `(1,5)=1`.
- ACTIVE-ROSTER SOURCE (L205–217): the wired `activeRosterSize` IntSupplier (the showrunner's
  `readActiveRoster(windowMs)` surfaced into the plugin) OR, unwired/failed, `Bukkit.getOnlinePlayers().size()`
  (the active set at the instant of the bow). **[GAP — TO BUILD]:** the wiring of `readActiveRoster` into the
  plugin supplier (the single owner is `backlog-full-showrunner`, WEB-MASTER §0.5); unwired today → falls back
  to online count (source-compatible, never blocks).
- READY GATE (L219–225, fail-CLOSED): the Threshold must already be open (`threshold_open`, set by the
  three-hands coop gate). A null/unknown answer reads as NOT ready — the finale never fires early. **[GAP —
  TO BUILD]:** the `readyGate` Supplier wiring (unwired today → legacy always-ready, L222).
- WHAT IT IS NOT: fate-NEUTRAL (L61–65). The detector posts ONE token; WHICH close composes is decided
  downstream by the M5 composer, never here. It never branches the ending, never mutates the world, never
  messages players.

### 3.2 The opaque token + config — `config.yml` rites.accepting (L155–168)

- WHAT IT IS (verbatim): `enabled: true`; `token: "k7q2m9 x4r8p3 w1n6z5 t0j4h2 b8f1v7 c3d6s9"`;
  `puzzle-key: "accepting-crouch"`; `quorum: 6`; `cooldown-seconds: 300`.
- THE LAW: the token MUST byte-match the seed row's accepted_answers — `riteTokenSelfTest` enforces it at
  build time so the climax can never silently fail (L158–159). The default `quorum: 6` is the full cast so
  two stragglers can't fire the finale (L164–167); INV-19 clamps it down to the active set, never up.

### 3.3 The terminal rite row — `accepting-crouch` (`puzzles_seed.sql` L499–520)

- WHAT IT IS: a `main_beat` row, the TERMINAL rite, DETECTED in-world ONLY — never typeable. Accepted
  answers is the SINGLE opaque, wordless, high-entropy token (L502): `'k7q2m9 x4r8p3 w1n6z5 t0j4h2 b8f1v7
  c3d6s9'` (byte-matches `config.yml` token; no human-readable phrase opens the climax, red-team B-5,
  no-leaked-sentinel self-test).
- EFFECT (verbatim payload): `voice_key: oracleMainBeat`; `set_flags {bowed_as_one: true}`;
  `next_puzzle_key: record-receives`; beat `unlock` → `site_id: unbroken_light`, `step: door_open`,
  `step_payload {open: true}`, priority 8.
- WATCHER LINE (`voice.ts` `oracleMainBeat`, L284): *"what was shut is shut no longer. the record keeps the
  hand that opened it."*
- WHERE / GATING: movement 5, `active: true`. (Per §3.1 the listener's `readyGate` is the runtime
  precondition that `threshold_open` is set, so a sub-chain group can't fire it early even though the row
  is `active: true`.)

### 3.4 The world flips to KEPT — `record-receives` (`puzzles_seed.sql` L525–544)

- WHAT IT IS: the world's response. A `main_beat` row, opaque sentinel. Accepted answers is the single
  opaque token (L528): `'p2w8k4 m9x1r6 z5t3j7 h2b4f8 v1c6d3 s9q7n0'`. FACT 14 — the record RECEIVES. The door
  to FACT 15 (felt, never stated by any node).
- EFFECT (verbatim payload): `voice_key: oracleMainBeat`; `set_flags {record_received: true, world_kept:
  true}`; beat `unlock` → `step: advancement_toast`, `step_payload {key:
  'observance:the_record_receives_you'}`, priority 30.
- THE HIDDEN ADVANCEMENT TOAST (FACT 14, canon-spine L204–208): *"⟡ the record receives you."* (the toast
  key `observance:the_record_receives_you`). **[GAP — TO BUILD]:** the advancement definition datapack JSON
  for that key (named by the beat payload; not verified on disk in this pass).
- WHERE / GATING: movement 5, `active: false` — STAGED for M5 (flips on the bow → `next_puzzle_key:
  record-receives` from `accepting-crouch`). No oracle gate normally (the bow's `next_puzzle_key` carries it).
- DIRECTOR ACTION: approve the `advancement_toast` beat (priority 30).

---

## 4. THE PAYOFFS THAT LAND AT V (the "oh, that is what that was for")

### 4.1 The future-dated grave opens from the inside (FACT 13b, ledger #21)

- THE PLANT (M2, `puzzles_seed.sql` near L380 / `grave.ts` carve row): the headstone is cut with a living
  ACTIVE name + a FUTURE date (read as a death clock — the misread IS the mechanic). `voice.ts`
  `graveCarved(name)` (L525–527): *"the stone for the one called {name} is cut. it carries a date that has
  not come. the stone is cut before the keeper is kept."*
- THE PAYOFF (V): on the date == the single Accepting instant (§0.4), the grave OPENS FROM THE INSIDE.
  `grave.ts decideGrave` emits the `open` row when `carved && !opened && nowMs >= acceptingInstantMs`
  (L84–101), voiceKey `graveOpened`. `voice.ts` `graveOpened(name)` (L529–531): *"the stone for the one
  called {name} is opened from the inside. the date was not a death. it was an appointment. the hole is the
  deposit slot."* The mound becomes the deposit slot (FACT 13). `KEPT — NOT YET → KEPT`.
- THE PRIVATE RECEIPT (`voice.ts` `graveReceipt`, L533–534, handed to the one read first): *"the one called
  {name}. read first. cut first. the rest are not yet cut. they will be."*
- SITE: `the_threshold` by default; the OPTIONAL `grave_spur` (sites.yml L384–392, `type: grave`,
  `enabled: false`) lets a builder sit the grave just off the threshold line. The producer falls back to
  `the_threshold` until the spur is flipped.
- GROUNDING: names a real resolvable ACTIVE player only — a nameless/inactive subject → NO grave
  (`grave.ts` L104–107). INV-14: the date is READ, never typed.
- **[GAP — TO BUILD]:** `grave.run.ts` (the I/O: pick the active name, read the instant, fire
  SignWriteBeat/LecternFillBeat, persist marks — named at `grave.ts` L23–24, not on disk). The pure policy
  `grave.ts` IS built.

### 4.2 The Hold-Book's last page rewrites (FACT 14, WEB-MASTER §4)

- WHAT IT IS: the Hold-Book's final-page rewrite to the record's flat closing hand. `voice.ts` `docketEven()`
  (L434–436): *"the present hands are entered. the book is even. the same book, the same hand, as all the
  ways above you."* The group is in the SAME book as everyone above (FACT 15 felt, never stated).
- WHERE: `stone_of_reckoning`'s companion lectern (the ONE Hold-Book anchor, WEB-MASTER §0.4). Produced by
  `keeper-record.ts` (the deterministic page-body chooser behind the optional LLM scalpel).
- THE M4 RE-READ THAT PRECEDES IT (the bait paid off) — `base-docket-reread` (`puzzles_seed.sql` L596–611,
  voice_key `docketReread`): *"the muster is read. the count was never of the dark. it was of the hands. the
  hands are almost in."* (the chiasmus is CUT, slop A3). The down-count was a roll-call, not a doom-clock.

### 4.3 The cursed-map struck-seventh row fills (felt, never stated — ledger #24/#26)

- THE PLANT (M0-remote, the lure page `/record/the-record-keeps`): a static authored `kept: 6` counter with
  a struck row beneath it, dead uploader `m.kept` (Mara's hand). `six-were-kept-before-you.md` is the canon
  home (L26–59).
- THE PAYOFF (V): `/record` adds the group's own names via `recordReceives()`. `voice.ts` `recordReceives()`
  (L544–546): *"the record receives the present hands. they are entered in the other place too, against the
  loss of this one."* The struck seventh row is, structurally, now the present hands — kept. The `6`
  resolves: six prior groups kept the same way; the group was the seventh the record would not keep AS A FILE
  — it keeps them as HANDS (FACT 14/15).
- THE VERBATIM PAYOFF TEXT (`six-were-kept-before-you.md` L47–50, set apart): *"six were kept before you. /
  the count of them is kept. / you are the seventh. / a file is kept until it is opened. a hand is kept
  after."* And the closing gloss (L54–59): *"… the struck row is not a row that failed to fill. it is the row
  that fills last, with present hands, when the keeping is done."*
- **[GAP — TO BUILD]:** the live `/record/the-record-keeps` page (the static lure shell + the M5
  `recordReceives` injection) and the off-world Record website route (`record-projection.ts` is referenced in
  WEB-MASTER §1.M0 but the page build is go-live).

### 4.4 The herd's full pale field + the name-carves persist (FACT 15 visual)

- THE HERD: the cosmetic Pale field (sites.yml `herd_anchor` L370–378, `radius: 16`) stands at full spread as
  the FACT-15 visual — all facing one way; only the GLOWING Beast was ever tracked (INV-13). Config: `herd`
  block (`config.yml` L232–237), `max-pale: 16`.
- THE NAME-CARVES: `name-where-never-been` carves persist as next-group markers (FACT 16). Producer
  `name-where-never-been.ts` (built, `discord/src/showrunner/`); carve anchors `carve_anchor_01..03`
  (sites.yml L337–365). Re-read row `name-where` (`puzzles_seed.sql` L908–917, dead_end `place` kind):
  accepted answers `'the record files the living by place not only by name'`, `'against each name a ground'`,
  `'before you was never about strangers'`. INV-16: ACTIVE-only subjects ROTATE (a chorus), never the
  divergence extremes.

---

## 5. THE FIVE DIVERGENT ENDINGS — the exact selectors + the literal outcomes

> The Accepting does NOT resolve to one ending. `decideFate` (`fate.ts`) returns ONE base fate enum; the M5
> composer (WEB-MASTER §5) opens with it + at most one tinted clause + at most one codicil clause. The
> divergence is delivered by the composer, never carved or written as a canon row (canon-spine §3b: "the
> divergence is NOT a FACT"; demoted to the mechanical expression of FACT 10). **It names NO player, reads
> the GROUP enum ONLY** (INV-11, INV-16). The persistent floor DRESSING is the camera-legible delta; the
> sentence only confirms what the floor already showed.

### 5.0 The selector — `decideFate` (`fate.ts` L84–117), PURE + DETERMINISTIC

- INPUTS (`FateInput`, L44–63, ACTIVE-only by construction; the bond/Whisper tally is ABSENT — no field for
  it, INV-11): `honoredActive`, `violatedActive`, `leftAtActive`, `seventhFound`, `issCaught`, `quorumMet`,
  `refusalSignal`.
- "Dominates" = a STRICT majority of the active honored/violated split (`x * 2 > decided`, L86–87), so a
  dead-even or empty arc lands on DIVIDED (L79, L110–116).
- FIXED PRECEDENCE (L88–116): **1. REFUSERS → 2. KEPT → 3. CAST_OUT → 4. DIVIDED (the floor).**
- WHERE THE I/O LIVES: the resolve.ts fate-sentinel branch reads the spread + writes
  `arc_state.ending_fate` (set-once, idempotent). **[GAP — TO BUILD]:** that `resolve.ts` fate-sentinel
  branch + `fate.run.ts` wrapper (named at `fate.ts` L11–13; only the pure `fate.ts` policy is on disk).
- **[GAP — TO BUILD]:** the M5 COMPOSER itself. WEB-MASTER §5 specifies "one synthesis-owned M5 composition
  pass" reading all colorant flags and emitting the bounded close (neutral + ≤1 tinted + ≤1 codicil by fixed
  priority). No `composer.ts` / `m5*.ts` exists in `discord/src/showrunner/` (verified this pass). The base
  fate lines, fork lines, seventh lines, and codicil line ALL exist in `voice.ts`; the bounded-priority
  ASSEMBLER that picks ≤2 of them is not yet authored.

### 5.1 ENDING 1 — `KEPT` (`fate.ts` L94–100)

- SELECT CONDITION (verbatim): `honoredDominates && (inp.seventhFound || inp.issCaught) && inp.quorumMet`.
  (WEB-MASTER §8: high honored ratio + (seventh found OR Iss caught) + full quorum.)
- WORLD OUTCOME: **the markers face OUT; the floor light holds whole.** The group is received. (Persistent
  dressing on `unbroken_light` — marker facing out, floor light whole. **[GAP — TO BUILD]:** the dressing
  producer/builder; sites.yml has no facing/light-geometry field.)
- WATCHER BASE CLAUSE (`voice.ts` `fateKept`, L455–457): *"the hands are in, and they are kept. the markers
  face out. the way is open the way it was open before you, and will be after."*
- KEEPER FACE (`npc-and-watcher-voice.md` C5 `keeper.endings.kept`, L671): *"it is done, and it is kept. the
  markers face the road now. you are the road's edge for whoever comes down next. i told you the rite
  receives. you are received. i will not say the rest of it. you will keep it, the way i keep it."*

### 5.2 ENDING 2 — `CAST_OUT` (`fate.ts` L102–108)

- SELECT CONDITION (verbatim): `violatedDominates && inp.leftAtActive >= 2` (violation dominates + ≥2 reached
  the LEFT_AT rung). 
- WORLD OUTCOME: **the markers face AWAY; the abandonment is written in passive voice.** The land said no.
- WATCHER BASE CLAUSE (`voice.ts` `fateCastOut`, L459–461): *"the count is closed and it is short. the
  markers face away. what was owed was not returned, and the record enters it so."*
- KEEPER FACE (C5 `keeper.endings.castOut`, L674): *"it is done. the markers face the wall. some of you were
  left at the threshold, as some were left before. i do not name which. the record does not name which. the
  threshold does not open twice for the same standing. come down again, kept, and stand again."*
- THE END LANE LANDS IT AS A PLACE (P2/cuttable, WEB-MASTER §1.M5): for a `cast_out` group with
  `seventh_seen_out`, the `end_exile_hold` (sites.yml L494–502, `enabled: false`) reads as their own
  exile-hold — markers all facing away. **GATED OFF (the default):** `enabled: false` until the INV-16
  binding is BUILT (S10) — the hold must name NO living player, encode NO per-player side, rhyme only on a
  CHORUS (*"you only came to look"*), never the `LEFT_AT` set, no dressing spatially corresponding to a
  per-player carve. If that cannot be guaranteed, the End ships as the Seventh shrine ALONE.

### 5.3 ENDING 3 — `DIVIDED` (`fate.ts` L110–116, the floor)

- SELECT CONDITION: the default — any real arc that is neither honored-dominant nor violated-dominant (a
  real honored/violated spread, OR a dead-even/empty arc → "the land holds on neither side").
- WORLD OUTCOME: **the light holds on HALF the floor BY GEOMETRY, never by player** (INV-16). The split must
  NOT spatially correspond to any per-player carve.
- WATCHER BASE CLAUSE (`voice.ts` `fateDivided`, L463–465): *"the light holds on one side of the floor and
  not the other. the record does not say which hands stood where. it says only that the floor is divided."*
- KEEPER FACE (C5 `keeper.endings.divided`, L677): *"it is done, and it is half-kept. the light holds on one
  side of the floor. it did not choose by who stood there. it chose by what was done. the floor only shows
  it. you may read the floor; you may not read it onto a face."*

### 5.4 ENDING 4 — `REFUSERS` (secret, `fate.ts` L89–92)

- SELECT CONDITION (verbatim, CHECKED FIRST): `inp.quorumMet && inp.refusalSignal`. `refusalSignal` is a
  POSITIVE, plugin-detected defiance act (L57–62), NEVER `quorum && !bowed` — a slow/absent group is NEVER
  read as refusing (PRECISION, canon-spine §8.4, WEB-MASTER §8).
- WORLD OUTCOME: quorum present, the bow window left empty by a positive act. The refusal is entered.
- WATCHER BASE CLAUSE (`voice.ts` `fateRefusers`, L467–469): *"the hands were all present, and the bow was
  not made. that too is entered. the record keeps the refusal as plainly as it keeps the keeping."*
- KEEPER FACE (C5 `keeper.endings.refusers`, L680): *"the way was open and you did not bow. that is not a
  thing the land has seen often. it is not an absence. it is a refusal, and the record keeps a refused thing
  differently than an empty one. i do not know what it keeps it as. i was kept. i did not refuse."*
- THE END LANE RE-READ (WEB-MASTER §1.M5): for `refusers` with `seventh_seen_out`, the End shrine re-reads as
  the model they followed — *"so did one before you"* (the Seventh, group-scoped, never "player X"). **[GAP —
  TO BUILD]:** the `fateRefusers` End-tint clause is not a distinct voice key; it would be a composer
  variant (see §5.0 composer GAP).
- **[GAP — TO BUILD]:** the plugin-detected refusal rite that sets `refusalSignal` (no `RefusalRiteListener`
  found this pass; `fate.ts` L60–62 says it is "Set ONLY by a plugin-detected refusal rite"). Without it,
  REFUSERS is unreachable and the arc falls through to KEPT/CAST_OUT/DIVIDED — safe by precision, but the
  fifth ending does not yet fire.

### 5.5 The 5th "ending" — `INHERITORS` (a CODICIL, not a base fate)

- WHAT IT IS: a boolean codicil that may append to ANY of the four base fates (canon-spine §8.4, WEB-MASTER
  §5). It is the SAME ACT as the Seventh `restore`/deposit — ONE flag (`seventh_choice = restore`), no
  separate site or `dark_shrine` (sites.yml L265–266; seventh_seed.sql L13–15).
- SELECT CONDITION: `seventh_choice = restore` (set by the `seventh-choice` rite, §6 below) → `ending_codicil
  = true`.
- THE +1 CLAUSE (`voice.ts` `fateInheritorsCodicil`, L471–473): *"a mark is left for a hand not yet here. the
  deposit slot is cut and waiting, the way yours was cut and waiting before you came."* Plants FACT 14 (the
  record receives, and keeps for whoever comes next) WITHIN this arc.
- COMPOSER PRIORITY (WEB-MASTER §5): `ending_codicil` is the +1 — always allowed on top of the ≤1 tinted
  clause (the base fate selects the open; seventh/fork may add ONE tint; INHERITORS may append the ONE
  codicil line). The cap is ≤2 clauses + the codicil.

---

## 6. THE SEVENTH CHOICE — restore / erase (the tint + the persistent block-state)

> Read by the M5 composer for ONE tinted clause + one persistent block-state. Colors the close, GATES
> NOTHING (INV-12). `restore` is ALSO the INHERITORS codicil act (§5.5) — ONE flag-origin, no separate site.

### 6.1 The choice rite — `seventh-choice` (`puzzles_seed.sql` L731–751)

- WHAT IT IS: a `main_beat` row, DETECTED IN-WORLD ONLY (the `SeventhChoiceListener`'s rite at
  `the_unwriting`, sites.yml L279–287, `type: seventh_shrine`). The TWO opaque, wordless tokens (accepted
  answers, L734–735) are posted by the plugin on real detection — `'r7n4k2 m1x8p5 w3j6h9'` (restore) vs
  `'e5t0b7 c2d4s8 v6f1z3'` (erase), never human-typeable (no-leaked-sentinel).
- EFFECT (verbatim payload): `voice_key: oracleMainBeat`; beat `unlock` → `site_id: the_unwriting`,
  `step: reveal`, `step_payload {fragment: 'seventh_choice_marked'}`, priority 11. The resolver's
  Seventh-choice sentinel branch sets `seventh_choice` + `ending_codicil` from WHICH token matched (TS-SHOWRUN
  owns the branch). **[GAP — TO BUILD]:** the resolver sentinel branch that maps token→`seventh_choice`
  value + `ending_codicil` (named in the seed comment L728–729; not verified on disk).
- WHERE / GATING: movement 3, `active: false`, `requires_flags {seventh_named: true}` (`metapuzzle_seed.sql`
  L130–131). GATES NOTHING on the spine (colors the ending only).

### 6.2 The tint clauses + persistent block-state (V)

- RESTORE — `voice.ts` `keeperCloseSeventhRestored` (L482–483): *"the name that was cut out is cut back in.
  the hearth below the cold hearth is lit again. one that broke nothing is kept, late."* PERSISTENT
  BLOCK-STATE: the re-warmed hearth (lit) below `the_cold_hearth`.
- ERASE — `voice.ts` `keeperCloseSeventhErased` (L486–487): *"the name stays out. the wall below the cold
  hearth stays blank. the record keeps the blank where the name would go, and does not fill it."* PERSISTENT
  BLOCK-STATE: the blank wall (`the_unwriting` stays unwritten).
- KEEPER FACE (`npc-and-watcher-voice.md` C3, L624–631):
  - `keeper.seventhChoice.offer` (L625): *"below the cold hearth the deep is open now. the seal there was a
    name … you may write it again, or leave the blank. the land made its choice. you make the record's.
    neither opens the road. both are kept."*
  - `keeper.seventhChoice.restored` (L628): *"you wrote the name back. the hearth takes the light. a mark is
    left there now for a hand not yet here, the way a mark was left for you. that is the older keeping. …"*
    (pairs with `fateInheritorsCodicil`).
  - `keeper.seventhChoice.erased` (L631): *"you left the blank. the hearth stays cold. … i will not say you
    chose wrong. the land cast that one out. you let the land keep its choosing. that is also a keeping."*

### 6.3 The deepening-lane tints (color the seventh clause, never change the mechanic)

- THE END LANE (`seventh_seen_out`) — `progression_seed.sql` end-seventh-out row (L106–122): a `lore` row at
  `end_seventh_shrine` (sites.yml L476–486). Accepted answers (verbatim): `'i kept all the ways and it did
  not matter'`, `'the keeping was never the price'`, `'i went out past the door that is not a threshold'`,
  `'you only came to look'`. `set_flags {seventh_seen_out: true}`. `voice_key: 'end.shrineArrive'`
  **[GAP — voice key not in voice.ts/voice.archive.ts this pass; the seed `voice_args.fragment` is the
  authored body, and a missing voice key is SILENT at runtime by design, `puzzles_seed.sql` L554–555]**.
  Fragment (verbatim): *"the seventh kept every way and was not kept, and went out past the door that is not a
  threshold, to the one place the record does not reach, and cut the name themselves. exile is the other side
  of keeping. you are not cast out. you only came to look."* THE LORE IT RENDERS:
  `the-name-i-cut-myself.md` (the Seventh's own carving, verbatim L31–36, L49–61).
- THE NETHER LANE (`nether_forge_found`) — sets up "the keeping was a carrying" tint (Kept-Light origin).
  Both flags are group-scoped, active-only, names-no-player, NEVER fate-selector inputs (WEB-MASTER §5; S2:
  `seventh_seen_out` is NOT in `FateInput` — confirmed, `fate.ts` `FateInput` has no such field).

---

## 7. THE FORKS — the permanence colorants at the close (INV-12, color never gate)

> Each fork sets a flag at its earlier movement; the M5 composer may add ONE as the heaviest-tint clause
> (priority: fate base → seventh OR heaviest fork → codicil). Forks NEVER gate (seedcheck-asserted). Produced
> via `forks.ts` (built, `discord/src/showrunner/`).

| Fork | Flag (V leaf) | Set by | `voice.ts` clause (verbatim) |
|---|---|---|---|
| **A — Sacred Beast** | `sacred_beast_broken` | the kill of the GLOWING Beast (INV-13) | `forkSacredBeastBroken` (L497–499): *"the one that glowed is down. the boon it would have lent is closed, and stays closed. the herd keeps the death-spot in its facing."* |
| **B — First Light (boon)** | `light_kept` | `fork-light` M3 choice (`puzzles_seed.sql` L759–772) | `forkLightKept` (L501–503): *"the light came up the stair on its own. you carried it. that is how it is carried."* |
| **B — First Light (transgressor)** | `light_taken` | `fork-light` alt plaintext | `forkLightTaken` (L505–507): *"the flame is banked, and the room it warmed stays dark. the light that was lent is taken, and the deep is colder by it."* |
| **C — Spoken Name (boon)** | `name_unspoken` | `fork-name` M4 (`puzzles_seed.sql` L778–790) | `forkNameUnspoken` (L509–511): *"the name was not shaped. the word stays shut, the way the sixth way is left blank in the book."* |
| **C — Spoken Name (transgressor)** | `name_spoken` | `fork-name` carve act (in-world) | `forkNameSpoken` (L513–515): *"the name was cut into the stone. the record keeps it, and keeps a faint line under it, the way it kept the one who turned away."* |

- Fork C is **P2 / cuttable on blurt risk** (WEB-MASTER §3.4). The transgressor leaves (`light_taken`,
  `name_spoken`, `sacred_beast_broken`) carry a persistent world delta (Undercroft dark / a faint carve / the
  herd facing the death-spot).

---

## 8. THE PRIOR-DOWNLOADS / 6-DOWNLOADS PAYOFF (the day-zero number pays off)

- Already laid in §4.3. Restated as the FINALE re-read so the editor can cut to it: the lure page's
  `kept: 6` (ledger #24) + dead uploader `m.kept` (ledger #26) + the README "it does not connect to anything"
  (ledger #25) ALL re-read at V as: the `6` was six prior keeper-GENERATIONS the record already did this to;
  the group is the seventh it would not keep AS A FILE; the struck row fills LAST, with present hands, when
  the keeping is done. `recordReceives()` (`voice.ts` L544–546) is the V-line that fills it.
- THE "it does not connect to anything" inversion (ledger #25): the MAP connects to nothing; the SERVER does
  — the lie was true, misread as comfort. (Payoff at M4, re-felt at V.)
- **[GAP — TO BUILD]:** the live `/record/the-record-keeps` lure page + `/record` V-injection (see §4.3 GAP).

---

## 9. THE SEASON-2 SEED — the INHERITORS / "a hand not yet here"

- WHAT IT IS: the season-2 (Inheritors) seed is NOT a separate mechanism — it is the `INHERITORS` codicil
  (§5.5) made persistent. The `restore` act leaves a deposit slot "cut and waiting" for "a hand not yet here"
  (`voice.ts` `fateInheritorsCodicil`, L471–473; `keeper.seventhChoice.restored`, C3 L628). The
  `name-where-never-been` carves persist as next-group markers (§4.4). The world flips to `world_kept`
  (`record-receives`, L533) — the group becomes, in the persistent world, the markers/Keeper-adjacent for a
  hypothetical NEXT group (FACT 15, canon-spine L210–215).
- THE FELT SEED LINE (`keeper.becomingKeepers.neutral`, L648): *"… the ones before brought theirs, and were
  received, and are kept — you have read where they are kept; you are reading it now, in the same book, in
  the same hand."* The group is now an entry the NEXT group will read — the Inheritors are whoever comes down
  after, and the deposit slot is already cut for them.
- DISCIPLINE: the season-2 seed is delivered by WHAT PERSISTS (the cut slot, the kept world, the next-group
  carves), never by a sentence that names "season 2" or finishes the recursion (canon-spine §6.2; the
  half-veil stops at "we would keep you").
- **[GAP — TO BUILD]:** there is no authored season-2 arc content on disk — only the persistent hooks above.
  The Inheritors seed is the set of durable world-states (`world_kept`, the restore deposit slot, the
  persistent name-carves), not a written next-season corpus.

---

## 10. DIRECTOR ACTION CHECKLIST (the finale console + setup steps)

1. **Bind the single Accepting instant** in `arc_state` (the showrunner; §0.4) — shared by the grave date,
   the Record timestamp, the summons `not_before`. **[GAP:** the binding writer + `grave.run.ts`/`fate.run.ts`
   wrappers.]
2. **Arm the take window** if filming: `config.yml` `event-window.enabled: true` with start/end, OR
   `/observance window on` (force-override, L268/L274).
3. **Approve the Oracle beats** as they enqueue (`status='approved'`): `true-walk-arrive` arrival;
   `rite-tokens` 6-slot reveal; `accepting-crouch` door_open; `record-receives` advancement_toast (pri 30).
4. **Confirm the bow gate is fully wired** before the take: `readyGate` (Threshold open) + `activeRosterSize`
   (`readActiveRoster`) suppliers. **[GAP:** both unwired today → fail-closed ready=always, roster=online
   count — safe but not the intended showrunner-fed values.]
5. **Set the fate dressing** on `unbroken_light` (marker facing in/out, floor-light geometry) to match the
   composed `ending_fate`. **[GAP:** the dressing producer; sites.yml has no facing/geometry field — builder
   dressing.]
6. **Repoint the catch** so `no-wall-catch` does NOT carry `next_puzzle_key: rite-tokens`
   (`puzzles_seed.sql` L294). **[GAP — TO BUILD, inherited from M-IV.]**
7. **Author the M5 COMPOSER** (the ≤2-clause bounded assembler reading all colorant flags). **[GAP — the
   single largest finale gap; all the clause TEXT exists in voice.ts, the ASSEMBLER does not.]**

---

## 11. THE [GAP — TO BUILD] LEDGER (this segment)

1. `no-wall-catch` L294 `next_puzzle_key: rite-tokens` shortcut not yet removed (the F1 monorail).
2. The arc_state Accepting-instant binding writer + `grave.run.ts` + `fate.run.ts` I/O wrappers.
3. The M5 COMPOSER (the bounded ≤2-clause + codicil assembler) — `composer.ts`/`m5*.ts` absent.
4. The resolve.ts fate-sentinel branch (reads spread → writes `arc_state.ending_fate`).
5. The resolve.ts Seventh-choice sentinel branch (token → `seventh_choice` + `ending_codicil`).
6. The plugin `RefusalRiteListener` that sets `refusalSignal` → REFUSERS unreachable until built.
7. The `AcceptingRiteListener` `readyGate` + `activeRosterSize` supplier wiring (unwired → fail-safe default).
8. The fate floor-dressing producer (marker facing / floor-light geometry on `unbroken_light`).
9. The `observance:the_record_receives_you` advancement datapack JSON (FACT-14 toast).
10. The live `/record/the-record-keeps` lure page + `/record` V-injection (`recordReceives`).
11. `end.shrineArrive` voice key not in voice.ts (silent-at-runtime by design; seed fragment carries the body).
12. The Nether `carrying` / Kept-Light-origin V-tint voice key (the `nether_forge_found` close clause).
13. The `end_exile_hold` INV-16 binding (S10) — `enabled: false`; ships as Seventh-shrine-alone until built.
14. No authored season-2 (Inheritors) arc corpus — only the persistent world-state hooks exist.
