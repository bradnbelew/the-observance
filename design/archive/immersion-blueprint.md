# The Observance — Immersion Blueprint (one world, three surfaces)

> Companion to `DESIGN.md` (the anti-jank bible), `FLOW.md` (pacing/gates), and
> `design/arg-deepening.md` (the concrete ARG mechanics). This file is the **fusion
> layer**: how the Minecraft server, the Discord Watcher, and the dashboard
> director's console add up to a *single* world that knows the friends by name —
> coherent enough for an ARG critic, hard enough for this friend group, and
> shootable as an autonomous "but it knows your name" mystery video.
>
> **Spoiler discipline.** This document is safe for Ethan and the dashboard. It
> never states the sealed twist; anything that depends on it lives in `arc/`
> (`canon-spine.md §0`, `_SEALED_ARC_BIBLE.md`). Where a beat *leans toward* the
> ending, it foreshadows in the world's own register and stops — per
> `canon-spine §6` rule 2 ("foreshadow, never blurt").
>
> **Nothing here invents engine capability.** Every beat names an existing class in
> `plugin/.../beats/lib/*.java`; every Discord move names an existing handler in
> `discord/src/`; every dashboard control names an existing component in
> `dashboard/src/components/`. The build list at the end marks the few stubs to
> finish (`SmallStructureBeat`, `UnlockBeat` producers, the showrunner).

---

## 0. The three surfaces, and the one law that binds them

| Surface | Home | What it IS, in fiction | What it CANNOT do |
|---|---|---|---|
| **The world** (Paper plugin) | PebbleHost | the land that watches and keeps the record | speak first-person; generate geometry at runtime |
| **The Watcher** (Discord bot) | VPS | the record-keeping *facet* of the same presence — its written voice | break character; improvise a hint or a callout |
| **The console** (Vercel dashboard) | Vercel | nothing, in fiction — it is invisible to players | be seen by players; ever appear in-world |

**The single binding law (the congruence invariant):** *there is exactly one
presence, and it speaks in exactly one voice across all surfaces.* A sign in a cave,
a line in `#the-record`, a lectern report, a toast, and a dashboard-staged beat are
all the **same entity** writing in the same hand. `discord/src/voice.ts` is the
canonical tongue (its header: "Nothing else in the bot may speak ad-hoc English");
the plugin's `SignWriteBeat`/`LecternFillBeat`/`BookAppearsBeat` payloads and the
`arc/lore/documents/*` corpus are the *same register* rendered in the world. If a
player screenshots a Discord line and a cave sign side by side, they must read as
one author. Section 3 makes this a checkable set of invariants.

Everything below serves that law.

---

## 1. THE FIVE-MOVEMENT ARC, DEEPENED

The five Movements map onto the three dashboard Acts (`ArcControl.tsx` /
`arc_state.current_act`): **Movement I = Act 1; Movements II–IV = Act 2; Movement V
= Act 3** (`canon-spine` preamble). Acts advance on **behavioral gates** in
`arc_state.gates`, never timers (`FLOW §2`). Each Movement below gives: the **player
experience**, the **cross-surface beats** (named to real beat classes + Discord
handlers + dashboard controls), the **escalation**, and a **foreshadow→payoff
ledger**. The throughline: this is *worldbuilding the group excavates*, not a quest
with a step counter.

### Movement I — THE NOTICE  *(Act 1; ≈ Days 1–5; drama budget LOW)*

**Player experience.** Nothing is announced. They play normally; the Signal Tracker
quietly fills `dossiers` (solo_ratio, deaths, blocks_mined, group_distance,
chat_sentiment, hoard_summary) and `heatmap_cells`/`bases`. Around Day 3–4 someone
finds, on a lectern *in their own base*, a report that **names a player for a habit
nobody told them was a law** (`the-record-opens.md`, FACT 1+2). The dread is not a
monster — it's the realization that *the world was already grading them.*

**Cross-surface beats.**
- World: `LecternFillBeat` writes the first report at `first_report_lectern_01`
  (`sites.yml`), placed by reveal discipline (`DESIGN §2.2`, never witnessed).
  Optional: `BookAppearsBeat` drops the founders' journal page.
- World ambient (rare, deniable): `PrivateSoundBeat` (`behind:true`) and
  `TorchGutterBeat` — one player, once, no group witness. Budget caps to ~1–2/session
  (`FLOW §2`).
- Discord: silence, or **one** late cryptic line via `poster.postReport` →
  `voice.recordOpened()` ("the record is open. it was open before you."). This is
  the rabbit-hole's first thread: the bot's *presence* already reads "The Watcher —
  Watching the ways" (`BOT_PRESENCE`), and the **server icon is the rune ring**
  (`learn-them-as-we-learned-them.md`, the master rabbit hole).
- Console: AUTO for ambient sound/torch beats; **CONFIRM** for the first report
  (Ethan, or the showrunner, approves the marquee naming beat in `BeatQueue.tsx`).

**Escalation.** From "did you hear that?" (one private sound) to "it wrote my name."
The shift is from *sensory deniability* to *being named in ink* — the first time the
world proves it is keeping a list.

