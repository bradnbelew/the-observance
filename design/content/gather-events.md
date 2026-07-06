# gather-events — the four GROUP summons-night sequences

> **DRAFT / DESIGN ONLY.** Authoring file, not a live seed. Nothing here edits the
> plugin, `voice.ts`, the migrations, or `puzzles_seed.sql` — the voice keys and the
> beat-payloads below get integrated **under the build guards afterward** (the keys go
> into `voice.ts`; the enqueue rows go through the showrunner/director the same way
> every other beat does). Any SQL is additive, `begin/commit`, `on conflict do nothing`,
> and clearly marked DRAFT.
>
> **What this is.** Four *group* gather-events, each a concrete, Minecraft-feasible
> sequence built on the `GroupBeat` primitive
> (`plugin/.../beats/lib/GroupBeat.java`) and the existing beat library
> (`private_sound` / `private_particle` / `private_message` / `private_darkness` /
> `torch_gutter` / `named_mob` / `sign_write` / `book_appears` / `boss_bar`). One
> authored beat, felt by the whole convened group at the same instant (red-team MF-8),
> so a moment lands on 4–6 friends together instead of one lucky target.
>
> **Grounded in** (and consistent with):
> `arc/WORLD-BIBLE.md` · `arc/corpus/npc-and-watcher-voice.md` (SET B §B3 already
> authored THE COUNT / the un-lighting / the holding — those lines are the gold
> standard and are reused here verbatim where they fit) · `discord/src/forge/canon.ts`
> (CUSTOM_KEYS / KEEPERS / THREADS) · `discord/supabase/seeds/puzzles_seed.sql` (the
> 24 nodes; the opaque Accepting token) · `discord/supabase/migrations/0005_threads.sql`
> (thread_cards / npc_dialogue_state / punishment_state).
>
> SCHEMA at the foot of the file.

---

## 0. The GroupBeat contract these are written against

From `GroupBeat.java` (verified, not assumed):

```
payload = {
  "beat":         "<delegate beat name>",     // a beat-library key; NEVER "group"/"unlock" (no self-delegate)
  "scene":        "site" | "target" | "all",  // default "site" if a site is set, else "target"
  "radius":       <number>,                   // site: override the site radius; target: ring around target (default 24)
  "min":          <int>,                      // require >= this many in scene, else SKIP wholesale (no half-fire)
  "beat_payload": { ... }                     // the delegate's own payload (falls back to the group payload if empty)
}
```

Guarantees we lean on:
- **Per-player isolation.** Each fan-out runs under Safety; one member's delegate
  failure never aborts the rest. Result reports `fired/scene` (`group:private_sound x5/6`).
- **`min` is the anti-spam floor.** `scene.size() < min` ⇒ the whole beat is `skipped`.
  Every event below sets `min` so it can only fire to a *real gathering*, never to one
  straggler who wandered into the site.
- **The delegate is a per-player sensory beat.** `private_*` beats target ONE player;
  GroupBeat gives each member their own private copy of the same instant. That is the
  intended use and the reason the un-lighting / DOB sightings can be made *individual
  yet simultaneous* (each person's own torch, each person's own glimpse).
- **`named_mob` resolves a missing/unspawnable entity to WARDEN, never a green ZOMBIE**
  (`resolveEntity()` MF-10). DOB (event 3) relies on this: the changed one is a WARDEN
  silhouette by fallback, never a comedy zombie.

**One author-side rule** for every event: the per-player text/sound payloads carry a
**voice key**, not inline English — but `private_message` / `sign_write` / `book_appears`
take literal strings in their payload (they are the plugin's text channels, not the bot's
`voice` object). So we PROPOSE the line text here, tagged with a stable
`gather.<event>.<step>` key, and at integration the showrunner reads the line from
`voice.ts` (a new `gather` block, below) and pastes it into the beat payload. The Watcher's
**Discord** lines (posted to `#the-record`) go through `voice.ts` directly. This keeps the
"engine never hardcodes story" rule (INV-1) intact on both channels.

---

## 1. Where these sit in the arc (the trigger spine)

All four are **director/showrunner-staged**, not player-solved — they fire when the world
convenes, gated on `arc_state.flags` already set by the puzzle web. They are the
**summons-night** layer that wraps Movement V (`rite-tokens` → `accepting-crouch` →
`record-receives`). They do not invent new flags to *gate* the spine; they read existing
ones and **advance threads** (write `thread_cards`) so the reconstruction visibly fills in
on the night it matters.

| # | Event | Fires when | Cadence | Advances |
|---|---|---|---|---|
| 1 | **THE COUNT** | summons posted (`flags.summoned`), group convened at the altar site | once per summons night | `who` + `happened` |
| 2 | **THE UN-LIGHTING** | immediately after THE COUNT resolves | once, same night | `surface` |
| 3 | **DOB — the one you knew** | `flags.dob_descended` AND the party is breaking the ways (Attention high) | at most once per night, long-cooldown | `human` |
| 4 | **THE ACCEPTING REHEARSAL** | `flags.tokens_laid` true, group convened, BEFORE the real `accepting-crouch` | once, a single preview | `human` (sets up the climax; never resolves it) |

