# THE ORACLE — the closed clue loop

The Oracle is the part of The Observance that **answers**. A player solves a forged
clue, submits the plaintext (in Discord `#the-record` OR on an in-world answer-sign),
and the world responds — or, in the watcher's register, stays silent.

This spec is the contract the **bot agent** and the **plugin agent** both build to.
It defines: the normalization algorithm (byte-for-byte identical on both surfaces),
the five outcome types, how an outcome enqueues an in-world beat, and how the
plugin's in-world sign verb shares the same `puzzles` table.

Schema: `discord/supabase/migrations/0004_oracle.sql`. Tables: `puzzles`, `solves`,
`answer_attempts` (+ an additive reconciliation of `beat_queue`).

---

## 1. The loop (verify it closes)

```
forgeClue(spec) ──► puzzles row (puzzle_key, accepted_answers[], outcome_type, outcome_payload)
       │                                   ▲
       ▼                                   │  authored once, active = true
   carved runes (PNG)  ──player solves──►  plaintext answer
                                           │
        ┌──────────────────────────────────┴───────────────────────────────┐
        │                                                                    │
  DISCORD surface                                                     WORLD surface
  #the-record message scan                                           answer-sign at a site
  (bot, MessageContent)                                              (plugin, sign listener)
        │                                                                    │
        └──────────────► resolveAnswer(player, raw, surface) ◄───────────────┘
                                   │
                 normalize → rate-limit → match OPEN puzzles
                                   │ matched?
                         no ───────┴─────── yes
                          │                  │
                  log attempt          INSERT solves ON CONFLICT DO NOTHING (replay guard)
                  STAY SILENT           │ genuinely new?
                  (no error,       no ──┴── yes
                   no "closeness")  already solved   apply outcome_type:
                                    → silent          speak voice line; maybe enqueue beat
                                                      (status 'approved' → fires immediately)
```

The loop **closes** because both surfaces call the same resolver against the same
`puzzles` table, and a match on either surface produces the same outcome. A solved
clue is never a dead UI: matched → the watcher speaks; matched + has a beat → the
world changes.

---

## 2. Normalization — the EXACT algorithm (identical on both surfaces)

Both the bot (TypeScript) and the plugin (Java) MUST normalize the player's raw
input AND store `accepted_answers` already-normalized, with this exact algorithm.
Drift here breaks the loop silently.

```
normalize(s):
  1. Unicode NFKC normalize.                          (TS: s.normalize('NFKC'); Java: Normalizer.normalize(s, NFKC))
  2. case-fold to lower.                              (TS: .toLowerCase();      Java: .toLowerCase(Locale.ROOT))
  3. replace every run of chars NOT in [a-z0-9 ] with a SINGLE SPACE.  (regex replace /[^a-z0-9 ]+/g → " ")
       - non-alnum → space (NOT empty): so "bow,at" and "bow at" both → "bow at".
       - this removes punctuation, accents-left-over, runes, emoji, symbols — but as word breaks.
       - it KEEPS letters, digits, and spaces only.
  4. collapse internal whitespace runs to ONE space.  (replace /\s+/g → " ")
  5. trim leading/trailing space.
  result: the normalized form.
```

> **CANONICAL — do not drift.** Step 3 maps non-alphanumerics to a **space**, never
> to empty. (An earlier draft of this line said "→ \"\"" — that was wrong; the space
> form is canon and is what both implementations ship.) This is verified byte-for-byte
> identical across the TS `normalizeAnswer` (`oracle/resolve.ts`) and the Java
> `AnswerNormalizer.normalize` (`oracle/AnswerNormalizer.java`) on a battery of tricky
> inputs — Greek (`ΣΊΣΥΦΟΣ → ""`), Turkish dotted-İ (`İstanbul → "i stanbul"`),
> ligatures (`ﬁre → "fire"`), fullwidth (`ＡＢＣ１２３ → "abc123"`), fractions
> (`½ → "1 2"`), Roman numerals (`Ⅻ → "xii"`), zero-width space, tabs, and emoji.
> Both surfaces also CAP raw input length before normalizing (Discord `MAX_RAW_LEN`,
> world sign ≤ 4 short lines) so a giant paste cannot bloat `answer_attempts.raw` or
> the regex.