**Foreshadow → payoff ledger.**
| Planted in M-I | Pays off |
|---|---|
| Report names a habit before it was a "law" (`the-record-opens` F2) | M-II: the custom that habit belongs to becomes legible; M-IV Orin's biography re-reads it (`observed-warned-left-at-threshold`) |
| Buried line "we left, and the light was kept" (F14) | M-III: the Undercroft's one eternal light; M-V the rite's "receive/keep" verb |
| "six are named… and there is a seventh mark the record will not [...]" | M-III: the Seventh Stone side-thread (`the-seventh-not-kept`) |
| Server-icon rune ring (unreadable now) | M-II: the Rosetta teaches the script; the icon becomes readable — the metadata was a clue all along |

### Movement II — THE KEEPER-STONES  *(Act 2; ≈ Days 6–11; budget RISES; **Whispers go live**)*

**Player experience.** The land opens into a **non-linear web** of expeditions. Six
keeper-stones "appear" between sessions (`SmallStructureBeat` / FAWE, out of sight),
each a **different cipher + different physical verb + a different prior keeper's
apparition** (`arg-deepening §1.2`, §2). Many open at once; players split up, trade
glyphs, and the Discord channel becomes the group's evolving "list of laws." This is
where the world stops being a haunting and becomes a *language they are learning to
read* (`arg-deepening §1.4`).

**Cross-surface beats.**
- The Rosetta (`learn-them-as-we-learned-them`) seeds ~5 glyphs; the clue forge
  (`forge/index.ts`) renders **every** Discord artifact in the *same* rune font
  (`forge/runes.ts`) as the in-world stones, so a glyph learned at a stone unlocks a
  Discord card and vice-versa.
- Each of the six keeper-stones is a forged clue with its own cipher (`forgeClue` →
  `caesar`, `atbash`, `vigenere`, `bookCipher`, `substitution`, `coordEncode` — six of
  the forge's eleven transforms; rail-fence/columnar/polybius/a1z26/morse carry other
  nodes per `cipher-web.md §1`) and its own answer-verb
  (`arg-deepening §1.7` verb menu): rotate item-frames, walk a lectern shelf
  (`Lectern.getPage()`), read a water reflection, fill a sign (`SignChangeEvent`),
  light campfires in order, travel to a coordinate.
- Solving is the **Oracle** (`ORACLE.md`): plaintext submitted in `#the-record`
  (the `messageCreate` scan), via `/answer`, OR on an in-world answer-sign
  (`answer_sign_01`) — **all three call the same `resolveAnswer`** against the same
  `puzzles` table. A genuinely-new solve speaks one in-register line
  (`voice.oracleNextClue` / `oracleSideQuest` / `oracleMainBeat`) and enqueues the
  reward beat (`status:'approved'`, fires immediately — player-earned).