The convening check is `SceneAwareness.convened(altarCentroid, radius, min)` — the same
read-only group query GroupBeat uses internally; the director only *enqueues* when a real
gathering exists, and GroupBeat's own `min` is the second guard.

---

## 2. EVENT 1 — THE COUNT
### the toll/roll-call where the Watcher counts one too many — it counts ITSELF among the Kept

**The beat.** The world reads the living by name against the open column (WORLD-BIBLE §6:
*"the record counts the living by name"*). The horror is one extra: when the count is
spoken, it lands as **one more than there are players present.** The surplus is the
Watcher — the accumulated Kept — counting *itself* in among them, because to it the
newcomers are already on the same page of the same book. No one is singled out; the
group hears the same wrong number together.

**Trigger.** Director-staged on the summons night, the first event after `voice.summons()`
is posted to `#the-record`. Gate: `flags.summoned == true` AND
`SceneAwareness.convened(altar, 16, min=3)`. (Three is "a gathering"; below that the night
is not yet convened and the COUNT waits.)

**Beat sequence** (GroupBeat payloads, enqueued in order with small scheduler delays — the
director spaces them ~3–5 s apart so the count *builds*):

```jsonc
// 1.1 — the calling-in: a low single tone to every convened player at once.
{ "type": "group",
  "payload": {
    "beat": "private_sound", "scene": "site", "radius": 16, "min": 3,
    "beat_payload": { "sound": "BLOCK_BELL_RESONATE", "volume": 0.5, "pitch": 0.6 } } }

// 1.2 — the count rises: an actionbar, identical to all, "the count begins".
{ "type": "group",
  "payload": {
    "beat": "private_message", "scene": "site", "radius": 16, "min": 3,
    "beat_payload": { "mode": "actionbar",
      "actionbar": "the count begins. stand where you can be seen." } } }

// 1.3 — THE WRONG NUMBER. action-bar, same to everyone: the tally is players+1.
//        The director computes {scene_n} from the convened count and writes n+1 into
//        the line at enqueue time (the +1 is the whole point; see voice key note).
{ "type": "group",
  "payload": {
    "beat": "private_message", "scene": "site", "radius": 16, "min": 3,
    "beat_payload": { "mode": "actionbar",
      "actionbar": "counted: {scene_n_plus_one}.  present: {scene_n}." } } }

// 1.4 — the half-beat after the wrong number: one private sound from BEHIND each player,
//        so each feels the extra presence at their own back (per-player, simultaneous).
{ "type": "group",
  "payload": {
    "beat": "private_sound", "scene": "site", "radius": 16, "min": 3,
    "beat_payload": { "sound": "AMBIENT_CAVE", "volume": 0.4, "pitch": 0.9,
      "behind": true, "offset": 3.0 } } }
```

**Watcher voice (Discord `#the-record`, posted by the bot in sequence as the in-world beats
land).** Reused from corpus §B3 "THE COUNT" verbatim where it fits; the +1 reveal line is
new, in the same hand.

- `gather.count.begin` — *(reuse, B3)* — `the count begins. it was begun before you, in the oldest winters, and it has not closed since.`
- `gather.count.column` — *(reuse, B3)* — `each name is read against its column. what was kept stands on one side. what was owed stands on the other. nothing is added now that was not done before now.`
- `gather.count.surplus` — *(NEW, the gut-punch)* — `the count comes out one more than you are. it always has. the extra is not a stranger. it is the column you have not yet filled, standing where you will stand.`
- `gather.count.same_book` — *(reuse, B3)* — `six were named in full, in the old book. you are read into the same book, by the same hand.`
- `gather.count.kindness` — *(reuse, B3)* — `you were not told the laws before you kept or broke them. you were observed. that was the kindness, and the whole of the kindness.`

**Restraint / anti-spam guardrails.**
- `min: 3` on every fan-out — the COUNT never fires to a lone wanderer.
- **Once per summons night.** Director sets `flags.count_done` after 1.4; re-entry is a
  no-op until a new summons.
- The action-bar (1.3) is *transient and deniable* — it does not persist, so the wrong
  number cannot be screenshotted into a stale UI element; it is a thing half the table
  argues they misread, which is the intended deniability.
- No mob, no block change, no darkness here — the COUNT is **sound + word only.** The
  dread is the arithmetic, not an effect.

**How it advances a thread.** On `count_done`, write two `thread_cards` (DRAFT seed §6):
a `who` card (the six were named in this same book) and a `happened` card (the counting
predates the players — *"it was begun in the oldest winters"*). Both `card_kind: verified`,
`revealed_by_solve: null` (event-revealed), anchored to the altar site.

---