Reference (TypeScript):
```ts
export function normalizeAnswer(s: string): string {
  return s
    .normalize('NFKC')
    .toLowerCase()
    .replace(/[^a-z0-9 ]+/g, ' ')   // non-alnum → space (so "BOW,AT" → "bow at")
    .replace(/\s+/g, ' ')
    .trim();
}
```
> Note: step 3 maps non-alphanumerics to a **space**, not empty — so `"bow,at"` and
> `"bow at"` both normalize to `"bow at"`, while `"-1280, 64"` → `"1280 64"`
> (the minus sign is dropped; author coordinate answers without sign, or include
> both signed/unsigned forms in `accepted_answers`).

Reference (Java, plugin):
```java
static String normalizeAnswer(String s) {
  if (s == null) return "";
  String n = java.text.Normalizer.normalize(s, java.text.Normalizer.Form.NFKC)
      .toLowerCase(java.util.Locale.ROOT)
      .replaceAll("[^a-z0-9 ]+", " ")
      .replaceAll("\\s+", " ")
      .trim();
  return n;
}
```

**Gibberish guard.** Normalization is deliberately NOT loose enough to let noise
match: matching is exact set-membership of the *whole* normalized string against
`accepted_answers`, not substring/fuzzy. An empty normalized result (`""`) never
matches and is never even logged as an attempt (it is not "plausibly an answer").

**Matching.** `matched = puzzle.accepted_answers.includes(normalize(raw))` for some
OPEN puzzle. First open puzzle whose array contains the normalized string wins. The
resolver NEVER reports which puzzle/part matched on a miss.

---

## 3. Outcome types — what a correct answer DOES

`puzzles.outcome_type` ∈ `{ next_clue, lore, dead_end, side_quest, main_beat }`.
All five are wired through `outcome_payload` (jsonb). On a genuinely-new solve the
resolver: (a) speaks the voice line named by `outcome_payload.voice_key`,
(b) optionally sets `arc_state.flags` from `set_flags`, (c) optionally enqueues the
`beat`. The difference between types is **semantic** (does it open a door?) and is
realized purely by what authors put in the payload:

| outcome_type | opens a door? | typical payload | voice register |
|---|---|---|---|
| `next_clue`  | YES — advances the web | `next_puzzle_key`, maybe `beat` (reveal the next carving) | "the way goes on. …" |
| `lore`       | NO — reveals story only | `voice_key` (a fragment of character/world), NO `next_puzzle_key` | a told secret; nothing unlocks |
| `dead_end`   | **NO — true but not a door** | `voice_key` ONLY; no `next_puzzle_key`, no `beat` | acknowledges it is *right*, leads nowhere |
| `side_quest` | YES — optional branch | `next_puzzle_key` (off-spine), maybe `beat` | "this is not the way. but it is a way." |
| `main_beat`  | YES — main-story event | `beat` (an `unlock`/named main beat), `set_flags`, maybe `next_puzzle_key` | a turn in the arc |

### dead_end — the load-bearing one
A `dead_end` is a **TRUE answer that is deliberately not a door**. The player solved
a real clue; the watcher *acknowledges it in voice* — it does NOT pretend the answer
was wrong, and it does NOT advance. It is red-herring texture for a HARD web: some
correct solutions simply lead nowhere, and the watcher's calm acknowledgement is the
reward. A `dead_end` row therefore has `voice_key` only — **no `next_puzzle_key`,
no `beat`**. It is still recorded in `solves` (so it can't be farmed for repeated
acknowledgement) and still fires its voice line exactly once.

> Contrast with a genuine **miss** (no open puzzle matched): the watcher is *silent*.
> A `dead_end` is heard; a miss is not. Never collapse the two.

### Wrong answers / misses / caps — always silence, never a tell
- No open puzzle matched → log the attempt (`matched=false`), say **nothing**.
- Already solved (replay) → say nothing (the conflict insert short-circuits).
- Rate-limited or per-puzzle `max_attempts` reached → in-voice withholding
  (`voice.oracleWithheld()` / silence), **never** "wrong", "close", or an error.
- A wrong answer must NEVER reveal which part was right or how close it was.

---

## 4. How an outcome enqueues an in-world beat

