# THE OBSERVANCE — GO-LIVE FOR TONIGHT'S FIRST TEST

> **Goal of tonight:** prove the **core loop on a real server** (BUILD-PLAN Phase A) — the smallest
> genuinely-real slice: *ignition → a gated cipher → solve it (with a hint) at an in-world sign → the
> gate opens → a beat/scare fires.* NOT the full game. The big subsystems (Observer voice, the companion
> arc, the co-op vault, the full generated world, the record website) are built/in-progress but need
> this loop proven first + iteration to verify — they are explicitly **not** in tonight's test.
>
> **State as of 2026-07-01:** engine (TS) all-green; plugin **compiles + jars** (`plugin/build/libs/
> observance-0.2.1.jar`, 331 KB); lore reconciled; canon docs + the zero-manual `placeroom` command
> in progress. What's below is what YOU do (the infra Claude can't reach).

---

## 0. PREREQS (one-time)
- A **Paper 1.21.11** server (verified — the plugin compiles clean against 1.21.11; **1.21.4 also
  works**). Everyone's client must match the server's exact version.
- Java 21 (Paper 1.21 needs it).
- 3–4 friends for the playtest.
- (For tonight's slice only — **skip** Citizens2 / Multiverse / the fog datapack; those are for later phases.)

## 1. DATABASE (Supabase) — apply in this order
Everything here is **additive**; nothing already applied changes. Run as the **service role** (these are
spoiler tables behind RLS).
1. **Apply migration `0006_requires_flags.sql`** (`discord/supabase/migrations/`). This is the keystone —
   it adds `puzzles.requires_flags` + the flag-merge RPC. Additive + idempotent (safe to re-run). *Without
   it the back half stays dark.*
2. **Re-run the seeds** (idempotent `ON CONFLICT` upserts — safe over existing data). At minimum, in order:
   `puzzles_seed.sql` → `seventh_seed.sql` → `progression_seed.sql` → `thread_cards.sql` →
   `thread_tags.sql` → `side_quests.sql` → `hints_seed.sql` → `metapuzzle_seed.sql`.
   (Re-running all of them is fine; they don't duplicate.)
3. *(Not needed tonight — `0007_answer_kind` is for the diverse-answer types, a later phase.)*

## 2. PLUGIN
1. The jar is already built: **`plugin/build/libs/observance-0.2.1.jar`**. (To rebuild:
   `cd plugin && "D:/_gradle/gradle-8.10.2/bin/gradle" --offline jar`.)
2. Drop it in the server's `plugins/` folder.
3. Point the plugin at your DB: set the env var **`OBSERVANCE_SUPABASE_KEY`** = your Supabase
   **service-role key** on the server. (The project URL is already in `config.yml` →
   `fdnmhbpxnodrnbrzrlqq.supabase.co`.) **Never** put the key in the committed config — env var only.
   The plugin degrades silently if it can't reach the DB, so if nothing happens, check this first.
4. Start the server; confirm the plugin loads with no errors in the console.

## 3. BUILD ONE TEST SITE (two options — use whichever is ready)
- **Option A (zero-manual, preferred — READY):** stand where you want the room and run
  **`/observance placeroom <keeperId>`** (admin, e.g. `/observance placeroom brann`). It builds a
  keeper-stone + an editable answer-sign and registers that exact spot as a live answer site immediately.
  **Caveat:** the site is runtime-only — it does NOT survive a server restart or `/observance reload`, so
  place it *after* your final restart and just re-run the command if you reload. (sites.yml write-back is
  a later nicety.)
- **Option B (manual fallback):** pick a spot, note its X Y Z; set those coords on one `keeper_stone`
  (e.g. `stone_brann`) in `plugin/src/main/resources/sites.yml`, rebuild/reload; place a sign there.
  Only if you'd rather not use the command.

## 4. IGNITE THE ARC (stopgap)
- Use the admin command **`/observance flag prologue_ignited true`** (the acknowledged stopgap — the
  in-world `IgnitionListener` is a later nicety). This starts the arc so the first puzzle is live.

## 5. RUN THE LOOP (the actual test)
1. With friends on, find the first live cipher (the seeds provide it + a tiered hint via the hint rail).
2. **Type the answer on the sign** at your test site. The `AnswerSignListener` normalizes + matches it
   against open puzzles (identical logic to the proven Discord path).
3. On a correct solve: the gate flips, the solve is recorded, and the authored **reward beat** enqueues +
   fires on the next poll. **That fire — the world reacting — is the thing to watch.**
4. Watch for: did they find it (or dead air)? did the hint help? did the beat land? Capture it — that
   feedback is BUILD-PLAN §14 Playtest 1.

## 6. IF SOMETHING'S OFF
- Nothing happens on solve → check the plugin reached the DB (step 2.3) and that `0006` applied (step 1.1).
- Answer never matches → confirm the seeds re-ran (step 1.2) and the sign is inside the site radius.
- Beat never fires → check the beat-queue poller is running (console) + the reward row seeded.

---

**Report back what happened** — findability, whether a scare landed, any friction. That's the input that
turns "the loop runs" into "it's actually good." Everything past this loop (companion, Observer voice,
co-op, the record website, the full world) builds on a proven Phase A.