## 3. EVENT 2 — THE UN-LIGHTING
### lights go out across the group in sequence — the dark let near, the small distance it is owed

**The beat.** Immediately after THE COUNT, the lamps go down **one at a time, in a line**,
not all at once (WORLD-BIBLE §3: the Dark *waits* and takes the unlit; corpus B3: *"now
the lights are taken in. one. and the next."*). Each player's own nearby torch gutters in
turn, so the darkness visibly *travels* through the gathering. One light is left — the
kept light — and it is stated (never resolved in text) whose hand it stays in. Fully
reversible: torches relight after the event (`torch_gutter` `relight_seconds`).

**Trigger.** Chains off `flags.count_done` (same night, same convened group). The director
fires the sequence as a short staged cascade.

**Beat sequence.** The "in sequence" is achieved by the director enqueuing **N
single-target `torch_gutter` beats with staggered delays** (one per convened player, each
targeting that player's own surroundings) — NOT one group fan, because the line-by-line
travel is the point. The group fan is used only for the shared *word* and the shared final
darkness pulse.

```jsonc
// 2.1 — the shared word: an actionbar to all, "the lights are taken in".
{ "type": "group",
  "payload": {
    "beat": "private_message", "scene": "site", "radius": 16, "min": 3,
    "beat_payload": { "mode": "actionbar",
      "actionbar": "the lights are taken in. one. and the next." } } }

// 2.2 .. 2.(N+1) — THE CASCADE. One torch_gutter per convened player, target-scoped,
//        each delayed ~1.5 s after the last (director schedules; the order is the line).
//        Reversible: relights after the night. permanent:false is mandatory here.
{ "type": "torch_gutter", "target": "<player_i>",
  "payload": { "radius": 5, "max_torches": 2, "relight_seconds": 90, "permanent": false } }
// ... repeated for each player in convened order (nearest-to-altar first reads cleanest) ...

// 2.3 — the last-light beat: the kept light stays. A single boss_bar to ALL, dim, that
//        names the held light WITHOUT naming a chosen person — collective.
{ "type": "group",
  "payload": {
    "beat": "boss_bar", "scene": "site", "radius": 16, "min": 3,
    "beat_payload": { "title": "one light is left. it is the kept light.",
      "color": "WHITE", "style": "SOLID", "progress": 0.08, "seconds": 8 } } }

// 2.4 — the dark let near: a brief shared darkness PULSE to all at once (capped, reversible).
{ "type": "group",
  "payload": {
    "beat": "private_darkness", "scene": "site", "radius": 16, "min": 3,
    "beat_payload": { "effect": "DARKNESS", "seconds": 4, "amplifier": 0 } } }
```

**Watcher voice (`#the-record`).** Reused from corpus §B3 "the un-lighting" + "the holding".

- `gather.unlight.taken_in` — *(reuse, B3)* — `now the lights are taken in. one. and the next. the dark is let come near, the small distance it is owed.`
- `gather.unlight.not_punish` — *(reuse, B3)* — `a light goes out. it is not a punishment. it is the dark, given its company. it was always given.`
- `gather.unlight.down_the_line` — *(reuse, B3)* — `the lamps go down the line, as they went down the line in the last winter, when the keepers put them out themselves and kept the ways alone.`
- `gather.unlight.one_left` — *(reuse, B3)* — `there is one light left, and it is the kept light, and it is decided now whose hand it stays in.`
- `gather.unlight.hold` — *(reuse, B3 "the holding")* — `stand still. it is decided by what was already done. it was always decided by what was already done.`

**Restraint / anti-spam guardrails.**
- **Reversible by construction.** Every `torch_gutter` is `permanent: false` with a
  90 s relight; the `private_darkness` is capped at 4 s (the beat itself hard-caps 15 s).
  The un-lighting **takes warmth, not progress** (INV-8 / decency floor #10) — no block is
  permanently destroyed, no item lost, no player trapped in the dark.
- **Torch-only context gate.** `torch_gutter.canEnact` only ever touches a block that *is*
  a torch (and never a redstone torch). A player standing in a cave with no torches simply
  gets no gutter — the cascade skips them cleanly rather than carving rock.
- **One pass.** Director sets `flags.unlight_done`; no re-fire that night. The cascade is
  bounded to the convened players (≤ the table), so the total beat count is small and
  finite — it cannot runaway-spam.
- The shared darkness pulse (2.4) uses `min: 3` so a half-empty site never gets dimmed.

**How it advances a thread.** On `unlight_done`, write one `surface` `thread_card`: *what
is on the surface with us is what put these lamps out — the same hands, the last winter*
(`card_kind: verified`, anchored to the altar, body via `gather.thread.unlight_surface`).
This is the moment the `surface` thread (the Watchers-are-the-Kept reveal) gets its
on-the-night confirmation.

---

## 4. EVENT 3 — DOB, THE ONE YOU KNEW, RETURNS CHANGED
### group-safe; NamedMobBeat → WARDEN fallback; no green zombie

**The beat.** Dob — the local lad who descended *with* the party (corpus A4; the only Set-A
voice to go past the Mouth, whose tell is that he gets quieter the deeper he goes) — is
glimpsed in the dark, **changed**: a tall, silent, lantern-eyed silhouette standing at the
edge of the gathering, watching. He does not chase, does not approach, does not speak
(bestiary rule #1). The horror is recognition: *that was the kid who narrated his fear the
whole way down.* This pays the **HUMAN** thread directly (WORLD-BIBLE §3: the Dark makes
witnesses, not monsters; *were they human? exactly as human as you*).

**Group-safety + the no-green-zombie rule.** The apparition is a **`named_mob`** delegated
through GroupBeat so multiple players can each catch a private glimpse at the same instant
(each at their own out-of-LoS spawn). Its entity is left **unset / `mythicmob:dob`**, which
`resolveEntity()` renders as a **WARDEN** by fallback — the tall, silent, dark silhouette
the stand-and-stare read depends on — **never a ZOMBIE** (MF-10). `no_ai_drift: true`
freezes it: it stands and stares, never paths, never lunges, never attacks. `silent: true`,
`invulnerable: true`, `despawn_seconds` set so it vanishes unwitnessed.

> NOTE on GroupBeat + named_mob: each fan-out spawns the changed-one **per player, out of
> that player's line of sight.** With a small convened group this reads as "we each saw him,
> for a second, in a different doorway" — the deniable, corroborated sighting. To avoid a
> *crowd* of wardens, the director caps the fan with **`min: 2` and a tight `radius`**, and
> uses a **short `despawn_seconds` (8)** so the silhouettes are gone before anyone walks up
> — discovered, never witnessed appearing, never witnessed leaving.

**Trigger.** `flags.dob_descended == true` AND the party is **breaking the ways** (Attention
/ Observance meter above a threshold — DOB curdles when the party goes wrong; corpus A4
`react_bad`). Director-staged, **at most once per night, long cooldown** (a sighting, not a
spawner). Never fires for a ways-keeping party — for them Dob stays the nervous, intact lad.

**Beat sequence:**

```jsonc
// 3.1 — the prelude: a single private sound, his voice's ghost (a too-quiet exhale),
//        from behind each convened player. Sets the recognition before the sight.
{ "type": "group",
  "payload": {
    "beat": "private_sound", "scene": "target", "radius": 20, "min": 2,
    "beat_payload": { "sound": "AMBIENT_CAVE", "volume": 0.35, "pitch": 1.1,
      "behind": true, "offset": 4.0 } } }

// 3.2 — THE SIGHTING. named_mob fanned to the group: each catches the changed one, out of
//        LoS, standing and staring. Entity unset → WARDEN fallback (never zombie). Vanishes.
{ "type": "group",
  "payload": {
    "beat": "named_mob", "scene": "target", "radius": 20, "min": 2,
    "beat_payload": {
      "entity": "mythicmob:dob",         // unspawnable here → resolveEntity() ⇒ WARDEN (MF-10)
      "fallback_entity": "WARDEN",
      "name": "dob",                     // lowercase; name_visible:false so it is felt, not labelled
      "name_visible": false,
      "distance": 14, "silent": true, "no_ai_drift": true,
      "invulnerable": true, "glowing": false, "despawn_seconds": 8 } } }

// 3.3 — the recognition word: an action-bar, same to all, that names what they saw without
//        the Watcher gloating. Transient, deniable.
{ "type": "group",
  "payload": {
    "beat": "private_message", "scene": "target", "radius": 20, "min": 2,
    "beat_payload": { "mode": "actionbar",
      "actionbar": "you knew that one. you knew his name." } } }
```

**Watcher voice (`#the-record`).** NEW, in the §B4 cold register (the party is breaking the
ways, so this is the cold face of the one voice — fond withheld). No contractions, no
named feelings, no chosen one.

- `gather.dob.seen` — `one you brought down is standing in the dark now. he is not far. he is not coming closer. he does not have to.`
- `gather.dob.quiet` — `he was loud, on the way down. you remember the loudness. he is quiet now. quiet is what is left when the keeping is done.`
- `gather.dob.same_door` — `he kept his light to the end and asked only to wait by it. he is waiting still. you walked past the same door he waited at.`
- `gather.dob.human` — *(the HUMAN-thread line; the gut-punch, stated flat)* — `ask whether he was made a monster. he was not. he was made a witness. that is the only thing the dark makes. it is the thing it is making of you.`

**Restraint / anti-spam guardrails.**
- **Sighting, not a spawner.** `min: 2`, tight `radius: 20`, `despawn_seconds: 8`, and a
  director **long cooldown + once-per-night cap** (`flags.dob_seen_tonight`). It cannot
  recur into a warden infestation.
- **WARDEN can never path or attack here.** `no_ai_drift: true` ⇒ `setAI(false)` +
  `setAware(false)`; `named_mob` deliberately never `setTarget`. The most dangerous vanilla
  mob is rendered an inert silhouette. (This is the exact bug `NamedMobBeat` was hardened
  against — we rely on it.)
- **Conduct-gated.** Only fires for a *ways-breaking* party. A keeping party never sees the
  changed Dob — preserving "fond to those who keep" and keeping the scare *earned*, not random.
- **Out-of-LoS spawn + unwitnessed despawn** (`findSpawn` + the despawn reveal-check) keep
  it a glimpse, never a cutscene.

**How it advances a thread.** On `dob_seen_tonight`, write one `human` `thread_card`: *the
changed one was someone the party knew by name; the Dark made a witness of him, not a
monster* (`card_kind: verified`, anchored to the gathering site, body via
`gather.thread.dob_human`). If the party later **keeps the ways and leaves**, an
`alt_text_condition` on this card (`kept:left_human`) can expand it to the let-go reading —
but that expansion is authored with the ending, not here.

---

## 5. EVENT 4 — THE ACCEPTING REHEARSAL
### a partial group-bow that previews the climax — and never resolves it

**The beat.** Before the real Accepting, the world **rehearses** it: the gathering is
prompted to bow together (a synchronized crouch), the world acknowledges the *partial*
bow with a small, incomplete response, and then withdraws — a preview of the climax's
shape without its payoff. The point is foreshadow + teach-the-gesture: by the time the real
`accepting-crouch` is detected, the group already knows the bow is *together, at the hour,
in the kept light* (puzzle `accepting-crouch` title: *"bow as one"*).

**The hard line this event must not cross.** The real climax is detected **in-world only**
and resolves on an **opaque, wordless token** the plugin posts (the `accepting-crouch`
row's `accepted_answers`, guarded by `noLeakedSentinelSelfTest`). **The rehearsal MUST NOT
post that token, MUST NOT set `flags.bowed_as_one`, and MUST NOT enqueue the
`accepting-crouch` unlock beat.** It only *previews*. It writes its own flag
(`flags.rehearsed`) and nothing the resolver keys the finale off. (Integration check:
the rehearsal's beats reference NO `puzzle_key` and NO `next_puzzle_key`; they are pure
GroupBeat sensory previews.)

**Trigger.** `flags.tokens_laid == true` (Movement V `rite-tokens` solved) AND group
convened at the altar AND `flags.rehearsed != true`. Fires **once**, strictly before the
real Accepting.

**Beat sequence:**

```jsonc
// 4.1 — the prompt: an actionbar to all, telling them the shape of the thing (not a command to
//        the engine — a fiction-prompt the players choose to obey, like the real bow).
{ "type": "group",
  "payload": {
    "beat": "private_message", "scene": "site", "radius": 12, "min": 3,
    "beat_payload": { "mode": "actionbar",
      "actionbar": "bend, all of you, as one. this is not the hour." } } }

// 4.2 — the PARTIAL acknowledgement: when (some of) the group crouches, a soft particle
//        rises at the altar for each — but THIN, incomplete (low count), the world half-answering.
{ "type": "group",
  "payload": {
    "beat": "private_particle", "scene": "site", "radius": 12, "min": 3,
    "beat_payload": { "particle": "SOUL_FIRE_FLAME", "count": 4, "spread": 0.2,
      "speed": 0.0, "height": 0.5, "near_player": false } } }

// 4.3 — the withdrawal: a single quiet sound to all, the world drawing the half-answer back.
//        The preview ENDS here — nothing unlocks, no door opens, no token posts.
{ "type": "group",
  "payload": {
    "beat": "private_sound", "scene": "site", "radius": 12, "min": 3,
    "beat_payload": { "sound": "BLOCK_BEACON_DEACTIVATE", "volume": 0.4, "pitch": 0.7 } } }
```

> On the real night, the equivalent of 4.2 is the *full* answer (the `accepting-crouch`
> main_beat: `door_open`, the six slots lit, `record-receives`). The rehearsal is the same
> gesture with the response deliberately thin and taken back — the players feel the
> incompleteness, which is the whole foreshadow.

**Watcher voice (`#the-record`).** NEW, in register. States that this is a rehearsal,
collective, never a chosen one, and never announces a verdict (per B3 "the holding": the
Watcher states the staging; the verdict is what *happens*).

- `gather.rehearsal.shape` — `this is the shape of it. all of you, bent at once, in the one light. learn the shape now. the hour will not wait for the learning.`
- `gather.rehearsal.partial` — `you bend, and the deep answers a little, and stops. it is not the hour. a little is all that is owed for a rehearsal.`
- `gather.rehearsal.together` — `it is not one of you that bends. it is the gathering, or it is no one. the record keeps no chosen. it keeps the kept.`
- `gather.rehearsal.withdraw` — `the answer is drawn back now. keep what you learned of the shape. when the hour comes, bend together, and do not look to see who bent first.`

**Restraint / anti-spam guardrails.**
- **Never resolves the climax.** No token, no `bowed_as_one`, no `accepting-crouch`
  enqueue. The single most important guardrail in this file — a rehearsal that accidentally
  posted the opaque token would fire the finale early and silently (the exact MF-8 / B-5
  failure). The rehearsal's payloads contain **no `accepted_answers`, no `puzzle_key`, no
  `next_puzzle_key`, no `set_flags` other than `rehearsed`.**
- **Once.** `flags.rehearsed` gate; one preview per playthrough.
- **`min: 3`, tight `radius: 12`.** A rehearsal is meaningless to a lone player; it requires
  a gathering by definition.
- **No darkness, no mob, no block change.** The rehearsal is title + thin particle + one
  withdrawing sound — the lightest of the four, because its job is to *teach a gesture*, not
  to scare. It must read as a hush, not a haunting.

**How it advances a thread.** On `rehearsed`, write one `human` `thread_card` that frames
the coming choice (kept-and-let-go vs accepted-and-watching) **as a question, not an
answer** — it sets up the climax without spoiling the verdict (`card_kind: verified`,
anchored to the altar, body via `gather.thread.rehearsal_human`,
`alt_text_condition: bowed:as_one` so it can expand *after* the real Accepting).

---

## 6. DRAFT seed — the thread_cards these events write
### additive, parse-clean, ON CONFLICT DO NOTHING; INTEGRATED LATER (not the live seed)

> These are the cards the four events surface (§§2–5 "advances a thread"). They reference
> only canon THREADS (`who/place/happened/surface/human`) and real `sites.yml` anchors
> (placeholder `altar_site` here = the Undercroft altar, `site_id: unbroken_light` in the
> live seed — swap at integration). `body_voice_key` points at the new `voice.ts` `gather`
> block (§7). No `revealed_by_solve` — these are **event-revealed**, written by the director
> when the gather flag flips, so they carry `card_kind: verified`.

```sql
-- DRAFT — design/content/gather-events.md §6. Integrate under build guards; NOT live yet.
begin;

insert into public.thread_cards
  (card_key, thread_key, title, body_voice_key, anchor_site_id, card_kind, references_card_key, revealed_by_solve, alt_text_condition, sort_order)
values
  ('gather-count-who',        'who',      'named in the same book',        'gather.thread.count_who',        'unbroken_light', 'verified', '{}', null, null, 40),
  ('gather-count-happened',   'happened', 'the count predates you',        'gather.thread.count_happened',   'unbroken_light', 'verified', '{}', null, null, 41),
  ('gather-unlight-surface',  'surface',  'the same hands, the last winter','gather.thread.unlight_surface', 'unbroken_light', 'verified', '{}', null, null, 42),
  ('gather-dob-human',        'human',    'a witness, not a monster',      'gather.thread.dob_human',        'unbroken_light', 'verified', '{}', null, 'kept:left_human', 43),
  ('gather-rehearsal-human',  'human',    'the shape of the choice',       'gather.thread.rehearsal_human',  'unbroken_light', 'verified', '{}', null, 'bowed:as_one', 44)
on conflict (card_key) do nothing;

commit;
```

---

## 7. PROPOSED voice.ts additions — the `gather` block
### the EXACT text, to be added to `voice.ts` at integration (never inline at a call site, INV-1)

> Two homes for these strings, per §0: the `#the-record` Discord lines extend the canon
> `voice` object; the in-world beat-payload lines (titles, action-bars, sign/book text)
> are read by the showrunner from this same `gather` map and pasted into the GroupBeat
> `beat_payload`. All are in the §B register (lowercase, sparse, no contractions, no
> capitals, no exclamation, no named feeling, no chosen one). Reused B3 lines are marked.

```ts
// voice.ts — ADD to the voice object (or a sibling `gather` export the showrunner reads).
// REGISTER: identical to the canon Watcher register. {scene_n} / {scene_n_plus_one} are
// filled by the director from SceneAwareness at enqueue time — the ONLY interpolations.
export const gather = {
  // -- EVENT 1: THE COUNT (#the-record) --
  'gather.count.begin':      'the count begins. it was begun before you, in the oldest winters, and it has not closed since.',            // reuse B3
  'gather.count.column':     'each name is read against its column. what was kept stands on one side. what was owed stands on the other. nothing is added now that was not done before now.', // reuse B3
  'gather.count.surplus':    'the count comes out one more than you are. it always has. the extra is not a stranger. it is the column you have not yet filled, standing where you will stand.', // NEW
  'gather.count.same_book':  'six were named in full, in the old book. you are read into the same book, by the same hand.',                // reuse B3 (condensed)
  'gather.count.kindness':   'you were not told the laws before you kept or broke them. you were observed. that was the kindness, and the whole of the kindness.', // reuse B3

  // in-world beat text (1.2 actionbar / 1.3 actionbar)
  'gather.count.begin_actionbar': 'the count begins. stand where you can be seen.',
  'gather.count.actionbar':  'counted: {scene_n_plus_one}.  present: {scene_n}.',

  // -- EVENT 2: THE UN-LIGHTING (#the-record) --
  'gather.unlight.taken_in':      'now the lights are taken in. one. and the next. the dark is let come near, the small distance it is owed.', // reuse B3
  'gather.unlight.not_punish':    'a light goes out. it is not a punishment. it is the dark, given its company. it was always given.',          // reuse B3
  'gather.unlight.down_the_line': 'the lamps go down the line, as they went down the line in the last winter, when the keepers put them out themselves and kept the ways alone.', // reuse B3
  'gather.unlight.one_left':      'there is one light left, and it is the kept light, and it is decided now whose hand it stays in.',           // reuse B3
  'gather.unlight.hold':          'stand still. it is decided by what was already done. it was always decided by what was already done.',       // reuse B3 (the holding)

  // in-world beat text (2.1 actionbar / 2.3 boss_bar)
  'gather.unlight.actionbar': 'the lights are taken in. one. and the next.',
  'gather.unlight.bossbar':   'one light is left. it is the kept light.',

  // -- EVENT 3: DOB (#the-record; cold register, party is breaking the ways) --
  'gather.dob.seen':       'one you brought down is standing in the dark now. he is not far. he is not coming closer. he does not have to.',
  'gather.dob.quiet':      'he was loud, on the way down. you remember the loudness. he is quiet now. quiet is what is left when the keeping is done.',
  'gather.dob.same_door':  'he kept his light to the end and asked only to wait by it. he is waiting still. you walked past the same door he waited at.',
  'gather.dob.human':      'ask whether he was made a monster. he was not. he was made a witness. that is the only thing the dark makes. it is the thing it is making of you.',

  // in-world beat text (3.3 action-bar)
  'gather.dob.actionbar':  'you knew that one. you knew his name.',

  // -- EVENT 4: THE ACCEPTING REHEARSAL (#the-record; never a verdict, never a chosen one) --
  'gather.rehearsal.shape':     'this is the shape of it. all of you, bent at once, in the one light. learn the shape now. the hour will not wait for the learning.',
  'gather.rehearsal.partial':   'you bend, and the deep answers a little, and stops. it is not the hour. a little is all that is owed for a rehearsal.',
  'gather.rehearsal.together':  'it is not one of you that bends. it is the gathering, or it is no one. the record keeps no chosen. it keeps the kept.',
  'gather.rehearsal.withdraw':  'the answer is drawn back now. keep what you learned of the shape. when the hour comes, bend together, and do not look to see who bent first.',

  // in-world beat text (4.1 actionbar)
  'gather.rehearsal.actionbar': 'bend, all of you, as one. this is not the hour.',

  // -- thread_card bodies (read into thread_cards.body_voice_key; §6) --
  'gather.thread.count_who':         'the six were named in this book, by name, against this same column. you were read into it tonight, by the same hand.',
  'gather.thread.count_happened':    'the counting was begun in the oldest winters and has not closed. it was here before the first of you found the mouth.',
  'gather.thread.unlight_surface':   'what is above with you now is what put these lamps out. the same hands. the last winter. they did not depart. they were kept.',
  'gather.thread.dob_human':         'the one in the dark was known to you by name. he was not made a monster. he was made a witness. the dark makes nothing else.',
  'gather.thread.rehearsal_human':   'the bow you rehearsed is the choice. kept, and let go up into the air. or accepted, and kept below, watching. it is not decided here. it is decided by what you have already done.',
} as const;
```

---

## SCHEMA

```yaml
file: design/content/gather-events.md
status: DRAFT                       # design only; integrated under build guards afterward
purpose: >
  Four GROUP gather-events as concrete, Minecraft-feasible sequences on the GroupBeat
  primitive — THE COUNT, THE UN-LIGHTING, DOB (the one you knew, changed), THE ACCEPTING
  REHEARSAL — each with trigger, GroupBeat payload sequence, Watcher voice lines (proposed
  voice keys), anti-spam guardrails, and the thread each advances.
grounded_in:
  - arc/WORLD-BIBLE.md                              # the count, the un-lighting, the taken/Watchers, the Accepting
  - arc/corpus/npc-and-watcher-voice.md             # SET B §B3 (THE COUNT / un-lighting / holding) reused verbatim; A4 = Dob
  - discord/src/forge/canon.ts                      # CUSTOM_KEYS / KEEPERS / THREADS (closed registries)
  - discord/supabase/seeds/puzzles_seed.sql         # the 24 nodes; the opaque accepting-crouch token (must not be reposted)
  - discord/supabase/migrations/0005_threads.sql    # thread_cards / npc_dialogue_state / punishment_state schemas
built_on_primitive:
  beat: group                                       # plugin/.../beats/lib/GroupBeat.java
  payload: { beat, scene, radius, min, beat_payload }
  scene: [site, target, all]
delegate_beats_used:                                # all verified against plugin/.../beats/lib/*
  - private_sound        # {sound|named_sound, volume, pitch, behind, offset}
  - private_particle     # {particle, count, spread, speed, height, near_player}
  - private_message      # {mode: actionbar|title, actionbar; title only for rare boundary breaks}
  - private_darkness     # {effect: DARKNESS|BLINDNESS, seconds<=15, amplifier<=2}  reversible
  - torch_gutter         # {radius, max_torches, relight_seconds, permanent:false}  reversible
  - named_mob            # {entity, fallback_entity:WARDEN, name, no_ai_drift, silent, invulnerable, despawn_seconds}  WARDEN-fallback, never ZOMBIE
  - boss_bar             # {title, color, style, progress, seconds<=60}             auto-removes
events:
  - id: 1
    name: THE COUNT
    hook: the roll-call counts one too many — the Watcher counts ITSELF among the Kept
    trigger: flags.summoned AND convened(altar,16,min>=3)
    fires_once_flag: count_done
    beats: [private_sound, private_message(actionbar:begin), private_message(actionbar:+1), private_sound(behind)]
    advances: [who, happened]
    guardrails: [min>=3, once_per_summons, deniable_transient_actionbar, sound+word_only_no_mob]
  - id: 2
    name: THE UN-LIGHTING
    hook: lamps go out one at a time, down the line; one kept light left
    trigger: flags.count_done (same night)
    fires_once_flag: unlight_done
    beats: [private_message(actionbar), "torch_gutter x N (per-player cascade, staggered, permanent:false)", boss_bar, private_darkness]
    advances: [surface]
    guardrails: [reversible_relight_90s, darkness_capped_4s, torch_only_context_gate, bounded_to_convened, min>=3_on_shared_pulse]
  - id: 3
    name: DOB
    hook: the one you descended with returns changed — group-safe sighting
    trigger: flags.dob_descended AND ways_being_broken(Attention high)
    fires_once_flag: dob_seen_tonight
    cooldown: long; once_per_night
    beats: [private_sound(behind), named_mob(WARDEN-fallback), private_message(actionbar)]
    fallback_rule: entity unspawnable -> WARDEN (MF-10); NEVER green zombie; no_ai_drift -> setAI(false), never setTarget
    advances: [human]
    guardrails: [min>=2_tight_radius, despawn_8s, out_of_LoS_spawn, conduct_gated_breaking_only, inert_silhouette_never_paths]
  - id: 4
    name: THE ACCEPTING REHEARSAL
    hook: a partial group-bow that previews the climax — and never resolves it
    trigger: flags.tokens_laid AND convened(altar,12,min>=3) AND NOT flags.rehearsed
    fires_once_flag: rehearsed
    beats: [private_message(actionbar), private_particle(thin SOUL_FIRE_FLAME), private_sound(withdraw)]
    advances: [human]
    HARD_GUARDRAIL: >
      MUST NOT post the opaque accepting-crouch token, MUST NOT set flags.bowed_as_one,
      MUST NOT enqueue the accepting-crouch unlock or any next_puzzle_key. Preview only.
    guardrails: [no_climax_resolution, once, min>=3, no_mob_no_darkness_no_block_change]
voice_keys_proposed:                                # added to voice.ts `gather` block at integration; B-register
  discord_record: [gather.count.*, gather.unlight.*, gather.dob.*, gather.rehearsal.*]
  in_world_beat_text: [gather.*.actionbar, gather.*.bossbar]
  thread_card_bodies: [gather.thread.count_who, gather.thread.count_happened, gather.thread.unlight_surface, gather.thread.dob_human, gather.thread.rehearsal_human]
canon_refs_used:
  threads: [who, place(unused), happened, surface, human]
  keepers_referenced: [iss(the line/grave, B3), sella(implied), the six(the count)]   # Dob is a Set-A NPC, not a keeper
  custom_keys_referenced: [the_kept_light, the_deep_line, the_dark_hours, the_offering, the_bow, the_unspoken, the_sacred_beast]   # via reused B3 context; no unprefixed forms
deslop_compliance:
  banned_absent: ["a testament to", "little did they know", "the air was thick", named_emotions, three_adjective_lists, "not just X but Y", melodrama, tidy_bows]
  technique: concrete_mundane_detail + omission(iceberg) + reuse_of_corpus_B3_voice
integration_notes:
  - voice keys -> voice.ts `gather` export (showrunner reads in-world text from it; bot reads record text)
  - thread_cards DRAFT (§6) -> live seed under build guards; swap altar_site -> unbroken_light
  - events are director/showrunner-staged on existing flags; they add only gather-local flags (count_done, unlight_done, dob_seen_tonight, rehearsed); they GATE NOTHING on the spine
  - {scene_n}/{scene_n_plus_one} filled from SceneAwareness at enqueue time — the +1 IS event 1's payload
```