When `outcome_payload.beat` is present AND the solve is genuinely new, the resolver
inserts a `beat_queue` row. **Player-earned unlocks fire immediately**, so the oracle
inserts them with **`status = 'approved'`** (NOT `'pending'`). The future showrunner's
AUTO↔CONFIRM toggle gates *authored/AI* beats; a reward a player just earned must
never wait on a human. (The CONFIRM path uses `'pending'`; reserve it for the
showrunner, not the oracle.)

### beat_queue row shape (what the resolver inserts)
The plugin reads `beat_queue` via `mc_uuid` / `site_id` / `priority` / `payload`
(see `RealBeatEnactor.buildRequest`). The oracle inserts:

```jsonc
{
  "type":     "unlock",                 // beat kind; "unlock" → UnlockBeat dispatcher
  "mc_uuid":  "<the solving player's mc_uuid>",  // resolve "{solver}" placeholder → solver.mc_uuid
  "site_id":  "well_shrine",            // optional sites.yml id (world/ambient beats omit mc_uuid)
  "priority": 10,                       // optional ordering hint
  "payload":  {                         // jsonb; the UnlockBeat payload
    "step":         "door_open",        // names ANOTHER beat type the unlock delegates to
    "step_payload": { "radius": 3, "open": true }
  },
  "status":   "approved"                // ← player reward: fires on next poll, no human gate
}
```

- `type: "unlock"` routes to `beats/lib/UnlockBeat.java`, the DIRECTED dispatcher
  that delegates to `payload.step` (`door_open`, `grant_advancement`, `reveal`, …)
  with `payload.step_payload`. This composes the existing beat palette — the oracle
  introduces no new beat type; it is the **producer** that makes `UnlockBeat` (until
  now dead code) fire.
- The `{solver}` placeholder in an authored `outcome_payload.beat.mc_uuid` is
  replaced by the resolving player's real `mc_uuid` at enqueue time, so one authored
  row rewards whoever solves it.
- For a `main_beat`, `type` may instead be a named main beat the plugin knows; the
  same row shape and `status:'approved'` apply.

### Plugin payload typing — RESOLVED (was load-bearing)
`beat_queue.payload` is `jsonb`, so PostgREST returns it as a JSON **object**, not a
string. The plugin's `BeatQueueRow.payload` is now typed `com.google.gson.JsonElement`
(NOT `String`) and `RealBeatEnactor.buildRequest` feeds it through
`BeatPayload.of(row.payloadObject())`. Before this fix the `String` field made Gson
throw "Expected a string but was BEGIN_OBJECT" on every non-empty payload, which the
read path turned into a parse error that dropped the **entire** beat batch — silently
breaking even the existing `whisper_toll` beats. The write side mirrors it:
`BeatQueueInsertRow.payload` is also `JsonElement`, so the oracle writes a real jsonb
object, not a quoted string. No further action — this is closed, not a pending flag.

---

## 5. The in-world answer-sign shares the SAME puzzles table

The plugin's answer-sign verb (a sign-edit / interact listener at a `sites.yml`
answer-site, following `CustomComplianceListener`'s Safety-wrapped, main-thread,
RateLimiter, MONITOR-priority pattern) is the world-surface entry to the **same**
resolver path:

1. Player writes/edits an answer-sign at a configured answer-site (or runs the
   in-world answer verb). The listener reads the line text on the main thread.
2. Hop **async**, then `normalizeAnswer(line)` with the §2 algorithm.
3. `SELECT puzzle_key, accepted_answers, outcome_type, outcome_payload
   FROM puzzles WHERE active = true` and match by set-membership — the **same query
   and same matching** the bot uses. (Add `SupabaseClient.fetchOpenPuzzles()` mirroring
   `fetchActionableBeats`.)
4. Rate-limit by `mc_uuid` via `answer_attempts` (+ the plugin `RateLimiter`); log
   every attempt (`surface='world'`).
5. On a genuinely-new match (INSERT `solves` ON CONFLICT (puzzle_key, player_id) DO
   NOTHING), apply the outcome: enqueue the `beat` row directly (status `'approved'`)
   — the plugin is already the enactor, so a world-surface solve can enqueue and the
   next poll enacts it. The voice reply on the world surface is the in-world beat
   itself (a particle/sound/structure), not a Discord line; if a Discord echo is
   wanted, enqueue a `report`/whisper-style beat too.