- **Whispers** go live: `/whisper <puzzle>` (`commands/whisper.ts`) → tiered
  pre-authored hint (`getHint`) → in-world **toll** beat (`WhisperTollBeat`, takes
  the asker's light for the night) + a neutral `bond_ledger` tick.
- The six keeper apparitions (ZNPCsPlus, per-player) branch on the dossier — a keeper
  whose custom you've kept speaks plainly and gives its fragment; one you've
  transgressed against withholds (`arg-deepening §2`).
- Console: marquee stone reveals and the scalpel-personalized keeper line wait in
  `BeatQueue.tsx` (CONFIRM); whisper tolls and oracle unlocks fire approved.

**Escalation.** From *reading the world* to *the world reading back*: the personalized
scalpel report lands here (`DESIGN §2.5`), naming a real tracked habit. And **The
Liar** (Keeper Iss, `the-ways-are-a-wall`) gives a comforting doctrine and a
*nearly-right* fragment — the seed that will force a re-walk (the M-II surprise that
detonates in M-IV).

**Foreshadow → payoff ledger.**
| Planted in M-II | Pays off |
|---|---|
| Iss's "the ways are a wall" + Vigenère key = his own name | M-IV: `no-wall-was-ever-built-here` contradicts him line-for-line; whole dialogue tree re-reads cold |
| Each keeper's fate *rhymes* with a tracked behavior (the Hoarder ↔ high solo_ratio+hoard, the Drowned ↔ high group_distance) | M-IV: "the dread had a biography" (FACT 9) — a M-I haunting is revealed as a keeper's fate re-enacted |
| Sella's margin "the last marker is not the last" (`what-the-surface-keeps`) | M-III: the Seventh Stone expedition (`the-seventh-not-kept`) |
| Mara's book-cipher resolves to "DESCEND AND BOW AT THE UNBROKEN LIGHT" (`page-line-word`) | M-III: the literal descent into the Undercroft; M-V the synchronized bow |
| First personal-token demand (F13, Mara/`page-line-word`) | M-V: six personal tokens at the altar (`arg-deepening §1.6`) |

### Movement III — THE UNDERCROFT  *(Act 2 midpoint; ≈ within Days 6–11; the FALSE-CLIMAX REVERSAL)*

**Player experience.** The decoded instruction ("descend and bow at the unbroken
light") routes the group to a lectern-comparator door that drops into a dedicated
**custom dimension** (Multiverse void world, ambient-light-0, built-in fog —
`arg-deepening §1.3` of Path A, the only no-install true-fog). They go *into the
keepers' memory made literal.* They think this is the climax — gather, descend, bow,
done. Then the altar room **visibly rebuilds itself into something wrong** while
they're not looking (a `SmallStructureBeat` A→B schematic swap, reveal-disciplined),
and the Watcher reveals **"the rite is not a transaction."** The floor drops out of
the group's model of the game.

**Cross-surface beats.**
- World: the descent door (`DoorOpenBeat` gated on a lectern page or a solved
  oracle main_beat); the Undercroft set-piece (`SmallStructureBeat`/FAWE); the single
  **kept light** at its center (FACT 11) — one lit point in a doused world, beautiful
  and wrong.
- The reversal: the A→B room swap (`SmallStructureBeat`, out of sight, idempotent per
  `DESIGN §3.9`), so the group walks *back into a changed room.* A `main_beat` oracle
  outcome (`voice.oracleMainBeat`: "so. it turns. what was shut is shut no longer.")
  can gate it, or the showrunner stages it from the console.
- Discord: the Watcher does NOT explain. It posts one line in register that
  *reframes without revealing* — drawn from the M-III foreshadow facts (F11/F12),
  never naming the twist. The corpus's strongest foreshadow doc (`do-not-close-your-
  eyes-here`, Brann) and the abandonment register feed this.
- Side-thread: the **Seventh Stone** (`the-seventh-not-kept`) is reachable here — a
  ruined, light-doused shrine that proves *acceptance is a choice the land can refuse*
  (FACT 10). Finding it earns Whisper budget (`whisper_budgets.earned`) and reframes
  the rite as refusable — without touching the sealed twist.
- Console: this is a **CONFIRM-mode showcase** — Ethan/showrunner stages the room
  swap for maximum effect (and, for the video, for the camera; §5).

**Escalation.** The genre flips. Up to here the group was *solving*; now they learn
the thing they're solving toward is not what they assumed. The dread stops being
"something is in the dark" and becomes "we have misunderstood what is being asked of
us." This is the gut-punch midpoint that *points at* the sealed truth (`arg-deepening
§3`, Movement III surprise) and stops short of it.

**Foreshadow → payoff ledger.**
| Planted in M-III | Pays off |
|---|---|
| The one eternal kept light (F11) | M-V: the rite's light; the "kept" world-flip |
| "the kept ones did not depart. they were kept." (F12, passive voice) | M-V: the felt meaning of being *received* |
| "this is not a transaction" (reversal) | M-IV: the keepers turn and demand atonement, not payment |
| The Seventh: the land *can say no* (F10) | M-V: the collective ending's stakes — kept OR cast out |

### Movement IV — THE RECKONING  *(Act 2 late; the record stops being passive)*

**Player experience.** The keepers **turn on the group.** Apparitions that gave
fragments freely now **withhold** them, branching on the dossier (`arg-deepening §2`):
a keeper whose custom you broke speaks in riddle or refuses outright until you
**atone** — go back and honor the custom you skipped (gesture / offering / time /
refrain). The record stops counting and starts *naming what was done with the count*
(`observed-warned-left-at-threshold` changes register exactly here). And the catch
lands: a clue the group marked "solved" is **a lie** (`no-wall-was-ever-built-here`
behind a falsely "kept" gate), forcing them to re-walk it — and re-reading Iss's
whole tree as cold.

**Cross-surface beats.**
- World: keeper dialogue branches on `dossiers` + `custom_compliance` (the same data
  the `Dossiers.tsx` panel shows). Atonement = performing a previously-broken custom,
  detected by the existing custom listeners (`PlayerToggleSneakEvent` bow,
  `PlayerDropItemEvent` offering, moon-phase time taboo, `AsyncChatEvent` refrain).
- World consequence (cold, reversible): escalated `report` beats via
  `LecternFillBeat` + `voice.reportEscalated(name)` ("they have been told. if they
  will not keep the ways, the ways will not keep them.") — true because measured
  (`canon-spine §6` rule 4, grounding).
- Discord: the Liar's exposure. The re-walk's correct answer is an oracle
  `main_beat` that flips an `arc_state.flags` value (`set_flags`), and the Watcher's
  line turns. FACT 9 ("the dread had a biography") is delivered here by the exposed
  Iss / a keeper NPC connecting a M-I haunting to a named keeper's fate.
- Console: escalation reports and the dialogue-flip are CONFIRM beats (the showrunner
  authors the "turn" between sessions); atonement acknowledgements fire approved.

**Escalation.** The soft-pressure current finally shows teeth — but *per-conduct, never
the group-as-a-whole*, and **never an absent member** (`canon-spine §6` rule 3;
ending satisfiability gates on active players, per the hard constraints). The
withholding is the engine saying: the record has been patient; now it asks you to
prove you mean it.

**Foreshadow → payoff ledger.**
| Planted in M-IV | Pays off |
|---|---|
| The re-walk proves a "solved" clue was false | M-V: the group distrusts easy answers at the threshold — the rite is *earned*, not claimed |
| Atonement = honoring a broken custom | M-V: the altar accepts only a group that kept (or atoned for) the ways |
| "kept vs left is a real binary" (Orin, F6) | M-V: the collective judgment — faithful kept, careless cast out |

### Movement V — THE ACCEPTING  *(Act 3; ≈ Days 12–14; the collective ending)*

**Player experience.** Everything converges on a single rite (`arg-deepening §1.6`,
`bring-the-thing-only-you-can-give`). Together the **active** group gathers named
components + **one personal token per keeper** (six), wakes the Keeper NPC
(Citizens2), deposits each into its exact slot, and **all present bow at the right
hour, together.** The world flips to **kept** — or, if the record is careless, casts
them out. It is judged collectively; there is no chosen one.

**Cross-surface beats.**
- Discord: the summons — `voice.summons()` ("the way is open. come — all of you — at
  the dark hour. bring what is owed.") posted via `poster.postReport`.
- World: component validation (name + lore + PDC + `custom_model_data`), slot
  validation (frame/barrel contents), and the final trigger = proximity (all active
  present) + time window + **simultaneous `PlayerToggleSneakEvent`** within a sync
  window. On success: persistent world flips to **kept**; a hidden advancement toast
  (`AdvancementToastBeat`, "⟡ the record receives you") seals it diegetically.
- Console: the **AcceptingTrigger.tsx** control is the manual fire-path (for testing
  and, in CONFIRM mode, for staging the final beat on the night). `WatcherSleepToggle`
  remains the master kill-switch throughout (the only safety control).

**Escalation.** The peak — but quiet, not cheered (`voice` register forbids
exclamation). The recontextualization is the real payload: every M-I haunting was the
entrance exam. (The sealed meaning of *what they are accepted INTO* lives in `arc/`;
this document stops at the threshold.)

**Foreshadow → payoff ledger (the closing of the web).**
| Long-planted | Pays off in M-V |
|---|---|
| "the record does not close at the rite… it [...] you" (M-I, F14) | the world-flip's "receive/keep" verb is felt, never explained |
| six personal tokens (M-II Mara → M-III demand) | the altar's six labelled slots |
| collective judgment (`canon-spine §6` rule 3) | kept/cast-out applied to active players as one body |
| the kept light (M-III, F11) | the rite's light; the persistent flip |

> **Why it reads as worldbuilding, not a checklist:** there is never a visible step
> count. Many stones open at once (the non-linear web); some correct answers are
> `dead_end`s (true but open nothing — `ORACLE.md §3`); the Seventh and the
> self-rewriting journal (`arg-deepening §3`) are pure-lore side-mysteries that gate
> nothing. The group reconstructs the *history of a place* from fragments
> (`canon-spine §2` timeline), and the mechanics are the texture of that history, not
> a quest log.

---

## 2. "IT KNOWS ME" — PERSONALIZED MOMENTS GROUNDED IN THE DOSSIER

The dossier is real measurement, not vibes: `dossiers` (solo_ratio, deaths,
hoard_summary, group_distance, chat_sentiment, blocks_mined), `custom_compliance`
(per-custom status + violation_count), `bases`, `heatmap_cells`, and the
`bond_ledger`/`whisper_events` tally. Every moment below names the **signal**, the
**beat class**, and the **surface**, and obeys precision-over-recall (`DESIGN §3.6`):
the world only calls someone out when the signal is overwhelming, and there is always
a deterministic fallback (`canon-spine §6` rule 4 — no invented transgressions).

1. **The Hoarder's mirror.** Signal: high `solo_ratio` + a `hoard_summary` (e.g.
   "iron ×4 stacks, never traded"). Beat: a scalpel-personalized report
   (`LecternFillBeat`, validated `{lines:[…]}`) at the player's `bases` coordinate —
   *"the one called {name} keeps. and keeps. the deep counts what is taken and not
   given back."* Vaun's stone (Keeper 1) later **rhymes** with this without naming the
   living player. Fallback if the scalpel is down: the deterministic
   `voice.reportObserved(name, days, "given back to the deep")`.

2. **The Wanderer's cold path.** Signal: high `group_distance` (the player who keeps
   leaving the group). Beat: `PrivateSoundBeat` (`behind:true`) + `BossBarBeat`
   (proximity "attention" that intensifies the *further they stray from the others*),
   fired only on that player. Sella, the Drowned (Keeper 3), is the keeper whose fate
   this rhymes with.

3. **It writes where you sleep.** Signal: `bases` (bed/respawn anchor + container
   density, `DESIGN §2.2`). Beat: the **first report** appears on a lectern *inside
   the group's own base* (`first_report_lectern_01` retargeted to the detected base),
   `LecternFillBeat`. The dread is "it knows which walls are mine."

4. **It walks where you walk.** Signal: `heatmap_cells` hot cells (`Heatmap.tsx` on
   the dashboard shows them). Beat: ambient beats (`TorchGutterBeat`, a named mob via
   `NamedMobBeat`) are **placed in hot cells, ~30 blocks down the player's facing,
   out of line of sight** (`DESIGN §2.2` predictive placement) — so the haunting is
   always *on the path they were already taking*, never random.

5. **The custom you didn't know you broke.** Signal: `custom_compliance` status flips
   to `violating` (e.g. passing `bow_marker_01` standing, via `PlayerToggleSneakEvent`
   absence; or skipping the Offering at `offering_cairn_01`). Beat: a soft, reversible
   toll on the *next* relevant action — the deep "goes dark" (`TorchGutterBeat` +
   `PrivateSoundBeat`), then, on repeat, `voice.reportObserved(name, days, custom)`
   names it. The escalation ladder is Orin's exact biography
   (`observed-warned-left-at-threshold`): observed → warned → left.

6. **It knows your hours.** Signal: time-of-day pattern + the black-moon phase
   (`fullTime/24000 % 8`) cross-referenced with sleeping. Beat: for a player who
   sleeps on the dark hours, a private night-only apparition (Brann, Keeper 5, "speaks
   only at night") and a `PrivateDarknessBeat` / `PrivateTimeShiftBeat` — the world is
   *darker for them specifically.*

7. **It heard what you said.** Signal: `AsyncChatEvent` scan for the forbidden word
   (The Unspoken) and `chat_sentiment`. Beat: an immediate, deniable ambient to the
   speaker (`PrivateSoundBeat` + `TorchGutterBeat`) the instant the taboo word is
   typed (`FLOW §1` trigger table) — the only *always-fires* cheap beat. A run of
   negative `chat_sentiment` can color which keeper's voice the world uses.

8. **The Whisper-leaner's name in the dark.** Signal: `bond_ledger.bond_points` (rises
   each `/whisper` spend). Beat: Iss, the Liar (Keeper 6), is the keeper whose fate
   rhymes with "leaned hardest on Whispers" — the one who most wanted to be *told the
   comforting answer.* The bond tally is a **neutral colorant only** (`canon-spine §6`
   rule 3), never a callout, never a "chosen."

9. **The Watcher greets you by your world-name.** Signal: `players.name` bound via
   `/link` (`commands/link.ts`). Beat: `voice.linked(name)` — *"you are {name}. the
   record knows you now."* From that moment Discord addresses the player by the exact
   name they wear in-game; the two identities are stitched.

10. **The sacred animal you spared (or killed).** Signal: a tagged mob protected or
    killed (`SacredAnimalBeat` / `EntityDeathEvent` on a tagged entity,
    `arg-deepening §3` Haunted Herd). Beat: a player who protects it across the run
    earns a quiet boon acknowledgment; killing it is a tracked transgression that
    surfaces in a report. Personal, opt-in, and remembered at the Accepting.

11. **The death it counts.** Signal: `dossiers.deaths`. Beat: a death in a hot cell
    can trigger a single private line (`PrivateMessageBeat`) in register — not mockery,
    *observation* — reinforcing "everything you do is written."

12. **The journal that changes only for the one who reads it.** Signal: re-reads of a
    base lectern across sessions (`Lectern` interaction). Beat: the self-rewriting
    journal (`arg-deepening §3.2`) — `BookAppearsBeat`/`LecternFillBeat` swaps `pages`
    NBT out of sight, so the attentive player privately watches a keeper's journal
    *evolve.* Rewards attention; gates nothing.

> **The grounding contract (why "it knows me" never misfires):** the scalpel only
> personalizes on an *overwhelming* signal; every personalized line has a
> deterministic fallback in `voice.ts`; and a report names a living player **only**
> for something `dossiers`/`custom_compliance` actually measured. A wrong callout is
> worse than a generic one (`DESIGN §3.6`).

---

## 3. CROSS-SURFACE CONSISTENCY — THE INVARIANTS

These are the rules that make three surfaces feel like one world. Treat them as
*checkable assertions*; a beat that violates one is a bug.

**INV-1 — One voice, verbatim.** Every player-facing string on *every* surface is in
the `voice.ts` register: lowercase, sparse, certain, no exclamation, no emoji (one
`▒`/rune as a rare motif), speaks of "the ways / the record / the keepers / what is
owed / kept" (`voice.ts` REGISTER block; `canon-spine §0`). Discord strings come from
`voice.ts` literally; in-world `SignWriteBeat`/`LecternFillBeat`/`BookAppearsBeat`
payloads and `arc/lore/documents/*` are authored in the same register. **Test:** a
Discord line and a cave sign, side by side, read as one author.

**INV-2 — Never name it; never break character.** No surface ever says ai / bot /
game / server, uses normal capitalization, or gives the presence a proper name or
first-person "I" (except the Keeper NPC at the very end). The bot enforces this
structurally — handlers write **no** English, only call `voice.ts`
(`whisper.ts`/`answer.ts`/`link.ts` headers). The plugin's beats carry only authored
payloads. (`canon-spine §6` rule 1.)

**INV-3 — A clue that starts on one surface resolves on the other.** The congruence
rule (`DESIGN §2.8`, `arg-deepening §1.5`). Concrete loops the engine already
supports:
- **World → Discord → World:** a decoded in-world word is typed to the Watcher; the
  bot replies with the next coordinate *in runes* (a forged `coord` clue,
  `coordEncode`) that must be decoded again and **physically traveled** to.
- **Discord → World:** the clue forge posts a rune card (`postClue` → `forgeClue`)
  readable only once the in-world Rosetta is learned; its plaintext is a coordinate.
- **Either surface, one solve:** the Oracle keys on `players` (`mc_uuid` in-world,
  `discord_id` in Discord) and writes `solves` with `unique(puzzle_key, player_id)`
  — so a clue solved in-world **cannot** be re-solved for reward in Discord, and
  vice-versa (`ORACLE.md §5`). **Design rule: every major cipher crosses at least
  once** (`arg-deepening §1.5`).

**INV-4 — An event on one surface is acknowledged on the others.** State lives in
Supabase, shared by all three (`README` architecture table):
- A `/whisper` spend (`whisper_events`, `bond_ledger`) enqueues a `whisper_toll`
  beat (`status:'approved'`) the **plugin** enacts in-world (`whisper.ts` → `WhisperTollBeat`).
  The hint is asked in Discord; the cost is paid in the world.
- An in-world oracle solve (answer-sign) enqueues a beat AND can post a `#the-record`
  echo; a Discord solve enqueues the in-world reward beat (`ORACLE.md §4`).
- The dashboard's `arc_state` advance/`flags` writes (`ArcControl`, `actions.ts`) are
  read by both the plugin (gating beats) and the bot (`getArcAct` in `whisper.ts`
  reads the act to scope budgets). One act, all surfaces.

**INV-5 — The same fact has ≥2 doors, and they agree.** The web rule
(`canon-spine §3`): most facts surface on ≥2 surfaces, and those surfaces must not
contradict. The lore-bible's consistency check (`LORE-BIBLE.md §6`) is the audit:
keeper count, voices, and the sealed-fact discipline are verified internally
consistent. **Any new cross-surface fact must be added to that audit.**

**INV-6 — Player-earned fires now; curatorial waits (and the timing is invisible).**
`status:'approved'` beats (oracle unlocks, whisper tolls) fire on the plugin's next
poll with no human gate; `status:'pending'` beats (showrunner/curatorial) wait on
`BeatQueue.tsx` approval in CONFIRM mode (`ORACLE.md §4`, `FLOW §4`). The player never
perceives the difference — both arrive as the world simply *acting*. The
`inFlight`/`status='fired'` guards make it idempotent across restarts (`ORACLE.md §8`).

**INV-7 — Silence is canon.** A wrong answer is **silence**, never a tell; a missed
soft-pressure hook goes quiet, not louder (`DESIGN §1`, `ORACLE.md §3/§6`). Across all
surfaces, the world's default state is ~90% quiet. Consistency includes *consistent
restraint*: no surface nags.

**INV-8 — Tolls take warmth, not progress.** Every consequence on every surface is
reversible and atmospheric (doused torches relight, kept light returns) — never the
loss of hard-won progress (`DESIGN §3.10`, `canon-spine §6` rule 10).

**INV-9 — Collective judgment; never an absent member.** No surface elects a chosen
one or punishes the group for someone who isn't online; ending satisfiability gates on
**active** players (the hard constraint; `canon-spine §6` rule 3). The bond/Whisper
tally is neutral color, not a verdict.

---

## 4. THE ASYNC ENGAGEMENT ENGINE — KEEPING THE MYSTERY ALIVE BETWEEN SESSIONS

The server is empty most of the time. The mystery must **breathe** when nobody's on
and **pull people back** without nagging. Discord is the always-on heartbeat; the
showrunner is the slow hand that authors between sessions.

**The clue drip (≤1 Watcher-initiated post/day — `FLOW §2`).** The bot posts to
`#the-record` exactly one of: a report excerpt (`postReport` → a `voice.ts` line), a
fresh forged clue card (`postClue` → `forgeClue`/`renderClueDetailed`), or a single
cryptic line. The cadence is enforced by the showrunner's budget, not a chat trigger,
so the channel is *sparse and precious* — every Watcher post is an event. This is the
between-session tide: a clue surfaces overnight; the group logs in *because the
Watcher spoke.*

**The Watcher is present even in silence.** Its Discord presence reads "The Watcher —
Watching the ways" (`BOT_PRESENCE`) — the member list itself is in-fiction, 24/7. It
never goes offline-feeling; it is *patient*, which is scarier than active.

**`#the-record` is the group's shared workbench.** The `messageCreate` scan
(`bot/index.ts`) means *anyone can submit an answer at any time*, from their phone,
with no one on the server. Solving a stone in the channel at 2am fires the reward beat
the moment someone next logs in (or immediately if it's a Discord-only reward). The
channel naturally becomes the group's evolving glyph table and "list of laws"
(`arg-deepening §1.4`) — a living artifact they build together.

**Whispers are an async pressure-relief valve.** A group stuck overnight can
`/whisper` for a tiered hint (`commands/whisper.ts`) — rationed (`whisper_budgets`),
earnable (find optional lore → `earned`), and auto-gifted if the dossier sees them
truly stalled (`FLOW §1` backstop). The toll (`WhisperTollBeat`) is the diegetic cost,
collected next time they're in-world. The hint economy keeps a HARD web from becoming
a dead end while nobody's online.

**The showrunner is the between-session author (`DESIGN §2.10`).** A Claude Agent SDK
process on the VPS, **cron'd to wake between sessions** (not a live puppeteer). It
reads `dossiers` / `custom_compliance` / `arc_state` / `heatmap_cells`, then:
- queues the next day's clue drip (a `beat_queue` row or a scheduled `#the-record`
  post),
- authors the next personalized report/journal (scalpel, validated, with fallback),
- tunes upcoming stone difficulty and Whisper budgets to the group's pace,
- places the next stone/structure as a `pending` beat for the console.
In CONFIRM mode its output waits in `BeatQueue.tsx`; in AUTO it deploys. Either way
the world *changes overnight*, so logging in tomorrow is never the same world as
today — the core pull.

**Self-rewriting artifacts (`arg-deepening §3.2`).** The base journal's pages change
between sessions (`pages` NBT swap, out of sight). A player who re-reads catches it
evolving — a private reason to come back and *look again.*

**The pull-back loop, summarized:** overnight, the Watcher drips one clue + the
showrunner changes one thing in the world → the group sees the Discord post on their
phones → they discuss/solve in `#the-record` async → they log in to act on it / see
what changed → their play feeds the dossier → the showrunner reads it and authors
tomorrow. The mystery is never "paused"; it has a pulse.

---

## 5. THE DIRECTOR'S CONSOLE — CAPTURE PLAN FOR THE YOUTUBE CUT

The differentiator vs. scripted Minecraft-mystery videos (Wifies et al.) is
**authenticity**: this is autonomous and reactive, and that's the whole pitch. The
dashboard is a **director's console** that lets Ethan *time and stage* beats for the
camera **without scripting them** — the reactions are real because the players really
don't know.

**The console as director's chair (already built).**
- `BeatQueue.tsx` — preview / **approve** / **force** / **skip** every queued beat.
  In **CONFIRM** mode, curatorial beats wait here; Ethan releases them on the night
  when POV cameras are rolling and the group is in the right place. (Player-earned
  beats still fire approved — autonomy preserved.)
- `ArcControl.tsx` — advance/rewind the Act for a recording session; see
  `gates`/`flags`.
- `WhisperBudgets.tsx` / `BondLedger.tsx` / `Dossiers.tsx` — read the group's state to
  *know which beat will land hardest on whom* before staging it.
- `WatcherSleepToggle.tsx` — the master kill-switch (the only safety control); also
  "mute everything" for a clean re-take.
- `AcceptingTrigger.tsx` — manually fire the finale for the climactic shoot.

**AUTO ↔ CONFIRM is the autonomy/authenticity dial.** Day-to-day the run can be AUTO
(genuinely autonomous — the honest version the video is *about*). For a recording
session, flip to **CONFIRM**: the showrunner still *authors* the beat autonomously
(the content is not scripted by Ethan), but Ethan controls **when** it fires — so the
camera is rolling, the group is gathered at the stone, and the gut-punch lands on
film. The authenticity is real because Ethan never writes the beat or tells the
players; he only chooses the moment. (This is the honest seam to disclose in the
video's description: "I controlled timing, not content.")

**Per-player POV + voice capture.** Each friend records their own POV + voice (the
veteran-group norm); Ethan records the **server B-roll** with his solo-only client
mods (shaders, fog — Path B, *never required of players*, per the hard constraint).
Per-player private beats (`PrivateSoundBeat`, `PrivateDarknessBeat`,
`PrivateMessageBeat`, `FakeBlockBeat`) are the gold: only one player's POV catches the
"did you hear that?" — the **reaction asymmetry** (one player swears something
happened, the others didn't see it) is the most shareable kind of footage, and it's
*genuinely* one-player because the beat is per-player by construction.

**What to record (the gold the edit hunts for).**
1. **First-naming reaction** (M-I): the moment a player reads their own habit on a
   lectern. Catch their face/voice on their POV.
2. **Reaction asymmetry**: a private beat that only one POV registers — cut between
   the one who reacted and the others who didn't.
3. **The cross-surface "click"**: a player decoding a Discord rune card, realizing it
   points to a coordinate, and the group traveling to it — three surfaces in one cut.
4. **The Undercroft reversal** (M-III): the group walking back into a changed room.
   Stage this in CONFIRM with all POVs present.
5. **The Liar catch** (M-IV): the re-walk + the dawning "he lied to us."
6. **The Accepting** (M-V): the synchronized bow, the world-flip, the toast.
7. **The console itself**: brief director's-console inserts (the beat queue, the
   dossier panel) sell the "autonomous AI watching them" frame — *show the machinery*
   the way ARG channels show their evidence boards.

**How the edit finds the gold without a script.** The `event_log` table
(`logEvent`, surfaced on the dashboard) is a **timestamped beat ledger**: every fired
beat, every solve, every whisper is logged with a time. Cross-reference those
timestamps against the POV recordings and you have an automatic "here's where
something happened" index — the editor jumps straight to each real reaction instead of
scrubbing hours of footage. The `solves`/`answer_attempts` rows mark the puzzle "aha"
moments; the `beat_queue.decided_at` marks each staged beat. The autonomy stays
authentic (the engine and showrunner drove it); the **edit** is where Ethan's
authorship lives — finding, in the real footage, the moments the world earned.

**Staging without breaking autonomy (the rule).** Ethan may control *timing, framing,
and which already-authored beat to release* — he must never *write the beat's content*
or *tell the players what's coming*. The showrunner authors; the dossier grounds; the
players don't know. That triangle is what an ARG critic will respect and what makes
the footage real.

---

## 6. WHY IT CLEARS ALL THREE BARS

- **YouTube-ARG-worthy.** The capture plan turns an autonomous, reactive system into a
  shootable narrative with a real midpoint reversal (Undercroft), a real betrayal
  (the Liar), and a collective finale — and the `event_log` makes the gold findable.
  The differentiator is honest: it *is* autonomous; the console controls timing, not
  truth.
- **Friend-group-worthy.** A non-linear HARD web (many stones at once, dead-ends, red
  herrings, the Seventh side-thread, no step count), full "it knows me" profiling
  (Section 2), and a rationed Whisper backstop so HARD never becomes *stuck.*
- **ARG-critic-worthy.** One presence, one voice, enforced by `voice.ts` and the
  consistency invariants (Section 3); every cross-surface handoff resolves; every
  callout is grounded in real measurement; the sealed twist is foreshadowed by ≥7
  independent threads and never blurted (`canon-spine §3` web check); TINAG holds
  because the bot never breaks character and the friends install nothing but one
  resource pack.

---

## 7. BUILD-MAPPING — WHAT THIS LANDS ON, AND WHAT'S STILL A STUB

Spoiler-free elements ↔ existing code:

| Immersion element | Maps to (existing) | Status |
|---|---|---|
| Reports / journals (M-I, scalpel) | `LecternFillBeat`, `BookAppearsBeat`, `SignWriteBeat`; `voice.reportObserved/reportEscalated` | built |
| Per-player "it knows me" beats | `PrivateSoundBeat`, `PrivateParticleBeat`, `PrivateDarknessBeat`, `PrivateMessageBeat`, `PrivateTimeShiftBeat`, `FakeBlockBeat`, `BossBarBeat`, `NamedMobBeat`, `SacredAnimalBeat` | built |
| Cipher web + cross-surface clues | `forge/index.ts` (`forgeClue`), `forge/ciphers.ts`, `forge/runes.ts` | built |
| Oracle (3 entry surfaces, one resolver) | `oracle/resolve.ts`, `commands/answer.ts`, `bot/index.ts` scan, in-world answer-sign (`sites.yml answer_sign_01`) | built |
| Whisper hint economy + toll | `commands/whisper.ts`, `WhisperTollBeat`, `whisper_budgets`/`whisper_events`/`bond_ledger` | built |
| Identity stitch | `commands/link.ts`, `voice.linked` | built |
| Dossier-driven personalization | `dossiers`/`custom_compliance` tables; `Dossiers.tsx` | built (showrunner consumer pending) |
| Director's console | `BeatQueue.tsx`, `ArcControl.tsx`, `WhisperBudgets.tsx`, `BondLedger.tsx`, `WatcherSleepToggle.tsx`, `AcceptingTrigger.tsx` | built |
| Stones / Undercroft / altar set-pieces | `SmallStructureBeat` (FAWE paste + A→B swap) | **STUB — finish for M-II/III/V** |
| Earned-unlock producers (doors/reveals/advancements) | `UnlockBeat` dispatcher + `DoorOpenBeat`/`AdvancementToastBeat` | dispatcher built; **needs authored producer rows** |
| Between-session author | the showrunner (Claude Agent SDK, VPS cron, `DESIGN §2.10`) | **NOT BUILT — the async engine's engine** |
| Keeper apparitions (per-player, branching) | Citizens2 (Keeper) + ZNPCsPlus (six prior), `arg-deepening §2` | Phase 2 — framework chosen, not built |
| AUTO↔CONFIRM toggle | a `settings` row read by the showrunner + the beat status path (`status` pending/approved) | toggle plumbing built; **showrunner side pending** |

> **Top three to make the fusion real:** (1) finish `SmallStructureBeat` (every
> set-piece depends on it); (2) build the **showrunner** (the entire async engine and
> the AUTO↔CONFIRM authenticity dial hang on it); (3) author the `UnlockBeat` producer
> rows so solved clues visibly *open the world.* Everything else is wired.