Because both surfaces key on `players` (`mc_uuid` in-world, `discord_id` in Discord)
and both write `solves` with the same `unique(puzzle_key, player_id)` guard, a clue
solved in-world cannot be re-solved for reward in Discord and vice-versa. One puzzle,
one solve, one reward — across both surfaces.

---

## 6. Anti-abuse checklist (both surfaces enforce)

- **Cooldown + token bucket** per player (linked: `player_id`; unlinked Discord:
  `discord_id`; world: `mc_uuid`), windowed over `answer_attempts.at`. Tune so a
  short answer can't be brute-forced before the bucket empties.
- **Per-puzzle `max_attempts`** (nullable) caps tries on one puzzle; reaching it =
  in-voice withholding, never a hint.
- **Normalize but don't over-loosen** (§2): whole-string exact match only; empty
  normalized result never matches and is not logged.
- **Log every attempt** (`answer_attempts`, matched bool only — never which part).
- **Idempotent reward** via `solves` unique + ON CONFLICT DO NOTHING *before*
  enqueue.
- **Wrong = silence**, never an error, never a closeness tell.
- **Fault isolation**: bot handler in try/catch (never crash the process) → on
  throw, `voice.quiet()`; plugin listener Safety-wrapped (never crash the server);
  every Supabase call async + graceful on outage (silent, never an error at players).
- **#the-record scan discipline**: ignore the bot's own messages; only react in
  `config.channels.theRecord`; only treat a message as an attempt when it plausibly
  is one (non-empty normalized AND it matches an open puzzle) — otherwise stay
  silent, never spam-reply ordinary chat.

---

## 7. Voice lines to add (the bot agent adds these to `voice.ts`)

New lines, same register (lowercase, sparse, certain — never "error"/"wrong"/"close"):
- `oracleNextClue(...)` — the way goes on.
- `oracleLore(...)` — a told fragment of story.
- `oracleDeadEnd(...)` — acknowledges a TRUE answer that opens nothing.
- `oracleSideQuest(...)` — "this is not the way. but it is a way."
- `oracleMainBeat(...)` — a turn in the arc.
- `oracleWithheld()` — rate-limit/cap reached; withholds, does not refuse.

`outcome_payload.voice_key` names which of these speaks; `voice_args` are spread in.
Authors never write English into payloads — only a `voice_key` + structured args.

---

## 8. Adversarial hardening (the reliability pass) — single source of truth

This section is the verified, post-hardening state. If any of these claims stops being
true, the loop is broken — re-audit before shipping.

### Rate limits (per surface — both REAL, intentionally not identical)
| surface | first gate (in-memory) | durable gate (`answer_attempts`) | per-puzzle |
|---|---|---|---|
| Discord (`resolve.ts`) | — | token bucket **8 / 60 s**, keyed `player_id` else `discord_id` | `puzzles.max_attempts`, same 60 s window, `>=` cap |
| World (`OracleResolver`) | cooldown **5 s** + token bucket **3 / 30 s** per `mc_uuid` | durable ceiling `burst*4` over the bucket window (fail-open on DB error) | `puzzles.max_attempts`, scoped to the puzzle + window, `>=` cap |

Both fail **open** (a DB hiccup never locks a real keeper out) and both withhold with
the SAME neutral line (`oracleWithheld`) / silence — never "wrong", never "too many",
never a closeness tell. The shortest sane answer space (a 4-letter word / 2-number
coord) has thousands of candidates, so these caps make brute force take days while a
real solver (1–2 tries) never feels them.

### Exploits checked and closed
- **Brute-force a 1–3 char answer** → blocked by the token bucket on both surfaces;
  the durable `answer_attempts` window backs it across restarts.
- **Spam `#the-record`** → ordinary chat normalizes to a non-match → logged
  `matched=false`, **silent**. The scan never spam-replies and (being a real answer
  every message) is still rate-limited by the SAME bucket, so a flood self-throttles.
- **Submit the same correct answer 10×** → first solve inserts `solves`
  `unique(puzzle_key, player_id)`; every replay is `silent('already-solved')`, fires
  NOTHING. True across BOTH surfaces (one table, one key) — a Discord solve cannot be
  re-claimed in-world and vice-versa.
- **Answer-sign spam** → 3 s per-player+site cooldown + the resolver's own limiter;
  the sign is **blanked** on edit so a guess never persists; a solved puzzle can't
  re-enqueue a beat (solve guard).
- **Unicode / emoji / huge input** → NFKC + `[^a-z0-9 ]→space` makes emoji/runes
  word-breaks; raw is length-capped before normalize/log so a giant paste can't bloat
  `answer_attempts.raw`.
- **Answers to inactive / nonexistent puzzles** → only `active = true` rows are ever
  matched; an inactive puzzle is indistinguishable from a miss (silence).
- **A player answering a puzzle they "haven't reached"** → **ALLOWED, by design.** The
  web is non-linear and the resolver does NOT gate on `movement`; if you can produce a
  correct plaintext you earn the outcome. There is no "reached" state to exploit, so
  there is no out-of-order exploit. (Spoiler answers leaking is a content/`active`
  concern, handled by authors flipping `active`, not by the resolver.)
- **The bot replying to its own messages (loop)** → the scan ignores `author.bot`
  (the bot is a bot), plus `webhookId` and `system` — no self-trigger is possible.

### dead_end UX (the load-bearing subtlety)
A `dead_end` is a TRUE answer that opens nothing. It speaks `oracleDeadEnd()` —
*"yes. that is the true name of it. and it opens nothing. some things are only true."*
— which **acknowledges the answer is right** and advances nothing. It records a
`solves` row (so it fires its line exactly once, can't be farmed) but enqueues no beat
and surfaces no `next_puzzle_key`. It is NEVER an error and NEVER reads as "wrong":
a wrong answer is pure **silence**; a dead_end is **heard**. The `/answer` solved
branch speaks the single resolved line verbatim (no appended next-clue nudge that
could contradict a dead_end / side_quest / main_beat).

### Idempotency & no-double-fire (verified end-to-end)
`recordSolve` (Discord) / `insertSolveIfNew` (world) insert with ON-CONFLICT /
`resolution=ignore-duplicates` **before** any reward; only a genuinely-new row
proceeds. A lost race returns `silent` / `ALREADY_SOLVED`. On the firing side,
`BeatQueuePoller` guards double-enact with an in-process `inFlight` set keyed on beat
id, and the durable `status='fired'` PATCH is the cross-restart guard (re-queued on
failure). Player-earned unlocks are enqueued `status='approved'` so they fire on the
next poll with **no human gate**; the showrunner's CONFIRM path keeps `'pending'` for
its OWN authored/AI beats — the two never collide.

### Manual test script (run on the live stack after applying 0004 + seeding puzzles)
Seed at least: one `next_clue` puzzle P1 (accepted `"bow at"`, `max_attempts: 5`), one
`dead_end` puzzle P2 (accepted `"the old name"`), both `active = true`, with a linked
keeper K.

1. **Right answer** — K runs `/answer bow at` (or types `bow,at` in `#the-record`).
   Expect: the watcher speaks `oracleNextClue` publicly; a `solves` row for (P1, K);
   if P1 carried a `beat`, the unlock fires in-world on the next poll.
2. **Wrong answer** — K runs `/answer not even close`. Expect: **silence** in the open
   (ephemeral `oracleWithheld` on the slash so the interaction is acknowledged); an
   `answer_attempts` row `matched=false`; no `solves` row; no closeness tell.
3. **Dead-end answer** — K runs `/answer the old name`. Expect: `oracleDeadEnd`
   ("…it opens nothing…"); a `solves` row for (P2, K); NO beat, NO next clue.
4. **Replay** — K runs `/answer bow at` again. Expect: **silence** (ephemeral
   withhold); NO second `solves` row; NO second unlock. Repeat in `#the-record` — still
   silent. Repeat in-world on the sign — still nothing fires.
5. **Spam** — K submits 10 wrong answers in under a minute. Expect: after the bucket
   empties (8/60 s Discord; 3/30 s world) the reply is `oracleWithheld` / silence,
   never a hint or a count; ordinary `#the-record` chatter in between draws no reply.
6. **Sign answer** — K edits an `answer_sign` at a configured site with `bow at` for a
   still-unsolved puzzle. Expect: the sign blanks; on a genuinely-new solve the beat
   fires in-world; a `solves` row appears; submitting again does nothing (cross-surface
   replay guard). Editing it as a random non-answer → blanks, stays silent.
